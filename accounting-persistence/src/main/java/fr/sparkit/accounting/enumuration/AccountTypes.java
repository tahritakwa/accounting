package fr.sparkit.accounting.enumuration;

public enum AccountTypes {

    ALL, IS_LITERABLE, IS_NOT_LITERABLE;

    public static Boolean geStateOfAccountByType(String type) {
        switch (AccountTypes.valueOf(type)) {
        case ALL:
            return null;
        case IS_LITERABLE:
            return Boolean.TRUE;
        case IS_NOT_LITERABLE:
            return Boolean.FALSE;
        default:
            return null;
        }
    }

}
