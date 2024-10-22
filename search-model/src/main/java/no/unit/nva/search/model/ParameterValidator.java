package no.unit.nva.search.model;

import static no.unit.nva.search.model.constant.ErrorMessages.RELEVANCE_SEARCH_AFTER_ARE_MUTUAL_EXCLUSIVE;
import static no.unit.nva.search.model.constant.ErrorMessages.requiredMissingMessage;
import static no.unit.nva.search.model.constant.ErrorMessages.validQueryParameterNamesMessage;

import static java.util.Objects.isNull;

import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.search.model.constant.Functions;
import no.unit.nva.search.model.constant.Patterns;
import no.unit.nva.search.model.constant.Words;
import no.unit.nva.search.model.enums.ParameterKey;
import no.unit.nva.search.model.enums.ValueEncoding;

import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder for OpenSearchQuery.
 *
 * @param <K> Enum of ParameterKeys
 * @param <Q> Instance of OpenSearchQuery
 * @author Stig Norland
 */
public abstract class ParameterValidator<
        K extends Enum<K> & ParameterKey<K>, Q extends SearchQuery<K>> {

    protected static final Logger logger = LoggerFactory.getLogger(ParameterValidator.class);

    protected final transient Set<String> invalidKeys = new HashSet<>(0);
    protected final transient SearchQuery<K> searchQuery;
    protected transient boolean notValidated = true;

    /**
     * Constructor of QueryBuilder.
     *
     * <p>Usage: <samp>Query.builder()<br>
     * .fromRequestInfo(requestInfo)<br>
     * .withRequiredParameters(FROM, SIZE)<br>
     * .build() </samp>
     */
    public ParameterValidator(SearchQuery<K> searchQuery) {
        this.searchQuery = searchQuery;
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
        return (Q) searchQuery;
    }

    /**
     * Validator of QueryBuilder.
     *
     * @throws BadRequestException if parameters are invalid or missing
     */
    public ParameterValidator<K, Q> validate() throws BadRequestException {
        assignDefaultValues();
        for (var entry : searchQuery.parameters().getSearchEntries()) {
            validatesEntrySet(entry);
        }
        for (var entry : searchQuery.parameters().getPageEntries()) {
            validatesEntrySet(entry);
        }
        if (!requiredMissing().isEmpty()) {
            throw new BadRequestException(requiredMissingMessage(getMissingKeys()));
        }
        if (!invalidKeys.isEmpty()) {
            throw new BadRequestException(
                    validQueryParameterNamesMessage(invalidKeys, validKeys()));
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
            searchQuery.sort().asSplitStream(Words.COMMA).forEach(this::validateSortKeyName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    protected Set<String> getMissingKeys() {
        return requiredMissing().stream()
                .map(ParameterKey::asCamelCase)
                .collect(Collectors.toSet());
    }

    protected Set<K> requiredMissing() {
        return required().stream()
                .filter(key -> !searchQuery.parameters().isPresent(key))
                .collect(Collectors.toSet());
    }

    protected Set<K> required() {
        return searchQuery.parameters().otherRequired;
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
        return (qpKey.valueEncoding() == ValueEncoding.NONE ? value : Functions.decodeUTF(value))
                .replaceAll(Patterns.PATTERN_IS_NON_PRINTABLE_CHARACTERS, StringUtils.EMPTY_STRING);
    }

    /** Adds query and path parameters from requestInfo. */
    public ParameterValidator<K, Q> fromRequestInfo(RequestInfo requestInfo) {
        searchQuery.setMediaType(requestInfo.getHeaders().get(UriRetriever.ACCEPT));
        var uri = URI.create(Words.HTTPS + requestInfo.getDomainName() + requestInfo.getPath());
        searchQuery.setAccessRights(requestInfo.getAccessRights());
        searchQuery.setNvaSearchApiUri(uri);
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
     * <p>In order to improve ease of use, you can add a default value to each required parameter,
     * and it will be used, if it is not proved by the requester. Implement default values in {@link
     * #assignDefaultValues()}
     *
     * @param requiredParameters comma seperated QueryParameterKeys
     */
    @SafeVarargs
    public final ParameterValidator<K, Q> withRequiredParameters(K... requiredParameters) {
        var tmpSet = Set.of(requiredParameters);
        searchQuery.parameters().otherRequired.addAll(tmpSet);
        return this;
    }

    public final ParameterValidator<K, Q> withMediaType(String mediaType) {
        searchQuery.setMediaType(mediaType);
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
        searchQuery.parameters().set(key, value);
        return this;
    }

    /**
     * When running docker tests, the current host needs to be specified.
     *
     * @param uri URI to local docker test instance
     * @apiNote This is intended to be used when setting up tests.
     */
    public final ParameterValidator<K, Q> withDockerHostUri(URI uri) {
        searchQuery.setOpenSearchUri(uri);
        return this;
    }

    public final ParameterValidator<K, Q> withAlwaysExcludedFields(List<String> excludedFields) {
        searchQuery.setAlwaysExcludedFields(excludedFields);
        return this;
    }

    public final ParameterValidator<K, Q> withAlwaysExcludedFields(String... excludedFields) {
        searchQuery.setAlwaysExcludedFields(List.of(excludedFields));
        return this;
    }

    protected void mergeToKey(K key, String value) {
        searchQuery
                .parameters()
                .set(
                        key,
                        Functions.mergeWithColonOrComma(
                                searchQuery.parameters().get(key).as(), value));
    }

    protected String ignoreInvalidFields(String value) {
        return Words.ALL.equalsIgnoreCase(value) || isNull(value)
                ? Words.ALL
                : Arrays.stream(value.split(Words.COMMA))
                        .filter(this::isKeyValid) // ignoring invalid keys
                        .collect(Collectors.joining(Words.COMMA));
    }

    protected boolean invalidQueryParameter(K key, String value) {
        return isNull(value)
                || Arrays.stream(value.split(Words.COMMA))
                        .noneMatch(singleValue -> singleValue.matches(key.valuePattern()));
    }

    private boolean hasSearchAfterAndSortByRelevance() {
        return searchQuery.parameters().isPresent(searchQuery.keySearchAfter())
                && searchQuery.sort().toString().contains(Words.RELEVANCE_KEY_NAME);
    }
}
