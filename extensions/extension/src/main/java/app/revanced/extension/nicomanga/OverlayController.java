package app.revanced.extension.nicomanga;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class OverlayController {
    private static final int BACKGROUND = Color.rgb(8, 8, 10);
    private static final int CARD = Color.rgb(28, 28, 32);
    private static final int ACCENT = Color.rgb(240, 201, 104);
    private static final int MUTED = Color.rgb(145, 145, 154);
    private static final long TICK_MILLIS = 650L;

    private final Activity activity;
    private final FrameLayout root;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ReVancedPreferences preferences;
    private final Translations translations;
    private final NavigationController navigation;
    private final LibraryWebView library;
    private final LinearLayout bottomBar;
    private final List<Button> tabButtons = new ArrayList<>();
    private final Button settingsEntry;
    private final Button addToList;
    private final boolean fabric;
    private final Runnable tick = this::tick;

    private MangaSnapshot currentManga;
    private String rememberedTitle;
    private String lastProgressSignature;
    private WeakReference<View> developmentCard = new WeakReference<>(null);
    private int developmentExpandedHeight = -1;
    private android.graphics.Rect developmentBounds;
    private final List<View> detailShiftedViews = new ArrayList<>();
    private WeakReference<ViewGroup> detailSpacingParent = new WeakReference<>(null);
    private int currentSection;
    private boolean destroyed;

    OverlayController(Activity activity) {
        this.activity = activity;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            throw new IllegalStateException("Android content root is not a FrameLayout");
        }
        root = (FrameLayout) content;
        boolean usesFabric = false;
        for (View view : ViewTree.flatten(root)) {
            if (view.getClass().getName().contains("ReactSurfaceView")) {
                usesFabric = true;
                break;
            }
        }
        fabric = usesFabric;
        preferences = new ReVancedPreferences(activity);
        translations = Translations.from(activity);
        navigation = new NavigationController(activity, translations);
        currentManga = preferences.lastManga();
        rememberedTitle = currentManga == null ? null : currentManga.title;

        library = new LibraryWebView(activity, navigation, translations);
        library.setTag(ViewTree.OVERLAY_TAG);
        FrameLayout.LayoutParams libraryParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        libraryParams.bottomMargin = dp(72);
        root.addView(library, libraryParams);

        bottomBar = createBottomBar();
        bottomBar.setTag(ViewTree.OVERLAY_TAG);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72), Gravity.BOTTOM);
        root.addView(bottomBar, barParams);

        settingsEntry = createFloatingButton(translations.get(Translations.TITLE));
        settingsEntry.setTag(ViewTree.OVERLAY_TAG);
        settingsEntry.setOnClickListener(view -> showSettingsDialog());
        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(dp(230), dp(48), Gravity.TOP | Gravity.END);
        settingsParams.topMargin = dp(72);
        settingsParams.rightMargin = dp(14);
        root.addView(settingsEntry, settingsParams);

        addToList = createFloatingButton("＋ " + translations.get(Translations.ADD_LIST));
        addToList.setTag(ViewTree.OVERLAY_TAG);
        addToList.setOnClickListener(view -> addCurrentManga());
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(dp(154), dp(44), Gravity.TOP | Gravity.END);
        addParams.rightMargin = dp(14);
        root.addView(addToList, addParams);

        bottomBar.setVisibility(View.GONE);
        settingsEntry.setVisibility(View.GONE);
        addToList.setVisibility(View.GONE);
        handler.post(tick);
    }

    private LinearLayout createBottomBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(BACKGROUND);
        bar.setElevation(dp(12));
        bar.setPadding(0, dp(2), 0, dp(5));
        if (translations.isRtl()) bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addTab(bar, "⌂", translations.get(Translations.HOME), () -> selectSection(0));
        addTab(bar, "☰", translations.get(Translations.LIST), () -> selectSection(1));
        addTab(bar, "◷", translations.get(Translations.HISTORY), () -> selectSection(2));
        addTab(bar, "⚙", translations.get(Translations.SETTINGS), () -> selectSection(3));
        updateTabColors();
        return bar;
    }

    private void addTab(LinearLayout parent, String icon, String label, Runnable action) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(icon + "\n" + label);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(3), 0, dp(3), 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setSingleLine(false);
        button.setOnClickListener(view -> action.run());
        button.setTag(ViewTree.OVERLAY_TAG);
        parent.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        tabButtons.add(button);
    }

    private Button createFloatingButton(String label) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setPadding(dp(10), 0, dp(10), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), Color.rgb(70, 70, 78));
        button.setBackground(background);
        button.setElevation(dp(8));
        return button;
    }

    private void selectSection(int section) {
        currentSection = section;
        updateTabColors();
        settingsEntry.setVisibility(View.GONE);
        addToList.setVisibility(View.GONE);
        switch (section) {
            case 0:
                library.hide();
                NetworkObserver.markHome();
                navigation.clickNativeTab(0);
                break;
            case 1:
                showLibrary("list");
                break;
            case 2:
                showLibrary("history");
                break;
            case 3:
                library.hide();
                navigation.clickNativeTab(-1);
                settingsEntry.setVisibility(View.VISIBLE);
                settingsEntry.bringToFront();
                break;
            default:
                break;
        }
    }

    private void showLibrary(String screen) {
        library.show(screen);
        library.bringToFront();
        bottomBar.bringToFront();
    }

    private void updateTabColors() {
        for (int index = 0; index < tabButtons.size(); index++) {
            tabButtons.get(index).setTextColor(index == currentSection ? ACCENT : MUTED);
        }
    }

    private void tick() {
        if (destroyed || activity.isFinishing()) return;
        ViewTree.hideAdViews(root);
        MangaSnapshot observedManga = NetworkObserver.currentManga();
        if (observedManga != null) {
            currentManga = observedManga;
            rememberedTitle = observedManga.title;
            preferences.setLastManga(observedManga);
        }
        List<View> nativeTabs = ViewTree.bottomTabs(root);
        boolean bypass = preferences.isBypassMode();
        ReadingProgress progress = fabric
                ? NetworkObserver.currentReadingProgress()
                : ViewTree.readerProgress(root, currentManga);
        if (fabric && progress == null) progress = ViewTree.readerProgress(root, currentManga);
        if (fabric && progress != null) {
            NetworkObserver.markReader();
            NetworkObserver.setReaderTotalPages(progress.totalPages);
            progress = new ReadingProgress(
                    progress.manga,
                    progress.chapter,
                    Math.min(NetworkObserver.readerPage(), progress.totalPages),
                    progress.totalPages);
        }
        boolean readerScreen = fabric &&
                (NetworkObserver.screen() == NetworkObserver.SCREEN_READER || progress != null);
        boolean detailScreen = fabric && !readerScreen &&
                NetworkObserver.screen() == NetworkObserver.SCREEN_DETAIL;
        boolean onNativeRoot = !nativeTabs.isEmpty() && !detailScreen && !readerScreen;
        if (!detailScreen || !bypass) clearDetailButtonSpace();

        if (bypass && (onNativeRoot || library.isOpen())) {
            bottomBar.setVisibility(View.VISIBLE);
            bottomBar.bringToFront();
        } else {
            bottomBar.setVisibility(View.GONE);
        }

        int selectedNative = ViewTree.selectedTab(nativeTabs);
        boolean settingsScreen = onNativeRoot && selectedNative == nativeTabs.size() - 1;
        if ((!bypass && (settingsScreen || currentSection == 3) && onNativeRoot) ||
                (bypass && currentSection == 3 && onNativeRoot)) {
            settingsEntry.setVisibility(View.VISIBLE);
            settingsEntry.bringToFront();
        } else if (currentSection != 3 || !onNativeRoot) {
            settingsEntry.setVisibility(View.GONE);
        }

        if (onNativeRoot && (fabric || selectedNative == 0 || (bypass && currentSection == 0))) {
            updateDevelopmentNotice();
            addToList.setVisibility(View.GONE);
        } else if (detailScreen && !library.isOpen()) {
            if (bypass && currentManga != null) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) addToList.getLayoutParams();
                params.topMargin = prepareDetailButtonSpace();
                addToList.setLayoutParams(params);
                addToList.setVisibility(View.VISIBLE);
                addToList.bringToFront();
            }
        } else if (!fabric && !onNativeRoot && !library.isOpen()) {
            MangaSnapshot snapshot = ViewTree.detailSnapshot(root, rememberedTitle);
            if (snapshot != null) {
                currentManga = snapshot;
                rememberedTitle = snapshot.title;
                preferences.setLastManga(snapshot);
                if (bypass) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) addToList.getLayoutParams();
                    params.topMargin = Math.max(dp(100), (int) (root.getHeight() * 0.49f));
                    addToList.setLayoutParams(params);
                    addToList.setVisibility(View.VISIBLE);
                    addToList.bringToFront();
                }
            } else {
                addToList.setVisibility(View.GONE);
                if (bypass && progress != null && !progress.signature().equals(lastProgressSignature)) {
                    lastProgressSignature = progress.signature();
                    library.upsertHistory(progress);
                }
            }
        } else if (readerScreen && !library.isOpen()) {
            clearDetailButtonSpace();
            addToList.setVisibility(View.GONE);
            if (bypass && progress != null && !progress.signature().equals(lastProgressSignature)) {
                lastProgressSignature = progress.signature();
                library.upsertHistory(progress);
            }
        } else {
            clearDetailButtonSpace();
            addToList.setVisibility(View.GONE);
        }

        handler.postDelayed(tick, TICK_MILLIS);
    }

    private void updateDevelopmentNotice() {
        View card = developmentCard.get();
        if (card == null || card.getParent() == null) {
            card = ViewTree.findDevelopmentCard(root);
            developmentCard = new WeakReference<>(card);
        }
        if (card != null) setDevelopmentSectionVisible(card, preferences.showDevelopmentNotice());
    }

    private void setDevelopmentSectionVisible(View card, boolean visible) {
        if (!(card.getParent() instanceof ViewGroup)) {
            card.setVisibility(visible ? View.VISIBLE : View.GONE);
            return;
        }
        ViewGroup container = (ViewGroup) card.getParent();
        android.graphics.Rect section = ViewTree.bounds(card);
        if (section.width() > 0 && section.height() > 0) {
            developmentBounds = new android.graphics.Rect(section);
        } else if (developmentBounds != null) {
            section = new android.graphics.Rect(developmentBounds);
        }
        ViewGroup.LayoutParams containerParams = container.getLayoutParams();
        if (visible) {
            if (developmentExpandedHeight > 0 && containerParams != null) {
                containerParams.height = developmentExpandedHeight;
                container.setLayoutParams(containerParams);
            }
            for (int index = 0; index < container.getChildCount(); index++) {
                View child = container.getChildAt(index);
                android.graphics.Rect rect = ViewTree.bounds(child);
                if (rect.bottom > section.top && rect.top < section.bottom) child.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (containerParams != null && developmentExpandedHeight < 0) {
            developmentExpandedHeight = containerParams.height;
        }
        int contentTop = ViewTree.bounds(container).top;
        int collapsedBottom = contentTop;
        for (int index = 0; index < container.getChildCount(); index++) {
            View child = container.getChildAt(index);
            android.graphics.Rect rect = ViewTree.bounds(child);
            if (rect.bottom > section.top && rect.top < section.bottom) {
                child.setVisibility(View.GONE);
            } else if (child.getVisibility() == View.VISIBLE) {
                collapsedBottom = Math.max(collapsedBottom, rect.bottom);
            }
        }
        if (containerParams != null && collapsedBottom > contentTop) {
            containerParams.height = collapsedBottom - contentTop;
            container.setLayoutParams(containerParams);
        }
    }

    private void addCurrentManga() {
        if (currentManga == null) return;
        library.upsertList(currentManga);
        Toast.makeText(activity, translations.get(Translations.ADDED), Toast.LENGTH_SHORT).show();
    }

    private int prepareDetailButtonSpace() {
        TextView label = ViewTree.detailViewsLabel(root);
        if (label == null || !(label.getParent() instanceof ViewGroup)) {
            return Math.max(dp(100), (int) (root.getHeight() * 0.51f));
        }
        ViewGroup parent = (ViewGroup) label.getParent();
        if (detailSpacingParent.get() != parent) {
            clearDetailButtonSpace();
            detailSpacingParent = new WeakReference<>(parent);
        }
        int gap = dp(52);
        int cutoff = ViewTree.bounds(label).bottom;
        if (detailShiftedViews.isEmpty()) {
            for (int index = 0; index < parent.getChildCount(); index++) {
                View child = parent.getChildAt(index);
                if (child != label && ViewTree.bounds(child).top >= cutoff - dp(2)) {
                    child.setTranslationY(gap);
                    detailShiftedViews.add(child);
                }
            }
        }
        return cutoff - ViewTree.bounds(root).top + dp(4);
    }

    private void clearDetailButtonSpace() {
        for (View view : detailShiftedViews) view.setTranslationY(0f);
        detailShiftedViews.clear();
        detailSpacingParent.clear();
    }

    private void showSettingsDialog() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(10), dp(22), dp(4));
        if (translations.isRtl()) panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView modeLabel = new TextView(activity);
        modeLabel.setText(translations.get(Translations.MODE));
        modeLabel.setTextSize(17);
        modeLabel.setTextColor(Color.WHITE);
        panel.addView(modeLabel);

        RadioGroup modes = new RadioGroup(activity);
        modes.setOrientation(LinearLayout.VERTICAL);
        RadioButton bypass = new RadioButton(activity);
        bypass.setText(translations.get(Translations.BYPASS));
        bypass.setTextColor(Color.WHITE);
        RadioButton login = new RadioButton(activity);
        login.setText(translations.get(Translations.LOGIN));
        login.setTextColor(Color.WHITE);
        modes.addView(bypass);
        modes.addView(login);
        (preferences.isBypassMode() ? bypass : login).setChecked(true);
        modes.setOnCheckedChangeListener((group, checkedId) -> {
            preferences.setBypassMode(bypass.isChecked());
            if (!bypass.isChecked()) {
                library.hide();
                currentSection = 3;
            }
        });
        panel.addView(modes);

        Switch development = new Switch(activity);
        development.setText(translations.get(Translations.DEV_NOTICE));
        development.setTextColor(Color.WHITE);
        development.setChecked(preferences.showDevelopmentNotice());
        development.setPadding(0, dp(12), 0, dp(8));
        development.setOnCheckedChangeListener((button, checked) -> {
            preferences.setShowDevelopmentNotice(checked);
            View card = developmentCard.get();
            if (card != null) setDevelopmentSectionVisible(card, checked);
        });
        panel.addView(development);

        new AlertDialog.Builder(activity)
                .setTitle(translations.get(Translations.TITLE))
                .setView(panel)
                .setPositiveButton(translations.get(Translations.CLOSE), null)
                .show();
    }

    void captureSelection(float rawX, float rawY) {
        if (NetworkObserver.screen() == NetworkObserver.SCREEN_READER) {
            ViewGroup dots = ViewTree.findPageDots(root);
            if (dots != null) {
                android.graphics.Rect rect = ViewTree.bounds(dots);
                int total = Math.max(1, dots.getChildCount());
                if (rect.contains((int) rawX, (int) rawY)) {
                    int page = (int) (((rawX - rect.left) * total) / Math.max(1, rect.width())) + 1;
                    NetworkObserver.markReaderPage(Math.min(total, page));
                } else if (rawY > root.getHeight() * 0.90f) {
                    int page = NetworkObserver.readerPage();
                    if (rawX < root.getWidth() * 0.28f) page--;
                    if (rawX > root.getWidth() * 0.72f) page++;
                    NetworkObserver.markReaderPage(Math.max(1, Math.min(total, page)));
                }
            }
        }
        if (rawX < root.getWidth() * 0.20f && rawY < root.getHeight() * 0.20f) {
            NetworkObserver.markBack();
        }
        List<View> tabs = ViewTree.bottomTabs(root);
        if (!tabs.isEmpty()) {
            if (rawY > root.getHeight() * 0.80f) {
                int closest = 0;
                int distance = Integer.MAX_VALUE;
                for (int index = 0; index < tabs.size(); index++) {
                    int candidate = Math.abs(ViewTree.bounds(tabs.get(index)).centerX() - (int) rawX);
                    if (candidate < distance) {
                        distance = candidate;
                        closest = index;
                    }
                }
                if (!preferences.isBypassMode()) currentSection = closest == tabs.size() - 1 ? 3 : 0;
                return;
            }
            String title = ViewTree.mangaTitleAt(root, rawX, rawY);
            if (title != null) rememberedTitle = title;
        }
    }

    boolean handleBack() {
        if (!library.isOpen()) return false;
        library.hide();
        currentSection = 0;
        updateTabColors();
        navigation.clickNativeTab(0);
        return true;
    }

    void destroy() {
        if (destroyed) return;
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        library.dispose();
        root.removeView(library);
        root.removeView(bottomBar);
        root.removeView(settingsEntry);
        root.removeView(addToList);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
