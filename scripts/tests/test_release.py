import contextlib
import datetime as dt
import io
import subprocess
import tempfile
import unittest
from pathlib import Path

from scripts.release import (
    CHANGELOG_FILE,
    ReleaseError,
    ReleaseTool,
    SemVer,
    VersionInfo,
    changelog_has_version,
    meaningful_unreleased_content,
    prepare_changelog,
    read_version_file,
    write_version_file,
)


def run(cwd, *args):
    return subprocess.run(args, cwd=cwd, check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout


class ReleaseToolTest(unittest.TestCase):
    def test_semver_parsing_and_bumps(self):
        self.assertEqual(str(SemVer.parse("1.1.2").bump("patch")), "1.1.3")
        self.assertEqual(str(SemVer.parse("1.1.2").bump("minor")), "1.2.0")
        self.assertEqual(str(SemVer.parse("1.1.2").bump("major")), "2.0.0")
        for value in ["1.1", "v1.2.0", "1.2.0-beta", "1.2.0+5", "01.2.0"]:
            with self.assertRaises(ReleaseError):
                SemVer.parse(value)

    def test_explicit_version_must_increase_and_code_increments(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_version_file(root / "version.properties", VersionInfo(SemVer.parse("1.1.2"), 7))
            tool = ReleaseTool(root, validation_commands=[])
            target = tool.determine_target(tool.read_version(), None, "1.2.0")
            self.assertEqual(str(target.name), "1.2.0")
            self.assertEqual(target.code, 8)
            with self.assertRaises(ReleaseError):
                tool.determine_target(tool.read_version(), None, "1.1.2")
            with self.assertRaises(ReleaseError):
                tool.determine_target(tool.read_version(), None, "1.0.9")

    def test_version_properties_parsing_and_writing(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "version.properties"
            write_version_file(path, VersionInfo(SemVer.parse("1.2.3"), 9))
            self.assertEqual(path.read_text(), "VERSION_NAME=1.2.3\nVERSION_CODE=9\n")
            parsed = read_version_file(path)
            self.assertEqual(str(parsed.name), "1.2.3")
            self.assertEqual(parsed.code, 9)

            path.write_text("VERSION_CODE=1\n")
            with self.assertRaises(ReleaseError):
                read_version_file(path)
            path.write_text("VERSION_NAME=1.2.3\n")
            with self.assertRaises(ReleaseError):
                read_version_file(path)
            path.write_text("VERSION_NAME=1.2.3\nVERSION_CODE=zero\n")
            with self.assertRaises(ReleaseError):
                read_version_file(path)

    def test_changelog_empty_and_meaningful_unreleased_detection(self):
        empty = "\n### Added\n\n### Changed\n\n### Fixed\n"
        self.assertFalse(meaningful_unreleased_content(empty))
        self.assertTrue(meaningful_unreleased_content("\n### Changed\n\n- Real note\n"))

    def test_changelog_release_section_and_notes(self):
        text = "# Changelog\n\n## Unreleased\n\n### Added\n\n### Changed\n\n### Fixed\n"
        updated = prepare_changelog(text, SemVer.parse("1.2.0"), dt.date(2026, 7, 13), "Ship release tooling")
        self.assertIn("## Unreleased", updated)
        self.assertIn("## 1.2.0 - 2026-07-13", updated)
        self.assertIn("- Ship release tooling", updated)
        self.assertTrue(changelog_has_version(updated, SemVer.parse("1.2.0")))
        with self.assertRaises(ReleaseError):
            prepare_changelog(updated, SemVer.parse("1.2.0"), dt.date(2026, 7, 13), "Again")

    def test_dry_run_changes_nothing(self):
        with self.repo() as root:
            before_version = (root / "version.properties").read_text()
            before_changelog = (root / CHANGELOG_FILE).read_text()
            output = ReleaseTool(root, validation_commands=[]).dry_run(bump="patch", explicit=None, publish=False)
            self.assertIn("Proposed version: 1.1.3", output)
            self.assertIn("Proposed version code: 2", output)
            self.assertIn("Proposed tag: v1.1.3", output)
            self.assertEqual((root / "version.properties").read_text(), before_version)
            self.assertEqual((root / CHANGELOG_FILE).read_text(), before_changelog)

    def test_git_safety_detects_dirty_staged_untracked_wrong_branch_and_existing_tag(self):
        with self.repo() as root:
            tool = ReleaseTool(root, validation_commands=[])
            target = VersionInfo(SemVer.parse("1.1.3"), 2)
            (root / "untracked.txt").write_text("x")
            with self.assertRaises(ReleaseError):
                tool.preflight(target, branch="main", publish=False, remote="origin")
            (root / "untracked.txt").unlink()
            (root / "version.properties").write_text("VERSION_NAME=1.1.2\nVERSION_CODE=1\n# dirty\n")
            with self.assertRaises(ReleaseError):
                tool.preflight(target, branch="main", publish=False, remote="origin")
            run(root, "git", "restore", "version.properties")
            (root / "version.properties").write_text("VERSION_NAME=1.1.2\nVERSION_CODE=2\n")
            run(root, "git", "add", "version.properties")
            with self.assertRaises(ReleaseError):
                tool.preflight(target, branch="main", publish=False, remote="origin")
            run(root, "git", "restore", "--staged", "version.properties")
            run(root, "git", "restore", "version.properties")
            with self.assertRaises(ReleaseError):
                tool.preflight(target, branch="release", publish=False, remote="origin")
            run(root, "git", "tag", "-a", "v1.1.3", "-m", "existing")
            with self.assertRaises(ReleaseError):
                tool.preflight(target, branch="main", publish=False, remote="origin")

    def test_failed_validation_restores_managed_files_and_creates_no_tag(self):
        with self.repo() as root:
            class FailingRunner:
                def __init__(self, cwd):
                    self.real = ReleaseTool(cwd).runner

                def run(self, command, *, check=True, capture=True, stream_output=False):
                    if tuple(command) == ("fail-release-validation",):
                        raise ReleaseError("forced failure")
                    return self.real.run(command, check=check, capture=capture, stream_output=stream_output)

            before_version = (root / "version.properties").read_text()
            before_changelog = (root / CHANGELOG_FILE).read_text()
            tool = ReleaseTool(root, runner=FailingRunner(root), validation_commands=[("fail-release-validation",)])
            with self.assertRaises(ReleaseError):
                with contextlib.redirect_stdout(io.StringIO()):
                    tool.release(bump="patch", explicit=None, branch="main", notes="Release notes")
            self.assertEqual((root / "version.properties").read_text(), before_version)
            self.assertEqual((root / CHANGELOG_FILE).read_text(), before_changelog)
            self.assertEqual(run(root, "git", "tag", "--list", "v1.1.3"), "")

    def test_successful_release_creates_commit_annotated_tag_and_artifacts(self):
        with self.repo() as root:
            self.fake_apks(root)
            tool = ReleaseTool(root, today=dt.date(2026, 7, 13), validation_commands=[])
            with contextlib.redirect_stdout(io.StringIO()):
                tool.release(bump="patch", explicit=None, branch="main", notes="Release notes")
            self.assertIn("VERSION_NAME=1.1.3", (root / "version.properties").read_text())
            self.assertIn("VERSION_CODE=2", (root / "version.properties").read_text())
            self.assertIn("## 1.1.3 - 2026-07-13", (root / CHANGELOG_FILE).read_text())
            self.assertEqual(run(root, "git", "log", "-1", "--pretty=%s").strip(), "Release v1.1.3")
            self.assertEqual(run(root, "git", "cat-file", "-t", "v1.1.3").strip(), "tag")
            self.assertTrue((root / "dist/snooze-reviews-1.1.3-code2-debug.apk").is_file())
            self.assertTrue((root / "dist/snooze-reviews-1.1.3-code2-release-unsigned.apk.sha256").is_file())
            self.assertEqual(run(root, "git", "status", "--porcelain=v1", "--untracked-files=all"), "")

    def test_publish_current_pushes_release_commit_and_tag_to_bare_remote(self):
        with tempfile.TemporaryDirectory() as remote_tmp:
            remote = Path(remote_tmp) / "remote.git"
            run(Path(remote_tmp), "git", "init", "--bare", str(remote))
            with self.repo() as root:
                run(root, "git", "remote", "add", "origin", str(remote))
                run(root, "git", "push", "-u", "origin", "main")
                self.fake_apks(root)
                tool = ReleaseTool(root, today=dt.date(2026, 7, 13), validation_commands=[])
                with contextlib.redirect_stdout(io.StringIO()):
                    tool.release(bump="patch", explicit=None, branch="main", notes="Release notes")
                    tool.publish_current(remote="origin", branch="main")
                self.assertIn("refs/tags/v1.1.3", run(root, "git", "ls-remote", "--tags", "origin", "refs/tags/v1.1.3"))

    class repo:
        def __enter__(self):
            self.tmp = tempfile.TemporaryDirectory()
            self.root = Path(self.tmp.name)
            run(self.root, "git", "init", "-b", "main")
            run(self.root, "git", "config", "user.email", "test@example.com")
            run(self.root, "git", "config", "user.name", "Test User")
            (self.root / "app/build/outputs/apk/debug").mkdir(parents=True)
            (self.root / "app/build/outputs/apk/release").mkdir(parents=True)
            (self.root / "app/build.gradle").parent.mkdir(exist_ok=True)
            (self.root / "app/build.gradle").write_text("// test\n")
            (self.root / "gradlew").write_text("#!/bin/sh\n")
            (self.root / ".gitignore").write_text("dist/\n**/build/\n")
            write_version_file(self.root / "version.properties", VersionInfo(SemVer.parse("1.1.2"), 1))
            (self.root / CHANGELOG_FILE).write_text("# Changelog\n\n## Unreleased\n\n### Added\n\n### Changed\n\n### Fixed\n")
            run(self.root, "git", "add", ".")
            run(self.root, "git", "commit", "-m", "Initial")
            return self.root

        def __exit__(self, exc_type, exc, tb):
            self.tmp.cleanup()

    @staticmethod
    def fake_apks(root):
        (root / "app/build/outputs/apk/debug/app-debug.apk").write_bytes(b"debug")
        (root / "app/build/outputs/apk/release/app-release-unsigned.apk").write_bytes(b"release")


if __name__ == "__main__":
    unittest.main()
