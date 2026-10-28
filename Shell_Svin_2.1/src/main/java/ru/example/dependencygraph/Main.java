package ru.example.dependencygraph;
import ru.example.dependencygraph.config.ConfigValidator;
import ru.example.dependencygraph.config.IniConfigLoader;
import ru.example.dependencygraph.config.IniLoadResult;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
public final class Main {
    private static final List<String> DISPLAYED_KEYS = List.of(
            "package_name",
            "repository",
            "repository_mode",
            "package_version",
            "graph_output_file"
    );
    private Main() {
    }
    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != 0) {
            System.exit(code);
        }
    }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length > 1) {
            err.println("Ошибка: ожидался один необязательный аргумент — путь к INI-файлу.");
            err.println("Использование: java -jar dependency-graph-cli.jar [config.ini]");
            return 2;
        }
        Path configFile = args.length == 0 ? Path.of("config.ini") : Path.of(args[0]);
        configFile = configFile.toAbsolutePath().normalize();
        IniLoadResult loaded = IniConfigLoader.load(configFile);
        Map<String, String> values = loaded.values();
        out.println("Параметры конфигурации:");
        for (String key : DISPLAYED_KEYS) {
            String value = values.get(key);
            out.println(key + "=" + (value == null || value.isBlank() ? "<не задано>" : value));
        }
        List<String> validationErrors = ConfigValidator.validate(values, configFile);
        if (!loaded.errors().isEmpty() || !validationErrors.isEmpty()) {
            err.println("Обнаружены ошибки конфигурации:");
            for (String error : loaded.errors()) {
                err.println("- " + error);
            }
            for (String error : validationErrors) {
                err.println("- " + error);
            }
            return 1;
        }
        out.println();
        out.println("Конфигурация корректна.");
        out.println("Этап 1 завершён: параметры загружены и проверены.");
        out.println("Построение графа зависимостей будет реализовано на следующих этапах.");
        return 0;
    }
}