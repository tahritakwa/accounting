package fr.sparkit.accounting.util;

import java.util.ArrayList;
import java.util.List;

public final class CompanyContextHolder {

    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();
    @SuppressWarnings("squid:S2156")
    public static final List<String> supportedCompanies = new ArrayList<>();

    private CompanyContextHolder() {
        super();
    }

    public static void setCompanyContext(String companyCode) {
        threadLocal.set(companyCode);
    }

    public static String getCompanyContext() {
        return threadLocal.get();
    }

}
