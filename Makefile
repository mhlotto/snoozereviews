.PHONY: help build debug test connected-test lint check clean install visual-assets

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
