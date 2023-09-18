package no.unit.nva.search2.constant;

public class Patterns {

    public static final String PATTERN_IS_DATE = "(\\d){4}-(\\d){2}-(\\d){2}[T]*[(\\d){2}:(\\d){2}:(\\d){2,6}Z]*";
    public static final String PATTERN_IS_SHORT_DATE = "(\\d){4}/(\\d){2}/(\\d){2}";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";
    public static final String PATTERN_IS_DOI_URL = "10.\\d{4,9}/[-._;()/:a-z0-9A-Z]+";
}