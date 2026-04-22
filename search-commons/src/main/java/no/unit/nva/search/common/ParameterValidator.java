package no.unit.nva.search.common;

import static java.util.Objects.isNull;
import static no.unit.nva.constants.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.constants.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.constants.ErrorMessages.PAGINATION_PARAMETERS_ARE_MUTUAL_EXCLUSIVE;
import static no.unit.nva.constants.ErrorMessages.RELEVANCE_SEARCH_AFTER_ARE_MUTUAL_EXCLUSIVE;
import static no.unit.nva.constants.ErrorMessages.RESULT_WINDOW_TOO_LARGE;
import static no.unit.nva.constants.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.constants.ErrorMessages.requiredMissingMessage;
import static no.unit.nva.constants.ErrorMessages.validQueryParameterNamesMessage;
import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.HTTPS;
import static no.unit.nva.constants.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.constants.Words.NONE;
import static no.unit.nva.constants.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search.common.ContentTypeUtils.extractContentTypeFromRequestInfo;
import static no.unit.nva.search.common.constant.Functions.decodeUTF;
import static no.unit.nva.search.common.constant.Functions.mergeWithColonOrComma;
import static no.unit.nva.search.common.constant.Functions.trimSpace;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static nva.commons.core.StringUtils.EMPTY_STRING;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.search.common.constant.Patterns;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.ValueEncoding;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for OpenSearchQuery.
 *
 * @param <K> Enum of ParameterKeys
 * @param <Q> Instance of OpenSearchQuery
 * @author Stig Norland
 */
