from __future__ import annotations

import asyncio
import getpass
import os
from pathlib import Path
import sys

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from telethon import TelegramClient
from telethon import errors as telethon_errors

from job_aggregator.config import load_dotenv, load_settings


async def main() -> None:
    load_dotenv(PROJECT_ROOT / ".env")
    settings = load_settings()
    login_method = os.getenv("TELEGRAM_LOGIN_METHOD", "code").strip().lower() or "code"
    phone = _get_phone() if login_method == "code" else None
    force_sms = _env_flag("TELEGRAM_FORCE_SMS")

    client = TelegramClient(
        settings.telegram_session,
        settings.telegram_api_id,
        settings.telegram_api_hash,
    )

    await client.connect()
    if await client.is_user_authorized():
        me = await client.get_me()
        display_name = " ".join(part for part in [getattr(me, "first_name", None), getattr(me, "last_name", None)] if part)
        print(f"Telegram session already authorized: {settings.telegram_session}")
        print(f"Authorized as: {display_name or getattr(me, 'username', 'unknown')}")
        await client.disconnect()
        return

    if login_method == "qr":
        qr_login = await client.qr_login()
        image_path = _qr_image_path()
        _render_qr_image(qr_login.url, image_path)
        print(f"QR login URL: {qr_login.url}")
        print(f"QR image saved to: {image_path}")
        print(f"QR expires at: {qr_login.expires.isoformat()}")
        try:
            await qr_login.wait()
        except telethon_errors.SessionPasswordNeededError:
            password = getpass.getpass("Please enter your 2FA password: ")
            await client.sign_in(password=password)
    else:
        sent_code = await client.send_code_request(phone=phone, force_sms=force_sms)
        print(f"Code delivery: {sent_code.type.__class__.__name__}")
        if sent_code.next_type is not None:
            print(f"Fallback delivery: {sent_code.next_type.__class__.__name__}")

        code = input("Please enter the code you received: ").strip()
        try:
            await client.sign_in(phone=phone, code=code)
        except telethon_errors.SessionPasswordNeededError:
            password = getpass.getpass("Please enter your 2FA password: ")
            await client.sign_in(phone=phone, password=password)

    me = await client.get_me()
    display_name = " ".join(part for part in [getattr(me, "first_name", None), getattr(me, "last_name", None)] if part)
    print(f"Telegram session created: {settings.telegram_session}")
    print(f"Authorized as: {display_name or getattr(me, 'username', 'unknown')}")
    await client.disconnect()


def _get_phone() -> str:
    env_phone = os.getenv("TELEGRAM_PHONE")
    if env_phone:
        return env_phone.strip()
    return input("Please enter your phone (international format): ").strip()


def _env_flag(name: str) -> bool:
    return os.getenv(name, "").strip().lower() in {"1", "true", "yes", "on"}


def _qr_image_path() -> Path:
    return PROJECT_ROOT / "data" / "telegram_login_qr.png"


def _render_qr_image(url: str, image_path: Path) -> None:
    image_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        import qrcode
    except ImportError as exc:
        raise RuntimeError("qrcode dependency is required for QR login mode") from exc

    qr = qrcode.QRCode(border=2)
    qr.add_data(url)
    qr.make(fit=True)
    image = qr.make_image(fill_color="black", back_color="white")
    image.save(image_path)


if __name__ == "__main__":
    asyncio.run(main())
