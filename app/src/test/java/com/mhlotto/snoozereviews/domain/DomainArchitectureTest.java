package com.mhlotto.snoozereviews.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DomainArchitectureTest {
    @Test
    public void domainSourcesDoNotImportAndroidRoomOrMaterialPackages() throws IOException {
        Path domainRoot = findDomainSourceRoot();
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(domainRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> scan(path, violations));
        }

        assertTrue("Forbidden domain imports:\n" + String.join("\n", violations), violations.isEmpty());
    }

    private Path findDomainSourceRoot() {
        Path moduleRelative = Paths.get("src/main/java/com/mhlotto/snoozereviews/domain");
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        Path rootRelative = Paths.get("app/src/main/java/com/mhlotto/snoozereviews/domain");
        assertTrue("Domain source directory not found", Files.exists(rootRelative));
        return rootRelative;
    }

    private void scan(Path path, List<String> violations) {
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("import ")) {
                    continue;
                }
                assertFalse(trimmed.contains("*"));
                if (trimmed.startsWith("import android.")
                        || trimmed.startsWith("import androidx.")
                        || trimmed.startsWith("import com.google.android.material.")) {
                    violations.add(path + ": " + trimmed);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan " + path, exception);
        }
    }
}
