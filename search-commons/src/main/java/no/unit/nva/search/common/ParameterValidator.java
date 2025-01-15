package no.unit.nva.search.common;

import static java.util.Objects.isNull;
import static no.unit.nva.constants.ErrorMessages.RELEVANCE_SEARCH_AFTER_ARE_MUTUAL_EXCLUSIVE;
import static no.unit.nva.constants.ErrorMessages.requiredMissingMessage;
import static no.unit.nva.constants.ErrorMessages.validQueryParameterNamesMessage;
import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.HTTPS;
import static no.unit.nva.constants.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search.common.ContentTypeUtils.extractContentTypeFromRequestInfo;
import static no.unit.nva.search.common.constant.Functions.decodeUTF;
import static no.unit.nva.search.common.constant.Functions.mergeWithColonOrComma;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;

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

    if (hasSearchAfterAndSortByRelevance()) {
      throw new BadRequestException(RELEVANCE_SEARCH_AFTER_ARE_MUTUAL_EXCLUSIVE);
    }
    applyRulesAfterValidation();
    notValidated = false;
    return this;
  }

  /**
   * DefaultValues are only assigned if they are set as required, otherwise ignored.
   *
   * <p>Usage: <samp>requiredMissing().forEach(key -> { <br>
   * switch (key) {<br>
   * case LANGUAGE -> query.setValue(key, DEFAULT_LANGUAGE_CODE);<br>
   * default -> { // do nothing }<br>
   * }});<br>
   * </samp>
   */
  protected abstract void assignDefaultValues();

  protected abstract void applyRulesAfterValidation();

  protected abstract Collection<String> validKeys();

  protected abstract boolean isKeyValid(String keyName);

  protected abstract void validateSortKeyName(String name);

  /**
   * Sample code for setValue.
   *
   * <p>Usage: <samp>var qpKey = keyFromString(key,value);<br>
   * if(qpKey.equals(INVALID)) {<br>
   * invalidKeys.add(key);<br>
   * } else {<br>
   * query.setValue(qpKey, value);<br>
   * }<br>
   * </samp>
   */
  protected abstract void setValue(String key, String value);

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
    if (attempt(requestInfo::getAuthHeader).isSuccess()) {
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
            .filter(this::isKeyValid) // ignoring invalid keys
            .collect(Collectors.joining(COMMA));
  }

  protected boolean invalidQueryParameter(K key, String value) {
    return isNull(value)
        || Arrays.stream(value.split(COMMA))
            .noneMatch(singleValue -> singleValue.matches(key.valuePattern()));
  }

  private boolean hasSearchAfterAndSortByRelevance() {
    return query.parameters().isPresent(query.keySearchAfter())
        && query.sort().toString().contains(RELEVANCE_KEY_NAME);
  }
}
