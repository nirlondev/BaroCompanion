#!/bin/sh
# Запуск Baro Companion на Linux. Нужна Java 21+ (java в PATH или $JAVA_HOME).
# Рядом со скриптом должен лежать BaroCompanion.jar. Настройки — в ~/.config/BaroCompanion.
set -eu
dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
jar="$dir/BaroCompanion.jar"

java=java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then java="$JAVA_HOME/bin/java"; fi
if ! command -v "$java" >/dev/null 2>&1; then
    echo "Не найдена Java. Установите Java 21 или новее (например: sudo apt install openjdk-21-jre)." >&2
    exit 1
fi
if [ ! -f "$jar" ]; then echo "Рядом нет BaroCompanion.jar: $jar" >&2; exit 1; fi

cfg="${XDG_CONFIG_HOME:-$HOME/.config}/BaroCompanion"
mkdir -p "$cfg"
exec "$java" -Dbaro.settings="$cfg/settings.properties" -jar "$jar" "$@"
