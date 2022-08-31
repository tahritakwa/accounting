package fr.sparkit.accounting.util;

public final class LanguageContextHolderService {

    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private LanguageContextHolderService() {
        super();
    }

    public static void setLanguageContext(String language) {
        threadLocal.set(language);
    }

    public static String getLanguageContext() {
        return threadLocal.get();
    }

}
