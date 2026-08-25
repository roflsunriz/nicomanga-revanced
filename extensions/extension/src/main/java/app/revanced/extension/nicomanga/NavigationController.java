package app.revanced.extension.nicomanga;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

final class NavigationController {
    private static final long STEP_DELAY = 900L;
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Translations translations;

    NavigationController(Activity activity, Translations translations) {
        this.activity = activity;
        this.translations = translations;
    }

    View root() {
        return activity.findViewById(android.R.id.content);
    }

    void openHome() {
        openInternalRoute(Uri.parse("nicomanga://"));
        NetworkObserver.markHome();
    }

    void openSettings() {
        Uri destination = new Uri.Builder()
                .scheme("nicomanga")
                .authority("setting")
                .build();
        openInternalRoute(destination);
        NetworkObserver.markSettings();
    }

    void openManga(String id) {
        if (id == null || !id.matches("^[0-9]+$")) return;
        Uri destination = new Uri.Builder()
                .scheme("nicomanga")
                .authority("posts")
                .appendQueryParameter("mangaId", id)
                .build();
        NetworkObserver.markDetail();
        openInternalRoute(destination);
    }

    void openSearch() {
        View search = ViewTree.topCornerButton(root(), true);
        if (search == null) {
            toast(translations.get(Translations.STORAGE_ERROR));
            return;
        }
        NetworkObserver.markOther();
        tap(search);
    }

    void resume(JSONObject record) {
        String title = record.optString("title", "").trim();
        String id = record.optString("id", "").trim();
        int chapter = Math.max(1, record.optInt("lastChapter", 1));
        int page = Math.max(1, record.optInt("lastPage", 1));
        int totalPages = Math.max(1, record.optInt("totalPages", 1));
        if (title.isEmpty()) {
            toast(translations.get(Translations.STORAGE_ERROR));
            return;
        }
        if (id.matches("^[0-9]+$")) {
            Uri detail = new Uri.Builder()
                    .scheme("nicomanga")
                    .authority("posts")
                    .appendQueryParameter("mangaId", id)
                    .build();
            openInternalRoute(detail);
            handler.postDelayed(() -> {
                Uri reader = new Uri.Builder()
                        .scheme("nicomanga")
                        .authority("posts")
                        .appendPath("reader")
                        .appendQueryParameter("mangaId", id)
                        .appendQueryParameter("chapter", Integer.toString(chapter))
                        .build();
                openInternalRoute(reader);
                NetworkObserver.markReaderPage(page);
                handler.postDelayed(() -> jumpToPage(page, totalPages, 0), STEP_DELAY * 3);
            }, STEP_DELAY * 4);
            return;
        }
        reachHomeThenSearch(title, chapter, page, totalPages, 0);
    }

    private void reachHomeThenSearch(String title, int chapter, int page, int totalPages, int attempts) {
        View root = root();
        List<View> tabs = ViewTree.bottomTabs(root);
        if (tabs.isEmpty()) {
            if (attempts >= 5) {
                toast(translations.get(Translations.STORAGE_ERROR));
                return;
            }
            View back = ViewTree.topCornerButton(root, false);
            if (back != null) tap(back);
            handler.postDelayed(() -> reachHomeThenSearch(title, chapter, page, totalPages, attempts + 1), STEP_DELAY);
            return;
        }
        tap(tabs.get(0));
        handler.postDelayed(() -> openSearch(title, chapter, page, totalPages), STEP_DELAY);
    }

    private void openSearch(String title, int chapter, int page, int totalPages) {
        View search = ViewTree.topCornerButton(root(), true);
        if (search == null) {
            toast(translations.get(Translations.STORAGE_ERROR));
            return;
        }
        tap(search);
        handler.postDelayed(() -> enterSearch(title, chapter, page, totalPages), STEP_DELAY);
    }

    private void enterSearch(String title, int chapter, int page, int totalPages) {
        EditText field = ViewTree.firstVisibleEditText(root());
        if (field == null) {
            toast(translations.get(Translations.STORAGE_ERROR));
            return;
        }
        field.requestFocus();
        field.setText(title);
        field.setSelection(field.length());
        field.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        field.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        InputMethodManager input = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (input != null) input.hideSoftInputFromWindow(field.getWindowToken(), 0);
        handler.postDelayed(() -> selectSearchResult(title, chapter, page, totalPages), STEP_DELAY * 2);
    }

    private void selectSearchResult(String title, int chapter, int page, int totalPages) {
        View result = ViewTree.firstClickableContaining(root(), title);
        if (result == null) result = ViewTree.firstLargeClickable(root());
        if (result == null) {
            toast(translations.get(Translations.STORAGE_ERROR));
            return;
        }
        tap(result);
        handler.postDelayed(() -> selectChapter(chapter, page, totalPages, 0), STEP_DELAY * 2);
    }

    private void selectChapter(int chapter, int page, int totalPages, int attempts) {
        View chapterRow = ViewTree.firstClickableStartingWithNumber(root(), chapter);
        if (chapterRow != null) {
            tap(chapterRow);
            handler.postDelayed(() -> jumpToPage(page, totalPages, 0), STEP_DELAY * 2);
            return;
        }
        if (attempts < 5) {
            handler.postDelayed(() -> selectChapter(chapter, page, totalPages, attempts + 1), STEP_DELAY);
            return;
        }
        EditText chapterSearch = ViewTree.firstVisibleEditText(root());
        if (chapterSearch != null && attempts == 5) {
            chapterSearch.setText(Integer.toString(chapter));
            chapterSearch.setSelection(chapterSearch.length());
            handler.postDelayed(() -> selectChapter(chapter, page, totalPages, attempts + 1), STEP_DELAY);
            return;
        }
        if (attempts < 10) {
            handler.postDelayed(() -> selectChapter(chapter, page, totalPages, attempts + 1), STEP_DELAY);
        } else {
            toast(translations.get(Translations.STORAGE_ERROR));
        }
    }

    private void jumpToPage(int page, int expectedTotalPages, int attempts) {
        View root = root();
        ViewGroup dots = ViewTree.findPageDots(root);
        if (dots == null) {
            if (attempts < 6) handler.postDelayed(() -> jumpToPage(page, expectedTotalPages, attempts + 1), STEP_DELAY);
            else toast(translations.get(Translations.STORAGE_ERROR));
            return;
        }
        int total = dots.getChildCount() > 0 ? dots.getChildCount() : expectedTotalPages;
        int target = Math.max(1, Math.min(page, total));
        NetworkObserver.markReaderPage(target);
        Rect rect = ViewTree.bounds(dots);
        int[] rootLocation = new int[2];
        root.getLocationOnScreen(rootLocation);
        float x = rect.left - rootLocation[0] + rect.width() * ((target - 0.5f) / total);
        float y = rect.centerY() - rootLocation[1];
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        activity.dispatchTouchEvent(down);
        activity.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    private void openInternalRoute(Uri destination) {
        Intent intent = new Intent(Intent.ACTION_VIEW, destination);
        intent.setClass(activity, activity.getClass());
        activity.startActivity(intent);
    }

    private void tap(View target) {
        if (target.isClickable() &&
                target.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null)) return;
        View root = root();
        Rect rect = ViewTree.bounds(target);
        int[] rootLocation = new int[2];
        root.getLocationOnScreen(rootLocation);
        float x = rect.centerX() - rootLocation[0];
        float y = rect.centerY() - rootLocation[1];
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        activity.dispatchTouchEvent(down);
        activity.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }
}
