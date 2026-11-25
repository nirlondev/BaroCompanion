# Baro Companion

[Русская версия](README.ru.md)

A companion app for Barotrauma: items, prices, recipes, deconstruction, medicine, talents, submarines, wiring,
missions and the bestiary, plus a cargo planner and an in-game overlay (Shift+T). The interface is available in English and Russian.

## Download

Ready-to-use files are on the [Releases](https://github.com/nirlondev/BaroCompanion/releases/latest) page:

- **Windows**: `BaroCompanion.exe`. It's a single file with Java built in, so there's nothing to install.
  On first launch the app unpacks itself (≈2–5 s) into `%LOCALAPPDATA%\BaroCompanion` and starts instantly after that.
  To uninstall, delete the exe and the `%LOCALAPPDATA%\BaroCompanion` folder.
- **Linux and macOS**: `BaroCompanion-<version>-linux-macos.zip`. Requires Java 21+.
  Run `./run-linux.sh`, or double-click `run-macos.command`.
  Settings are stored in `~/.config/BaroCompanion` (Linux) and `~/Library/Application Support/BaroCompanion` (macOS).

The app finds the game in your Steam libraries on its own. It reads data from the game files and enabled mods, as well
as your latest save (difficulty, station, shop prices, reputation, missions). If the game isn't found, it uses built-in data.

## Settings (settings.properties)

```
game.dir=D:/SteamLibrary/steamapps/common/Barotrauma   game folder, if it isn't found automatically
game.mods=false                                         ignore mods
game.files=false                                        always use built-in data only
ui.language=en                                          English interface
hotkey=ctrl alt F1                                      custom overlay shortcut (default: "shift T")
ui.theme=light                                          light theme
ui.scale=1.25                                           font size (0.9–1.5)
save.auto=false                                         don't watch for new saves
update.check=false                                      don't check for updates
```

Most of these can also be changed in the Settings window.

## Source code

- `src/`: the Java 21 (Swing) app, covering the data model, game file and save readers, pricing, search and UI.
  Translations live in `src/data/lang/en.txt`, and the changelog is in `src/data/changelog.txt`.
- `native/`: the C launcher for the exe (DEFLATE unpacker, loads jvm.dll via JNI), the icon, the manifest and build scripts.

Building:

- Windows (needs JDK 21+ and Python): `powershell -ExecutionPolicy Bypass -File native\build.ps1 -SkipTests -Base path\to\BaroCompanion.exe`
  compiles the app and repacks the exe from a release (the launcher and bundled Java are taken from it).
- Linux: `native/build.sh` runs a full build (clang, lld, JDK).

Bundled Java: OpenJDK (GPLv2 + Classpath Exception; the license texts are in `runtime/legal` inside the app).

## Support

[Boosty](https://boosty.to/nirlond)
