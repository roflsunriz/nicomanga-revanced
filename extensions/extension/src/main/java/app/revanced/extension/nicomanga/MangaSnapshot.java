package app.revanced.extension.nicomanga;

import org.json.JSONException;
import org.json.JSONObject;

final class MangaSnapshot {
    final String id;
    final String title;
    final int totalChapters;

    MangaSnapshot(String title, int totalChapters) {
        this(null, title, totalChapters);
    }

    MangaSnapshot(String id, String title, int totalChapters) {
        this.title = title == null || title.trim().isEmpty() ? "Nicomanga" : title.trim();
        this.id = id == null || id.trim().isEmpty()
                ? this.title.toLowerCase(java.util.Locale.ROOT)
                : id.trim();
        this.totalChapters = Math.max(1, totalChapters);
    }

    MangaSnapshot withTotalChapters(int value) {
        return new MangaSnapshot(id, title, value);
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("title", title)
                .put("totalChapters", totalChapters)
                .put("addedAt", System.currentTimeMillis());
    }
}
