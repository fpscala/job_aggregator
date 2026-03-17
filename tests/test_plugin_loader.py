from __future__ import annotations

import unittest
from pathlib import Path

from job_aggregator.core.plugin_loader import PluginLoader
from job_aggregator.core.raw_message_parser import RawMessageParser
from job_aggregator.parsers.ishchi_bor_kerak_toshkent.parser import Parser as IshchiBorParser


class PluginLoaderTestCase(unittest.TestCase):
    def test_loads_source_specific_parser_class(self) -> None:
        loader = PluginLoader(Path(__file__).resolve().parents[1] / "job_aggregator" / "parsers")
        registrations = {item.plugin_name: item for item in loader.load()}

        self.assertIn("ishchi_bor_kerak_toshkent", registrations)
        self.assertIsInstance(registrations["ishchi_bor_kerak_toshkent"].parser, IshchiBorParser)

    def test_falls_back_to_raw_parser_without_parser_file(self) -> None:
        import tempfile
        from pathlib import Path

        with tempfile.TemporaryDirectory() as tmp_dir:
            parsers_path = Path(tmp_dir)
            plugin_dir = parsers_path / "config_only_source"
            plugin_dir.mkdir()
            (plugin_dir / "config.yaml").write_text(
                "channel: config_only_channel\nsource: config_only_source\nenabled: true\n",
                encoding="utf-8",
            )

            loader = PluginLoader(parsers_path)
            registrations = loader.load()

        self.assertEqual(len(registrations), 1)
        self.assertIsInstance(registrations[0].parser, RawMessageParser)
