"""Набор для Linux и macOS: dist/BaroCompanion-portable и dist/BaroCompanion-<версия>-linux-macos.zip.

Архив собирается здесь, а не через Compress-Archive: у скриптов запуска должны быть права на выполнение (0755),
иначе на Linux ./run-linux.sh не запустится, а macOS откажется открывать run-macos.command.

    py -I portable.py <BaroCompanion.jar> <версия> <папка dist>
"""
import os
import shutil
import sys
import time
import zipfile

here = os.path.dirname(os.path.abspath(__file__))
jar, version, dist = sys.argv[1], sys.argv[2], sys.argv[3]
scripts = ["run-linux.sh", "run-macos.command"]

folder = os.path.join(dist, "BaroCompanion-portable")
if os.path.isdir(folder):
    shutil.rmtree(folder)
os.makedirs(folder)

files = {"BaroCompanion.jar": open(jar, "rb").read()}
for name in scripts + ["README-portable.txt"]:
    data = open(os.path.join(here, name), "rb").read()
    if name in scripts:
        data = data.replace(b"\r\n", b"\n")  # CRLF ломает #!/bin/sh
    files[name] = data

archive = os.path.join(dist, "BaroCompanion-%s-linux-macos.zip" % version)
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
    for name, data in files.items():
        with open(os.path.join(folder, name), "wb") as f:
            f.write(data)
        info = zipfile.ZipInfo(name, date_time=time.localtime()[:6])
        info.create_system = 3  # Unix: права ниже читают unzip, «Архиватор» macOS и файловые менеджеры Linux
        info.external_attr = ((0o100755 if name in scripts else 0o100644) << 16)
        info.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(info, data)
print("Готово:", archive)
