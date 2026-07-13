.PHONY: help build debug test connected-test lint check clean install visual-assets \
	version release-check release-dry-run release release-publish release-patch \
	release-minor release-major release-publish-patch release-publish-minor \
	release-publish-major publish-current release-tool-test

ANDROID_STUDIO_JBR := /Applications/Android Studio.app/Contents/jbr/Contents/Home
ANDROID_STUDIO_JAVA := /Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/java

ifneq ($(wildcard $(ANDROID_STUDIO_JAVA)),)
GRADLE := JAVA_HOME="$(ANDROID_STUDIO_JBR)" ./gradlew
else
GRADLE := ./gradlew
endif

help:
	@printf "Available targets:\n"
	@printf "  build           Build the project\n"
	@printf "  debug           Create the debug APK\n"
	@printf "  test            Run local unit tests\n"
	@printf "  connected-test  Run instrumentation tests on a connected device or emulator\n"
	@printf "  lint            Run Android lint\n"
	@printf "  check           Run build, unit tests, and lint\n"
	@printf "  clean           Clean Gradle output\n"
	@printf "  install         Install the debug build on a connected device\n"
	@printf "  visual-assets   Generate Android visual assets from source artwork\n"
	@printf "  version         Show current release version and Git status\n"
	@printf "  release-patch   Run a complete local patch release\n"
	@printf "  release-minor   Run a complete local minor release\n"
	@printf "  release-major   Run a complete local major release\n"
	@printf "  release-publish-patch  Run and publish a patch release\n"
	@printf "  release-publish-minor  Run and publish a minor release\n"
	@printf "  release-publish-major  Run and publish a major release\n"
	@printf "  release VERSION=x.y.z  Run a complete local explicit-version release\n"
	@printf "  release-tool-test      Run release tooling unit tests\n"

build:
	$(GRADLE) build

debug:
	$(GRADLE) assembleDebug

test:
	$(GRADLE) testDebugUnitTest

connected-test:
	$(GRADLE) connectedDebugAndroidTest

lint:
	$(GRADLE) lintDebug

check: debug test lint

clean:
	$(GRADLE) clean

install:
	$(GRADLE) installDebug

visual-assets:
	./scripts/generate-visual-assets.sh

version:
	@python3 scripts/release.py status

release-check:
	@python3 scripts/release.py check --branch "$${RELEASE_BRANCH:-main}"

release-dry-run:
	@if [ -n "$(VERSION)" ]; then \
		python3 scripts/release.py dry-run --version "$(VERSION)"; \
	elif [ -n "$(BUMP)" ]; then \
		python3 scripts/release.py dry-run --bump "$(BUMP)"; \
	else \
		printf "Set BUMP=patch|minor|major or VERSION=x.y.z\n" >&2; \
		exit 2; \
	fi

release:
	@if [ -z "$(VERSION)" ]; then \
		printf "VERSION is required. Example: make release VERSION=1.3.0\n" >&2; \
		exit 2; \
	fi
	@python3 scripts/release.py release --version "$(VERSION)" --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-publish:
	@if [ -z "$(VERSION)" ]; then \
		printf "VERSION is required. Example: make release-publish VERSION=1.3.0\n" >&2; \
		exit 2; \
	fi
	@python3 scripts/release.py release --version "$(VERSION)" --publish --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-patch:
	@python3 scripts/release.py release --bump patch --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-minor:
	@python3 scripts/release.py release --bump minor --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-major:
	@python3 scripts/release.py release --bump major --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-publish-patch:
	@python3 scripts/release.py release --bump patch --publish --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-publish-minor:
	@python3 scripts/release.py release --bump minor --publish --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

release-publish-major:
	@python3 scripts/release.py release --bump major --publish --branch "$${RELEASE_BRANCH:-main}" --remote "$${REMOTE:-origin}"

publish-current:
	@python3 scripts/release.py publish-current --remote "$${REMOTE:-origin}" $${RELEASE_BRANCH:+--branch "$$RELEASE_BRANCH"}

release-tool-test:
	@python3 -m unittest discover -s scripts/tests -p 'test_*.py'
