"""Treat PR artifacts as data; publish only bounded PNG files under generated names."""

import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_IMAGES = 200
MAX_IMAGE_BYTES = 10 * 1024 * 1024
MAX_TOTAL_BYTES = 50 * 1024 * 1024


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


def prepare_images(source, destination):
    """Never preserve artifact paths in git, Markdown, shell scripts, or outputs."""
    source = Path(source)
    destination = Path(destination)
    if not source.is_dir() or source.is_symlink():
        raise ValueError("Missing or invalid artifact directory")
    destination.mkdir(parents=True, exist_ok=False)
    names = []
    total = 0
    for root, directories, files in os.walk(source, followlinks=False):
        for name in directories + files:
            if (Path(root) / name).is_symlink():
                raise ValueError("Artifact contains a symbolic link")
        for name in sorted(files):
            if not name.endswith("_compare.png"):
                continue
            path = Path(root) / name
            info = path.stat()
            if not stat.S_ISREG(info.st_mode) or info.st_size > MAX_IMAGE_BYTES:
                raise ValueError("Invalid or oversized comparison image")
            total += info.st_size
            if len(names) >= MAX_IMAGES or total > MAX_TOTAL_BYTES:
                raise ValueError("Comparison report exceeds its size limit")
            data = path.read_bytes()
            if not data.startswith(PNG_SIGNATURE):
                raise ValueError("Comparison image is not a PNG")
            safe_name = hashlib.sha256(str(path.relative_to(source)).encode()).hexdigest() + ".png"
            (destination / safe_name).write_bytes(data)
            names.append(safe_name)
    return sorted(names)


def api(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", endpoint], text=True))


def output(name, value):
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
        stream.write(f"{name}={value}\n")


def main():
    repository = os.environ["GITHUB_REPOSITORY"]
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Invalid repository")
    if sys.argv[1] == "resolve":
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
    elif sys.argv[1] == "prepare":
        number = os.environ["PR_NUMBER"]
        run_id = os.environ["RUN_ID"]
        if not number.isdecimal() or not run_id.isdecimal():
            raise ValueError("Invalid report identifier")
        names = prepare_images(sys.argv[2], sys.argv[3])
        branch = f"companion_pr_{number}_run_{run_id}"
        body = "## Screenshot Comparison Results\n\n" + "\n".join(
            f"![](https://github.com/{repository}/blob/{branch}/{name}?raw=true)" for name in names
        )
        Path(sys.argv[4]).write_text(body + "\n", encoding="utf-8")
        output("has_images", "true" if names else "false")
        output("branch", branch)
    else:
        raise ValueError("Unknown command")


if __name__ == "__main__":
    main()
