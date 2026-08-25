package app.revanced.extension.nicomanga;

import android.content.Context;
import android.content.SharedPreferences;

final class ReVancedPreferences {
    private static final String FILE = "nicomanga_revanced";
    private static final String BYPASS = "login_free_mode";
    private static final String DEVELOPMENT_NOTICE = "show_development_notice";
    private static final String LAST_TITLE = "last_manga_title";
    private static final String LAST_ID = "last_manga_id";
    private static final String LAST_TOTAL_CHAPTERS = "last_manga_total_chapters";
    private final SharedPreferences preferences;

    ReVancedPreferences(Context context) {
        preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    boolean isBypassMode() {
        return preferences.getBoolean(BYPASS, true);
    }

    void setBypassMode(boolean enabled) {
        preferences.edit().putBoolean(BYPASS, enabled).apply();
    }

    boolean showDevelopmentNotice() {
        return preferences.getBoolean(DEVELOPMENT_NOTICE, false);
    }

    void setShowDevelopmentNotice(boolean visible) {
        preferences.edit().putBoolean(DEVELOPMENT_NOTICE, visible).apply();
    }

    MangaSnapshot lastManga() {
        String title = preferences.getString(LAST_TITLE, null);
        if (title == null || title.trim().isEmpty()) return null;
        return new MangaSnapshot(
                preferences.getString(LAST_ID, null),
                title,
                preferences.getInt(LAST_TOTAL_CHAPTERS, 1));
    }

    void setLastManga(MangaSnapshot manga) {
        preferences.edit()
                .putString(LAST_TITLE, manga.title)
                .putString(LAST_ID, manga.id)
                .putInt(LAST_TOTAL_CHAPTERS, manga.totalChapters)
                .apply();
    }
}
