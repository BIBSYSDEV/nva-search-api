package no.unit.nva.search.common.enums;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Words.PIPE;
import static no.unit.nva.search.common.constant.Words.PREFIX;
import static no.unit.nva.search.common.constant.Words.SUFFIX;

public enum ParameterSubKind {
    INVALID(),
    MUST(""),
    MUST_NOT(".?NOT"),
    SHOULD(),
    BETWEEN(),
    BEFORE(),
    SINCE(),
    EXIST();

    private final String pattern;

    ParameterSubKind() {
        this.pattern =  PATTERN_IS_NONE_OR_ONE + name();
    }

    ParameterSubKind(String pattern) {
        this.pattern = pattern;
    }

    public String fieldPattern() {
        return pattern;
    }

    public static ParameterSubKind keyFromString(String postFix){
        var result = Arrays.stream(ParameterSubKind.values())
            .filter(equalTo(postFix))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }


    static Predicate<ParameterSubKind> equalTo(String name) {
        return key -> name.matches(PATTERN_IS_IGNORE_CASE + key.fieldPattern() + "$");
    }

    public static final String SUB_KIND_PATTERNS =
        Arrays.stream(ParameterSubKind.values())
            .map(ParameterSubKind::fieldPattern)
            .collect(Collectors.joining(PIPE, PREFIX, SUFFIX));

}
