package fr.sparkit.accounting.util;

//TODO: in the new version that uses AuthV2 we'll get the user from the token (TokenContextHolder)
public final class UserContextHolder {

    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private UserContextHolder() {
        super();
    }

    public static void setUserContext(String userEmail) {
        threadLocal.set(userEmail);
    }

    public static String getUserContext() {
        return threadLocal.get();
    }

}
