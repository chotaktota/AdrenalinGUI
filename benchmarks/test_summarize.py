import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("summarize.py")


class SummarizeTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.source = Path(directory.name) / "samples.json"
        self.target = self.source.with_name("summary.json")
        self.samples = [
            {"phase": phase, "menu": f"{engine}/{mode}", "metric": metric, "nanos": 1000}
            for phase in ("round1", "round2", "round3")
            for engine in ("abstract", "agui", "bmenu")
            for mode in ("dynamic", "static")
            for metric in ("click_action", "open")
            for _ in range(400)
        ]

    def run_summary(self, samples, *args):
        self.source.write_text(json.dumps({"failures": 0, "samples": samples}), encoding="utf-8")
        return subprocess.run(
            [sys.executable, str(SCRIPT), str(self.source), *args],
            capture_output=True,
            env={**os.environ, "PYTHONIOENCODING": "utf-8"},
            encoding="utf-8",
        )

    def assert_rejected(self, result, group, count):
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(f"{group}: ожидалось 400, получено {count}", result.stderr)
        self.assertFalse(self.target.exists())

    def test_empty_run(self):
        self.assert_rejected(self.run_summary([]), "round1/abstract/dynamic/click_action", 0)

    def test_missing_metric(self):
        samples = [
            sample for sample in self.samples
            if (sample["menu"], sample["metric"]) != ("agui/static", "click_action")
        ]
        for args in ((), ("--allow-incomplete-actions",)):
            with self.subTest(args=args):
                self.assert_rejected(self.run_summary(samples, *args), "round1/agui/static/click_action", 0)

    def test_even_aggregate_can_hide_unbalanced_rounds(self):
        sample = self.samples[0]
        self.samples[0] = {**sample, "phase": "round2"}
        self.assert_rejected(self.run_summary(self.samples), "round1/abstract/dynamic/click_action", 399)

    def test_partial_actions_with_flag(self):
        result = self.run_summary(self.samples[1:], "--allow-incomplete-actions")
        self.assertEqual(result.returncode, 0, result.stderr)
        summary = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertEqual(
            summary["incomplete_actions"],
            {"abstract/dynamic/click_action": {"expected": 1200, "recorded": 1199}},
        )

    def test_complete_run(self):
        result = self.run_summary(self.samples)
        self.assertEqual(result.returncode, 0, result.stderr)
        summary = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertEqual(len(summary["aggregate"]), 12)
        self.assertEqual(len(summary["rounds"]), 36)
        self.assertEqual(summary["incomplete_actions"], {})


if __name__ == "__main__":
    unittest.main()
