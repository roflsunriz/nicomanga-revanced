package app.revanced.extension.nicomanga;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

final class NicomangaSettingsView {
    interface Listener {
        void onModeChanged(boolean bypass);
        void onDevelopmentNoticeChanged(boolean visible);
    }

    private static final int BACKGROUND = Color.rgb(15, 15, 16);
    private static final int CARD = Color.rgb(28, 28, 29);
    private static final int DIVIDER = Color.rgb(58, 58, 60);
    private static final int TEXT = Color.rgb(225, 225, 228);
    private static final int MUTED = Color.rgb(170, 170, 174);

    private final Activity activity;
    private final FrameLayout root;
    private final ReVancedPreferences preferences;
    private final Translations translations;
    private final Listener listener;
    private final LinearLayout entryCard;
    private final FrameLayout page;
    private final RadioButton bypassMode;
    private final RadioButton loginMode;
    private final Switch developmentNotice;
    private boolean synchronizing;

    NicomangaSettingsView(
            Activity activity,
            FrameLayout root,
            ReVancedPreferences preferences,
            Translations translations,
            Listener listener
    ) {
        this.activity = activity;
        this.root = root;
        this.preferences = preferences;
        this.translations = translations;
        this.listener = listener;

        entryCard = createEntryCard();
        entryCard.setTag(ViewTree.OVERLAY_TAG);
        entryCard.setVisibility(View.GONE);
        root.addView(entryCard, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));

        page = createPage();
        page.setTag(ViewTree.OVERLAY_TAG);
        page.setVisibility(View.GONE);
        FrameLayout.LayoutParams pageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        pageParams.bottomMargin = dp(72);
        root.addView(page, pageParams);

        bypassMode = page.findViewWithTag("mode-bypass");
        loginMode = page.findViewWithTag("mode-login");
        developmentNotice = page.findViewWithTag("development-notice");
        bindSettings();
    }

    void showEntry(int top, int bottomLimit) {
        if (isPageOpen()) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) entryCard.getLayoutParams();
        params.leftMargin = dp(15);
        params.rightMargin = dp(15);
        params.topMargin = Math.max(dp(88), Math.min(top, bottomLimit - dp(108)));
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = dp(96);
        entryCard.setLayoutParams(params);
        entryCard.setVisibility(View.VISIBLE);
        entryCard.bringToFront();
    }

    void hideEntry() {
        entryCard.setVisibility(View.GONE);
    }

    void showPage() {
        synchronizeControls();
        hideEntry();
        page.setVisibility(View.VISIBLE);
        page.bringToFront();
    }

    void hidePage() {
        page.setVisibility(View.GONE);
    }

    boolean isPageOpen() {
        return page.getVisibility() == View.VISIBLE;
    }

    boolean handleBack() {
        if (!isPageOpen()) return false;
        hidePage();
        return true;
    }

    void bringPageToFront() {
        if (isPageOpen()) page.bringToFront();
    }

    void dispose() {
        root.removeView(page);
        root.removeView(entryCard);
    }

    private LinearLayout createEntryCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(6));
        card.setBackground(cardBackground());
        if (translations.isRtl()) card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = text(translations.get(Translations.TITLE), 18, TEXT, Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        card.addView(divider(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        FrameLayout row = new FrameLayout(activity);
        TextView label = text(translations.get(Translations.SETTINGS), 18, TEXT, Typeface.NORMAL);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START);
        row.addView(label, labelParams);
        TextView chevron = text(translations.isRtl() ? "‹" : "›", 30, MUTED, Typeface.NORMAL);
        FrameLayout.LayoutParams chevronParams = new FrameLayout.LayoutParams(
                dp(34), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, chevronParams);
        row.setOnClickListener(view -> showPage());
        row.setClickable(true);
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return card;
    }

    private FrameLayout createPage() {
        FrameLayout result = new FrameLayout(activity);
        result.setBackgroundColor(BACKGROUND);

        FrameLayout header = new FrameLayout(activity);
        header.setBackgroundColor(Color.BLACK);
        TextView back = text(translations.isRtl() ? "›" : "‹", 38, TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> hidePage());
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
                dp(64), dp(64), Gravity.START | Gravity.BOTTOM);
        header.addView(back, backParams);
        TextView title = text(translations.get(Translations.TITLE), 22, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64), Gravity.BOTTOM));
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(88), Gravity.TOP);
        result.addView(header, headerParams);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(15), dp(16), dp(15), dp(24));
        if (translations.isRtl()) content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.addView(createModeCard(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(178)));
        LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(106));
        noticeParams.topMargin = dp(16);
        content.addView(createNoticeCard(), noticeParams);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.topMargin = dp(88);
        result.addView(scroll, scrollParams);
        return result;
    }

    private LinearLayout createModeCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(8));
        card.setBackground(cardBackground());
        TextView title = text(translations.get(Translations.MODE), 18, TEXT, Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        card.addView(divider(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        RadioGroup modes = new RadioGroup(activity);
        modes.setOrientation(LinearLayout.VERTICAL);
        RadioButton bypass = radio(translations.get(Translations.BYPASS), "mode-bypass");
        RadioButton login = radio(translations.get(Translations.LOGIN), "mode-login");
        modes.addView(bypass, new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        modes.addView(login, new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        card.addView(modes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        modes.setOnCheckedChangeListener((group, checkedId) -> {
            if (synchronizing) return;
            boolean bypassSelected = checkedId == bypass.getId();
            preferences.setBypassMode(bypassSelected);
            listener.onModeChanged(bypassSelected);
        });
        return card;
    }

    private LinearLayout createNoticeCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(8));
        card.setBackground(cardBackground());
        TextView title = text(translations.get(Translations.HOME), 18, TEXT, Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        card.addView(divider(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        Switch notice = new Switch(activity);
        notice.setTag("development-notice");
        notice.setText(translations.get(Translations.DEV_NOTICE));
        notice.setTextColor(TEXT);
        notice.setTextSize(17);
        notice.setTypeface(Typeface.SERIF);
        notice.setGravity(Gravity.CENTER_VERTICAL);
        notice.setOnCheckedChangeListener((button, checked) -> {
            if (synchronizing) return;
            preferences.setShowDevelopmentNotice(checked);
            listener.onDevelopmentNoticeChanged(checked);
        });
        card.addView(notice, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return card;
    }

    private void bindSettings() {
        synchronizeControls();
    }

    private void synchronizeControls() {
        synchronizing = true;
        (preferences.isBypassMode() ? bypassMode : loginMode).setChecked(true);
        developmentNotice.setChecked(preferences.showDevelopmentNotice());
        synchronizing = false;
    }

    private RadioButton radio(String label, String tag) {
        RadioButton button = new RadioButton(activity);
        button.setId(View.generateViewId());
        button.setTag(tag);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(17);
        button.setTypeface(Typeface.SERIF);
        button.setGravity(Gravity.CENTER_VERTICAL);
        return button;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(Typeface.SERIF, style));
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private View divider() {
        View view = new View(activity);
        view.setBackgroundColor(DIVIDER);
        return view;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(10));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
