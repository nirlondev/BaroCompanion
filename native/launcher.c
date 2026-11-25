/*
 * BaroCompanion.exe — нативный запускатель.
 * Внутри exe (после кода) лежит архив: встроенная Java (runtime/) и программа (app/BaroCompanion.jar).
 * При первом запуске архив распаковывается в %LOCALAPPDATA%\BaroCompanion\app-<версия>,
 * затем Java-машина (jvm.dll) загружается прямо в этот процесс и запускается baro.App.
 * Окно в диспетчере задач и на панели задач — это сам BaroCompanion.exe.
 */
#include "inflate.h"
#include <jni.h>

#define MAX_PATH_CH 1024
#define CHUNK_MAX (4u << 20)

/* ======================= платформа ======================= */
#ifdef _WIN32
typedef unsigned short pch;
typedef void *HANDLE;
typedef unsigned long DWORD;
typedef int BOOL;
typedef long LONG;
typedef struct { long long QuadPart; } LARGE_INTEGER_;
typedef struct { DWORD dwFileAttributes; DWORD a[6]; DWORD nFileSizeHigh, nFileSizeLow, r0, r1; pch cFileName[260]; pch cAlternateFileName[14]; DWORD x[3]; } FINDDATA;
typedef struct { void *hwnd; unsigned message; unsigned long long wParam; long long lParam; DWORD time; long pt[2]; DWORD priv; } MSG_;
#define DECL __declspec(dllimport)
#define WINAPI __stdcall
DECL DWORD WINAPI GetModuleFileNameW(HANDLE, pch *, DWORD);
DECL DWORD WINAPI GetEnvironmentVariableW(const pch *, pch *, DWORD);
DECL DWORD WINAPI GetFileAttributesW(const pch *);
DECL HANDLE WINAPI CreateFileW(const pch *, DWORD, DWORD, void *, DWORD, DWORD, HANDLE);
DECL BOOL WINAPI ReadFile(HANDLE, void *, DWORD, DWORD *, void *);
DECL BOOL WINAPI WriteFile(HANDLE, const void *, DWORD, DWORD *, void *);
DECL BOOL WINAPI SetFilePointerEx(HANDLE, long long, long long *, DWORD);
DECL BOOL WINAPI GetFileSizeEx(HANDLE, long long *);
DECL BOOL WINAPI CloseHandle(HANDLE);
DECL BOOL WINAPI CreateDirectoryW(const pch *, void *);
DECL BOOL WINAPI MoveFileExW(const pch *, const pch *, DWORD);
DECL BOOL WINAPI DeleteFileW(const pch *);
DECL BOOL WINAPI RemoveDirectoryW(const pch *);
DECL HANDLE WINAPI FindFirstFileW(const pch *, FINDDATA *);
DECL BOOL WINAPI FindNextFileW(HANDLE, FINDDATA *);
DECL BOOL WINAPI FindClose(HANDLE);
DECL void *WINAPI VirtualAlloc(void *, usize, DWORD, DWORD);
DECL HANDLE WINAPI LoadLibraryExW(const pch *, HANDLE, DWORD);
DECL BOOL WINAPI SetDllDirectoryW(const pch *);
DECL void *WINAPI GetProcAddress(HANDLE, const char *);
DECL void WINAPI ExitProcess(unsigned);
DECL pch *WINAPI GetCommandLineW(void);
DECL int WINAPI WideCharToMultiByte(unsigned, DWORD, const pch *, int, char *, int, const char *, BOOL *);
DECL DWORD WINAPI GetCurrentProcessId(void);
DECL DWORD WINAPI GetShortPathNameW(const pch *, pch *, DWORD);
DECL DWORD WINAPI GetLastError(void);
DECL void WINAPI Sleep(DWORD);
DECL int WINAPI MessageBoxW(HANDLE, const pch *, const pch *, unsigned);
DECL HANDLE WINAPI CreateWindowExW(DWORD, const pch *, const pch *, DWORD, int, int, int, int, HANDLE, HANDLE, HANDLE, void *);
DECL BOOL WINAPI DestroyWindow(HANDLE);
DECL BOOL WINAPI PeekMessageW(MSG_ *, HANDLE, unsigned, unsigned, unsigned);
DECL BOOL WINAPI TranslateMessage(const MSG_ *);
DECL long long WINAPI DispatchMessageW(const MSG_ *);
DECL int WINAPI GetSystemMetrics(int);
DECL long long WINAPI SendMessageW(HANDLE, unsigned, unsigned long long, long long);
DECL BOOL WINAPI UpdateWindow(HANDLE);
DECL HANDLE WINAPI GetStockObject(int);
DECL pch **WINAPI CommandLineToArgvW(const pch *, int *);
#define T(s) L##s
#define BAD_HANDLE ((HANDLE)(long long)-1)
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <dlfcn.h>
typedef char pch;
#define T(s) s
#endif

