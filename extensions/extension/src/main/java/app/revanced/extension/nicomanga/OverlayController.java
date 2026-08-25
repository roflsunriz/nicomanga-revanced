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
    private final Runnable tick = this::tick;

    private MangaSnapshot currentManga;
    private String rememberedTitle;
    private String lastProgressSignature;
    private WeakReference<View> developmentCard = new WeakReference<>(null);
    private int currentSection;
    private boolean destroyed;

    OverlayController(Activity activity) {
        this.activity = activity;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            throw new IllegalStateException("Android content root is not a FrameLayout");
        }
        root = (FrameLayout) content;
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
        List<View> nativeTabs = ViewTree.bottomTabs(root);
        boolean bypass = preferences.isBypassMode();
        boolean onNativeRoot = !nativeTabs.isEmpty();

        if (bypass && onNativeRoot) {
            bottomBar.setVisibility(View.VISIBLE);
            bottomBar.bringToFront();
        } else if (!library.isOpen()) {
            bottomBar.setVisibility(View.GONE);
        }

        int selectedNative = ViewTree.selectedTab(nativeTabs);
        boolean settingsScreen = onNativeRoot && selectedNative == nativeTabs.size() - 1;
        if ((!bypass && settingsScreen) || (bypass && currentSection == 3 && onNativeRoot)) {
            settingsEntry.setVisibility(View.VISIBLE);
            settingsEntry.bringToFront();
        } else if (currentSection != 3 || !onNativeRoot) {
            settingsEntry.setVisibility(View.GONE);
        }

        if (onNativeRoot && (selectedNative == 0 || (bypass && currentSection == 0))) {
            updateDevelopmentNotice();
            addToList.setVisibility(View.GONE);
        } else if (!onNativeRoot && !library.isOpen()) {
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
                ReadingProgress progress = ViewTree.readerProgress(root, currentManga);
                if (bypass && progress != null && !progress.signature().equals(lastProgressSignature)) {
                    lastProgressSignature = progress.signature();
                    library.upsertHistory(progress);
                }
            }
        } else {
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
        if (card != null) card.setVisibility(preferences.showDevelopmentNotice() ? View.VISIBLE : View.GONE);
    }

    private void addCurrentManga() {
        if (currentManga == null) return;
        library.upsertList(currentManga);
        Toast.makeText(activity, translations.get(Translations.ADDED), Toast.LENGTH_SHORT).show();
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
            if (card != null) card.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        panel.addView(development);

        new AlertDialog.Builder(activity)
                .setTitle(translations.get(Translations.TITLE))
                .setView(panel)
                .setPositiveButton(translations.get(Translations.CLOSE), null)
                .show();
    }

    void captureSelection(float rawX, float rawY) {
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
