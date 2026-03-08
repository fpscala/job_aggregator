from __future__ import annotations

import argparse
import asyncio
import json
from pathlib import Path
import sys

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from telethon import TelegramClient

from job_aggregator.config import load_dotenv, load_settings


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export Telegram channel history to JSONL")
    parser.add_argument("--channel", required=True, help="Telegram channel username, e.g. Xorazm_ish")
    parser.add_argument("--limit", type=int, default=1000, help="How many latest posts to export")
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output JSONL path. Defaults to data/exports/<channel>.jsonl",
    )
    return parser.parse_args()


async def main() -> None:
    args = parse_args()
    load_dotenv(PROJECT_ROOT / ".env")
    settings = load_settings()

    output_path = args.output or PROJECT_ROOT / "data" / "exports" / f"{args.channel.lower()}.jsonl"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    client = TelegramClient(
        settings.telegram_session,
        settings.telegram_api_id,
        settings.telegram_api_hash,
    )
    await client.connect()
    if not await client.is_user_authorized():
        raise RuntimeError("Telegram session is not authorized. Run scripts/create_telegram_session.py first.")

    entity = await client.get_entity(args.channel)
    exported = 0
    with output_path.open("w", encoding="utf-8") as handle:
        async for message in client.iter_messages(entity, limit=args.limit):
            payload = {
                "id": message.id,
                "date": message.date.isoformat() if message.date else None,
                "text": (message.raw_text or "").strip(),
                "views": getattr(message, "views", None),
                "forwards": getattr(message, "forwards", None),
                "channel": args.channel,
            }
            handle.write(json.dumps(payload, ensure_ascii=False) + "\n")
            exported += 1

    await client.disconnect()
    print(f"Exported {exported} posts to {output_path}")


if __name__ == "__main__":
    asyncio.run(main())