/* ======================= строки ======================= */
#ifdef _WIN32
void *memset(void *d, int c, usize n) { u8 *p = d; while (n--) *p++ = (u8)c; return d; }
void *memcpy(void *d, const void *s, usize n) { u8 *p = d; const u8 *q = s; while (n--) *p++ = *q++; return d; }
#endif
static int plen(const pch *s) { int n = 0; while (s[n]) n++; return n; }
static void pcat(pch *d, const pch *s) { int n = plen(d); while (*s && n < MAX_PATH_CH - 1) d[n++] = *s++; d[n] = 0; }
static void pcpy(pch *d, const pch *s) { d[0] = 0; pcat(d, s); }
static void pcat_ascii(pch *d, const char *s) { int n = plen(d); while (*s && n < MAX_PATH_CH - 1) d[n++] = (pch)(u8)*s++; d[n] = 0; }
static void hex8(char *o, u32 v) { int i; for (i = 7; i >= 0; i--) { o[i] = "0123456789abcdef"[v & 15]; v >>= 4; } o[8] = 0; }
static int clen(const char *s) { int n = 0; while (s[n]) n++; return n; }
static void ccat(char *d, const char *s, int cap) { int n = clen(d); while (*s && n < cap - 1) d[n++] = *s++; d[n] = 0; }
#ifdef _WIN32
#define SEP T("\\")
#else
#define SEP T("/")
#endif

/* ======================= файлы ======================= */
typedef struct { usize start, len; u32 key, count; } Trailer;

#ifdef _WIN32
typedef HANDLE FH;
static FH f_open(const pch *p) { return CreateFileW(p, 0x80000000u, 7, 0, 3, 0x80, 0); }
static FH f_create(const pch *p) { return CreateFileW(p, 0x40000000u, 0, 0, 2, 0x80, 0); }
static int f_ok(FH h) { return h != BAD_HANDLE; }
static int f_read(FH h, void *b, u32 n) { DWORD r = 0; return ReadFile(h, b, n, &r, 0) && r == n; }
static int f_write(FH h, const void *b, u32 n) { DWORD w = 0; return n == 0 || (WriteFile(h, b, n, &w, 0) && w == n); }
static int f_seek(FH h, usize off) { return SetFilePointerEx(h, (long long)off, 0, 0); }
static usize f_size(FH h) { long long s = 0; GetFileSizeEx(h, &s); return (usize)s; }
static void f_close(FH h) { CloseHandle(h); }
static int p_exists(const pch *p) { return GetFileAttributesW(p) != 0xFFFFFFFFu; }
static void p_mkdir(const pch *p) { CreateDirectoryW(p, 0); }
static void *mem(usize n) { return VirtualAlloc(0, n, 0x3000, 4); }
#else
typedef int FH;
static FH f_open(const pch *p) { return open(p, O_RDONLY); }
static FH f_create(const pch *p) { return open(p, O_WRONLY | O_CREAT | O_TRUNC, 0755); }
static int f_ok(FH h) { return h >= 0; }
static int f_read(FH h, void *b, u32 n) { return read(h, b, n) == (long)n; }
static int f_write(FH h, const void *b, u32 n) { return write(h, b, n) == (long)n; }
static int f_seek(FH h, usize off) { return lseek(h, (off_t)off, SEEK_SET) >= 0; }
static usize f_size(FH h) { struct stat st; fstat(h, &st); return (usize)st.st_size; }
static void f_close(FH h) { close(h); }
static int p_exists(const pch *p) { struct stat st; return stat(p, &st) == 0; }
static void p_mkdir(const pch *p) { mkdir(p, 0755); }
static void *mem(usize n) { return malloc(n); }
#endif

