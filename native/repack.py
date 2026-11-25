"""Новый exe из готового: та же заготовка и тот же runtime (байт в байт), новый jar программы.
Нужен, когда нет clang/lld (сборка на Windows): заготовка и Java меняются редко, программа — часто.
Формат — как в pack.py.

  python repack.py <старый.exe> <новый.jar> <выход.exe>     (выход может совпадать со старым)
"""
import os, sys, struct, zlib, hashlib

CHUNK = 4 << 20
JAR_NAME = 'app/BaroCompanion.jar'
OLD_JAR_NAMES = ['app/BaroSpravochnik.jar']  # в exe до переименования программы в Baro Companion


def _same_len(was, now):
    """Строка с нулём в конце: новая короче — добиваем нулями до прежней длины (после нуля код ничего не читает)."""
    assert len(now) <= len(was), now
    return was, now + b'\0' * (len(was) - len(now))


def _wide(was, now):
    return _same_len((was + '\0').encode('utf-16-le'), (now + '\0').encode('utf-16-le'))


# Правки запускателя без смены длины: в launcher.c и app.rc они уже внесены, а заготовку без clang не пересобрать.
# Порядок важен: сначала пути, потом короткое «BaroSpravochnik» из сведений о версии.
STUB_FIXES = [
    (b'-Xmx512m\0', b'-Xmx256m\0'),
    _wide('\\BaroSpravochnik', '\\BaroCompanion'),
    _wide('\\app\\BaroSpravochnik.jar', '\\app\\BaroCompanion.jar'),
    _wide('Справочник Barotrauma\nПервый запуск: подготовка… 0%', 'Baro Companion\nПервый запуск: подготовка… 0%'),
    _wide('Справочник Barotrauma\nПервый запуск: подготовка… ', 'Baro Companion\nПервый запуск: подготовка… '),
    _wide('Справочник Barotrauma', 'Baro Companion'),
    _wide('Spravochnik Barotrauma', 'Baro Companion'),
    _wide('BaroSpravochnik.exe', 'BaroCompanion.exe'),
    _wide('BaroSpravochnik', 'BaroCompanion'),
    (b'name="BaroSpravochnik"', b'name="BaroCompanion"  '),  # манифест: лишние пробелы между атрибутами XML допустимы
]
STUB_LEFTOVERS = ['BaroSpravochnik', 'Spravochnik', 'Справочник Barotrauma']


def entry(name, raw):
    chunks = [raw[i:i + CHUNK] for i in range(0, len(raw), CHUNK)] or [b'']
    b = struct.pack('<H', len(name)) + name + struct.pack('<QI', len(raw), len(chunks))
    for ch in chunks:
        c = zlib.compressobj(9, zlib.DEFLATED, -15)
        z = c.compress(ch) + c.flush()
        if len(z) >= len(ch): z = ch
        b += struct.pack('<II', len(z), len(ch)) + z
    return b


def read(path):
    """Заготовка и записи полезной нагрузки: [(имя, байты записи как есть, распакованное содержимое)]."""
    data = open(path, 'rb').read()
    start, plen, key, n = struct.unpack('<QQII', data[-32:-8])
    if data[-8:] != b'BAROPAK1' or start + plen + 32 != len(data): raise SystemExit(path + ': это не exe Baro Companion')
    payload = data[start:start + plen]
    if int.from_bytes(hashlib.sha1(payload).digest()[:4], 'little') != key: raise SystemExit(path + ': повреждён (ключ не совпал)')
    items, pos = [], 0
    while pos < len(payload):
        begin = pos
        (l,) = struct.unpack_from('<H', payload, pos); pos += 2
        name = payload[pos:pos + l].decode('utf-8'); pos += l
        size, cnt = struct.unpack_from('<QI', payload, pos); pos += 12
        raw = bytearray()
        for _ in range(cnt):
            zl, rl = struct.unpack_from('<II', payload, pos); pos += 8
            p = payload[pos:pos + zl]; pos += zl
            raw += p if zl == rl else zlib.decompress(p, -15)
        if len(raw) != size: raise SystemExit(path + ': повреждена запись ' + name)
        items.append((name, payload[begin:pos], bytes(raw)))
    if len(items) != n: raise SystemExit(path + ': число файлов не совпало')
    return data[:start], items


def repack(old, jar, out):
    stub, items = read(old)
    for was, now in STUB_FIXES: stub = stub.replace(was, now)
    for s in STUB_LEFTOVERS:
        if s.encode('utf-16-le') in stub or s.encode('utf-8') in stub: raise SystemExit(old + ': в заготовке осталось «' + s + '»')
    jar_bytes = open(jar, 'rb').read()
    new, replaced = bytearray(), False
    for name, rec, _ in items:
        if name == JAR_NAME or name in OLD_JAR_NAMES: new += entry(JAR_NAME.encode('utf-8'), jar_bytes); replaced = True
        else: new += rec
    if not replaced: raise SystemExit(old + ': внутри нет ' + JAR_NAME)
    key = int.from_bytes(hashlib.sha1(bytes(new)).digest()[:4], 'little')
    tmp = out + '.tmp'
    with open(tmp, 'wb') as f:
        f.write(stub); f.write(new)
        f.write(struct.pack('<QQII', len(stub), len(new), key, len(items)) + b'BAROPAK1')
    # проверка: заготовка и runtime те же, jar — новый
    stub2, items2 = read(tmp)
    jar_name = lambda n: JAR_NAME if n in OLD_JAR_NAMES else n
    assert stub2 == stub and [n for n, _, _ in items2] == [jar_name(n) for n, _, _ in items]
    for (n1, _, r1), (n2, _, r2) in zip(items, items2):
        assert (r2 == jar_bytes) if n2 == JAR_NAME else (r1 == r2), n1
    try: os.replace(tmp, out)
    except PermissionError:
        os.remove(tmp)
        raise SystemExit(out + ': файл занят — закройте запущенный из него Baro Companion и соберите снова')
    print('%s: файлов %d (runtime %d + jar), %.1f MB, ключ %08x' % (out, len(items), len(items) - 1, len(new) / 1e6, key))


if __name__ == '__main__':
    if len(sys.argv) != 4: raise SystemExit(__doc__)
    repack(*sys.argv[1:])
