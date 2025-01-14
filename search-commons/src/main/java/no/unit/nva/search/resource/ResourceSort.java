package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.CHAR_UNDERSCORE;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.constants.Words.KEYWORD;
import static no.unit.nva.constants.Words.YEAR;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search.resource.Constants.IDENTIFIER_KEYWORD;
import static nva.commons.core.StringUtils.EMPTY_STRING;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.enums.SortKey;
import org.apache.commons.text.CaseUtils;

/**
 * Enum for sorting resources.
 *
 * @author Stig Norland
 */
public enum ResourceSort implements SortKey {
  INVALID(EMPTY_STRING),
  IDENTIFIER(IDENTIFIER_KEYWORD),
  RELEVANCE(Words.SCORE),
  CATEGORY(Constants.PUBLICATION_INSTANCE_TYPE),
  INSTANCE_TYPE(Constants.PUBLICATION_INSTANCE_TYPE),
  CREATED_DATE(Words.CREATED_DATE),
  MODIFIED_DATE(Words.MODIFIED_DATE),
  PUBLISHED_DATE(Words.PUBLISHED_DATE),
  PUBLICATION_DATE(ENTITY_DESCRIPTION + DOT + Words.PUBLICATION_DATE + DOT + YEAR + DOT + KEYWORD),
  TITLE(Constants.ENTITY_DESCRIPTION_MAIN_TITLE_KEYWORD),
  UNIT_ID(Constants.CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
  USER("(?i)(user)|(owner)", Constants.RESOURCE_OWNER_OWNER_KEYWORD);

  private final String keyValidationRegEx;
  private final String path;

  ResourceSort(String pattern, String jsonPath) {
    this.keyValidationRegEx = pattern;
    this.path = jsonPath;
  }

  ResourceSort(String jsonPath) {
    this.keyValidationRegEx = SortKey.getIgnoreCaseAndUnderscoreKeyExpression(this.name());
    this.path = jsonPath;
  }

  public static ResourceSort fromSortKey(String keyName) {
    var result =
        Arrays.stream(values()).filter(SortKey.equalTo(keyName)).collect(Collectors.toSet());
    return result.size() == 1 ? result.stream().findFirst().get() : INVALID;
  }

  public static Collection<String> validSortKeys() {
    return Arrays.stream(values())
        .sorted(SortKey::compareAscending)
        .skip(1)
        .map(SortKey::asLowerCase)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public String asCamelCase() {
    return CaseUtils.toCamelCase(this.name(), false, CHAR_UNDERSCORE);
  }

  @Override
  public String asLowerCase() {
    return this.name().toLowerCase(Locale.getDefault());
  }

  @Override
  public String keyPattern() {
    return keyValidationRegEx;
  }

  @Override
  public Stream<String> jsonPaths() {
    return Arrays.stream(path.split(PATTERN_IS_PIPE));
  }
}