static int read_trailer(FH h, Trailer *t) {
    u8 b[32]; usize sz = f_size(h);
    if (sz < 32 || !f_seek(h, sz - 32) || !f_read(h, b, 32)) return 0;
    if (b[24] != 'B' || b[25] != 'A' || b[26] != 'R' || b[27] != 'O' || b[28] != 'P' || b[29] != 'A' || b[30] != 'K' || b[31] != '1') return 0;
    memcpy(&t->start, b, 8); memcpy(&t->len, b + 8, 8); memcpy(&t->key, b + 16, 4); memcpy(&t->count, b + 20, 4);
    return 1;
}

/* Создать все родительские папки для пути. */
static void make_parents(pch *path) {
    int i, n = plen(path);
    for (i = 1; i < n; i++) if (path[i] == '/' || path[i] == '\\') {
        pch c = path[i]; path[i] = 0;
        if (!(i == 2 && path[1] == ':')) p_mkdir(path);
        path[i] = c;
    }
}

typedef void (*Progress)(u32 done, u32 total);

/* Распаковать весь архив в папку dir. 0 — успех. */
static int extract(FH h, const Trailer *t, const pch *dir, Progress progress) {
    u8 *in = mem(CHUNK_MAX + 1024), *out = mem(CHUNK_MAX);
    u32 f;
    if (!in || !out) return 100;
    if (!f_seek(h, t->start)) return 101;
    for (f = 0; f < t->count; f++) {
        u16 nl; char name[512]; usize size; u32 chunks, c; pch path[MAX_PATH_CH]; FH o; int i;
        if (!f_read(h, &nl, 2) || nl >= sizeof name || !f_read(h, name, nl)) return 102;
        name[nl] = 0;
        if (!f_read(h, &size, 8) || !f_read(h, &chunks, 4)) return 103;
        pcpy(path, dir); pcat(path, SEP);
        {   /* имя в архиве — ASCII с '/', переводим в путь системы */
            int n = plen(path);
            for (i = 0; i < nl && n < MAX_PATH_CH - 1; i++) path[n++] = (pch)(name[i] == '/' ? SEP[0] : (u8)name[i]);
            path[n] = 0;
        }
        make_parents(path);
        o = f_create(path);
        if (!f_ok(o)) return 104;
        for (c = 0; c < chunks; c++) {
            u32 z, raw;
            if (!f_read(h, &z, 4) || !f_read(h, &raw, 4) || z > CHUNK_MAX + 1024 || raw > CHUNK_MAX) { f_close(o); return 105; }
            if (!f_read(h, in, z)) { f_close(o); return 106; }
            if (z == raw) { if (!f_write(o, in, raw)) { f_close(o); return 107; } }
            else {
                if (baro_inflate(in, z, out, raw)) { f_close(o); return 108; }
                if (!f_write(o, out, raw)) { f_close(o); return 109; }
            }
        }
        f_close(o);
        if (progress) progress(f + 1, t->count);
    }
    return 0;
}

/* ======================= запуск Java ======================= */
typedef jint (JNICALL *CreateJavaVM)(JavaVM **, void **, void *);

/* Запускает baro.App.main(args). argv — строки в UTF-8. Возвращает код ошибки или 0. */
static int run_java(CreateJavaVM create, char **options, int nopt, char **argv, int argc, char *err, int errcap) {
    JavaVMOption opt[16]; JavaVMInitArgs vmargs; JavaVM *vm = 0; JNIEnv *env = 0;
    jclass app, str; jmethodID main; jobjectArray jargs; int i;
    for (i = 0; i < nopt; i++) { opt[i].optionString = options[i]; opt[i].extraInfo = 0; }
    vmargs.version = JNI_VERSION_10;
    vmargs.nOptions = nopt;
    vmargs.options = opt;
    vmargs.ignoreUnrecognized = JNI_TRUE;
    if (create(&vm, (void **)&env, &vmargs) != JNI_OK) { ccat(err, "JNI_CreateJavaVM", errcap); return 1; }
    app = (*env)->FindClass(env, "baro/App");
    if (!app) { (*env)->ExceptionClear(env); ccat(err, "baro/App", errcap); return 2; }
    main = (*env)->GetStaticMethodID(env, app, "main", "([Ljava/lang/String;)V");
    if (!main) { (*env)->ExceptionClear(env); ccat(err, "main", errcap); return 3; }
    str = (*env)->FindClass(env, "java/lang/String");
    jargs = (*env)->NewObjectArray(env, argc, str, 0);
    for (i = 0; i < argc; i++) (*env)->SetObjectArrayElement(env, jargs, i, (*env)->NewStringUTF(env, argv[i]));
    (*env)->CallStaticVoidMethod(env, app, main, jargs);
    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); ccat(err, "main exception", errcap); return 4; }
    /* ждём, пока работают окна (поток Swing); выход — через System.exit */
    (*vm)->DestroyJavaVM(vm);
    return 0;
}

