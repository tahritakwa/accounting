package fr.sparkit.accounting.constants;

public final class FilterConstants {

    public static final String EQUAL_FILTER_OPERATOR = "eq";
    public static final String NOT_EQUAL_FILTER_OPERATOR = "neq";
    public static final String CONTAINS_FILTER_OPERATOR = "contains";
    public static final String DOES_NOT_CONTAIN_FILTER_OPERATOR = "doesnotcontain";
    public static final String STARTS_WITH_FILTER_OPERATOR = "startswith";
    public static final String END_WITH_FILTER_OPERATOR = "endswith";
    public static final String IS_NULL_FILTER_OPERATOR = "isnull";
    public static final String IS_NOT_NULL_FILTER_OPERATOR = "isnotnull";
    public static final String IS_EMPTY_FILTER_OPERATOR = "isempty";
    public static final String GREATER_THAN_OR_EQUAL_OPERATOR = "gte";
    public static final String LESS_THAN_OR_EQUAL_OPERATOR = "lte";
    public static final String GREATER_THAN_OPERATOR = "gt";
    public static final String LESS_THAN_OPERATOR = "lt";
    public static final String IS_NOT_EMPTY_FILTER_OPERATOR = "isnotempty";

    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String DATE = "date";
    public static final String DROP_DOWN_LIST = "dropdownlist";

    private static final String PREDICATE_LIKE_SYMBOL = "%%";
    public static final String PREDICATE_FILTER_ENDS_WITH = PREDICATE_LIKE_SYMBOL + "%s";
    public static final String PREDICATE_FILTER_STARTS_WITH = "%s" + PREDICATE_LIKE_SYMBOL;
    public static final String PREDICATE_FILTER_LIKE = PREDICATE_LIKE_SYMBOL + "%s" + PREDICATE_LIKE_SYMBOL;

    private FilterConstants() {

    }
}
