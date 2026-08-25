package app.revanced.extension.nicomanga;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

/** Removes the Fabric development section before a frame can display it. */
final class DevelopmentNoticeController implements ViewTreeObserver.OnPreDrawListener {
    private final View decor;
    private final ReVancedPreferences preferences;
    private final Map<View, Integer> sectionVisibility = new LinkedHashMap<>();

    private WeakReference<View> card = new WeakReference<>(null);
    private WeakReference<ViewGroup> container = new WeakReference<>(null);
    private WeakReference<ScrollView> scroll = new WeakReference<>(null);
    private int containerHeight;
    private int scrollHeight;
    private int containerBottom;
    private int scrollBottom;
    private int containerMinimumHeight;
    private boolean fillViewport;
    private int overScrollMode;
    private boolean verticalScrollBarEnabled;
    private boolean collapsed;
    private boolean disposed;

    DevelopmentNoticeController(Activity activity) {
        decor = activity.getWindow().getDecorView();
        preferences = new ReVancedPreferences(activity);
        decor.getViewTreeObserver().addOnPreDrawListener(this);
    }

    void setVisible(boolean visible) {
        preferences.setShowDevelopmentNotice(visible);
        if (visible) restore();
        decor.requestLayout();
        decor.invalidate();
    }

    @Override
    public boolean onPreDraw() {
        if (disposed) return true;
        if (preferences.showDevelopmentNotice()) {
            restore();
            return true;
        }

        View currentCard = card.get();
        if (currentCard == null || !currentCard.isAttachedToWindow()) {
            restore();
            clearCapture();
            currentCard = ViewTree.findDevelopmentCard(decor);
            if (currentCard != null) capture(currentCard);
        }
        collapse();
        return true;
    }

    void dispose() {
        if (disposed) return;
        disposed = true;
        restore();
        ViewTreeObserver observer = decor.getViewTreeObserver();
        if (observer.isAlive()) observer.removeOnPreDrawListener(this);
        clearCapture();
    }

    private void capture(View developmentCard) {
        if (!(developmentCard.getParent() instanceof ViewGroup)) return;
        ViewGroup content = (ViewGroup) developmentCard.getParent();
        ScrollView contentScroll = content.getParent() instanceof ScrollView
                ? (ScrollView) content.getParent()
                : null;

        android.graphics.Rect section = ViewTree.bounds(developmentCard);
        if (section.width() <= 0 || section.height() <= 0) return;

        sectionVisibility.clear();
        for (int index = 0; index < content.getChildCount(); index++) {
            View child = content.getChildAt(index);
            android.graphics.Rect bounds = ViewTree.bounds(child);
            if (bounds.bottom > section.top && bounds.top < section.bottom) {
                sectionVisibility.put(child, child.getVisibility());
            }
        }
        if (sectionVisibility.isEmpty()) return;

        card = new WeakReference<>(developmentCard);
        container = new WeakReference<>(content);
        scroll = new WeakReference<>(contentScroll);
        ViewGroup.LayoutParams contentParams = content.getLayoutParams();
        containerHeight = contentParams == null ? ViewGroup.LayoutParams.WRAP_CONTENT : contentParams.height;
        containerBottom = content.getBottom();
        containerMinimumHeight = content.getMinimumHeight();
        if (contentScroll != null) {
            ViewGroup.LayoutParams scrollParams = contentScroll.getLayoutParams();
            scrollHeight = scrollParams == null ? ViewGroup.LayoutParams.WRAP_CONTENT : scrollParams.height;
            scrollBottom = contentScroll.getBottom();
            fillViewport = contentScroll.isFillViewport();
            overScrollMode = contentScroll.getOverScrollMode();
            verticalScrollBarEnabled = contentScroll.isVerticalScrollBarEnabled();
        }
    }

    private void collapse() {
        View currentCard = card.get();
        ViewGroup content = container.get();
        if (currentCard == null || content == null || !currentCard.isAttachedToWindow()) return;

        int contentTop = ViewTree.bounds(content).top;
        int collapsedBottom = contentTop;
        for (int index = 0; index < content.getChildCount(); index++) {
            View child = content.getChildAt(index);
            if (sectionVisibility.containsKey(child)) {
                child.setVisibility(View.GONE);
            } else if (child.getVisibility() == View.VISIBLE) {
                collapsedBottom = Math.max(collapsedBottom, ViewTree.bounds(child).bottom);
            }
        }
        int collapsedHeight = collapsedBottom - contentTop;
        if (collapsedHeight <= 0) return;

        ScrollView contentScroll = scroll.get();
        if (contentScroll != null) {
            if (!collapsed && contentScroll.getScrollY() != 0) contentScroll.scrollTo(0, 0);
            int expectedBottom = contentScroll.getTop() + collapsedHeight;
            if (contentScroll.getBottom() > expectedBottom) scrollBottom = contentScroll.getBottom();
            setHeight(contentScroll, collapsedHeight);
            contentScroll.setBottom(expectedBottom);
            contentScroll.setFillViewport(false);
            contentScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            contentScroll.setVerticalScrollBarEnabled(false);
        }

        int expectedContentBottom = content.getTop() + collapsedHeight;
        if (content.getBottom() > expectedContentBottom) containerBottom = content.getBottom();
        content.setMinimumHeight(0);
        setHeight(content, collapsedHeight);
        content.setBottom(expectedContentBottom);
        collapsed = true;
    }

    private void restore() {
        if (!collapsed) return;
        ViewGroup content = container.get();
        ScrollView contentScroll = scroll.get();
        for (Map.Entry<View, Integer> entry : sectionVisibility.entrySet()) {
            entry.getKey().setVisibility(entry.getValue());
        }
        if (content != null) {
            content.setMinimumHeight(containerMinimumHeight);
            setHeight(content, containerHeight);
            content.setBottom(containerBottom);
            content.requestLayout();
        }
        if (contentScroll != null) {
            setHeight(contentScroll, scrollHeight);
            contentScroll.setBottom(scrollBottom);
            contentScroll.setFillViewport(fillViewport);
            contentScroll.setOverScrollMode(overScrollMode);
            contentScroll.setVerticalScrollBarEnabled(verticalScrollBarEnabled);
            contentScroll.requestLayout();
        }
        collapsed = false;
    }

    private void clearCapture() {
        card.clear();
        container.clear();
        scroll.clear();
        sectionVisibility.clear();
        collapsed = false;
    }

    private static void setHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null || params.height == height) return;
        params.height = height;
        view.setLayoutParams(params);
    }
}