@SuppressWarnings("PMD.GodClass")
public abstract class ParameterValidator<
    K extends Enum<K> & ParameterKey<K>, Q extends SearchQuery<K>> {

  protected static final Logger logger = LoggerFactory.getLogger(ParameterValidator.class);

  private static final int MAX_RESULT_WINDOW_SIZE = 10_000;
  private static final String FROM_KEY_NAME = "FROM";
  private static final String SIZE_KEY_NAME = "SIZE";
  private static final String PAGE_KEY_NAME = "PAGE";
  private static final String SORT_KEY_NAME = "SORT";
  private static final String SORT_ORDER_KEY_NAME = "SORT_ORDER";
  private static final String AGGREGATION_KEY_NAME = "AGGREGATION";
  private static final String SEARCH_AFTER_KEY_NAME = "SEARCH_AFTER";
  private static final String NODES_SEARCHED_KEY_NAME = "NODES_SEARCHED";
  protected final transient Set<String> invalidKeys = new HashSet<>(0);
  protected final transient SearchQuery<K> query;
  protected transient boolean notValidated = true;

  /**
   * Constructor of QueryBuilder.
   *
   * <p>Usage: <samp>Query.builder()<br>
   * .fromRequestInfo(requestInfo)<br>
   * .withRequiredParameters(FROM, SIZE)<br>
   * .build() </samp>
   */
  public ParameterValidator(SearchQuery<K> query) {
    this.query = query;
  }

  /**
   * Builder of Query.
   *
   * @throws BadRequestException if parameters are invalid or missing
   */
  @SuppressWarnings("unchecked")
  public Q build() throws BadRequestException {
    if (notValidated) {
      validate();
    }
    return (Q) query;
  }

  /**
   * Validator of QueryBuilder.
   *
   * @throws BadRequestException if parameters are invalid or missing
   */
  public ParameterValidator<K, Q> validate() throws BadRequestException {
    validatePaginationParameters();
    assignDefaultValues();
    for (var entry : query.parameters().getSearchEntries()) {
      validatesEntrySet(entry);
    }
    for (var entry : query.parameters().getPageEntries()) {
      validatesEntrySet(entry);
    }
    if (!requiredMissing().isEmpty()) {
      throw new BadRequestException(requiredMissingMessage(getMissingKeys()));
    }
    if (!invalidKeys.isEmpty()) {
      throw new BadRequestException(validQueryParameterNamesMessage(invalidKeys, validKeys()));
    }
    validatedSort();

    applyRulesAfterValidation();
    validatePostProcessedPaginationParameters();
    notValidated = false;
    return this;
  }

  /**
   * Assigns defaults for required-but-missing pagination, sort and aggregation keys. Subclasses
   * override {@link #getDefaultSort()} to change the default sort; other defaults are fixed.
   */
  protected void assignDefaultValues() {
    requiredMissing()
        .forEach(
            key -> {
              switch (key.name()) {
                case FROM_KEY_NAME -> setValue(key.name(), DEFAULT_OFFSET);
                case SIZE_KEY_NAME -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                case SORT_KEY_NAME -> setValue(key.name(), getDefaultSort());
                case AGGREGATION_KEY_NAME -> setValue(key.name(), NONE);
                default -> {}
              }
            });
  }

  /** Default sort applied when SORT is required but missing. Override to customize. */
  protected String getDefaultSort() {
    return RELEVANCE_KEY_NAME;
  }

  /**
   * Runs the shared page-to-offset conversion, then delegates to {@link
   * #applyAdditionalRulesAfterValidation()} for type-specific rules.
   */
  protected void applyRulesAfterValidation() {
    convertPageToOffsetIfNeeded();
    applyAdditionalRulesAfterValidation();
  }

  /** Hook for type-specific post-validation rules (e.g. business-rule parameter coupling). */
  protected abstract void applyAdditionalRulesAfterValidation();

  private void convertPageToOffsetIfNeeded() {
    var pageKey = query.toKey(PAGE_KEY_NAME);
    if (query.parameters().isPresent(pageKey)) {
      var fromKey = query.toKey(FROM_KEY_NAME);
      if (query.parameters().isPresent(fromKey)) {
        var page = query.parameters().get(pageKey).<Number>as().longValue();
        var perPage = query.parameters().get(query.toKey(SIZE_KEY_NAME)).<Number>as().longValue();
        query.parameters().set(fromKey, String.valueOf(page * perPage));
      }
      query.parameters().remove(pageKey);
    }
  }

  protected abstract Collection<String> validKeys();

  protected abstract Collection<String> validSortKeys();

  protected void validateSortKeyName(String name) {
    var nameSort = name.split(COLON_OR_SPACE);
    if (nameSort.length == NAME_AND_SORT_LENGTH) {
      SortOrder.fromString(nameSort[1]);
    } else if (nameSort.length > NAME_AND_SORT_LENGTH) {
      throw new IllegalArgumentException(TOO_MANY_ARGUMENTS + name);
    }
    if (query.toSortKey(nameSort[0]).isInvalid()) {
      throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(name, validSortKeys()));
    }
  }

  /**
   * Decodes and stores a parameter value. Unrecognized keys are collected in {@link #invalidKeys}
   * (reported later as a batch). Pagination, sort, sort-order and field-filter keys have shared
   * handling here; anything else is delegated to {@link #setValueSpecific(Enum, String)}.
   */
  protected void setValue(String key, String value) {
    var qpKey = query.toKey(key);
    if (qpKey.isInvalid()) {
      invalidKeys.add(key);
    } else {
      var decodedValue = getDecodedValue(qpKey, value);
      switch (qpKey.name()) {
        case SEARCH_AFTER_KEY_NAME,
            FROM_KEY_NAME,
            SIZE_KEY_NAME,
            PAGE_KEY_NAME,
            AGGREGATION_KEY_NAME ->
            query.parameters().set(qpKey, decodedValue);
        case NODES_SEARCHED_KEY_NAME ->
            query.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
        case SORT_KEY_NAME -> mergeToKey(qpKey, trimSpace(decodedValue));
        case SORT_ORDER_KEY_NAME -> mergeToKey(query.toKey(SORT_KEY_NAME), decodedValue);
        default -> setValueSpecific(qpKey, decodedValue);
      }
    }
  }

  /**
   * Hook for type-specific parameter handling (e.g. URI transformation, enum conversion). Override
   * to transform the value before storing it under its key.
   */
  protected void setValueSpecific(K qpKey, String decodedValue) {
    mergeToKey(qpKey, decodedValue);
  }

  /**
   * Validate sort keys.
   *
   * @throws BadRequestException if sort key is invalid
   */
  protected void validatedSort() throws BadRequestException {
    try {
      query.sort().asSplitStream(COMMA).forEach(this::validateSortKeyName);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  protected Set<String> getMissingKeys() {
    return requiredMissing().stream().map(ParameterKey::asCamelCase).collect(Collectors.toSet());
  }

  protected Set<K> requiredMissing() {
    return required().stream()
        .filter(key -> !query.parameters().isPresent(key))
        .collect(Collectors.toSet());
  }

  protected Set<K> required() {
    return query.parameters().otherRequired;
  }

  protected void validatesEntrySet(Map.Entry<K, String> entry) throws BadRequestException {
    final var key = entry.getKey();
    final var value = entry.getValue();
    if (invalidQueryParameter(key, value)) {
      final var keyName = key.asCamelCase();
      final var errorMessage = key.errorMessage().formatted(keyName, value);
      throw new BadRequestException(errorMessage);
    }
  }

  protected String getDecodedValue(ParameterKey<K> qpKey, String value) {
    return (qpKey.valueEncoding() == ValueEncoding.NONE ? value : decodeUTF(value))
        .replaceAll(Patterns.PATTERN_IS_NON_PRINTABLE_CHARACTERS, EMPTY_STRING);
  }

  /** Adds query and path parameters from requestInfo. */
  public ParameterValidator<K, Q> fromRequestInfo(RequestInfo requestInfo) {
    var contentType = extractContentTypeFromRequestInfo(requestInfo);
    query.setMediaType(isNull(contentType) ? null : contentType.getMimeType());
    var uri = URI.create(HTTPS + requestInfo.getDomainName() + requestInfo.getPath());
    if (requestInfo.getHeaders().containsKey("Authorization")) {
      query.setAccessRights(requestInfo.getAccessRights());
    }
    query.setNvaSearchApiUri(uri);
    return fromMultiValueParameters(requestInfo.getMultiValueQueryStringParameters());
  }

  /**
   * Adds testParameters from query.
   *
   * @apiNote This is intended to be used in runtime
   */
  public ParameterValidator<K, Q> fromMultiValueParameters(Map<String, List<String>> parameters) {
    parameters.forEach((k, v) -> v.forEach(value -> setValue(k, value)));
    return this;
  }

  /**
   * Adds testParameters from query.
   *
   * @apiNote This is intended to be used when setting up tests, or from {@link #fromRequestInfo}
   */
  public ParameterValidator<K, Q> fromTestParameterMap(Map<String, String> parameters) {
    parameters.forEach(this::setValue);
    return this;
  }

  /**
   * Adds testParameters from query.
   *
   * @apiNote This is intended to be used when setting up tests.
   */
  public ParameterValidator<K, Q> fromTestQueryParameters(
      Collection<Map.Entry<String, String>> testParameters) {
    testParameters.forEach(this::setEntryValue);
    return this;
  }

  private void setEntryValue(Map.Entry<String, String> entry) {
    setValue(entry.getKey(), entry.getValue());
  }

  /**
   * Defines which parameters are required.
   *
   * <p>In order to improve ease of use, you can add a default value to each required parameter, and
   * it will be used, if it is not proved by the requester. Implement default values in {@link
   * #assignDefaultValues()}
   *
   * @param requiredParameters comma seperated QueryParameterKeys
   */
  @SafeVarargs
  public final ParameterValidator<K, Q> withRequiredParameters(K... requiredParameters) {
    var tmpSet = Set.of(requiredParameters);
    query.parameters().otherRequired.addAll(tmpSet);
    return this;
  }

  public final ParameterValidator<K, Q> withMediaType(String mediaType) {
    query.setMediaType(mediaType);
    return this;
  }

  /**
   * Overrides/set a key value pair
   *
   * @param key to assign value to
   * @param value assigned
   * @return builder
   */
  public ParameterValidator<K, Q> withParameter(K key, String value) {
    query.parameters().set(key, value);
    return this;
  }

  /**
   * When running docker tests, the current host needs to be specified.
   *
   * @param uri URI to local docker test instance
   * @apiNote This is intended to be used when setting up tests.
   */
  public final ParameterValidator<K, Q> withDockerHostUri(URI uri) {
    query.setOpenSearchUri(uri);
    return this;
  }

  public final ParameterValidator<K, Q> withAlwaysIncludedFields(List<String> includedFields) {
    query.setAlwaysIncludedFields(includedFields);
    return this;
  }

  public final ParameterValidator<K, Q> withAlwaysExcludedFields(List<String> excludedFields) {
    query.setAlwaysExcludedFields(excludedFields);
    return this;
  }

  public final ParameterValidator<K, Q> withAlwaysExcludedFields(String... excludedFields) {
    query.setAlwaysExcludedFields(List.of(excludedFields));
    return this;
  }

  protected void mergeToKey(K key, String value) {
    query.parameters().set(key, mergeWithColonOrComma(query.parameters().get(key).as(), value));
  }

  protected String ignoreInvalidFields(String value) {
    return ALL.equalsIgnoreCase(value) || isNull(value)
        ? ALL
        : Arrays.stream(value.split(COMMA))
            .filter(name -> query.toKey(name).isValid())
            .collect(Collectors.joining(COMMA));
  }

  protected boolean invalidQueryParameter(K key, String value) {
    return isNull(value)
        || Arrays.stream(value.split(COMMA))
            .noneMatch(singleValue -> singleValue.matches(key.valuePattern()));
  }

  private void validatePaginationParameters() throws BadRequestException {
    var hasPage = query.page().isPresent();
    var hasFrom = query.from().isPresent();
    var hasSearchAfter = query.parameters().isPresent(query.keySearchAfter());
    if (hasSearchAfter && (hasPage || hasFrom)) {
      throw new BadRequestException(PAGINATION_PARAMETERS_ARE_MUTUAL_EXCLUSIVE);
    }
    if (hasPage && hasFrom) {
      throw new BadRequestException(PAGINATION_PARAMETERS_ARE_MUTUAL_EXCLUSIVE);
    }
  }

  private void validatePostProcessedPaginationParameters() throws BadRequestException {
    validateSearchAfterNotCombinedWithRelevanceSort();
    validateResultWindowSize();
  }

  private void validateSearchAfterNotCombinedWithRelevanceSort() throws BadRequestException {
    var hasSearchAfter = query.parameters().isPresent(query.keySearchAfter());
    var hasSortByRelevance = query.sort().toString().contains(RELEVANCE_KEY_NAME);
    if (hasSearchAfter && hasSortByRelevance) {
      throw new BadRequestException(RELEVANCE_SEARCH_AFTER_ARE_MUTUAL_EXCLUSIVE);
    }
  }

  private void validateResultWindowSize() throws BadRequestException {
    var from = query.from().isEmpty() ? 0 : query.from().asNumber().intValue();
    var size = query.size().isEmpty() ? 0 : query.size().asNumber().intValue();
    var resultWindow = from + size;
    if (resultWindow > MAX_RESULT_WINDOW_SIZE) {
      throw new BadRequestException(
          RESULT_WINDOW_TOO_LARGE.formatted(MAX_RESULT_WINDOW_SIZE, resultWindow));
    }
  }
}
