#!/bin/sh
# Сборка BaroCompanion.exe (на Linux: clang + lld + llvm-rc + llvm-dlltool, JDK 21 для javac и jni.h).
# RUNTIME — папка Java 21+ для Windows x64 (bin, conf, lib, legal из JDK).
set -e
J=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
RUNTIME=${1:?укажите папку Java для Windows}
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 21 -encoding UTF-8 -d build/classes $(find ../src -name '*.java')
mkdir -p build/classes/image && cp ../src/image/* build/classes/image/
java -cp build/classes baro.data.gamefiles.BestiaryReader ../src/data/bestiary.bin && java -cp build/classes baro.data.gamefiles.BestiaryReader ../src/data/bestiary.en.bin English || echo "игра не найдена — встроенный бестиарий не обновлён"
mkdir -p build/classes/data && cp -r ../src/data/* build/classes/data/
jar cfm build/BaroCompanion.jar ../manifest.txt -C build/classes .
for l in kernel32 user32 gdi32 shell32; do llvm-dlltool -m i386:x86-64 -d $l.def -l build/$l.lib; done
CF="--target=x86_64-pc-windows-msvc -O2 -ffreestanding -fno-stack-protector -fno-builtin -nostdinc -isystem $(clang -print-resource-dir)/include -Iwinmd -I$J/include"
llvm-rc -fo build/app.res app.rc
clang $CF -c launcher.c -o build/launcher.obj
clang $CF -c inflate.c -o build/inflate.obj
clang --target=x86_64-pc-windows-msvc -c chkstk.s -o build/chkstk.obj
lld-link /nologo /subsystem:windows,6.01 /entry:start /nodefaultlib /stack:0x800000,0x10000 /out:build/stub.exe \
  build/launcher.obj build/inflate.obj build/chkstk.obj build/app.res build/kernel32.lib build/user32.lib build/gdi32.lib build/shell32.lib
python3 pack.py build/stub.exe BaroCompanion.exe "$RUNTIME=runtime" build/BaroCompanion.jar=app/BaroCompanion.jar
python3 portable.py build/BaroCompanion.jar "$(sed -n 's/.*VERSION = "\([^"]*\)".*/\1/p' ../src/baro/App.java)" ../dist
