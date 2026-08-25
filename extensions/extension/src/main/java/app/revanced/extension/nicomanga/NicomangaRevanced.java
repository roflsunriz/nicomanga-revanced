package app.revanced.extension.nicomanga;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class NicomangaRevanced {
    private static final Map<Activity, OverlayController> CONTROLLERS = new HashMap<>();
    private static final Map<Activity, DevelopmentNoticeController> DEVELOPMENT_NOTICES = new HashMap<>();
    private static boolean applicationInitialized;

    private NicomangaRevanced() {}

    public static synchronized void initializeApplication(Application application) {
        if (applicationInitialized) return;
        applicationInitialized = true;
        NetworkObserver.setContext(application);
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle state) {
                if ("com.lovehug.MainActivity".equals(activity.getClass().getName())) initialize(activity);
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
            @Override
            public void onActivityDestroyed(Activity activity) {
                synchronized (NicomangaRevanced.class) {
                    if (CONTROLLERS.containsKey(activity)) return;
                    DevelopmentNoticeController notice = DEVELOPMENT_NOTICES.remove(activity);
                    if (notice != null) notice.dispose();
                }
            }
        });
    }

    public static void initialize(Activity activity) {
        DevelopmentNoticeController developmentNotice;
        synchronized (NicomangaRevanced.class) {
            developmentNotice = DEVELOPMENT_NOTICES.get(activity);
            if (developmentNotice == null) {
                developmentNotice = new DevelopmentNoticeController(activity);
                DEVELOPMENT_NOTICES.put(activity, developmentNotice);
            }
        }
        DevelopmentNoticeController controller = developmentNotice;
        activity.getWindow().getDecorView().postDelayed(
                () -> initializeOnMainThread(activity, controller),
                2500L);
    }

    private static synchronized void initializeOnMainThread(
            Activity activity,
            DevelopmentNoticeController developmentNotice
    ) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (CONTROLLERS.containsKey(activity)) return;
        OverlayController controller = new OverlayController(activity, developmentNotice);
        CONTROLLERS.put(activity, controller);

        Window window = activity.getWindow();
        Window.Callback original = window.getCallback();
        Window.Callback proxy = (Window.Callback) Proxy.newProxyInstance(
                activity.getClassLoader(),
                new Class<?>[]{Window.Callback.class},
                (object, method, args) -> invokeCallback(controller, original, object, method, args));
        window.setCallback(proxy);

        Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity value, Bundle state) {}
            @Override public void onActivityStarted(Activity value) {}
            @Override public void onActivityResumed(Activity value) {}
            @Override public void onActivityPaused(Activity value) {}
            @Override public void onActivityStopped(Activity value) {}
            @Override public void onActivitySaveInstanceState(Activity value, Bundle state) {}

            @Override
            public void onActivityDestroyed(Activity value) {
                if (value != activity) return;
                synchronized (NicomangaRevanced.class) {
                    CONTROLLERS.remove(activity);
                    DEVELOPMENT_NOTICES.remove(activity);
                }
                if (window.getCallback() == proxy) window.setCallback(original);
                controller.destroy();
                developmentNotice.dispose();
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            }
        };
        activity.getApplication().registerActivityLifecycleCallbacks(callbacks);
    }

    private static Object invokeCallback(
            OverlayController controller,
            Window.Callback original,
            Object proxy,
            Method method,
            Object[] args
    ) throws Throwable {
        String name = method.getName();
        if ("dispatchTouchEvent".equals(name) && args != null && args.length == 1 && args[0] instanceof MotionEvent) {
            MotionEvent event = (MotionEvent) args[0];
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                controller.captureSelection(event.getRawX(), event.getRawY());
            }
        }
        if ("dispatchKeyEvent".equals(name) && args != null && args.length == 1 && args[0] instanceof KeyEvent) {
            KeyEvent event = (KeyEvent) args[0];
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (controller.handleBack()) return true;
                NetworkObserver.markBack();
            }
        }
        if ("equals".equals(name) && method.getParameterTypes().length == 1) return proxy == args[0];
        if ("hashCode".equals(name) && method.getParameterTypes().length == 0) return System.identityHashCode(proxy);
        if ("toString".equals(name) && method.getParameterTypes().length == 0) return "NicomangaReVancedWindowCallback";
        try {
            return method.invoke(original, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
