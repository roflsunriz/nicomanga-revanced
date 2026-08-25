package app.revanced.extension.nicomanga;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class HomeWebView extends FrameLayout {
    private static final String ORIGIN = "https://nicomanga-revanced-home.local/";
    private final Activity activity;
    private final NavigationController navigation;
    private final WebView webView;
    private final Set<String> allowedImages = ConcurrentHashMap.newKeySet();
    private final List<String> queue = new ArrayList<>();
    private boolean ready;
    private boolean hasData;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    HomeWebView(Activity activity, NavigationController navigation, Translations translations) {
        super(activity);
        this.activity = activity;
        this.navigation = navigation;
        setBackgroundColor(Color.rgb(13, 13, 15));
        setVisibility(View.GONE);

        webView = new WebView(activity);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(false);
        webView.setBackgroundColor(Color.rgb(13, 13, 15));
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return imageOrEmpty(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return imageOrEmpty(Uri.parse(url));
            }
        });
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.loadDataWithBaseURL(ORIGIN, LocalHomeHtml.create(translations), "text/html", "UTF-8", null);
    }

    void show() {
        setVisibility(View.VISIBLE);
        bringToFront();
    }

    void hide() {
        setVisibility(View.GONE);
    }

    boolean isOpen() {
        return getVisibility() == View.VISIBLE;
    }

    void update(String payload) {
        if (payload == null || payload.isEmpty()) return;
        allowedImages.clear();
        boolean found = false;
        try {
            JSONObject root = new JSONObject(payload);
            JSONArray top = root.optJSONArray("top");
            found = top != null && top.length() > 0;
            for (String category : new String[]{"new", "top", "update"}) {
                JSONArray rows = root.optJSONArray(category);
                if (rows == null) continue;
                for (int index = 0; index < rows.length(); index++) {
                    JSONObject row = rows.optJSONObject(index);
                    if (row == null) continue;
                    String cover = row.optString("cover", "");
                    Uri uri = Uri.parse(cover);
                    if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                        allowedImages.add(cover);
                    }
                }
            }
        } catch (JSONException ignored) {
            return;
        }
        hasData = found;
        evaluate("window.NMRHome&&window.NMRHome.update(" + JSONObject.quote(payload) + ")");
    }

    boolean hasData() {
        return hasData;
    }

    void setDevelopmentNotice(boolean visible) {
        evaluate("window.NMRHome&&window.NMRHome.notice(" + visible + ")");
    }

    void dispose() {
        ready = false;
        queue.clear();
        allowedImages.clear();
        webView.removeJavascriptInterface("Android");
        webView.stopLoading();
        webView.destroy();
    }

    private void evaluate(String script) {
        activity.runOnUiThread(() -> {
            if (!ready) {
                queue.add(script);
                return;
            }
            webView.evaluateJavascript(script, null);
        });
    }

    private WebResourceResponse imageOrEmpty(Uri uri) {
        if (uri != null && allowedImages.contains(uri.toString())) return null;
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }

    private final class Bridge {
        @JavascriptInterface
        public void ready() {
            activity.runOnUiThread(() -> {
                ready = true;
                for (String script : new ArrayList<>(queue)) webView.evaluateJavascript(script, null);
                queue.clear();
            });
        }

        @JavascriptInterface
        public void openManga(String id) {
            activity.runOnUiThread(() -> {
                hide();
                navigation.openManga(id);
            });
        }

        @JavascriptInterface
        public void search() {
            activity.runOnUiThread(() -> {
                hide();
                navigation.openSearch();
            });
        }
    }
}
