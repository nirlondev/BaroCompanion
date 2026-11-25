package baro.hotkey;

/** Заглушка для систем, где глобальные сочетания не поддерживаются. */
final class NoHotkey implements GlobalHotkey {
    private final String reason;

    NoHotkey(String reason) { this.reason = reason; }

    @Override public void start(Runnable onPressed) { }
    @Override public void setActive(boolean active) { }
    @Override public void stop() { }
    @Override public boolean isSupported() { return false; }
    @Override public String status() { return reason; }
}
