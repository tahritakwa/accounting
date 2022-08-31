package fr.sparkit.accounting.util;

import javax.servlet.http.HttpServletRequest;

public final class CurrentRequestContextHolder {

    private static ThreadLocal<HttpServletRequest> threadLocal = new ThreadLocal<>();

    private CurrentRequestContextHolder() {
        super();
    }

    public static void setCurrentHttpServletRequest(HttpServletRequest httpServletRequest) {
        threadLocal.set(httpServletRequest);
    }

    public static HttpServletRequest getCurrentHttpServletRequest() {
        return threadLocal.get();
    }

}
