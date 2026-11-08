import copy
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from screenshot_report import PNG_SIGNATURE, prepare_images, resolve_pr


class ScreenshotReportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "input"
        self.source.mkdir()
        self.destination = self.root / "safe"
        self.run = {
            "event": "pull_request", "conclusion": "success",
            "path": ".github/workflows/screenshot-comparison.yml",
            "repository": {"full_name": "owner/repo"},
            "head_sha": "a" * 40, "head_repository": {"id": 2}, "head_branch": "feature",
        }
        self.pr = {
            "number": 42, "state": "open",
            "base": {"repo": {"full_name": "owner/repo"}, "ref": "main"},
            "head": {"repo": {"id": 2}, "sha": "a" * 40, "ref": "feature"},
        }

    def test_untrusted_names_are_never_published(self):
        for name in ['$(touch INJECTED)_compare.png', '`touch INJECTED`_compare.png',
                     'quote"\nmarkdown](url)_compare.png', '-option_compare.png']:
            (self.source / name).write_bytes(PNG_SIGNATURE + b"data")
        (self.source / "unrelated.sh").write_text("untrusted")
        names = prepare_images(self.source, self.destination)
        self.assertEqual(len(names), 4)
        for name in names:
            self.assertRegex(name, r"^[a-f0-9]{64}\.png$")
            self.assertEqual((self.destination / name).read_bytes(), PNG_SIGNATURE + b"data")
        self.assertFalse((self.root / "INJECTED").exists())
        self.assertEqual(sorted(p.name for p in self.destination.iterdir()), names)

    def test_same_basename_in_different_directories_is_preserved(self):
        for folder in ["one", "two"]:
            (self.source / folder).mkdir()
            (self.source / folder / "a_compare.png").write_bytes(PNG_SIGNATURE)
        self.assertEqual(len(prepare_images(self.source, self.destination)), 2)

    def test_missing_report_is_not_treated_as_no_diff(self):
        with self.assertRaises(ValueError):
            prepare_images(self.root / "missing", self.destination)

    def test_empty_report(self):
        self.assertEqual(prepare_images(self.source, self.destination), [])

    def test_rejects_non_png(self):
        (self.source / "bad_compare.png").write_text("not a png")
        with self.assertRaises(ValueError):
            prepare_images(self.source, self.destination)

    def test_rejects_symlink_files_and_directories(self):
        for directory in [False, True]:
            with self.subTest(directory=directory):
                link = self.source / "link_compare.png"
                link.symlink_to(self.root, target_is_directory=directory)
                with self.assertRaises(ValueError):
                    prepare_images(self.source, self.root / str(directory))
                link.unlink()

    def test_enforces_individual_total_and_count_limits(self):
        for limit in ["MAX_IMAGE_BYTES", "MAX_TOTAL_BYTES", "MAX_IMAGES"]:
            with self.subTest(limit=limit), patch(f"screenshot_report.{limit}", 0):
                (self.source / "a_compare.png").write_bytes(PNG_SIGNATURE)
                with self.assertRaises(ValueError):
                    prepare_images(self.source, self.root / limit)

    def test_resolves_fork_pr_from_api_without_artifact_number(self):
        self.assertEqual(resolve_pr(self.run, [self.pr], "owner/repo"), 42)

    def test_rejects_wrong_workflow_and_event(self):
        for key, value in [("event", "push"), ("conclusion", "failure"), ("path", "other.yml")]:
            run = dict(self.run, **{key: value})
            with self.subTest(key=key), self.assertRaises(ValueError):
                resolve_pr(run, [self.pr], "owner/repo")

    def test_skips_closed_stale_foreign_and_ambiguous_prs(self):
        variants = []
        pr = copy.deepcopy(self.pr)
        pr["state"] = "closed"
        variants.append([pr])
        for key, value in [("sha", "b" * 40), ("repo", {"id": 999}), ("ref", "other")]:
            pr = copy.deepcopy(self.pr)
            pr["head"][key] = value
            variants.append([pr])
        pr = copy.deepcopy(self.pr)
        pr["base"]["repo"]["full_name"] = "other/repo"
        variants.extend([[pr], [], [self.pr, self.pr]])
        for pulls in variants:
            with self.subTest(pulls=pulls):
                self.assertIsNone(resolve_pr(self.run, pulls, "owner/repo"))


if __name__ == "__main__":
    unittest.main()
