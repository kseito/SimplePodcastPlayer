"""Resolve the pull request for a completed screenshot workflow."""

import json
import os
import re
import subprocess


def resolve_pr(run, pulls, repository):
    """Only an open PR whose current head produced this successful run may be updated."""
    if (
        run["event"] != "pull_request"
        or run["conclusion"] != "success"
        or run["path"] != ".github/workflows/screenshot-comparison.yml"
        or run["repository"]["full_name"] != repository
    ):
        raise ValueError("Unexpected workflow run")
    matches = [
        pr for pr in pulls
        if pr["state"] == "open"
        and pr["base"]["repo"]["full_name"] == repository
        and pr["base"]["ref"] == "main"
        and pr["head"]["sha"] == run["head_sha"]
        and pr["head"]["repo"] is not None
        and pr["head"]["repo"]["id"] == run["head_repository"]["id"]
        and pr["head"]["ref"] == run["head_branch"]
    ]
    if len(matches) != 1:
        # Closed / superseded runs and ambiguous matches must not update any PR.
        return None
    number = matches[0]["number"]
    if type(number) is not int or number <= 0:
        raise ValueError("Invalid pull request number")
    return number


def api(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", endpoint], text=True))


def output(name, value):
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
        stream.write(f"{name}={value}\n")


def main():
    repository = os.environ["GITHUB_REPOSITORY"]
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Invalid repository")
    run_id = os.environ["RUN_ID"]
    if not run_id.isdecimal():
        raise ValueError("Invalid run ID")
    run = api(f"repos/{repository}/actions/runs/{run_id}")
    sha = run["head_sha"]
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Invalid commit")
    pulls = api(f"repos/{repository}/commits/{sha}/pulls?per_page=100")
    number = resolve_pr(run, pulls, repository)
    output("number", number if number is not None else "")


if __name__ == "__main__":
    main()
