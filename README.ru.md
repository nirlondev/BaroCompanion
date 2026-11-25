# Baro Companion

[English version](README.md)

Справочник-компаньон для Barotrauma: предметы, цены, рецепты, разбор, медицина, таланты, подлодки, проводка,
миссии и бестиарий, планировщик груза и окно поверх игры (Shift+T). Интерфейс на русском и английском.

## Скачать

Готовые файлы — на странице [Releases](https://github.com/nirlondev/BaroCompanion/releases/latest):

- **Windows** — `BaroCompanion.exe`: один файл, Java встроена, ничего ставить не нужно.
  При первом запуске программа распаковывает себя (≈2–5 с) в `%LOCALAPPDATA%\BaroCompanion`, дальше стартует сразу.
  Удалить: удалить exe и папку `%LOCALAPPDATA%\BaroCompanion`.
- **Linux и macOS** — `BaroCompanion-<версия>-linux-macos.zip`: нужна Java 21+.
  Запуск: `./run-linux.sh` или двойной щелчок по `run-macos.command`.
  Настройки — в `~/.config/BaroCompanion` (Linux) и `~/Library/Application Support/BaroCompanion` (macOS).

Программа сама находит игру в библиотеках Steam, читает данные из файлов игры и включённых модов, а также самый
свежий сейв (сложность, станция, цены магазина, репутация, миссии). Если игра не найдена — используются встроенные данные.

## Настройки (settings.properties)

```
game.dir=D:/SteamLibrary/steamapps/common/Barotrauma   папка игры, если она не нашлась сама
game.mods=false                                         не учитывать моды
game.files=false                                        всегда только встроенные данные
ui.language=en                                          английский интерфейс
hotkey=ctrl alt F1                                      своё сочетание для окна поверх игры (по умолчанию «shift T»)
ui.theme=light                                          светлая тема
ui.scale=1.25                                           размер шрифта (0.9–1.5)
save.auto=false                                         не следить за сейвами
update.check=false                                      не проверять обновления
```

Большинство из них можно задать в окне «Настройки».

## Исходники

- `src/` — программа на Java 21 (Swing): модель, чтение файлов игры и сейвов, цены, поиск, интерфейс.
  Переводы — `src/data/lang/en.txt`, история изменений — `src/data/changelog.txt`.
- `native/` — запускатель exe на C (распаковщик DEFLATE, загрузка jvm.dll через JNI), иконка, манифест и скрипты сборки.

Сборка:

- Windows (нужен JDK 21+ и Python): `powershell -ExecutionPolicy Bypass -File native\build.ps1 -SkipTests -Base путь\к\BaroCompanion.exe`
  — компилирует программу и перепаковывает готовый exe из релиза (заготовка и встроенная Java берутся из него).
- Linux: `native/build.sh` — полная сборка (clang, lld, JDK).

Встроенная Java: OpenJDK (GPLv2 + Classpath Exception, тексты лицензий — в `runtime/legal` внутри программы).

## Поддержать

[Boosty](https://boosty.to/nirlond)
