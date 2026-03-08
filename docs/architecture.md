# Architecture

## Maqsad

Telegram kanallardan ish e'lonlarini olish, ularni strukturali `Job` modeliga normallashtirish va Kafka `jobs.raw` topiciga publish qilish.

## Asosiy oqim

```text
Telegram Channels
  -> Telethon Listener
  -> Parser Plugin System
  -> Job normalization
  -> Kafka Producer
  -> jobs.raw
```

## Komponentlar

| Komponent | Fayl | Vazifa |
| --- | --- | --- |
| Bootstrap | `job_aggregator/app.py` | Settinglarni yuklaydi, loglarni sozlaydi, parserlarni discover qiladi, Telegram client va Kafka producer start/stop qiladi. |
| Settings | `job_aggregator/config.py` | `.env` va environment variablelardan runtime config yig'adi. |
| Base parser | `job_aggregator/core/parser_base.py` | Parser kontrakti, umumiy `build_job()` helperlari va `ParserConfig` modelini beradi. |
| Plugin loader | `job_aggregator/core/plugin_loader.py` | `parsers/*` ichidan plugin folderlarni skan qiladi, `config.yaml`ni o'qiydi, `Parser` klassini dinamik yuklaydi. |
| Telegram client | `job_aggregator/telegram_client/client.py` | Telethon orqali kanallarga ulanadi, channel -> parser routing qiladi, parse xatolarini izolyatsiya qiladi. |
| Kafka producer | `job_aggregator/kafka/producer.py` | Async queue va workerlar orqali `Job` obyektlarini Kafka'ga JSON qilib yuboradi. |
| Job model | `job_aggregator/models/job.py` | Kafka payload uchun normallashtirilgan schema. |
| Channel parser | `job_aggregator/parsers/<plugin>/parser.py` | Kanalga xos parsing logikasi. |

## Message flow

1. `app.py` parser pluginlarni `PluginLoader` orqali yuklaydi.
2. Har bir plugin `channel` qiymati bilan Telegram clientga registratsiya qilinadi.
3. `TelegramJobClient` Telethon session bilan Telegram'ga ulanadi.
4. Yangi message kelganda client channel bo'yicha parserni topadi.
5. Parser `parse(message) -> Job | None` qaytaradi.
6. `Job` qaytsa, u `KafkaJobProducer.publish()` orqali ichki queue'ga tushadi.
7. Kafka worker payloadni `jobs.raw` topiciga yuboradi.

## Parser plugin kontrakti

Har bir yangi kanal parseri alohida papkada yashaydi:

```text
job_aggregator/parsers/
  my_channel/
    parser.py
    config.yaml
```

`config.yaml` minimum tarkibi:

```yaml
channel: my_channel_username
source: my_channel
enabled: true
```

`parser.py` minimum kontrakti:

```python
from job_aggregator.core.parser_base import BaseParser

class Parser(BaseParser):
    def parse(self, message):
        ...
```

Muhim qoida: yangi channel qo'shish uchun core kodga tegilmaydi. Faqat yangi parser folder qo'shiladi.

## Xatoliklarni boshqarish

- Plugin load xatolari log qilinadi va nosoz plugin skip qilinadi.
- Parser ichidagi exception butun servisni yiqitmaydi; faqat o'sha message skip qilinadi.
- Kafka publish xatolari log qilinadi; worker loop ishlashda davom etadi.
- Invalid yoki job bo'lmagan message `None` bo'lib skip qilinadi.

## Scale uchun hozirgi tayyorgarlik

- `parser_concurrency` orqali bir nechta message parallel parse qilinadi.
- Kafka publisher ichida `kafka_worker_count` va `kafka_queue_size` bilan backpressure mavjud.
- Channel nomlari hardcode qilinmagan; plugin discovery 100+ channel uchun mos.
- Parserlar modulga ajratilgan, shuning uchun channel-specific logika core servisdan tashqarida qoladi.

## Hozircha mavjud cheklovlar

- Hozircha bitta process va bitta Telethon session ishlaydi.
- Metrics, healthcheck, DLQ va retry policy hali alohida modul sifatida yozilmagan.
- Deduplication yo'q; bir xil message qayta o'qilsa publish qayta bo'lishi mumkin.
