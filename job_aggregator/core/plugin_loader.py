from __future__ import annotations

import importlib
import logging
from dataclasses import dataclass
from pathlib import Path

import yaml

from job_aggregator.core.parser_base import BaseParser, ParserConfig
from job_aggregator.core.raw_message_parser import RawMessageParser

logger = logging.getLogger(__name__)


@dataclass
class ParserRegistration:
    channel: str
    parser: BaseParser
    plugin_name: str
    config_path: Path


class PluginLoader:
    def __init__(self, parsers_path: Path) -> None:
        self.parsers_path = Path(parsers_path)

    def load(self) -> list[ParserRegistration]:
        if not self.parsers_path.exists():
            raise FileNotFoundError(f"Parsers directory does not exist: {self.parsers_path}")

        registrations: list[ParserRegistration] = []
        seen_channels: set[str] = set()

        for plugin_dir in sorted(self.parsers_path.iterdir()):
            if not plugin_dir.is_dir() or plugin_dir.name.startswith("__"):
                continue

            registration = self._load_single_plugin(plugin_dir)
            if registration is None:
                continue

            if registration.channel in seen_channels:
                logger.error(
                    "Skipping duplicate source channel '%s' from config '%s'",
                    registration.channel,
                    registration.plugin_name,
                )
                continue

            seen_channels.add(registration.channel)
            registrations.append(registration)
            logger.info(
                "Loaded source config '%s' for channel '%s'",
                registration.plugin_name,
                registration.channel,
            )

        return registrations

    def _load_single_plugin(self, plugin_dir: Path) -> ParserRegistration | None:
        config_path = plugin_dir / "config.yaml"
        if not config_path.exists():
            logger.warning("Skipping source config '%s': config.yaml is missing", plugin_dir.name)
            return None

        try:
            config = self._load_config(config_path)
            if not config.enabled:
                logger.info("Skipping disabled source config '%s'", plugin_dir.name)
                return None
            parser = self._build_parser(plugin_dir, config)
        except Exception:
            logger.exception("Failed to load source config '%s'", plugin_dir.name)
            return None

        return ParserRegistration(
            channel=config.channel,
            parser=parser,
            plugin_name=plugin_dir.name,
            config_path=config_path,
        )

    @staticmethod
    def _load_config(config_path: Path) -> ParserConfig:
        with config_path.open("r", encoding="utf-8") as config_file:
            raw_config = yaml.safe_load(config_file) or {}
        return ParserConfig.model_validate(raw_config)

    def _build_parser(self, plugin_dir: Path, config: ParserConfig) -> BaseParser:
        parser_path = plugin_dir / "parser.py"
        if not parser_path.exists():
            logger.info(
                "Source config '%s' has no parser.py; using RawMessageParser fallback",
                plugin_dir.name,
            )
            return RawMessageParser(config=config, plugin_name=plugin_dir.name)

        module = importlib.import_module(f"job_aggregator.parsers.{plugin_dir.name}.parser")
        parser_cls = getattr(module, "Parser", None)
        if parser_cls is None:
            raise AttributeError(f"Plugin '{plugin_dir.name}' does not define a Parser class")
        if not issubclass(parser_cls, BaseParser):
            raise TypeError(f"Plugin '{plugin_dir.name}' Parser must inherit from BaseParser")

        return parser_cls(config=config, plugin_name=plugin_dir.name)
