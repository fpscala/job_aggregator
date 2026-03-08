from __future__ import annotations

import importlib.util
import logging
import sys
from dataclasses import dataclass
from pathlib import Path

import yaml

from job_aggregator.core.parser_base import BaseParser, ParserConfig

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
                    "Skipping duplicate parser channel '%s' from plugin '%s'",
                    registration.channel,
                    registration.plugin_name,
                )
                continue

            seen_channels.add(registration.channel)
            registrations.append(registration)
            logger.info(
                "Loaded parser plugin '%s' for channel '%s'",
                registration.plugin_name,
                registration.channel,
            )

        return registrations

    def _load_single_plugin(self, plugin_dir: Path) -> ParserRegistration | None:
        parser_path = plugin_dir / "parser.py"
        config_path = plugin_dir / "config.yaml"
        if not parser_path.exists() or not config_path.exists():
            logger.warning("Skipping plugin '%s': parser.py or config.yaml is missing", plugin_dir.name)
            return None

        try:
            config = self._load_config(config_path)
            if not config.enabled:
                logger.info("Skipping disabled parser plugin '%s'", plugin_dir.name)
                return None
            parser_class = self._load_parser_class(plugin_dir.name, parser_path)
            parser = parser_class(config=config, plugin_name=plugin_dir.name)
        except Exception:
            logger.exception("Failed to load parser plugin '%s'", plugin_dir.name)
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

    @staticmethod
    def _load_parser_class(plugin_name: str, parser_path: Path) -> type[BaseParser]:
        module_name = f"job_aggregator.parsers.{plugin_name}.parser"
        spec = importlib.util.spec_from_file_location(module_name, parser_path)
        if spec is None or spec.loader is None:
            raise ImportError(f"Unable to create import spec for {parser_path}")

        module = importlib.util.module_from_spec(spec)
        sys.modules[module_name] = module
        spec.loader.exec_module(module)

        parser_class = getattr(module, "Parser", None)
        if parser_class is None:
            raise ImportError(f"Plugin '{plugin_name}' does not define Parser")
        if not issubclass(parser_class, BaseParser):
            raise TypeError(f"Parser in '{plugin_name}' must inherit from BaseParser")

        return parser_class
