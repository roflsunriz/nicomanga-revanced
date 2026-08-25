package app.revanced.extension.nicomanga;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class Translations {
    static final String HOME = "home";
    static final String LIST = "list";
    static final String HISTORY = "history";
    static final String SETTINGS = "settings";
    static final String TITLE = "title";
    static final String MODE = "mode";
    static final String BYPASS = "bypass";
    static final String LOGIN = "login";
    static final String DEV_NOTICE = "devNotice";
    static final String CLOSE = "close";
    static final String ADD_LIST = "addList";
    static final String ADDED = "added";
    static final String EMPTY_LIST = "emptyList";
    static final String EMPTY_HISTORY = "emptyHistory";
    static final String RESUME = "resume";
    static final String REMOVE = "remove";
    static final String CHAPTER = "chapter";
    static final String PAGE = "page";
    static final String READ = "read";
    static final String STORAGE_ERROR = "storageError";
    static final String SEARCH = "search";
    static final String DEV_NOTICE_TITLE = "devNoticeTitle";
    static final String DEV_NOTICE_BODY = "devNoticeBody";

    private final Map<String, String> values;
    private final boolean rtl;

    private Translations(Map<String, String> values, boolean rtl) {
        this.values = Collections.unmodifiableMap(values);
        this.rtl = rtl;
    }

    @SuppressWarnings("deprecation") // Configuration.locale is the only platform fallback before API 24.
    static Translations from(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= 24) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            locale = context.getResources().getConfiguration().locale;
        }
        String language = locale == null ? "en" : locale.getLanguage();
        Map<String, String> map = english();
        switch (language) {
            case "ja": japanese(map); break;
            case "zh": chinese(map); break;
            case "hi": hindi(map); break;
            case "es": spanish(map); break;
            case "fr": french(map); break;
            case "ar": arabic(map); break;
            case "pt": portuguese(map); break;
            case "bn": bengali(map); break;
            case "ru": russian(map); break;
            case "ur": urdu(map); break;
            default: break;
        }
        return new Translations(map, "ar".equals(language) || "ur".equals(language)
                || TextUtils.getLayoutDirectionFromLocale(locale) == android.view.View.LAYOUT_DIRECTION_RTL);
    }

    String get(String key) {
        String value = values.get(key);
        return value == null ? key : value;
    }

    boolean isRtl() {
        return rtl;
    }

    String toJson() {
        JSONObject object = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                object.put(entry.getKey(), entry.getValue());
            }
            object.put("rtl", rtl);
        } catch (JSONException ignored) {
            return "{}";
        }
        return object.toString();
    }

    private static Map<String, String> english() {
        Map<String, String> map = new HashMap<>();
        map.put(HOME, "Home"); map.put(LIST, "List"); map.put(HISTORY, "Reading History");
        map.put(SETTINGS, "Settings"); map.put(TITLE, "Nicomanga ReVanced");
        map.put(MODE, "Mode"); map.put(BYPASS, "Login-free mode"); map.put(LOGIN, "Login mode");
        map.put(DEV_NOTICE, "Show the development notice"); map.put(CLOSE, "Close");
        map.put(ADD_LIST, "Add to List"); map.put(ADDED, "Added to List");
        map.put(EMPTY_LIST, "No manga in your List yet.");
        map.put(EMPTY_HISTORY, "Reading History is empty."); map.put(RESUME, "Resume");
        map.put(REMOVE, "Remove"); map.put(CHAPTER, "Chapter"); map.put(PAGE, "Page");
        map.put(READ, "read"); map.put(STORAGE_ERROR, "Local data was recovered after a storage error.");
        map.put(SEARCH, "Search"); map.put(DEV_NOTICE_TITLE, "Nicomanga is under development");
        map.put(DEV_NOTICE_BODY, "Some features may be incomplete while development continues.");
        return map;
    }

    private static void japanese(Map<String, String> m) {
        m.put(HOME,"ホーム"); m.put(LIST,"リスト"); m.put(HISTORY,"読書履歴"); m.put(SETTINGS,"設定");
        m.put(MODE,"利用モード"); m.put(BYPASS,"ログイン不要"); m.put(LOGIN,"ログイン利用");
        m.put(DEV_NOTICE,"「現在開発中です」を表示"); m.put(CLOSE,"閉じる"); m.put(ADD_LIST,"リストに追加");
        m.put(ADDED,"リストに追加しました"); m.put(EMPTY_LIST,"リストにマンガはありません。");
        m.put(EMPTY_HISTORY,"読書履歴はありません。"); m.put(RESUME,"続きを読む"); m.put(REMOVE,"削除");
        m.put(CHAPTER,"章"); m.put(PAGE,"ページ"); m.put(READ,"読了");
        m.put(STORAGE_ERROR,"保存エラー後にローカルデータを復旧しました。");
        m.put(SEARCH,"検索"); m.put(DEV_NOTICE_TITLE,"アプリは現在開発中です");
        m.put(DEV_NOTICE_BODY,"現在も開発を継続しています。一部の機能が未完成の場合があります。");
    }

    private static void chinese(Map<String, String> m) {
        m.put(HOME,"首页"); m.put(LIST,"列表"); m.put(HISTORY,"阅读历史"); m.put(SETTINGS,"设置");
        m.put(MODE,"模式"); m.put(BYPASS,"免登录模式"); m.put(LOGIN,"登录模式");
        m.put(DEV_NOTICE,"显示开发中通知"); m.put(CLOSE,"关闭"); m.put(ADD_LIST,"添加到列表");
        m.put(ADDED,"已添加"); m.put(EMPTY_LIST,"列表中还没有漫画。"); m.put(EMPTY_HISTORY,"阅读历史为空。");
        m.put(RESUME,"继续阅读"); m.put(REMOVE,"删除"); m.put(CHAPTER,"章节"); m.put(PAGE,"页"); m.put(READ,"已读");
    }

    private static void hindi(Map<String, String> m) {
        m.put(HOME,"होम"); m.put(LIST,"सूची"); m.put(HISTORY,"पठन इतिहास"); m.put(SETTINGS,"सेटिंग");
        m.put(MODE,"मोड"); m.put(BYPASS,"बिना लॉगिन मोड"); m.put(LOGIN,"लॉगिन मोड");
        m.put(DEV_NOTICE,"विकास सूचना दिखाएँ"); m.put(CLOSE,"बंद करें"); m.put(ADD_LIST,"सूची में जोड़ें");
        m.put(EMPTY_LIST,"सूची में कोई मंगा नहीं है।"); m.put(EMPTY_HISTORY,"पठन इतिहास खाली है।");
        m.put(RESUME,"जारी रखें"); m.put(REMOVE,"हटाएँ"); m.put(CHAPTER,"अध्याय"); m.put(PAGE,"पृष्ठ"); m.put(READ,"पढ़ा");
    }

    private static void spanish(Map<String, String> m) {
        m.put(HOME,"Inicio"); m.put(LIST,"Lista"); m.put(HISTORY,"Historial de lectura"); m.put(SETTINGS,"Ajustes");
        m.put(MODE,"Modo"); m.put(BYPASS,"Modo sin inicio de sesión"); m.put(LOGIN,"Modo con inicio de sesión");
        m.put(DEV_NOTICE,"Mostrar aviso de desarrollo"); m.put(CLOSE,"Cerrar"); m.put(ADD_LIST,"Añadir a la lista");
        m.put(EMPTY_LIST,"Aún no hay manga en la lista."); m.put(EMPTY_HISTORY,"El historial está vacío.");
        m.put(RESUME,"Continuar"); m.put(REMOVE,"Eliminar"); m.put(CHAPTER,"Capítulo"); m.put(PAGE,"Página"); m.put(READ,"leído");
    }

    private static void french(Map<String, String> m) {
        m.put(HOME,"Accueil"); m.put(LIST,"Liste"); m.put(HISTORY,"Historique de lecture"); m.put(SETTINGS,"Réglages");
        m.put(MODE,"Mode"); m.put(BYPASS,"Mode sans connexion"); m.put(LOGIN,"Mode connecté");
        m.put(DEV_NOTICE,"Afficher l’avis de développement"); m.put(CLOSE,"Fermer"); m.put(ADD_LIST,"Ajouter à la liste");
        m.put(EMPTY_LIST,"La liste ne contient aucun manga."); m.put(EMPTY_HISTORY,"L’historique est vide.");
        m.put(RESUME,"Reprendre"); m.put(REMOVE,"Supprimer"); m.put(CHAPTER,"Chapitre"); m.put(PAGE,"Page"); m.put(READ,"lu");
    }

    private static void arabic(Map<String, String> m) {
        m.put(HOME,"الرئيسية"); m.put(LIST,"القائمة"); m.put(HISTORY,"سجل القراءة"); m.put(SETTINGS,"الإعدادات");
        m.put(MODE,"الوضع"); m.put(BYPASS,"وضع بلا تسجيل دخول"); m.put(LOGIN,"وضع تسجيل الدخول");
        m.put(DEV_NOTICE,"إظهار إشعار التطوير"); m.put(CLOSE,"إغلاق"); m.put(ADD_LIST,"إضافة إلى القائمة");
        m.put(EMPTY_LIST,"لا توجد مانغا في القائمة."); m.put(EMPTY_HISTORY,"سجل القراءة فارغ.");
        m.put(RESUME,"متابعة"); m.put(REMOVE,"حذف"); m.put(CHAPTER,"الفصل"); m.put(PAGE,"الصفحة"); m.put(READ,"مقروء");
    }

    private static void portuguese(Map<String, String> m) {
        m.put(HOME,"Início"); m.put(LIST,"Lista"); m.put(HISTORY,"Histórico de leitura"); m.put(SETTINGS,"Configurações");
        m.put(MODE,"Modo"); m.put(BYPASS,"Modo sem login"); m.put(LOGIN,"Modo com login");
        m.put(DEV_NOTICE,"Mostrar aviso de desenvolvimento"); m.put(CLOSE,"Fechar"); m.put(ADD_LIST,"Adicionar à lista");
        m.put(EMPTY_LIST,"Ainda não há mangá na lista."); m.put(EMPTY_HISTORY,"O histórico está vazio.");
        m.put(RESUME,"Continuar"); m.put(REMOVE,"Remover"); m.put(CHAPTER,"Capítulo"); m.put(PAGE,"Página"); m.put(READ,"lido");
    }

    private static void bengali(Map<String, String> m) {
        m.put(HOME,"হোম"); m.put(LIST,"তালিকা"); m.put(HISTORY,"পড়ার ইতিহাস"); m.put(SETTINGS,"সেটিংস");
        m.put(MODE,"মোড"); m.put(BYPASS,"লগইন ছাড়া মোড"); m.put(LOGIN,"লগইন মোড");
        m.put(DEV_NOTICE,"উন্নয়ন বিজ্ঞপ্তি দেখান"); m.put(CLOSE,"বন্ধ"); m.put(ADD_LIST,"তালিকায় যোগ করুন");
        m.put(EMPTY_LIST,"তালিকায় কোনো মাঙ্গা নেই।"); m.put(EMPTY_HISTORY,"পড়ার ইতিহাস খালি।");
        m.put(RESUME,"চালিয়ে যান"); m.put(REMOVE,"মুছুন"); m.put(CHAPTER,"অধ্যায়"); m.put(PAGE,"পৃষ্ঠা"); m.put(READ,"পঠিত");
    }

    private static void russian(Map<String, String> m) {
        m.put(HOME,"Главная"); m.put(LIST,"Список"); m.put(HISTORY,"История чтения"); m.put(SETTINGS,"Настройки");
        m.put(MODE,"Режим"); m.put(BYPASS,"Режим без входа"); m.put(LOGIN,"Режим со входом");
        m.put(DEV_NOTICE,"Показывать уведомление о разработке"); m.put(CLOSE,"Закрыть"); m.put(ADD_LIST,"Добавить в список");
        m.put(EMPTY_LIST,"В списке пока нет манги."); m.put(EMPTY_HISTORY,"История чтения пуста.");
        m.put(RESUME,"Продолжить"); m.put(REMOVE,"Удалить"); m.put(CHAPTER,"Глава"); m.put(PAGE,"Страница"); m.put(READ,"прочитано");
    }

    private static void urdu(Map<String, String> m) {
        m.put(HOME,"ہوم"); m.put(LIST,"فہرست"); m.put(HISTORY,"پڑھنے کی تاریخ"); m.put(SETTINGS,"ترتیبات");
        m.put(MODE,"موڈ"); m.put(BYPASS,"لاگ ان کے بغیر موڈ"); m.put(LOGIN,"لاگ ان موڈ");
        m.put(DEV_NOTICE,"ترقیاتی اطلاع دکھائیں"); m.put(CLOSE,"بند کریں"); m.put(ADD_LIST,"فہرست میں شامل کریں");
        m.put(EMPTY_LIST,"فہرست میں کوئی مانگا نہیں۔"); m.put(EMPTY_HISTORY,"پڑھنے کی تاریخ خالی ہے۔");
        m.put(RESUME,"جاری رکھیں"); m.put(REMOVE,"حذف کریں"); m.put(CHAPTER,"باب"); m.put(PAGE,"صفحہ"); m.put(READ,"پڑھا گیا");
    }
}
