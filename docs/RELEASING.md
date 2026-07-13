# Releasing

Snooze Reviews uses one root-level version source:

```properties
VERSION_NAME=1.1.2
VERSION_CODE=1
```

`VERSION_NAME` is the user-facing semantic version. It must use three numeric parts: `MAJOR.MINOR.PATCH`.

`VERSION_CODE` is the Android integer version code. Every successful release increments it by exactly one. It is not derived from the semantic version.

Gradle reads both values from `version.properties`. If the file is missing or malformed, Gradle configuration fails instead of falling back to a default.

## Local releases

Use one of these commands for normal local releases:

```bash
make release-patch
make release-minor
make release-major
```

Examples:

```text
1.1.2 -> 1.1.3
1.1.2 -> 1.2.0
1.1.2 -> 2.0.0
```

For an explicit version:

```bash
make release VERSION=1.3.0
```

The release command validates the repository, bumps `version.properties`, prepares `CHANGELOG.md`, runs non-device checks, builds APKs, collects artifacts, creates a release commit, and creates an annotated Git tag.

It does not push to a remote.

## Release and publish

Use these commands when the local release should also be pushed:

```bash
make release-publish-patch
make release-publish-minor
make release-publish-major
```

For an explicit version:

```bash
make release-publish VERSION=1.3.0
```

Publish commands push the release branch and annotated tag with an atomic push. The default remote is `origin`; override it with:

```bash
make release-publish-patch REMOTE=my-remote
```

The default allowed release branch is `main`; override it with:

```bash
make release-patch RELEASE_BRANCH=my-release-branch
```

The release tooling never force-pushes, fetches, pulls, rebases, overwrites tags, or pushes unless a publish command is explicitly selected.

## Dry run and status

Show the current release state:

```bash
make version
```

Preview a release without modifying files:

```bash
make release-dry-run BUMP=patch
make release-dry-run VERSION=1.3.0
```

The dry run prints the proposed version, version code, tag, validation commands, managed files, artifact names, and whether publishing is enabled.

## Changelog

`CHANGELOG.md` keeps an `Unreleased` section:

```markdown
## Unreleased

### Added

### Changed

### Fixed
```

During release, the existing `Unreleased` content is moved into:

```markdown
## 1.2.0 - YYYY-MM-DD
```

A fresh empty `Unreleased` section is inserted at the top.

If `Unreleased` contains no meaningful release notes, pass a summary:

```bash
RELEASE_NOTES="Improve release workflow" make release-patch
```

The tool does not invent release notes.

## Validation

Release validation runs:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
git diff --check
```

Connected instrumentation tests are not run automatically. Device testing remains a manual release responsibility.

## Artifacts

Release artifacts are copied into `dist/`, which is ignored by Git.

Expected names include:

```text
snooze-reviews-1.2.0-code2-debug.apk
snooze-reviews-1.2.0-code2-release-unsigned.apk
```

If a signed release APK is produced by existing local signing configuration, it is named:

```text
snooze-reviews-1.2.0-code2-release.apk
```

Unsigned APKs are never labeled as signed. SHA-256 checksum files are written next to each copied APK.

The tooling does not create signing keys, manage signing secrets, or commit APK artifacts.

## Failure behavior

Before a release commit is created, the tool saves original copies of:

```text
version.properties
CHANGELOG.md
```

If validation, build, lint, or artifact collection fails before the release commit, those files are restored, release artifacts from the failed attempt are removed, and no commit, tag, or push occurs.

If a failure happens after a release commit or tag exists, the tool does not rewrite history. Inspect the repository and recover manually.

If remote publishing fails after a valid local release, retry with:

```bash
make publish-current
```

`publish-current` verifies the current version tag exists locally, is annotated, points to `HEAD`, and matches a release commit before pushing the branch and tag.

## Tool tests

Run release-tool unit tests with:

```bash
make release-tool-test
```
