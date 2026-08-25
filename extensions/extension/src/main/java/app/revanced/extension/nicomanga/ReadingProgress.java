package app.revanced.extension.nicomanga;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ReadingProgress {
    final MangaSnapshot manga;
    final int chapter;
    final int page;
    final int totalPages;

    ReadingProgress(MangaSnapshot manga, int chapter, int page, int totalPages) {
        this.manga = manga;
        this.chapter = Math.max(1, chapter);
        this.page = Math.max(1, page);
        this.totalPages = Math.max(1, totalPages);
    }

    JSONObject toJson() throws JSONException {
        JSONArray completed = new JSONArray();
        if (page >= totalPages) completed.put(chapter);
        return manga.toJson()
                .put("lastChapter", chapter)
                .put("lastPage", page)
                .put("totalPages", totalPages)
                .put("completedChapters", completed)
                .put("updatedAt", System.currentTimeMillis());
    }

    String signature() {
        return manga.id + ':' + chapter + ':' + page + ':' + totalPages;
    }
}