/* ======================= Windows ======================= */
#ifdef _WIN32
#define TITLE T("Baro Companion")
static HANDLE splash, label;

static void pump(void) { MSG_ m; while (PeekMessageW(&m, 0, 0, 0, 1)) { TranslateMessage(&m); DispatchMessageW(&m); } }

static void show_splash(void) {
    int w = 380, h = 72, sw = GetSystemMetrics(0), sh = GetSystemMetrics(1);
    /* WS_POPUP|WS_BORDER|WS_VISIBLE, поверх всех, без кнопки на панели задач */
    splash = CreateWindowExW(0x00000008 | 0x00000080, T("STATIC"), T(""), 0x80000000u | 0x00800000 | 0x10000000,
                             (sw - w) / 2, (sh - h) / 2, w, h, 0, 0, 0, 0);
    label = CreateWindowExW(0, T("STATIC"), T("Baro Companion\nПервый запуск: подготовка… 0%"), 0x50000000u | 0x00000001,
                            8, 14, w - 16, h - 20, splash, 0, 0, 0);
    SendMessageW(label, 0x0030 /* WM_SETFONT */, (unsigned long long)GetStockObject(17 /* DEFAULT_GUI_FONT */), 1);
    UpdateWindow(splash);
    pump();
}

static void on_progress(u32 done, u32 total) {
    static int last = -1;
    int pct = (int)((usize)done * 100 / (total ? total : 1));
    if (pct != last && label) {
        pch text[96]; char num[8]; int i = 0, v = pct;
        last = pct;
        pcpy(text, T("Baro Companion\nПервый запуск: подготовка… "));
        if (v >= 100) num[i++] = '1', v -= 100, num[i++] = '0' + v / 10, num[i++] = '0' + v % 10;
        else { if (v >= 10) num[i++] = '0' + v / 10; num[i++] = '0' + v % 10; }
        num[i++] = '%'; num[i] = 0;
        pcat_ascii(text, num);
        SendMessageW(label, 0x000C /* WM_SETTEXT */, 0, (long long)text);
    }
    pump();
}

static void fail(const pch *what, const char *detail) {
    pch msg[MAX_PATH_CH];
    pcpy(msg, what);
    if (detail && detail[0]) { pcat(msg, T("\n\n")); pcat_ascii(msg, detail); }
    MessageBoxW(0, msg, TITLE, 0x10);
    ExitProcess(1);
}

static void rmtree(const pch *dir) {
    pch pat[MAX_PATH_CH], p[MAX_PATH_CH]; FINDDATA fd; HANDLE h;
    pcpy(pat, dir); pcat(pat, T("\\*"));
    h = FindFirstFileW(pat, &fd);
    if (h != BAD_HANDLE) {
        do {
            if (fd.cFileName[0] == '.' && (fd.cFileName[1] == 0 || (fd.cFileName[1] == '.' && fd.cFileName[2] == 0))) continue;
            pcpy(p, dir); pcat(p, T("\\")); pcat(p, fd.cFileName);
            if (fd.dwFileAttributes & 0x10) rmtree(p); else DeleteFileW(p);
        } while (FindNextFileW(h, &fd));
        FindClose(h);
    }
    RemoveDirectoryW(dir);
}

/* Удалить старые версии (app-*, tmp-*), кроме текущей. Занятые файлы просто пропускаются. */
static void cleanup(const pch *base, const pch *keepName) {
    pch pat[MAX_PATH_CH], p[MAX_PATH_CH]; FINDDATA fd; HANDLE h;
    pcpy(pat, base); pcat(pat, T("\\*"));
    h = FindFirstFileW(pat, &fd);
    if (h == BAD_HANDLE) return;
    do {
        const pch *n = fd.cFileName; int i, same = 1;
        if (!(fd.dwFileAttributes & 0x10)) continue;
        if (!((n[0] == 'a' && n[1] == 'p' && n[2] == 'p' && n[3] == '-') || (n[0] == 't' && n[1] == 'm' && n[2] == 'p' && n[3] == '-'))) continue;
        for (i = 0; n[i] || keepName[i]; i++) if (n[i] != keepName[i]) { same = 0; break; }
        if (same) continue;
        pcpy(p, base); pcat(p, T("\\")); pcat(p, n);
        rmtree(p);
    } while (FindNextFileW(h, &fd));
    FindClose(h);
}

