package app.revanced.extension.nicomanga;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NetworkObserver {
    static final int SCREEN_HOME = 0;
    static final int SCREEN_DETAIL = 1;
    static final int SCREEN_READER = 2;
    static final int SCREEN_SETTINGS = 3;
    static final int SCREEN_OTHER = 4;
    private static final int MAX_CAPTURE_BYTES = 1_500_000;
    private static final Set<Object> INSTALLED_BUILDERS =
            Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
    private static volatile Context context;
    private static volatile long lastScrollLogTime;
    private static volatile MangaSnapshot currentManga;
    private static volatile int screen = SCREEN_HOME;
    private static volatile int readerPage = 1;
    private static volatile int readerTotalPages = 1;
    private static volatile int readerChapter = 1;
    private static volatile boolean readerPagesFromNetwork;
    private static final Map<Integer, Integer> CHAPTER_PAGES = new ConcurrentHashMap<>();
    private static final Map<String, String> HOME_COLLECTIONS = new ConcurrentHashMap<>();
    private static final String HOME_CACHE = "nicomanga_revanced_home";
    private static volatile long homeVersion;
    private static final Pattern CHAPTER_NUMBER = Pattern.compile("^\\s*([0-9]+)(?:\\.[0-9]+)?\\s*(?:章|chapter)", Pattern.CASE_INSENSITIVE);

    private NetworkObserver() {}

    static void setContext(Context value) {
        context = value.getApplicationContext();
    }

    public static void installOkHttpInterceptor(Object builder) {
        if (builder == null || !INSTALLED_BUILDERS.add(builder)) return;
        try {
            ClassLoader loader = builder.getClass().getClassLoader();
            Class<?> interceptorClass = Class.forName("okhttp3.Interceptor", false, loader);
            Object interceptor = Proxy.newProxyInstance(
                    loader,
                    new Class<?>[]{interceptorClass},
                    new ObserverInvocationHandler());
            Method addInterceptor = builder.getClass().getMethod("addInterceptor", interceptorClass);
            addInterceptor.invoke(builder, interceptor);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            INSTALLED_BUILDERS.remove(builder);
        }
    }

    public static void onFabricEvent(Object manager, int reactTag, String eventName, Object data) {
        if (manager == null || eventName == null) return;
        boolean debug = isDebuggable();
        if (!debug && !"topTouchEnd".equals(eventName)) return;
        boolean scroll = eventName.toLowerCase(java.util.Locale.ROOT).contains("scroll");
        long now = android.os.SystemClock.uptimeMillis();
        if (scroll && now - lastScrollLogTime < 500L) return;
        if (scroll) lastScrollLogTime = now;
        try {
            Object resolved = manager.getClass().getMethod("resolveView", int.class).invoke(manager, reactTag);
            View resolvedView = resolved instanceof View ? (View) resolved : null;
            String viewSummary = summarizeView(resolvedView);
            if ("topTouchEnd".equals(eventName) && screen == SCREEN_DETAIL && isChapterView(resolvedView)) {
                screen = SCREEN_READER;
                readerPage = 1;
                readerChapter = Math.max(1, chapterNumber(resolvedView));
                Integer pages = CHAPTER_PAGES.get(readerChapter);
                if (pages != null) {
                    readerTotalPages = pages;
                    readerPagesFromNetwork = true;
                }
            }
            String eventData = "{}";
            Map<?, ?> eventMap = null;
            if (data != null) {
                try {
                    Object map = data.getClass().getMethod("toHashMap").invoke(data);
                    if (map instanceof Map) {
                        eventMap = (Map<?, ?>) map;
                        eventData = new JSONObject(eventMap).toString();
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    eventData = String.valueOf(data);
                }
            }
            if ("topTouchEnd".equals(eventName) && eventMap != null && readerTotalPages > 1) {
                updateReaderPageFromTouch(eventMap, resolvedView);
            }
            if (debug) writeFabricRecord(eventName, reactTag, viewSummary, eventData);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            if (debug) writeFabricRecord(eventName, reactTag, "unresolved", "{}");
        }
    }

    static MangaSnapshot currentManga() {
        return currentManga;
    }

    static int screen() {
        return screen;
    }

    static void markHome() {
        screen = SCREEN_HOME;
    }

    static void markSettings() {
        screen = SCREEN_SETTINGS;
    }

    static void markDetail() {
        screen = SCREEN_DETAIL;
    }

    static void markOther() {
        screen = SCREEN_OTHER;
    }

    static void markBack() {
        if (screen == SCREEN_READER) screen = SCREEN_DETAIL;
        else if (screen == SCREEN_DETAIL) screen = SCREEN_HOME;
        else if (screen == SCREEN_SETTINGS) screen = SCREEN_HOME;
        else if (screen == SCREEN_OTHER) screen = SCREEN_HOME;
    }

    static long homeVersion() {
        return homeVersion;
    }

    static String homePayload() {
        JSONObject result = new JSONObject();
        Context value = context;
        SharedPreferences cache = value == null ? null
                : value.getSharedPreferences(HOME_CACHE, Context.MODE_PRIVATE);
        try {
            for (String category : new String[]{"new", "top", "update"}) {
                String json = HOME_COLLECTIONS.get(category);
                if ((json == null || json.isEmpty()) && cache != null) {
                    json = cache.getString(category, null);
                }
                if (json != null && !json.isEmpty()) result.put(category, new JSONArray(json));
            }
        } catch (JSONException ignored) {
            return "{}";
        }
        return result.toString();
    }

    static int readerPage() {
        return readerPage;
    }

    static void markReaderPage(int page) {
        readerPage = Math.max(1, page);
    }

    static void markReader() {
        screen = SCREEN_READER;
    }

    static ReadingProgress currentReadingProgress() {
        MangaSnapshot manga = currentManga;
        if (manga == null || screen != SCREEN_READER) return null;
        return new ReadingProgress(manga, readerChapter, readerPage, readerTotalPages);
    }

    static void setReaderTotalPages(int totalPages) {
        if (!readerPagesFromNetwork) readerTotalPages = Math.max(1, totalPages);
    }

    private static void updateReaderPageFromTouch(Map<?, ?> event, View target) {
        Object rawX = event.get("pageX");
        Object rawY = event.get("pageY");
        Context value = context;
        if (!(rawX instanceof Number) || !(rawY instanceof Number) || value == null) return;
        float density = value.getResources().getDisplayMetrics().density;
        float width = value.getResources().getDisplayMetrics().widthPixels / density;
        float height = value.getResources().getDisplayMetrics().heightPixels / density;
        float x = ((Number) rawX).floatValue();
        float y = ((Number) rawY).floatValue();
        int total = readerTotalPages;
        ViewGroupMetrics dots = findPageGroup(target, value);
        if (dots != null) total = dots.childCount;
        if (y > height * 0.84f && y < height * 0.92f) {
            float left = dots == null ? width * 0.047f : dots.left / density;
            float trackWidth = dots == null ? width * 0.906f : dots.width / density;
            int page = (int) (((x - left) * total) / Math.max(1f, trackWidth)) + 1;
            readerPage = Math.max(1, Math.min(total, page));
            if (isDebuggable()) {
                writeFabricRecord("reader-page", readerPage, "reader", "{\"page\":" + readerPage + "}");
            }
        } else if (y >= height * 0.92f) {
            if (x < width * 0.28f) readerPage = Math.max(1, readerPage - 1);
            if (x > width * 0.72f) readerPage = Math.min(total, readerPage + 1);
        }
    }

    private static ViewGroupMetrics findPageGroup(View target, Context context) {
        int displayWidth = context.getResources().getDisplayMetrics().widthPixels;
        int displayHeight = context.getResources().getDisplayMetrics().heightPixels;
        for (int depth = 0; depth < 12 && target != null; depth++) {
            ViewParent parent = target.getParent();
            if (!(parent instanceof View)) break;
            target = (View) parent;
        }
        if (target == null) return null;
        ArrayDeque<View> pending = new ArrayDeque<>();
        pending.add(target);
        int inspected = 0;
        while (!pending.isEmpty() && inspected++ < 3000) {
            View view = pending.removeFirst();
            if (!(view instanceof android.view.ViewGroup)) continue;
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            int count = group.getChildCount();
            int[] location = new int[2];
            group.getLocationOnScreen(location);
            if (count >= 5 && count <= 1000 && group.getWidth() > displayWidth * 0.75f &&
                    group.getHeight() < displayHeight * 0.08f && location[1] > displayHeight * 0.82f &&
                    location[1] < displayHeight * 0.92f) {
                return new ViewGroupMetrics(location[0], group.getWidth(), count);
            }
            for (int index = 0; index < count; index++) pending.addLast(group.getChildAt(index));
        }
        return null;
    }

    private static final class ViewGroupMetrics {
        final int left;
        final int width;
        final int childCount;

        ViewGroupMetrics(int left, int width, int childCount) {
            this.left = left;
            this.width = width;
            this.childCount = childCount;
        }
    }

    private static final class ObserverInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "toString": return "NicomangaReVancedNetworkObserver";
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    default: return null;
                }
            }
            if (!"intercept".equals(method.getName()) || args == null || args.length != 1) return null;

            Object chain = args[0];
            Object request = invokeNoArgs(chain, "request");
            String url = requestUrl(request);
            Object response;
            try {
                response = chain.getClass().getMethod("proceed", request.getClass()).invoke(chain, request);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
            captureJsonResponse(response, url);
            return response;
        }
    }

    private static void captureJsonResponse(Object response, String url) {
        if (response == null || url == null) return;
        try {
            Object body = invokeNoArgs(response, "body");
            if (body == null) return;
            Object contentType = invokeNoArgs(body, "contentType");
            String type = contentType == null ? "" : contentType.toString().toLowerCase(java.util.Locale.ROOT);
            if (!type.contains("json")) return;
            long length = ((Number) invokeNoArgs(body, "contentLength")).longValue();
            if (length > MAX_CAPTURE_BYTES) return;
            Object copy = response.getClass().getMethod("peekBody", long.class)
                    .invoke(response, (long) MAX_CAPTURE_BYTES);
            String json = String.valueOf(invokeNoArgs(copy, "string"));
            Object parsed = new JSONTokener(json).nextValue();
            observeResponse(url, parsed);
            if (isDebuggable()) writeDebugRecord(url, summarize(parsed), json);
        } catch (ReflectiveOperationException | JSONException | RuntimeException ignored) {
            // Observation must never change the app's networking result.
        }
    }

    private static void observeResponse(String url, Object parsed) {
        try {
            if (parsed instanceof JSONArray && url.contains("/manga/collection?")) {
                captureHomeCollection(url, (JSONArray) parsed);
            } else if (parsed instanceof JSONObject && url.matches(".*/manga/[^/?]+$")) {
                JSONObject manga = (JSONObject) parsed;
                String id = manga.optString("id", url.substring(url.lastIndexOf('/') + 1));
                String title = manga.optString("name", "Nicomanga");
                int total = Math.max(1, (int) Math.ceil(manga.optDouble("lastChapter", 1d)));
                currentManga = new MangaSnapshot(id, title, total);
                readerPagesFromNetwork = false;
                if (screen != SCREEN_READER) screen = SCREEN_DETAIL;
            } else if (parsed instanceof JSONArray && url.matches(".*/chapter/[^/?]+$")) {
                JSONArray chapters = (JSONArray) parsed;
                CHAPTER_PAGES.clear();
                for (int index = 0; index < chapters.length(); index++) {
                    JSONObject chapter = chapters.optJSONObject(index);
                    if (chapter == null) continue;
                    int number = (int) Math.floor(chapter.optDouble("chapter", -1d));
                    JSONArray content = chapter.optJSONArray("content");
                    if (number >= 0 && content != null && content.length() > 0) {
                        CHAPTER_PAGES.put(number, content.length());
                    }
                }
                MangaSnapshot manga = currentManga;
                if (manga != null && chapters.length() > 0) {
                    currentManga = manga.withTotalChapters(chapters.length());
                }
                if (screen != SCREEN_READER) screen = SCREEN_DETAIL;
            }
        } catch (RuntimeException ignored) {
            // A response schema change must not affect the app.
        }
    }

    private static void captureHomeCollection(String url, JSONArray source) {
        String category = Uri.parse(url).getQueryParameter("desc");
        if (!"new".equals(category) && !"top".equals(category) && !"update".equals(category)) return;
        JSONArray rows = new JSONArray();
        try {
            for (int index = 0; index < Math.min(25, source.length()); index++) {
                JSONObject input = source.optJSONObject(index);
                if (input == null) continue;
                String id = input.optString("id", "").trim();
                String name = input.optString("name", "").trim();
                if (id.isEmpty() || name.isEmpty()) continue;
                String cover = input.optString("cover", "").trim();
                Uri coverUri = Uri.parse(cover);
                if (!"https".equalsIgnoreCase(coverUri.getScheme()) || coverUri.getHost() == null) cover = "";
                rows.put(new JSONObject()
                        .put("id", id)
                        .put("name", name)
                        .put("cover", cover)
                        .put("authors", input.optString("authors", input.optString("artists", "")))
                        .put("artists", input.optString("artists", ""))
                        .put("genres", input.optString("genres", ""))
                        .put("lastChapter", input.opt("lastChapter"))
                        .put("lastUpdate", input.optString("lastUpdate", "")));
            }
        } catch (JSONException ignored) {
            return;
        }
        String json = rows.toString();
        HOME_COLLECTIONS.put(category, json);
        Context value = context;
        if (value != null) value.getSharedPreferences(HOME_CACHE, Context.MODE_PRIVATE)
                .edit().putString(category, json).apply();
        homeVersion++;
    }

    private static String summarize(Object value) {
        if (value instanceof JSONObject) {
            JSONArray keys = ((JSONObject) value).names();
            return keys == null ? "object[]" : "object" + keys;
        }
        if (value instanceof JSONArray) return "array[length=" + ((JSONArray) value).length() + "]";
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private static String requestUrl(Object request) {
        if (request == null) return null;
        try {
            Object url = invokeNoArgs(request, "url");
            return url == null ? null : url.toString();
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static String summarizeView(View view) {
        if (view == null) return "null";
        StringBuilder result = new StringBuilder(view.getClass().getName());
        for (int depth = 0; depth < 4 && view != null; depth++) {
            CharSequence description = view.getContentDescription();
            CharSequence text = view instanceof TextView ? ((TextView) view).getText() : null;
            if (description != null && description.length() > 0) result.append(" desc=").append(description);
            if (text != null && text.length() > 0) result.append(" text=").append(text);
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
            if (view != null) result.append(" | parent ").append(view.getClass().getSimpleName());
        }
        return result.toString().replace('\n', ' ');
    }

    private static boolean isChapterView(View view) {
        for (int depth = 0; depth < 5 && view != null; depth++) {
            CharSequence description = view.getContentDescription();
            CharSequence text = view instanceof TextView ? ((TextView) view).getText() : null;
            if (isChapterLabel(description) || isChapterLabel(text)) return true;
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static int chapterNumber(View view) {
        for (int depth = 0; depth < 5 && view != null; depth++) {
            CharSequence description = view.getContentDescription();
            CharSequence text = view instanceof TextView ? ((TextView) view).getText() : null;
            int number = parseChapterNumber(description);
            if (number >= 0) return number;
            number = parseChapterNumber(text);
            if (number >= 0) return number;
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return -1;
    }

    private static int parseChapterNumber(CharSequence value) {
        if (value == null) return -1;
        Matcher matcher = CHAPTER_NUMBER.matcher(value);
        if (!matcher.find()) return -1;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isChapterLabel(CharSequence value) {
        if (value == null) return false;
        String normalized = value.toString().trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.matches("^[0-9]+(?:\\.[0-9]+)?\\s*(章|chapter).*?");
    }

    private static Object invokeNoArgs(Object receiver, String method)
            throws ReflectiveOperationException {
        return receiver.getClass().getMethod(method).invoke(receiver);
    }

    private static boolean isDebuggable() {
        Context value = context;
        return value != null && (value.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private static synchronized void writeDebugRecord(String url, String summary, String json) {
        Context value = context;
        if (value == null) return;
        File output = new File(value.getFilesDir(), "nicomanga-revanced-network.log");
        String record = "URL " + url + "\nSCHEMA " + summary + "\nJSON " + json + "\n---\n";
        try (FileOutputStream stream = new FileOutputStream(output, true)) {
            stream.write(record.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // Debug capture failure must not affect the app.
        }
    }


    private static synchronized void writeFabricRecord(
            String eventName, int reactTag, String view, String data
    ) {
        Context value = context;
        if (value == null) return;
        File output = new File(value.getFilesDir(), "nicomanga-revanced-fabric.log");
        String record = "EVENT " + eventName + " tag=" + reactTag + "\nVIEW " + view +
                "\nDATA " + data + "\n---\n";
        try (FileOutputStream stream = new FileOutputStream(output, true)) {
            stream.write(record.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // Debug capture failure must not affect the app.
        }
    }
}
