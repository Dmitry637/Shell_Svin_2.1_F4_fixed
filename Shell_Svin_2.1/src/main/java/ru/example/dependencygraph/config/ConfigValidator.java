package ru.example.dependencygraph.config;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
public final class ConfigValidator {
    private static final Pattern PACKAGE_NAME = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]$|^[A-Za-z0-9]$"
    );
    private static final Pattern VERSION = Pattern.compile(
            "^(?:[0-9]+\\.)*[0-9]+(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?$"
    );
    private ConfigValidator() {
    }
    public static List<String> validate(Map<String, String> values, Path configFile) {
        List<String> errors = new ArrayList<>();
        Path configDirectory = configFile.toAbsolutePath().normalize().getParent();
        validatePackageName(values, errors);
        boolean validMode = validateRepositoryMode(values, errors);
        validateRepository(values, configDirectory, validMode, errors);
        validatePackageVersion(values, errors);
        validateGraphOutputFile(values, configDirectory, errors);

        return List.copyOf(errors);
    }
    private static void validatePackageName(Map<String, String> values, List<String> errors) {
        String packageName = value(values, "package_name");
        if (isBlank(packageName)) {
            errors.add("Параметр 'package_name' обязателен.");
        } else if (!PACKAGE_NAME.matcher(packageName).matches()) {
            errors.add("Параметр 'package_name' имеет неверный формат: используйте буквы, "
                    + "цифры, '.', '_' или '-', без пробелов и разделителей по краям.");
        }
    }
    private static boolean validateRepositoryMode(Map<String, String> values, List<String> errors) {
        String mode = value(values, "repository_mode");
        if (isBlank(mode)) {
            errors.add("Параметр 'repository_mode' обязателен; допустимые значения: local, remote.");
            return false;
        } else if (!(mode.equals("local") || mode.equals("remote"))) {
            errors.add("Параметр 'repository_mode' должен быть 'local' или 'remote'.");
            return false;
        }
        return true;
    }
    private static void validateRepository(
            Map<String, String> values, Path configDirectory, boolean validMode, List<String> errors) {
        String repository = value(values, "repository");
        if (isBlank(repository)) {
            errors.add("Параметр 'repository' обязателен: укажите путь к файлу или URL репозитория.");
        } else if (validMode) {
            if (value(values, "repository_mode").equals("local")) {
                validateLocalRepository(repository, configDirectory, errors);
            } else {
                validateRemoteRepository(repository, errors);
            }
        }
    }
    private static void validatePackageVersion(Map<String, String> values, List<String> errors) {
        String version = value(values, "package_version");
        if (isBlank(version)) {
            errors.add("Параметр 'package_version' обязателен.");
        } else if (!VERSION.matcher(version).matches()) {
            errors.add("Параметр 'package_version' имеет неверный формат; "
                    + "пример допустимой версии: 1.2.3 или 1.2.3-SNAPSHOT.");
        }
    }
    private static void validateGraphOutputFile(
            Map<String, String> values, Path configDirectory, List<String> errors) {
        String graphOutput = value(values, "graph_output_file");
        if (isBlank(graphOutput)) {
            errors.add("Параметр 'graph_output_file' обязателен.");
        } else {
            validateGraphOutput(graphOutput, configDirectory, errors);
        }
    }
    private static void validateLocalRepository(String repository, Path configDirectory, List<String> errors) {
        try {
            Path path = resolvePath(repository, configDirectory);
            if (!Files.exists(path)) {
                errors.add("Параметр 'repository': файл тестового репозитория не найден: " + path);
            } else if (!Files.isRegularFile(path)) {
                errors.add("Параметр 'repository': локальный репозиторий должен быть файлом, "
                        + "а не каталогом: " + path);
            } else if (!Files.isReadable(path)) {
                errors.add("Параметр 'repository': нет прав на чтение файла: " + path);
            }
        } catch (InvalidPathException exception) {
            errors.add("Параметр 'repository' содержит недопустимый путь: " + exception.getInput());
        }
    }
    private static void validateRemoteRepository(String repository, List<String> errors) {
        try {
            URI uri = new URI(repository);
            String scheme = uri.getScheme();
            boolean validScheme = scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            if (!validScheme || uri.getHost() == null || uri.getHost().isBlank()) {
                errors.add("Параметр 'repository' в режиме remote должен быть корректным HTTP(S)-URL, "
                        + "например https://example.org/repository.");
            }
        } catch (URISyntaxException exception) {
            errors.add("Параметр 'repository' содержит некорректный URL: " + exception.getMessage());
        }
    }
    private static void validateGraphOutput(String graphOutput, Path configDirectory, List<String> errors) {
        if (!graphOutput.toLowerCase().endsWith(".png")) {
            errors.add("Параметр 'graph_output_file' должен указывать на PNG-файл с расширением .png.");
            return;
        }
        try {
            Path output = resolvePath(graphOutput, configDirectory);
            Path parent = output.getParent();
            if (parent == null || !Files.isDirectory(parent)) {
                errors.add("Параметр 'graph_output_file': каталог для результата не существует: "
                        + parent + ". Создайте его заранее.");
            } else if (!Files.isWritable(parent)) {
                errors.add("Параметр 'graph_output_file': нет прав на запись в каталог: " + parent);
            } else {
                validateOutputFile(output, errors);
            }
        } catch (InvalidPathException exception) {
            errors.add("Параметр 'graph_output_file' содержит недопустимый путь: " + exception.getInput());
        }
    }
    private static void validateOutputFile(Path output, List<String> errors) {
        if (Files.exists(output) && Files.isDirectory(output)) {
            errors.add("Параметр 'graph_output_file' указывает на каталог, а не на файл: " + output);
        } else if (Files.exists(output) && !Files.isWritable(output)) {
            errors.add("Параметр 'graph_output_file': существующий файл недоступен для записи: " + output);
        }
    }
    private static Path resolvePath(String pathStr, Path configDirectory) {
        Path path = Path.of(pathStr);
        if (!path.isAbsolute()) {
            path = configDirectory.resolve(path);
        }
        return path.normalize();
    }
    private static String value(Map<String, String> values, String key) {
        return values.get(key);
    }
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}