/* Путь для опции JVM: в кодировке ANSI; если путь с символами вне неё — короткое имя 8.3. */
static void jvm_path(const pch *p, char *out, int cap) {
    BOOL lossy = 0; pch sp[MAX_PATH_CH];
    int n = WideCharToMultiByte(0 /* CP_ACP */, 0, p, -1, out, cap, 0, &lossy);
    if (n > 0 && !lossy) return;
    if (GetShortPathNameW(p, sp, MAX_PATH_CH)) WideCharToMultiByte(0, 0, sp, -1, out, cap, 0, 0);
}

static void opt_path(char *dst, int cap, const char *prefix, const pch *path) {
    char tmp[2048]; tmp[0] = 0; dst[0] = 0;
    jvm_path(path, tmp, sizeof tmp);
    ccat(dst, prefix, cap); ccat(dst, tmp, cap);
}

static pch self[MAX_PATH_CH], base[MAX_PATH_CH], appdir[MAX_PATH_CH], tmpdir[MAX_PATH_CH], p[MAX_PATH_CH], name[64];
static char o_cp[2400], o_set[2400], err[256];

void start(void) {
    FH h; Trailer t; char hx[9]; int rc, argc = 0, i, nopt = 0, jargc = 0;
    pch **wargv; char *options[16], *jargv[32]; static char argbuf[32][1024];
    HANDLE jvm; CreateJavaVM create;

    GetModuleFileNameW(0, self, MAX_PATH_CH);
    h = f_open(self);
    if (!f_ok(h) || !read_trailer(h, &t)) fail(T("Файл программы повреждён — скачайте его заново."), 0);

    if (!GetEnvironmentVariableW(T("LOCALAPPDATA"), base, MAX_PATH_CH)) fail(T("Не найдена папка LOCALAPPDATA."), 0);
    pcat(base, T("\\BaroCompanion"));
    p_mkdir(base);
    hex8(hx, t.key);
    pcpy(name, T("app-")); pcat_ascii(name, hx);
    pcpy(appdir, base); pcat(appdir, T("\\")); pcat(appdir, name);
    pcpy(p, appdir); pcat(p, T("\\.ok"));

    if (!p_exists(p)) {
        char pid[12]; DWORD v = GetCurrentProcessId(); int k = 10; pid[11] = 0;
        do { pid[k--] = (char)('0' + v % 10); v /= 10; } while (v && k >= 0);
        pcpy(tmpdir, base); pcat(tmpdir, T("\\tmp-")); pcat_ascii(tmpdir, hx); pcat(tmpdir, T("-")); pcat_ascii(tmpdir, pid + k + 1);
        rmtree(tmpdir);
        p_mkdir(tmpdir);
        show_splash();
        rc = extract(h, &t, tmpdir, on_progress);
        if (rc) { char d[32] = "code "; char n[4] = {(char)('0' + rc / 100), (char)('0' + rc / 10 % 10), (char)('0' + rc % 10), 0}; ccat(d, n, 32);
                  rmtree(tmpdir); fail(T("Не удалось распаковать программу (мало места на диске?)."), d); }
        pcpy(p, tmpdir); pcat(p, T("\\.ok"));
        { FH ok = f_create(p); f_write(ok, "ok", 2); f_close(ok); }
        /* переименовать готовую папку; антивирус может ненадолго держать файлы — пробуем несколько раз */
        for (i = 0; i < 40; i++) {
            if (MoveFileExW(tmpdir, appdir, 0)) break;
            pcpy(p, appdir); pcat(p, T("\\.ok"));
            if (p_exists(p)) { rmtree(tmpdir); break; }   /* уже распаковала другая копия */
            Sleep(250);
        }
        if (i == 40) pcpy(appdir, tmpdir);                 /* не вышло — работаем из временной папки */
        if (splash) { DestroyWindow(splash); splash = 0; pump(); }
        if (i < 40) cleanup(base, name);
    }
    f_close(h);

    /* jvm.dll и её библиотеки */
    pcpy(p, appdir); pcat(p, T("\\runtime\\bin"));
    SetDllDirectoryW(p);
    pcat(p, T("\\server\\jvm.dll"));
    jvm = LoadLibraryExW(p, 0, 0x00000008 /* LOAD_WITH_ALTERED_SEARCH_PATH */);
    if (!jvm) fail(T("Не удалось загрузить встроенную Java (jvm.dll)."), 0);
    create = (CreateJavaVM)GetProcAddress(jvm, "JNI_CreateJavaVM");
    if (!create) fail(T("Встроенная Java повреждена."), 0);

    pcpy(p, appdir); pcat(p, T("\\app\\BaroCompanion.jar"));
    opt_path(o_cp, sizeof o_cp, "-Djava.class.path=", p);
    pcpy(p, base); pcat(p, T("\\settings.properties"));
    opt_path(o_set, sizeof o_set, "-Dbaro.settings=", p);
    options[nopt++] = o_cp;
    options[nopt++] = o_set;
    options[nopt++] = "--enable-native-access=ALL-UNNAMED";
    options[nopt++] = "-Dsun.java.launcher=SUN_STANDARD";
    options[nopt++] = "-XX:+UseSerialGC";
    options[nopt++] = "-Xmx256m";
    options[nopt++] = "-Dsun.java2d.d3d=false";
    options[nopt++] = "--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED"; /* цвет заголовка окна */
    options[nopt++] = "--add-opens=java.desktop/java.awt=ALL-UNNAMED";

    wargv = CommandLineToArgvW(GetCommandLineW(), &argc);
    for (i = 1; wargv && i < argc && jargc < 32; i++) {
        WideCharToMultiByte(65001 /* UTF-8 */, 0, wargv[i], -1, argbuf[jargc], 1024, 0, 0);
        jargv[jargc] = argbuf[jargc]; jargc++;
    }
    rc = run_java(create, options, nopt, jargv, jargc, err, sizeof err);
    if (rc) fail(T("Не удалось запустить программу."), err);
    ExitProcess(0);
}

