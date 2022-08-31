package fr.sparkit.accounting.services.utils;

import java.util.Locale;
import java.util.ResourceBundle;

import fr.sparkit.accounting.util.LanguageContextHolderService;

public final class TraductionServiceUtil {
    public static final String EN_LANGUAGE = "en";
    public static final String FR_LANGUAGE = "fr";

    private TraductionServiceUtil() {
        super();
    }

    public static ResourceBundle getI18nResourceBundle() {
        Locale locale;
        if (EN_LANGUAGE.equals(LanguageContextHolderService.getLanguageContext())) {
            locale = Locale.forLanguageTag(EN_LANGUAGE);
        } else {
            locale = Locale.forLanguageTag(FR_LANGUAGE);
        }
        return ResourceBundle.getBundle("i18n/jasper_report", locale);
    }
}
