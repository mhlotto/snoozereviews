#!/usr/bin/env python3
"""Local release tooling for Snooze Reviews."""

from __future__ import annotations

import argparse
import dataclasses
import datetime as _dt
import hashlib
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Callable, Iterable, List, Optional, Sequence


VERSION_FILE = "version.properties"
CHANGELOG_FILE = "CHANGELOG.md"
DIST_DIR = "dist"
DEFAULT_BRANCH = "main"
DEFAULT_REMOTE = "origin"
TAG_PREFIX = "v"

SEMVER_RE = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")
EMPTY_UNRELEASED = """## Unreleased

### Added

### Changed

### Fixed
"""
VALIDATION_COMMANDS = [
    ("./gradlew", "assembleDebug"),
    ("./gradlew", "testDebugUnitTest"),
    ("./gradlew", "lintDebug"),
    ("./gradlew", "assembleRelease"),
    ("./gradlew", "assembleDebugAndroidTest"),
    ("git", "diff", "--check"),
]


class ReleaseError(RuntimeError):
    """Raised for expected release-tool failures."""


@dataclasses.dataclass(frozen=True, order=True)
class SemVer:
    major: int
    minor: int
    patch: int

    @classmethod
    def parse(cls, text: str) -> "SemVer":
        match = SEMVER_RE.match(text.strip())
        if not match:
            raise ReleaseError(f"Invalid semantic version: {text!r}")
        return cls(*(int(part) for part in match.groups()))

    def bump(self, kind: str) -> "SemVer":
        if kind == "patch":
            return SemVer(self.major, self.minor, self.patch + 1)
        if kind == "minor":
            return SemVer(self.major, self.minor + 1, 0)
        if kind == "major":
            return SemVer(self.major + 1, 0, 0)
        raise ReleaseError(f"Unsupported version bump: {kind}")

    def __str__(self) -> str:
        return f"{self.major}.{self.minor}.{self.patch}"


@dataclasses.dataclass(frozen=True)
class VersionInfo:
    name: SemVer
    code: int

    @property
    def tag(self) -> str:
        return f"{TAG_PREFIX}{self.name}"

    def next_code(self) -> int:
        return self.code + 1


@dataclasses.dataclass(frozen=True)
class CommandResult:
    stdout: str
    returncode: int


class CommandRunner:
    def __init__(self, cwd: Path):
        self.cwd = cwd

    def run(
        self,
        command: Sequence[str],
        *,
        check: bool = True,
        capture: bool = True,
        stream_output: bool = False,
    ) -> CommandResult:
        env = os.environ.copy()
        android_studio_jbr = Path("/Applications/Android Studio.app/Contents/jbr/Contents/Home")
        if "JAVA_HOME" not in env and android_studio_jbr.exists():
            env["JAVA_HOME"] = str(android_studio_jbr)
        completed = subprocess.run(
            list(command),
            cwd=self.cwd,
            env=env,
            text=True,
            stdout=subprocess.PIPE if capture else None,
            stderr=subprocess.STDOUT if capture else None,
        )
        output = completed.stdout or ""
        if capture and stream_output and output:
            print(output, end="" if output.endswith("\n") else "\n")
        if check and completed.returncode != 0:
            raise ReleaseError(f"Command failed with exit {completed.returncode}: {' '.join(command)}")
        return CommandResult(output, completed.returncode)


def read_version_file(path: Path) -> VersionInfo:
    if not path.is_file():
        raise ReleaseError(f"Missing {VERSION_FILE}")
    values = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ReleaseError(f"Invalid line in {VERSION_FILE}: {raw_line!r}")
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    if "VERSION_NAME" not in values:
        raise ReleaseError("VERSION_NAME is missing")
    if "VERSION_CODE" not in values:
        raise ReleaseError("VERSION_CODE is missing")
    name = SemVer.parse(values["VERSION_NAME"])
    try:
        code = int(values["VERSION_CODE"])
    except ValueError as exc:
        raise ReleaseError("VERSION_CODE must be a positive integer") from exc
    if code <= 0:
        raise ReleaseError("VERSION_CODE must be a positive integer")
    return VersionInfo(name, code)


