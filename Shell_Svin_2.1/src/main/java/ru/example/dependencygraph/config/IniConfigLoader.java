package ru.example.dependencygraph.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
public final class IniConfigLoader {
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "package_name",
            "repository_mode",
            "repository",
            "package_version",
            "graph_output_file"
    );
    private IniConfigLoader() {
    }
    public static IniLoadResult load(Path file) {
        Map<String, String> values = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        Path absoluteFile = file.toAbsolutePath().normalize();
        if (!validateFile(absoluteFile, errors)) {
            return new IniLoadResult(values, errors);
        }
        List<String> lines = readLines(absoluteFile, errors);
        if (lines == null) {
            return new IniLoadResult(values, errors);
        }
        ParseState state = new ParseState();
        for (int index = 0; index < lines.size(); index++) {
            parseLine(lines.get(index), index + 1, values, errors, state);
        }
        if (!state.appSectionFound) {
            errors.add("В INI-файле отсутствует секция [app].");
        }
        return new IniLoadResult(values, errors);
    }
    private static boolean validateFile(Path absoluteFile, List<String> errors) {
        if (!Files.exists(absoluteFile)) {
            errors.add("INI-файл не найден: " + absoluteFile);
            return false;
        }
        if (!Files.isRegularFile(absoluteFile) || !Files.isReadable(absoluteFile)) {
            errors.add("INI-путь должен указывать на доступный для чтения файл: " + absoluteFile);
            return false;
        }
        return true;
    }
    private static List<String> readLines(Path absoluteFile, List<String> errors) {
        try {
            return Files.readAllLines(absoluteFile, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            errors.add("Не удалось прочитать INI-файл: " + exception.getMessage());
            return null;
        }
    }
    private static void parseLine(
            String rawLine,
            int lineNumber,
            Map<String, String> values,
            List<String> errors,
            ParseState state
    ) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
            return;
        }
        if (isSectionHeader(line)) {
            parseSection(line, lineNumber, errors, state);
            return;
        }
        if (!state.inAppSection) {
            errors.add("Строка " + lineNumber + ": параметр должен находиться внутри секции [app].");
            return;
        }
        parseKeyValue(line, lineNumber, values, errors);
    }
    private static boolean isSectionHeader(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }
    private static void parseSection(
            String line,
            int lineNumber,
            List<String> errors,
            ParseState state
    ) {
        String section = line.substring(1, line.length() - 1).trim();
        state.inAppSection = "app".equals(section);
        if (state.inAppSection) {
            state.appSectionFound = true;
        } else {
            errors.add("Строка " + lineNumber + ": неизвестная секция [" + section
                    + "]; ожидается [app].");
        }
    }
    private static void parseKeyValue(
            String line,
            int lineNumber,
            Map<String, String> values,
            List<String> errors
    ) {
        int separator = line.indexOf('=');
        if (separator < 1) {
            errors.add("Строка " + lineNumber + ": ожидалась запись key = value.");
            return;
        }
        String key = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        if (!ALLOWED_KEYS.contains(key)) {
            errors.add("Строка " + lineNumber + ": неизвестный параметр '" + key + "'.");
            return;
        }
        if (values.containsKey(key)) {
            errors.add("Строка " + lineNumber + ": параметр '" + key + "' указан повторно.");
            return;
        }
        values.put(key, value);
    }
    private static final class ParseState {
        private boolean inAppSection;
        private boolean appSectionFound;
    }
}