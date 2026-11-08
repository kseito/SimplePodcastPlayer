import copy
import unittest

from screenshot_report import resolve_pr


class ScreenshotReportTest(unittest.TestCase):
    def setUp(self):
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