def write_version_file(path: Path, version: VersionInfo) -> None:
    path.write_text(f"VERSION_NAME={version.name}\nVERSION_CODE={version.code}\n", encoding="utf-8")


def unreleased_bounds(text: str) -> tuple[int, int, str]:
    match = re.search(r"(?m)^## Unreleased\s*$", text)
    if not match:
        raise ReleaseError("CHANGELOG.md must contain an '## Unreleased' section")
    next_match = re.search(r"(?m)^## (?!Unreleased\b).+$", text[match.end():])
    end = len(text) if next_match is None else match.end() + next_match.start()
    return match.start(), end, text[match.end():end]


def meaningful_unreleased_content(section_body: str) -> bool:
    for line in section_body.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("### "):
            continue
        if stripped.startswith("<!--") and stripped.endswith("-->"):
            continue
        return True
    return False


def changelog_has_version(text: str, version: SemVer) -> bool:
    pattern = re.compile(rf"(?m)^##\s+{re.escape(str(version))}(?:\s+-\s+\d{{4}}-\d{{2}}-\d{{2}})?\s*$")
    return bool(pattern.search(text))


def prepare_changelog(text: str, version: SemVer, release_date: _dt.date, notes: Optional[str]) -> str:
    if changelog_has_version(text, version):
        raise ReleaseError(f"CHANGELOG.md already contains a section for {version}")
    start, end, body = unreleased_bounds(text)
    if meaningful_unreleased_content(body):
        release_body = body.strip("\n")
    else:
        if not notes or not notes.strip():
            raise ReleaseError("Unreleased changelog is empty; provide RELEASE_NOTES or run interactively")
        release_body = f"\n### Added\n\n### Changed\n\n- {notes.strip()}\n\n### Fixed"
    before = text[:start].rstrip() + "\n\n"
    after = text[end:].lstrip("\n")
    release_section = f"## {version} - {release_date.isoformat()}\n{release_body.rstrip()}\n"
    result = before + EMPTY_UNRELEASED.rstrip() + "\n\n" + release_section.rstrip() + "\n"
    if after:
        result += "\n" + after
    return result


