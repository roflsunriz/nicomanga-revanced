package app.revanced.extension.nicomanga;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ViewTree {
    static final String OVERLAY_TAG = "nicomanga-revanced-overlay";
    private static final Pattern INTEGER = Pattern.compile("(?<![\\d.])([1-9]\\d{0,4})(?![\\d.])");
    private static final String[] AD_VIEW_PREFIXES = {
            "com.applovin.", "com.facebook.ads.", "com.google.android.gms.ads.",
            "com.tradplus.", "com.vungle.", "com.bytedance.sdk.openadsdk.",
            "com.mbridge.msdk.", "com.ironsource.", "com.fyber.", "com.chartboost.",
            "com.unity3d.ads.", "sg.bigo.ads.", "expo.modules.tradplusad."
    };

    private ViewTree() {}

    static List<View> flatten(View root) {
        if (root == null) return Collections.emptyList();
        ArrayList<View> result = new ArrayList<>();
        append(root, result);
        return result;
    }

    private static void append(View view, List<View> result) {
        if (OVERLAY_TAG.equals(view.getTag())) return;
        result.add(view);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            append(group.getChildAt(index), result);
        }
    }

    static List<View> bottomTabs(View root) {
        int width = root.getWidth();
        int height = root.getHeight();
        if (width <= 0 || height <= 0) return Collections.emptyList();
        List<View> labels = new ArrayList<>();
        for (View view : flatten(root)) {
            if (!(view instanceof TextView) || !isShown(view)) continue;
            Rect rect = bounds(view);
            CharSequence text = ((TextView) view).getText();
            if (rect.top < height * 0.80 || rect.width() > width * 0.35 || rect.height() > height * 0.09
                    || text == null || text.length() == 0 || text.length() > 40) continue;
            labels.add(view);
        }
        labels.sort(Comparator.comparingInt(view -> bounds(view).centerX()));
        List<View> clusters = new ArrayList<>();
        for (View label : labels) {
            int center = bounds(label).centerX();
            boolean represented = false;
            for (int index = 0; index < clusters.size(); index++) {
                if (Math.abs(bounds(clusters.get(index)).centerX() - center) < width * 0.09) {
                    if (bounds(label).top > bounds(clusters.get(index)).top) clusters.set(index, label);
                    represented = true;
                    break;
                }
            }
            if (!represented) clusters.add(label);
        }
        return clusters.size() >= 2 && clusters.size() <= 7 ? clusters : Collections.emptyList();
    }

    static int selectedTab(List<View> tabs) {
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).isSelected() || tabs.get(index).isActivated()) return index;
        }
        return -1;
    }

    static void hideAdViews(View root) {
        for (View view : flatten(root)) {
            String className = view.getClass().getName();
            boolean ad = false;
            for (String prefix : AD_VIEW_PREFIXES) {
                if (className.startsWith(prefix)) {
                    ad = true;
                    break;
                }
            }
            if (!ad) continue;
            View target = view;
            if (view.getParent() instanceof ViewGroup) target = (View) view.getParent();
            collapse(target);
        }
    }

    static void collapse(View view) {
        if (view == null) return;
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && (params.width != 0 || params.height != 0)) {
            params.width = 0;
            params.height = 0;
            view.setLayoutParams(params);
        }
    }

    static int nativeSettingsContentBottom(View root) {
        int rootWidth = root.getWidth();
        int rootHeight = root.getHeight();
        int cardCount = 0;
        int bottom = 0;
        for (View view : flatten(root)) {
            if (!(view instanceof ViewGroup) || !isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.width() < rootWidth * 0.78f || rect.width() > rootWidth * 0.98f ||
                    rect.height() < rootHeight * 0.08f || rect.height() > rootHeight * 0.24f ||
                    rect.left > rootWidth * 0.12f || rect.top < rootHeight * 0.07f ||
                    rect.bottom > rootHeight * 0.88f || view.getBackground() == null) continue;
            int textChildren = 0;
            for (View child : flatten(view)) {
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    if (text != null && text.length() > 0) textChildren++;
                }
            }
            if (textChildren < 2) continue;
            cardCount++;
            bottom = Math.max(bottom, rect.bottom);
        }
        return cardCount >= 3 ? bottom : -1;
    }

    static void labelNativeSettings(View root, String settingsLabel) {
        int width = root.getWidth();
        int height = root.getHeight();
        for (View view : flatten(root)) {
            if (!(view instanceof TextView) || !isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.top < height * 0.10f && Math.abs(rect.centerX() - width / 2) < width * 0.20f) {
                ((TextView) view).setText(settingsLabel);
                break;
            }
        }
        labelNativeSettingsTab(root, settingsLabel);
    }

    static void labelNativeSettingsTab(View root, String settingsLabel) {
        List<View> tabs = bottomTabs(root);
        if (!tabs.isEmpty() && tabs.get(tabs.size() - 1) instanceof TextView) {
            ((TextView) tabs.get(tabs.size() - 1)).setText(settingsLabel);
        }
    }

    static View findDevelopmentCard(View root) {
        int rootWidth = root.getWidth();
        int rootHeight = root.getHeight();
        for (View view : flatten(root)) {
            if (!(view instanceof TextView) || !isShown(view)) continue;
            CharSequence text = ((TextView) view).getText();
            if (text == null || (!text.toString().contains("現在開発中") &&
                    !text.toString().toLowerCase(java.util.Locale.ROOT).contains("under development"))) continue;
            if (!(view.getParent() instanceof ViewGroup)) continue;
            ViewGroup parent = (ViewGroup) view.getParent();
            Rect title = bounds(view);
            for (int index = 0; index < parent.getChildCount(); index++) {
                View sibling = parent.getChildAt(index);
                if (!(sibling instanceof ViewGroup)) continue;
                Rect rect = bounds(sibling);
                if (rect.width() >= rootWidth * 0.75 && rect.height() >= rootHeight * 0.16 &&
                        rect.contains(title.centerX(), title.centerY())) return sibling;
            }
        }
        View best = null;
        long bestArea = Long.MAX_VALUE;
        for (View view : flatten(root)) {
            if (!(view instanceof ViewGroup) || !isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.width() < rootWidth * 0.75 || rect.height() < rootHeight * 0.16
                    || rect.height() > rootHeight * 0.72 || rect.top < rootHeight * 0.22) continue;
            List<View> descendants = flatten(view);
            int textLength = 0;
            int textCount = 0;
            boolean hasImage = false;
            for (View child : descendants) {
                if (child instanceof ImageView) hasImage = true;
                if (child instanceof TextView) {
                    CharSequence text = ((TextView) child).getText();
                    if (text != null && text.length() > 0) {
                        textLength += text.length();
                        textCount++;
                    }
                }
            }
            if (hasImage || textLength < 220 || textCount < 2 || view.getBackground() == null) continue;
            long area = (long) rect.width() * rect.height();
            if (area < bestArea) {
                bestArea = area;
                best = view;
            }
        }
        return best;
    }

    static MangaSnapshot detailSnapshot(View root, String rememberedTitle) {
        if (!bottomTabs(root).isEmpty()) return null;
        int chapterRows = 0;
        int maxHeaderNumber = 0;
        int rootHeight = root.getHeight();
        for (View view : flatten(root)) {
            if (!isShown(view)) continue;
            CharSequence description = view.getContentDescription();
            if (description != null && startsWithInteger(description.toString())) chapterRows++;
            if (view instanceof TextView) {
                Rect rect = bounds(view);
                CharSequence text = ((TextView) view).getText();
                if (rect.top > rootHeight * 0.62 && text != null && startsWithInteger(text.toString())) chapterRows++;
                if (text == null || text.length() > 80 || rect.top > rootHeight * 0.62) continue;
                Matcher matcher = INTEGER.matcher(text);
                while (matcher.find()) maxHeaderNumber = Math.max(maxHeaderNumber, parseInt(matcher.group(1), 0));
            }
        }
        if (chapterRows < 1 || maxHeaderNumber < 1) return null;
        return new MangaSnapshot(rememberedTitle, maxHeaderNumber);
    }

    static TextView detailViewsLabel(View root) {
        int rootHeight = root.getHeight();
        for (View view : flatten(root)) {
            if (!(view instanceof TextView) || !isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.top < rootHeight * 0.35f || rect.top > rootHeight * 0.62f) continue;
            CharSequence text = ((TextView) view).getText();
            if (text == null) continue;
            String value = text.toString().trim().toLowerCase(java.util.Locale.ROOT);
            if ((value.contains("ビュー:") || value.startsWith("view:")) && value.length() < 80) {
                return (TextView) view;
            }
        }
        return null;
    }

    static ReadingProgress readerProgress(View root, MangaSnapshot manga) {
        if (manga == null) return null;
        ViewGroup dots = findPageDots(root);
        if (dots == null) return null;
        int totalPages = dots.getChildCount();
        int page = 1;
        int largestHeight = -1;
        for (int index = 0; index < totalPages; index++) {
            int height = bounds(dots.getChildAt(index)).height();
            if (height > largestHeight) {
                largestHeight = height;
                page = index + 1;
            }
        }
        int chapter = centeredHeaderNumber(root);
        return new ReadingProgress(manga, Math.max(1, chapter), page, totalPages);
    }

    static ViewGroup findPageDots(View root) {
        int rootHeight = root.getHeight();
        for (View view : flatten(root)) {
            if (!(view instanceof ViewGroup) || !isShown(view)) continue;
            ViewGroup group = (ViewGroup) view;
            if (group.getChildCount() < 5 || group.getChildCount() > 1000) continue;
            Rect groupRect = bounds(group);
            if (groupRect.top < rootHeight * 0.68) continue;
            int aligned = 0;
            int previousLeft = -1;
            for (int index = 0; index < group.getChildCount(); index++) {
                Rect child = bounds(group.getChildAt(index));
                if (child.width() > 0 && child.width() < root.getWidth() * 0.08
                        && child.height() > 0 && child.height() < rootHeight * 0.08
                        && child.left >= previousLeft) aligned++;
                previousLeft = child.left;
            }
            if (aligned == group.getChildCount()) return group;
        }
        return null;
    }

    static int centeredHeaderNumber(View root) {
        int width = root.getWidth();
        int height = root.getHeight();
        for (View view : flatten(root)) {
            if (!(view instanceof TextView) || !isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.top > height * 0.22 || Math.abs(rect.centerX() - width / 2) > width * 0.22) continue;
            CharSequence text = ((TextView) view).getText();
            if (text == null || text.length() > 30) continue;
            Matcher matcher = INTEGER.matcher(text);
            if (matcher.find()) return parseInt(matcher.group(1), 1);
        }
        return 1;
    }

    static String mangaTitleAt(View root, float x, float y) {
        String result = null;
        int smallestArea = Integer.MAX_VALUE;
        for (View view : flatten(root)) {
            if (!isShown(view)) continue;
            Rect rect = bounds(view);
            if (!rect.contains((int) x, (int) y)) continue;
            String candidate = null;
            CharSequence description = view.getContentDescription();
            if (description != null && description.length() > 2) {
                candidate = firstMeaningfulSegment(description.toString());
            }
            if (candidate == null) {
                for (View child : flatten(view)) {
                    if (!(child instanceof TextView)) continue;
                    CharSequence text = ((TextView) child).getText();
                    candidate = text == null ? null : firstMeaningfulSegment(text.toString());
                    if (candidate != null) break;
                }
            }
            if (candidate == null) continue;
            int area = rect.width() * rect.height();
            if (area < smallestArea) {
                result = candidate;
                smallestArea = area;
            }
        }
        return result;
    }

    private static String firstMeaningfulSegment(String input) {
        for (String segment : input.split("[,\\n]")) {
            String value = segment.trim();
            if (value.length() >= 3 && !value.matches("^[\\p{So}\\p{Sk}\\p{Cn}]+$")
                    && !value.matches("(?i)^(home|list|settings?)$")) return value;
        }
        return null;
    }

    static EditText firstVisibleEditText(View root) {
        for (View view : flatten(root)) if (view instanceof EditText && isShown(view)) return (EditText) view;
        return null;
    }

    static View firstClickableContaining(View root, String query) {
        String needle = query == null ? "" : query.toLowerCase(java.util.Locale.ROOT);
        View result = null;
        int smallestArea = Integer.MAX_VALUE;
        for (View view : flatten(root)) {
            if (!isShown(view)) continue;
            StringBuilder text = new StringBuilder();
            if (view.getContentDescription() != null) text.append(view.getContentDescription());
            for (View child : flatten(view)) {
                if (child instanceof TextView) text.append(' ').append(((TextView) child).getText());
            }
            if (!needle.isEmpty() && text.toString().toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                Rect rect = bounds(view);
                int area = rect.width() * rect.height();
                if (area < smallestArea) {
                    result = view;
                    smallestArea = area;
                }
            }
        }
        return result;
    }

    static View firstLargeClickable(View root) {
        int width = root.getWidth();
        int height = root.getHeight();
        for (View view : flatten(root)) {
            if (!isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.top > height * 0.18 && rect.bottom < height * 0.88
                    && rect.width() > width * 0.45 && rect.height() > height * 0.06) return view;
        }
        return null;
    }

    static View firstClickableStartingWithNumber(View root, int number) {
        Pattern exactPrefix = Pattern.compile("^\\s*" + number + "(?:\\D|$)");
        View best = null;
        View bestClickable = null;
        int smallestArea = Integer.MAX_VALUE;
        int smallestClickableArea = Integer.MAX_VALUE;
        for (View view : flatten(root)) {
            if (!isShown(view) || view instanceof EditText) continue;
            CharSequence description = view.getContentDescription();
            CharSequence text = view instanceof TextView ? ((TextView) view).getText() : null;
            boolean matches = description != null && exactPrefix.matcher(description).find();
            if (!matches) matches = text != null && exactPrefix.matcher(text).find();
            if (!matches) continue;
            Rect rect = bounds(view);
            int area = rect.width() * rect.height();
            if (view.isClickable() && area < smallestClickableArea) {
                bestClickable = view;
                smallestClickableArea = area;
            }
            if (area < smallestArea) {
                best = view;
                smallestArea = area;
            }
        }
        return bestClickable == null ? best : bestClickable;
    }

    static View topCornerButton(View root, boolean right) {
        int width = root.getWidth();
        int height = root.getHeight();
        View best = null;
        for (View view : flatten(root)) {
            if (!isShown(view)) continue;
            Rect rect = bounds(view);
            if (rect.top > height * 0.22 || rect.height() > height * 0.14) continue;
            if (right ? rect.centerX() > width * 0.72 : rect.centerX() < width * 0.28) best = view;
        }
        return best;
    }

    static Rect bounds(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    static boolean isShown(View view) {
        return view.getVisibility() == View.VISIBLE && view.isShown() && view.getWidth() > 0 && view.getHeight() > 0;
    }

    private static boolean startsWithInteger(String value) {
        return value != null && value.trim().matches("^[0-9]+.*");
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
    }
}
