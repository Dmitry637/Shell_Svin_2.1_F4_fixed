#!/usr/bin/env sh
set -eu
# Используем UTF-8 для русскоязычного вывода, если системная локаль это поддерживает.
if locale -a 2>/dev/null | grep -Eiq '^C[._]UTF-?8$'; then
    export LANG=C.UTF-8
    export LC_ALL=C.UTF-8
fi
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="$ROOT/out"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$ROOT/src/main/java" -name '*.java' -print > "$OUT/main-sources.txt"
find "$ROOT/tests/java" -name '*.java' -print > "$OUT/test-sources.txt"
javac --release 17 -encoding UTF-8 -d "$OUT/classes" @"$OUT/main-sources.txt"
javac --release 17 -encoding UTF-8 -cp "$OUT/classes" -d "$OUT/test-classes" @"$OUT/test-sources.txt"
java -cp "$OUT/classes:$OUT/test-classes" ru.example.dependencygraph.ConfigValidatorTest