class ReleaseTool:
    def __init__(
        self,
        root: Path,
        runner: Optional[CommandRunner] = None,
        today: Optional[_dt.date] = None,
        validation_commands: Optional[List[Sequence[str]]] = None,
    ):
        self.root = root.resolve()
        self.runner = runner or CommandRunner(self.root)
        self.today = today or _dt.date.today()
        self.validation_commands = VALIDATION_COMMANDS if validation_commands is None else validation_commands

    @property
    def version_path(self) -> Path:
        return self.root / VERSION_FILE

    @property
    def changelog_path(self) -> Path:
        return self.root / CHANGELOG_FILE

    def read_version(self) -> VersionInfo:
        return read_version_file(self.version_path)

    def run_git(self, *args: str, check: bool = True) -> str:
        return self.runner.run(("git", *args), check=check).stdout.strip()

    def git_status_porcelain(self) -> str:
        return self.run_git("status", "--porcelain=v1", "--untracked-files=all")

    def current_branch(self) -> str:
        return self.run_git("rev-parse", "--abbrev-ref", "HEAD")

    def head_short(self) -> str:
        return self.run_git("rev-parse", "--short", "HEAD")

    def status(self) -> str:
        version = self.read_version()
        status = "dirty" if self.git_status_porcelain() else "clean"
        branch = self.current_branch()
        commit = self.head_short()
        lines = [
            f"Version name: {version.name}",
            f"Version code: {version.code}",
            f"Expected tag: {version.tag}",
            f"Branch: {branch}",
            f"Commit: {commit}",
            f"Working tree: {status}",
        ]
        return "\n".join(lines)

    def determine_target(self, current: VersionInfo, bump: Optional[str], explicit: Optional[str]) -> VersionInfo:
        if bool(bump) == bool(explicit):
            raise ReleaseError("Specify exactly one of --bump or --version")
        target_name = current.name.bump(bump) if bump else SemVer.parse(explicit or "")
        if target_name <= current.name:
            raise ReleaseError(f"Requested version {target_name} must be greater than {current.name}")
        return VersionInfo(target_name, current.next_code())

    def preflight(self, target: VersionInfo, *, branch: str, publish: bool, remote: str) -> None:
        self.preflight_current(branch=branch)
        if self.run_git("tag", "--list", target.tag):
            raise ReleaseError(f"Local tag already exists: {target.tag}")
        for path in (self.version_path, self.changelog_path):
            if not path.exists():
                raise ReleaseError(f"Missing managed release file: {path.name}")
            if not os.access(path, os.W_OK):
                raise ReleaseError(f"Managed release file is not writable: {path.name}")
        if publish:
            self.preflight_publish(target.tag, remote, branch)

    def preflight_current(self, *, branch: str) -> None:
        repo_root = Path(self.run_git("rev-parse", "--show-toplevel")).resolve()
        if repo_root != self.root:
            raise ReleaseError(f"Expected repository root {self.root}, got {repo_root}")
        if not (self.root / "gradlew").is_file():
            raise ReleaseError("Missing Gradle wrapper")
        if not (self.root / "app" / "build.gradle").is_file():
            raise ReleaseError("Missing app/build.gradle")
        current_branch = self.current_branch()
        if current_branch == "HEAD":
            raise ReleaseError("Cannot release from detached HEAD")
        if current_branch != branch:
            raise ReleaseError(f"Release must run on {branch}; current branch is {current_branch}")
        if self.git_status_porcelain():
            raise ReleaseError("Working tree must be completely clean before release")

    def preflight_publish(self, tag: str, remote: str, branch: str) -> None:
        self.run_git("remote", "get-url", remote)
        if self.run_git("ls-remote", "--tags", remote, f"refs/tags/{tag}"):
            raise ReleaseError(f"Remote tag already exists: {tag}")
        upstream = self.run_git("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{upstream}", check=False)
        if not upstream:
            raise ReleaseError("Current branch has no upstream")
        if not upstream.startswith(remote + "/"):
            raise ReleaseError(f"Current branch upstream {upstream} is not on remote {remote}")
        status = self.run_git("status", "-sb")
        if "behind " in status:
            raise ReleaseError("Current branch appears to be behind its upstream")
        if not branch:
            raise ReleaseError("Branch name is required for publishing")

    def dry_run(self, *, bump: Optional[str], explicit: Optional[str], publish: bool) -> str:
        current = self.read_version()
        target = self.determine_target(current, bump, explicit)
        commands = [" ".join(command) for command in self.validation_commands]
        lines = [
            f"Current version: {current.name}",
            f"Proposed version: {target.name}",
            f"Current version code: {current.code}",
            f"Proposed version code: {target.code}",
            f"Proposed tag: {target.tag}",
            f"Publishing enabled: {'yes' if publish else 'no'}",
            "Commands that would run:",
            *[f"  {command}" for command in commands],
            "Files that would be modified:",
            f"  {VERSION_FILE}",
            f"  {CHANGELOG_FILE}",
            "Artifacts that would be created:",
            f"  {DIST_DIR}/snooze-reviews-{target.name}-code{target.code}-debug.apk",
            f"  {DIST_DIR}/snooze-reviews-{target.name}-code{target.code}-release-unsigned.apk or release.apk",
        ]
        return "\n".join(lines)

    def release(
        self,
        *,
        bump: Optional[str],
        explicit: Optional[str],
        publish: bool = False,
        branch: str = DEFAULT_BRANCH,
        remote: str = DEFAULT_REMOTE,
        notes: Optional[str] = None,
    ) -> None:
        current = self.read_version()
        target = self.determine_target(current, bump, explicit)
        self.preflight(target, branch=branch, publish=publish, remote=remote)
        original_version = self.version_path.read_bytes()
        original_changelog = self.changelog_path.read_bytes()
        created_artifacts: List[Path] = []
        committed = False
        tagged = False
        try:
            print(f"Preparing release {target.tag}")
            write_version_file(self.version_path, target)
            changelog = self.changelog_path.read_text(encoding="utf-8")
            if not meaningful_unreleased_content(unreleased_bounds(changelog)[2]) and not notes and sys.stdin.isatty():
                notes = input("Unreleased changelog is empty. Enter a short release summary: ").strip()
            self.changelog_path.write_text(prepare_changelog(changelog, target.name, self.today, notes), encoding="utf-8")

            self.run_validation()
            self.verify_no_protected_diffs()
            created_artifacts = self.collect_artifacts(target)
            self.verify_only_managed_changes()

            self.run_git("add", VERSION_FILE, CHANGELOG_FILE)
            staged = self.run_git("diff", "--cached", "--name-only").splitlines()
            if sorted(staged) != [CHANGELOG_FILE, VERSION_FILE]:
                raise ReleaseError(f"Unexpected staged files: {staged}")
            self.run_git("commit", "-m", f"Release {target.tag}")
            committed = True
            self.run_git("tag", "-a", target.tag, "-m", f"Snooze Reviews {target.tag}")
            tagged = True

            if publish:
                self.push_current(remote=remote, branch=branch, version=target)

            print(f"Released: {target.name}")
            print(f"Version code: {target.code}")
            print(f"Commit: {self.run_git('rev-parse', '--short', 'HEAD')}")
            print(f"Tag: {target.tag}")
            if publish:
                print(f"Remote: {remote}")
            print("Artifacts:")
            for path in created_artifacts:
                print(f"  {path.relative_to(self.root)}")
        except Exception as exc:
            if not committed and not tagged:
                self.version_path.write_bytes(original_version)
                self.changelog_path.write_bytes(original_changelog)
                self.runner.run(("git", "restore", "--staged", VERSION_FILE, CHANGELOG_FILE), check=False)
                for path in created_artifacts:
                    path.unlink(missing_ok=True)
                    sha = path.with_suffix(path.suffix + ".sha256")
                    sha.unlink(missing_ok=True)
                print("Release failed before commit; managed files were restored.")
            else:
                print("Release failed after commit or tag creation. No history was rewritten.")
                print("Inspect the repository and recover manually before retrying.")
            if isinstance(exc, ReleaseError):
                raise
            raise ReleaseError(str(exc)) from exc

    def run_validation(self) -> None:
        for command in self.validation_commands:
            print(f"Running release validation: {' '.join(command)}")
            self.runner.run(command, check=True, stream_output=True)

    def verify_no_protected_diffs(self) -> None:
        for path in ("app/schemas", "app/src/main/AndroidManifest.xml"):
            diff = self.run_git("diff", "--", path)
            if diff:
                raise ReleaseError(f"Release unexpectedly changed {path}")

    def verify_only_managed_changes(self) -> None:
        allowed = {
            f" M {VERSION_FILE}",
            f" M {CHANGELOG_FILE}",
            f"M {VERSION_FILE}",
            f"M {CHANGELOG_FILE}",
        }
        lines = set(filter(None, self.git_status_porcelain().splitlines()))
        unexpected = [line for line in sorted(lines) if line not in allowed]
        if unexpected:
            raise ReleaseError(f"Unexpected working tree changes before commit: {unexpected}")

    def collect_artifacts(self, version: VersionInfo) -> List[Path]:
        dist = self.root / DIST_DIR
        dist.mkdir(exist_ok=True)
        prefix = f"snooze-reviews-{version.name}-code{version.code}"
        for stale in dist.glob(prefix + "*"):
            stale.unlink()

        outputs: List[tuple[Path, str]] = []
        debug_apk = self.root / "app/build/outputs/apk/debug/app-debug.apk"
        if not debug_apk.is_file():
            raise ReleaseError(f"Missing debug APK: {debug_apk}")
        outputs.append((debug_apk, f"{prefix}-debug.apk"))

        release_dir = self.root / "app/build/outputs/apk/release"
        signed = release_dir / "app-release.apk"
        unsigned = release_dir / "app-release-unsigned.apk"
        if signed.is_file():
            outputs.append((signed, f"{prefix}-release.apk"))
        elif unsigned.is_file():
            outputs.append((unsigned, f"{prefix}-release-unsigned.apk"))
        else:
            raise ReleaseError("Missing release APK")

        copied: List[Path] = []
        for source, filename in outputs:
            destination = dist / filename
            shutil.copy2(source, destination)
            self.write_sha256(destination)
            copied.append(destination)
            copied.append(destination.with_suffix(destination.suffix + ".sha256"))
        return copied

    @staticmethod
    def write_sha256(path: Path) -> None:
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        checksum = path.with_suffix(path.suffix + ".sha256")
        checksum.write_text(f"{digest}  {path.name}\n", encoding="utf-8")

    def check(self, *, branch: str = DEFAULT_BRANCH) -> None:
        self.read_version()
        self.preflight_current(branch=branch)
        self.run_validation()

    def publish_current(self, *, remote: str = DEFAULT_REMOTE, branch: Optional[str] = None) -> None:
        version = self.read_version()
        actual_branch = branch or self.current_branch()
        self.preflight_publish(version.tag, remote, actual_branch)
        self.push_current(remote=remote, branch=actual_branch, version=version)

    def push_current(self, *, remote: str, branch: str, version: VersionInfo) -> None:
        if self.git_status_porcelain():
            raise ReleaseError("Working tree must be clean before publishing")
        tag_type = self.run_git("cat-file", "-t", version.tag)
        if tag_type != "tag":
            raise ReleaseError(f"{version.tag} must be an annotated tag")
        tag_commit = self.run_git("rev-list", "-n", "1", version.tag)
        head = self.run_git("rev-parse", "HEAD")
        if tag_commit != head:
            raise ReleaseError(f"{version.tag} does not point to HEAD")
        subject = self.run_git("log", "-1", "--pretty=%s")
        if subject != f"Release {version.tag}":
            raise ReleaseError(f"HEAD is not the release commit for {version.tag}")
        print(f"Pushing {branch} and {version.tag} to {remote}")
        self.runner.run(("git", "push", "--atomic", remote, branch, version.tag), check=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Snooze Reviews release tooling")
    parser.add_argument("--root", default=".", help=argparse.SUPPRESS)
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("status")

    check = subparsers.add_parser("check")
    check.add_argument("--branch", default=os.environ.get("RELEASE_BRANCH", DEFAULT_BRANCH))

    dry = subparsers.add_parser("dry-run")
    dry.add_argument("--bump", choices=("patch", "minor", "major"))
    dry.add_argument("--version")
    dry.add_argument("--publish", action="store_true")

    release = subparsers.add_parser("release")
    release.add_argument("--bump", choices=("patch", "minor", "major"))
    release.add_argument("--version")
    release.add_argument("--publish", action="store_true")
    release.add_argument("--branch", default=os.environ.get("RELEASE_BRANCH", DEFAULT_BRANCH))
    release.add_argument("--remote", default=os.environ.get("REMOTE", DEFAULT_REMOTE))
    release.add_argument("--notes", default=os.environ.get("RELEASE_NOTES"))

    publish = subparsers.add_parser("publish-current")
    publish.add_argument("--remote", default=os.environ.get("REMOTE", DEFAULT_REMOTE))
    publish.add_argument("--branch", default=os.environ.get("RELEASE_BRANCH"))
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    tool = ReleaseTool(Path(args.root))
    try:
        if args.command == "status":
            print(tool.status())
        elif args.command == "check":
            tool.check(branch=args.branch)
        elif args.command == "dry-run":
            print(tool.dry_run(bump=args.bump, explicit=args.version, publish=args.publish))
        elif args.command == "release":
            tool.release(
                bump=args.bump,
                explicit=args.version,
                publish=args.publish,
                branch=args.branch,
                remote=args.remote,
                notes=args.notes,
            )
        elif args.command == "publish-current":
            tool.publish_current(remote=args.remote, branch=args.branch)
        else:
            parser.error(f"Unknown command: {args.command}")
    except ReleaseError as exc:
        print(f"release.py: error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
