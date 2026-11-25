"""Упаковывает файлы (runtime + приложение) в полезную нагрузку и приклеивает её к exe-заготовке.

Формат (little-endian):
  записи: u16 len, путь (UTF-8, '/'), u64 размер, u32 число кусков, куски: u32 сжато, u32 исходно, данные
          (если сжато == исходно — кусок хранится как есть, иначе raw deflate)
  хвост (32 байта): u64 начало, u64 длина, u32 ключ, u32 число файлов, b'BAROPAK1'
"""
import os, sys, struct, zlib, hashlib

CHUNK = 4 << 20

def entries(spec):
    for root, prefix in spec:
        if os.path.isfile(root):
            yield root, prefix
            continue
        for d, _, fs in os.walk(root):
            for f in sorted(fs):
                full = os.path.join(d, f)
                rel = os.path.relpath(full, root).replace(os.sep, '/')
                yield full, (prefix + '/' + rel) if prefix else rel

def build(stub, out, spec):
    payload = bytearray()
    files = list(entries(spec))
    for full, rel in files:
        data = open(full, 'rb').read()
        name = rel.encode('utf-8')
        chunks = [data[i:i + CHUNK] for i in range(0, len(data), CHUNK)] or [b'']
        payload += struct.pack('<H', len(name)) + name + struct.pack('<QI', len(data), len(chunks))
        for ch in chunks:
            c = zlib.compressobj(9, zlib.DEFLATED, -15)
            z = c.compress(ch) + c.flush()
            if len(z) >= len(ch): z = ch
            payload += struct.pack('<II', len(z), len(ch)) + z
    key = int.from_bytes(hashlib.sha1(payload).digest()[:4], 'little')
    exe = open(stub, 'rb').read()
    with open(out, 'wb') as f:
        f.write(exe)
        f.write(payload)
        f.write(struct.pack('<QQII', len(exe), len(payload), key, len(files)) + b'BAROPAK1')
    print(out, len(files), 'files, payload', round(len(payload) / 1e6, 1), 'MB, key %08x' % key)

if __name__ == '__main__':
    stub, out = sys.argv[1], sys.argv[2]
    spec = [tuple(a.split('=', 1)) if '=' in a else (a, '') for a in sys.argv[3:]]
    build(stub, out, [(p, pre) for p, pre in spec])
