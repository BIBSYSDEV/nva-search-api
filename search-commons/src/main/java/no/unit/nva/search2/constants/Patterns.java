package no.unit.nva.search2.constants;

public class Patterns {

    public static final String PATTERN_IS_DATE = "(\\d){4}-(\\d){2}-(\\d){2}[T]*[(\\d){2}:(\\d){2}:(\\d){2,6}Z]*";
    public static final String PATTERN_IS_BOOLEAN = "(?i)true|false";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";
    public static final String PATTERN_IS_RANGE = "\\d+\\-\\d+";
    public static final String PATTERN_IS_LANGUAGE = "(en|nb|nn|\\,)+";
    public static final String PATTERN_IS_TITLE = "^[æøåÆØÅ\\w-,\\.:; ]+$";
    public static final String PATTERN_IS_URL =
        "(http(s):\\/\\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";
}