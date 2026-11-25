/* Компактный распаковщик DEFLATE (RFC 1951), без библиотек. */
#ifndef BARO_INFLATE_H
#define BARO_INFLATE_H
typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
typedef unsigned long long u64;
typedef unsigned long long usize;

/* Распаковать raw-deflate in[0..inlen) в out (ровно outlen байт). 0 — успех. */
int baro_inflate(const u8 *in, usize inlen, u8 *out, usize outlen);
#endif