/* ======================= Linux (проверка логики) ======================= */
#else
static void prog(u32 d, u32 t) { if (d == t || d % 50 == 0) fprintf(stderr, "\rextract %u/%u", d, t); if (d == t) fprintf(stderr, "\n"); }
int main(int argc, char **argv) {
    char base[MAX_PATH_CH] = "/tmp/barotest", appdir[MAX_PATH_CH], p[MAX_PATH_CH], hx[9], err[256] = "";
    char cp[2048], set[2048]; char *options[8]; int nopt = 0, rc; Trailer t; FH h; void *lib; CreateJavaVM create;
    h = f_open(argv[1]);
    if (!f_ok(h) || !read_trailer(h, &t)) { fprintf(stderr, "no payload\n"); return 1; }
    hex8(hx, t.key);
    mkdir(base, 0755);
    snprintf(appdir, sizeof appdir, "%s/app-%s", base, hx);
    snprintf(p, sizeof p, "%s/.ok", appdir);
    if (!p_exists(p)) {
        mkdir(appdir, 0755);
        rc = extract(h, &t, appdir, prog);
        if (rc) { fprintf(stderr, "extract rc=%d\n", rc); return 1; }
        { FH ok = f_create(p); f_write(ok, "ok", 2); f_close(ok); }
    }
    snprintf(p, sizeof p, "%s/runtime/lib/server/libjvm.so", appdir);
    lib = dlopen(p, RTLD_NOW);
    if (!lib) { fprintf(stderr, "%s\n", dlerror()); return 1; }
    create = (CreateJavaVM)dlsym(lib, "JNI_CreateJavaVM");
    snprintf(cp, sizeof cp, "-Djava.class.path=%s/app/BaroCompanion.jar", appdir);
    snprintf(set, sizeof set, "-Dbaro.settings=%s/settings.properties", base);
    options[nopt++] = cp; options[nopt++] = set; options[nopt++] = "--enable-native-access=ALL-UNNAMED";
    options[nopt++] = "-XX:+UseSerialGC";
    rc = run_java(create, options, nopt, argv + 2, argc - 2, err, sizeof err);
    fprintf(stderr, "run_java rc=%d %s\n", rc, err);
    return rc;
}
#endif
