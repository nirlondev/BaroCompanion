#!/bin/sh
# Запуск Baro Companion на macOS (двойной щелчок в Finder). Нужна Java 21+.
# Рядом со скриптом должен лежать BaroCompanion.jar. Настройки — в ~/Library/Application Support/BaroCompanion.
set -eu
dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
jar="$dir/BaroCompanion.jar"

java=java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then java="$JAVA_HOME/bin/java"
elif [ -x /usr/libexec/java_home ]; then jh=$(/usr/libexec/java_home -v 21+ 2>/dev/null || true); [ -n "$jh" ] && java="$jh/bin/java"; fi
if ! command -v "$java" >/dev/null 2>&1; then
    echo "Не найдена Java. Установите Java 21 или новее (например: brew install openjdk@21)." >&2
    exit 1
fi
if [ ! -f "$jar" ]; then echo "Рядом нет BaroCompanion.jar: $jar" >&2; exit 1; fi

cfg="$HOME/Library/Application Support/BaroCompanion"
mkdir -p "$cfg"
exec "$java" -Xdock:name="Baro Companion" -Dbaro.settings="$cfg/settings.properties" -jar "$jar" "$@"
