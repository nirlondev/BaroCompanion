# Сборка BaroCompanion.exe на Windows без clang: компиляция, тесты, jar и перепаковка готового exe
# (заготовка и встроенная Java берутся из предыдущего exe, меняется только программа). Полная сборка с нуля — build.sh.
#
#   powershell -ExecutionPolicy Bypass -File native\build.ps1              — собрать и обновить native\BaroCompanion.exe
#   ... -SkipTests                                                          — без тестов
#   ... -Base C:\путь\BaroCompanion.exe                                   — взять заготовку и Java из другого exe
param(
    [switch]$SkipTests,
    [string]$Base = ""
)
$ErrorActionPreference = "Stop"
$native = $PSScriptRoot
$root = Split-Path $native -Parent
$exe = Join-Path $native "BaroCompanion.exe"
$build = Join-Path $native "build"
if ($Base -eq "") { $Base = $exe }
if (-not (Test-Path $Base)) { throw "Нет готового exe-основы: $Base. Укажите -Base (например, скачанный BaroCompanion.exe) или соберите через build.sh." }

# JDK: JAVA_HOME или самый новый из ~\.jdks
$jdk = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) { $jdk = $env:JAVA_HOME }
else {
    $jdk = Get-ChildItem (Join-Path $env:USERPROFILE ".jdks") -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName "bin\javac.exe") } | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $jdk) { throw "Не найден JDK 21+ (задайте JAVA_HOME)" }
$javac = Join-Path $jdk "bin\javac.exe"; $jar = Join-Path $jdk "bin\jar.exe"; $java = Join-Path $jdk "bin\java.exe"
$py = (Get-Command py -ErrorAction SilentlyContinue).Source
if (-not $py) { $py = (Get-Command python -ErrorAction SilentlyContinue).Source }
if (-not $py) { throw "Не найден Python 3 (py или python)" }

function Run($what, [scriptblock]$cmd) {
    Write-Host "== $what"
    & $cmd
    if ($LASTEXITCODE -ne 0) { throw "$what — ошибка (код $LASTEXITCODE)" }
}

$classes = Join-Path $build "classes"; $tclasses = Join-Path $build "test-classes"
if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force (Join-Path $classes "image"), $tclasses | Out-Null
$srcList = Join-Path $build "src.txt"; $testList = Join-Path $build "test.txt"
Get-ChildItem -Recurse (Join-Path $root "src") -Filter *.java | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } | Out-File -Encoding ascii $srcList
Run "Компиляция" { & $javac --release 21 -encoding UTF-8 -d $classes "@$srcList" }
Copy-Item (Join-Path $root "src\image\*") (Join-Path $classes "image")
# встроенный бестиарий и миссии: обновить из установленной игры (только ваниль), если она найдена; иначе остаётся прежний
$bestiary = Join-Path $root "src\data\bestiary.bin"
Write-Host "== Бестиарий из файлов игры"
& $java "-Dstdout.encoding=UTF-8" -cp $classes baro.data.gamefiles.BestiaryReader $bestiary
if ($LASTEXITCODE -ne 0) { Write-Host "   игра не найдена — встроенный бестиарий не обновлён" }
else {
    # английская копия — из английских текстов игры (для английского интерфейса без игры)
    & $java "-Dstdout.encoding=UTF-8" -cp $classes baro.data.gamefiles.BestiaryReader (Join-Path $root "src\data\bestiary.en.bin") English
}
New-Item -ItemType Directory -Force (Join-Path $classes "data") | Out-Null
Copy-Item -Recurse -Force (Join-Path $root "src\data\*") (Join-Path $classes "data")
$jarFile = Join-Path $build "BaroCompanion.jar"
Run "jar" { & $jar cfm $jarFile (Join-Path $root "manifest.txt") -C $classes . }

if (-not $SkipTests) {
    Get-ChildItem -Recurse (Join-Path $root "test") -Filter *.java | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } | Out-File -Encoding ascii $testList
    Run "Компиляция тестов" { & $javac --release 21 -encoding UTF-8 -cp $jarFile -d $tclasses "@$testList" }
    Run "Тесты" { & $java "-Dstdout.encoding=UTF-8" -cp "$jarFile;$tclasses" baro.AllTests }
}

Run "Перепаковка exe" { & $py -I (Join-Path $native "repack.py") $Base $jarFile $exe }
$ver = Select-String -Path (Join-Path $root "src\baro\App.java") -Pattern 'VERSION = "([^"]+)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
Run "Набор для Linux и macOS" { & $py -I (Join-Path $native "portable.py") $jarFile $ver (Join-Path $root "dist") }
Write-Host ""
Write-Host "Готово: $exe (версия $ver)"
Write-Host "Чтобы новая копия при запуске заменила уже запущенную, версия в App.VERSION должна отличаться от прежней."
