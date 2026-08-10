package com.accsaber.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvExampleTest {

    private static final List<String> CONFIG_FILES = List.of(
            "src/main/resources/application.yml",
            "src/main/resources/application-dev.yml",
            "src/main/resources/application-prod.yml");

    private static final List<String> COMPOSE_FILES = List.of(
            "docker-compose.yml",
            "docker-compose.prod.yml");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)[:}]");
    private static final Pattern COMPOSE_PLACEHOLDER = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)[:\\-}]");
    private static final Pattern DECLARATION = Pattern.compile("^([A-Z][A-Z0-9_]*)=", Pattern.MULTILINE);
    private static final Pattern README_ROW = Pattern.compile("^\\| `([A-Z][A-Z0-9_]*)`", Pattern.MULTILINE);

    private static final Set<String> INJECTED_BY_COMPOSE = Set.of(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD");

    private static final Set<String> COMPOSE_BUILT_INS = Set.of("COMPOSE_PROFILES");

    @Test
    @DisplayName(".env.example declares every variable the application and compose files read")
    void everyReadVariableIsDocumented() throws IOException {
        Set<String> missing = new LinkedHashSet<>(referencedVariables());
        missing.removeAll(declaredVariables());
        missing.removeAll(INJECTED_BY_COMPOSE);

        assertThat(missing)
                .as("variables read at runtime but absent from .env.example")
                .isEmpty();
    }

    @Test
    @DisplayName(".env.example declares nothing the application has stopped reading")
    void everyDocumentedVariableIsRead() throws IOException {
        Set<String> stale = new LinkedHashSet<>(declaredVariables());
        stale.removeAll(referencedVariables());
        stale.removeAll(COMPOSE_BUILT_INS);

        assertThat(stale)
                .as("variables in .env.example that nothing reads any more")
                .isEmpty();
    }

    @Test
    @DisplayName("the README configuration table matches .env.example exactly")
    void readmeMatchesEnvExample() throws IOException {
        Set<String> documented = new LinkedHashSet<>();
        collect(read("README.md"), README_ROW, documented);

        Set<String> declared = declaredVariables();

        Set<String> undocumented = new LinkedHashSet<>(declared);
        undocumented.removeAll(documented);

        Set<String> orphaned = new LinkedHashSet<>(documented);
        orphaned.removeAll(declared);

        assertThat(undocumented)
                .as("variables in .env.example with no row in the README table")
                .isEmpty();
        assertThat(orphaned)
                .as("README rows for variables that are no longer in .env.example")
                .isEmpty();
    }

    private Set<String> referencedVariables() throws IOException {
        Set<String> found = new LinkedHashSet<>();
        for (String file : CONFIG_FILES) {
            collect(read(file), PLACEHOLDER, found);
        }
        for (String file : COMPOSE_FILES) {
            collect(read(file), COMPOSE_PLACEHOLDER, found);
        }
        return found;
    }

    private Set<String> declaredVariables() throws IOException {
        Set<String> found = new LinkedHashSet<>();
        collect(read(".env.example"), DECLARATION, found);
        return found;
    }

    private static void collect(String content, Pattern pattern, Set<String> into) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            into.add(matcher.group(1));
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(projectRoot().resolve(relativePath));
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("could not locate the project root from " + Path.of("").toAbsolutePath());
        }
        return current;
    }
}
