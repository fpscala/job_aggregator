# TODO

Snapshot sana: `2026-03-07`

## P0 - Productionga yaqinlashtirish

- [ ] Live listener uchun process supervision qo'shish (`systemd`, `supervisord` yoki container restart policy).
- [ ] Runtime'ni to'liq `Python 3.11`ga ko'chirib, shu muhitda end-to-end test o'tkazish.
- [ ] Real-time Kafka publish uchun smoke-test runbook yozish: yangi Telegram post -> Kafka event tasdig'i.
- [ ] Sensitive fayllar (`.env`, `job_aggregator.session`) bo'yicha aniq deployment qoidalarini yozish.

## P1 - Reliability

- [ ] Kafka publish xatolari uchun DLQ topic qo'shish.
- [ ] Structured metrics qo'shish: parsed count, skipped count, publish error count, parser latency.
- [ ] Healthcheck endpoint yoki kamida heartbeat logika qo'shish.
- [ ] Graceful shutdown paytida queue holati bo'yicha aniq loglar qo'shish.
- [ ] Duplicate publish ehtimoli uchun deduplication strategiyasini belgilash.

## P1 - Parser quality

- [ ] `Xorazm_ish` unparsed failure sample'larini kategoriyalab qo'shimcha patternlar yozish.
- [ ] `ish_kerak` tipidagi job-seeker postlarni alohida reject qoidalari bilan mustahkamlash.
- [ ] Salary normalization qo'shish: valyuta, interval, fixed vs bonus ajratish.
- [ ] Location normalization qo'shish: shahar, tuman, mo'ljalni alohida struktura sifatida saqlash.
- [ ] Real postlardan ko'proq fixture yig'ib parser regression testlar sonini oshirish.

## P1 - Multi-channel onboarding

- [ ] Keyingi maqsad kanallar ro'yxatini shakllantirish.
- [ ] Har bir yangi kanal uchun `export -> analyze -> tune -> live verify` standart playbook qo'llash.
- [ ] Parser plugin template yaratish, yangi parser boshlash vaqtini qisqartirish.

## P2 - Platform engineering

- [ ] Application uchun Dockerfile yozish.
- [ ] `docker compose` ichiga app service'ni ham qo'shish.
- [ ] CI pipeline qo'shish: unit test, import check, packaging check.
- [ ] Config validatsiyasi va startup diagnostics'ni kuchaytirish.
- [ ] Log formatini JSON qilish va markaziy log yig'ishga moslashtirish.

## P2 - Data model evolution

- [ ] `Job` modeliga `employment_type`, `contact`, `language`, `tags` kabi maydonlar qo'shishni baholash.
- [ ] Raw telegram metadata uchun alohida envelope yoki audit schema kiritish.
- [ ] `jobs.raw` va keyingi normalizatsiya qatlamlari uchun schema versioning kiritish.
