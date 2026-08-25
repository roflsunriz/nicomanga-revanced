package app.revanced.extension.nicomanga;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

final class LibraryWebView extends FrameLayout {
    private static final String ORIGIN = "https://nicomanga-revanced.local/";
    private final Activity activity;
    private final WebView webView;
    private final NavigationController navigation;
    private final Translations translations;
    private final List<String> queue = new ArrayList<>();
    private boolean ready;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    LibraryWebView(Activity activity, NavigationController navigation, Translations translations) {
        super(activity);
        this.activity = activity;
        this.navigation = navigation;
        this.translations = translations;
        setBackgroundColor(Color.rgb(13, 13, 15));
        setVisibility(View.GONE);

        webView = new WebView(activity);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
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
            @SuppressWarnings("deprecation") // Required for Android 5-6 WebView compatibility.
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return emptyResponse();
            }

            @Override
            @SuppressWarnings("deprecation") // Required for Android 5-6 WebView compatibility.
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return emptyResponse();
            }
        });
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.loadDataWithBaseURL(ORIGIN, LocalLibraryHtml.create(translations), "text/html", "UTF-8", null);
    }

    void show(String screen) {
        setVisibility(View.VISIBLE);
        bringToFront();
        evaluate("window.NMR&&window.NMR.show(" + JSONObject.quote(screen) + ")");
    }

    void hide() {
        setVisibility(View.GONE);
    }

    boolean isOpen() {
        return getVisibility() == View.VISIBLE;
    }

    void dispose() {
        ready = false;
        queue.clear();
        webView.removeJavascriptInterface("Android");
        webView.stopLoading();
        webView.destroy();
    }

    void upsertList(MangaSnapshot manga) {
        try {
            evaluate("window.NMR&&window.NMR.upsertList(" + JSONObject.quote(manga.toJson().toString()) + ")");
        } catch (JSONException exception) {
            toast(exception.getMessage());
        }
    }

    void upsertHistory(ReadingProgress progress) {
        try {
            evaluate("window.NMR&&window.NMR.upsertHistory(" + JSONObject.quote(progress.toJson().toString()) + ")");
        } catch (JSONException exception) {
            toast(exception.getMessage());
        }
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

    private static WebResourceResponse emptyResponse() {
        return new WebResourceResponse("text/plain", "UTF-8",
                new ByteArrayInputStream(new byte[0]));
    }

    private void toast(String message) {
        activity.runOnUiThread(() -> Toast.makeText(activity,
                message == null ? translations.get(Translations.STORAGE_ERROR) : message,
                Toast.LENGTH_LONG).show());
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
        public void close() {
            activity.runOnUiThread(() -> LibraryWebView.this.hide());
        }

        @JavascriptInterface
        public void resume(String json) {
            activity.runOnUiThread(() -> {
                hide();
                try {
                    navigation.resume(new JSONObject(json));
                } catch (JSONException exception) {
                    toast(exception.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void storageRecovered() {
            toast(translations.get(Translations.STORAGE_ERROR));
        }
    }
}
