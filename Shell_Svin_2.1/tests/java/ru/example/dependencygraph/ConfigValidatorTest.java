package ru.example.dependencygraph;
import ru.example.dependencygraph.config.ConfigValidator;
import ru.example.dependencygraph.config.IniConfigLoader;
import ru.example.dependencygraph.config.IniLoadResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public final class ConfigValidatorTest {
    private int passed;
    public static void main(String[] args) throws Exception {
        new ConfigValidatorTest().runAll();
    }
    private void runAll() throws Exception {
        Path temp = Files.createTempDirectory("dependency-graph-test-");
        Path repo = temp.resolve("repo.txt");
        Files.writeString(repo, "test repository", StandardCharsets.UTF_8);
        runValidConfigTests(temp, repo);
        runInvalidConfigTests(temp, repo);
        runIniParserTests(temp);
        runCliTests(temp);
        System.out.println("Тесты пройдены: " + passed);
        System.out.println("Все проверки завершились успешно.");
    }
    private void runValidConfigTests(Path temp, Path repo) throws Exception {
        test("valid configuration", () -> {
            List<String> errors = ConfigValidator.validate(
                    validValues(repo),
                    temp.resolve("config.ini")
            );
            assertTrue(
                    errors.isEmpty(),
                    "valid configuration should have no errors: " + errors
            );
        });
    }
    private void runInvalidConfigTests(Path temp, Path repo) throws Exception {
        runInvalidPackageAndModeTests();
        runInvalidRepositoryTests(temp, repo);
        runInvalidVersionAndOutputTests();
    }

    private void runInvalidPackageAndModeTests() throws Exception {
        test("missing package name", () ->
                assertContains(
                        validateWith("package_name", ""),
                        "package_name"
                ).run()
        );
        test("invalid package name", () ->
                assertContains(
                        validateWith("package_name", "bad name"),
                        "package_name"
                ).run()
        );
        test("missing repository mode", () ->
                assertContains(
                        validateWith("repository_mode", ""),
                        "repository_mode"
                ).run()
        );
        test("invalid repository mode", () ->
                assertContains(
                        validateWith("repository_mode", "ftp"),
                        "repository_mode"
                ).run()
        );
    }

    private void runInvalidRepositoryTests(Path temp, Path repo) throws Exception {
        test("missing repository", () ->
                assertContains(
                        validateWith("repository", ""),
                        "repository"
                ).run()
        );
        test("missing local repository file", () ->
                assertContains(
                        validateWith("repository", "missing.txt"),
                        "repository"
                ).run()
        );
        test("invalid remote URL", () -> {
            Map<String, String> values = validValues(repo);
            values.put("repository_mode", "remote");
            values.put("repository", "not-a-url");
            assertContains(
                    ConfigValidator.validate(values, temp.resolve("config.ini")),
                    "repository"
            ).run();
        });
    }

    private void runInvalidVersionAndOutputTests() throws Exception {
        test("missing package version", () ->
                assertContains(
                        validateWith("package_version", ""),
                        "package_version"
                ).run()
        );
        test("invalid package version", () ->
                assertContains(
                        validateWith("package_version", "v-next"),
                        "package_version"
                ).run()
        );
        test("missing graph output", () ->
                assertContains(
                        validateWith("graph_output_file", ""),
                        "graph_output_file"
                ).run()
        );
        test("invalid graph output extension", () ->
                assertContains(
                        validateWith("graph_output_file", "graph.svg"),
                        "graph_output_file"
                ).run()
        );
    }

    private void runIniParserTests(Path temp) throws Exception {
        test("INI unknown key", () -> {
            Path ini = temp.resolve("unknown-key.ini");
            String content = "[app]\nunknown=value\n";
            Files.writeString(ini, content, StandardCharsets.UTF_8);

            IniLoadResult result = IniConfigLoader.load(ini);
            assertTrue(
                    result.errors().stream()
                            .anyMatch(error -> error.contains("неизвестный параметр")),
                    "unknown key should be rejected"
            );
        });
        test("INI duplicate key", () -> {
            Path ini = temp.resolve("duplicate.ini");
            String content = "[app]\npackage_name=one\npackage_name=two\n";
            Files.writeString(ini, content, StandardCharsets.UTF_8);

            IniLoadResult result = IniConfigLoader.load(ini);
            assertTrue(
                    result.errors().stream()
                            .anyMatch(error -> error.contains("повторно")),
                    "duplicate key should be rejected"
            );
        });
    }
    private void runCliTests(Path temp) throws Exception {
        test("CLI prints all parameters", () -> {
            Path ini = temp.resolve("valid.ini");
            String iniContent = "[app]\n" +
                    "package_name=demo\n" +
                    "repository_mode=local\n" +
                    "repository=repo.txt\n" +
                    "package_version=1.2.3\n" +
                    "graph_output_file=out.png\n";
            Files.writeString(ini, iniContent, StandardCharsets.UTF_8);
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            int code = Main.run(
                    new String[]{ini.toString()},
                    new PrintStream(stdout),
                    new PrintStream(stderr)
            );
            String output = stdout.toString(StandardCharsets.UTF_8);
            assertTrue(
                    code == 0,
                    "CLI should return 0, errors=" +
                            stderr.toString(StandardCharsets.UTF_8)
            );
            List<String> params = List.of(
                    "package_name=",
                    "repository=",
                    "repository_mode=",
                    "package_version=",
                    "graph_output_file="
            );
            for (String key : params) {
                assertTrue(
                        output.contains(key),
                        "missing printed parameter: " + key
                );
            }
        });
    }
    private Map<String, String> validValues(Path repo) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("package_name", "example-package");
        values.put("repository_mode", "local");
        values.put("repository", repo.toString());
        values.put("package_version", "1.2.3");
        values.put("graph_output_file", "dependencies.png");
        return values;
    }
    private List<String> validateWith(String key, String value) {
        try {
            Path temp = Files.createTempDirectory(
                    "dependency-graph-validation-"
            );
            Path repo = temp.resolve("repo.txt");
            Files.writeString(repo, "repo", StandardCharsets.UTF_8);
            Map<String, String> values = validValues(repo);
            values.put(key, value);
            return ConfigValidator.validate(
                    values,
                    temp.resolve("config.ini")
            );
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
    private Runnable assertContains(List<String> errors, String parameter) {
        return () -> assertTrue(
                errors.stream()
                        .anyMatch(error -> error.contains(parameter)),
                "expected validation error mentioning " + parameter +
                        ", got: " + errors
        );
    }
    private void test(String name, ThrowingRunnable body) throws Exception {
        try {
            body.run();
            passed++;
            System.out.println("[OK] " + name);
        } catch (Throwable failure) {
            System.err.println(
                    "[FAIL] " + name + ": " + failure.getMessage()
            );
            throw failure;
        }
    }
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}