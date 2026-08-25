package app.revanced.extension.nicomanga;

import org.json.JSONException;
import org.json.JSONObject;

final class MangaSnapshot {
    final String id;
    final String title;
    final int totalChapters;

    MangaSnapshot(String title, int totalChapters) {
        this.title = title == null || title.trim().isEmpty() ? "Nicomanga" : title.trim();
        this.id = this.title.toLowerCase(java.util.Locale.ROOT);
        this.totalChapters = Math.max(1, totalChapters);
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("title", title)
                .put("totalChapters", totalChapters)
                .put("addedAt", System.currentTimeMillis());
    }
}
