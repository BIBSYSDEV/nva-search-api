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
    BEFORE(".?BEFORE|.?LESS.?THAN"),
    SINCE(".?SINCE|.?LARGER.?THAN"),
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

    public static ParameterSubKind keyFromString(String parameterName) {
        var result = Arrays.stream(ParameterSubKind.values())
            .skip(2)       //Don't include INVALID
            .filter(equalTo(parameterName))
            .collect(Collectors.toSet());
        return result.isEmpty()
            ? MUST
            : result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }


    static Predicate<ParameterSubKind> equalTo(String name) {
        return key -> name.matches(PATTERN_IS_IGNORE_CASE + "\\w*(" + key.fieldPattern() + ")$");
    }

    public static final String SUB_KIND_PATTERNS =
        Arrays.stream(ParameterSubKind.values())
            .skip(1)        //Don't include INVALID
            .map(ParameterSubKind::fieldPattern)
            .collect(Collectors.joining(PIPE, PREFIX, SUFFIX));

}
