package baro.app;

import baro.data.gamefiles.GameInstall;
import baro.data.gamefiles.SubmarineReader;
import baro.settings.AppSettings;

/** Подлодки и улучшения — только из файлов игры (встроенной копии нет), в фоне при первом открытии вкладки. */
public final class SubmarineSource extends LazyData<SubmarineReader.Result> {
    public SubmarineSource(AppSettings settings) {
        super("submarines-load", () -> {
            if (!settings.getBoolean("game.files", true)) return null;
            GameInstall game = GameInstall.locate(settings);
            return game == null ? null : SubmarineReader.read(game, settings.getString("game.language", baro.Tr.gameLanguage()));
        });
    }
}
