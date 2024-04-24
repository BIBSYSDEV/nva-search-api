package no.unit.nva.search2.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search2.common.constant.ErrorMessages.requiredMissingMessage;
import static no.unit.nva.search2.common.constant.ErrorMessages.validQueryParameterNamesMessage;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_ASC_OR_DESC_GROUP;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SELECTED_GROUP;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.JANUARY_FIRST;
import static no.unit.nva.search2.common.constant.Words.PIPE;
import static nva.commons.core.StringUtils.SPACE;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.enums.ParameterKey;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for OpenSearchQuery.
 * @param <K> Enum of ParameterKeys
 * @param <Q> Instance of OpenSearchQuery
 */
public abstract class ParameterValidator<K extends Enum<K> & ParameterKey, Q extends Query<K>> {

    protected static final Logger logger = LoggerFactory.getLogger(ParameterValidator.class);

    protected final transient Set<String> invalidKeys = new HashSet<>(0);
    protected final transient Query<K> query;
    protected transient boolean notValidated = true;

    /**
     * Constructor of QueryBuilder.
     * <p>Usage:</p>
     * <samp>Query.builder()<br>
     * .fromRequestInfo(requestInfo)<br>
     * .withRequiredParameters(FROM, SIZE)<br>
     * .build()
     * </samp>
     */
    public ParameterValidator(Query<K> query) {
        this.query = query;
    }

    /**
     * Builder of Query.
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
     * Validator of CristinQuery.Builder.
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
            throw new BadRequestException(validQueryParameterNamesMessage(invalidKeys,validKeys()));
        }
        validatedSort();
        applyRulesAfterValidation();
        notValidated = false;
        return this;
    }

    /**
     * DefaultValues are only assigned if they are set as required, otherwise ignored.
     * <p>Usage:</p>
     * <samp>requiredMissing().forEach(key -> { <br>
     *     switch (key) {<br>
     *         case LANGUAGE -> query.setValue(key, DEFAULT_LANGUAGE_CODE);<br>
     *         default -> { // do nothing
     *             }<br>
     *     }});<br>
     * </samp>
     */
    protected abstract void assignDefaultValues();

    protected abstract void applyRulesAfterValidation();

    protected abstract Collection<String> validKeys();

    protected abstract boolean isKeyValid(String keyName);

    protected abstract void validateSortKeyName(String name);

    /**
     * Sample code for setValue.
     * <p>Usage:</p>
     * <samp>var qpKey = keyFromString(key,value);<br>
     * if(qpKey.equals(INVALID)) {<br> invalidKeys.add(key);<br> } else {<br> query.setValue(qpKey, value);<br> }<br>
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
            query.getSort().asSplitStream(COMMA)
                .forEach(this::validateSortKeyName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    protected Set<String> getMissingKeys() {
        return
            requiredMissing()
                .stream()
                .map(ParameterKey::asCamelCase)
                .collect(Collectors.toSet());
    }

    protected Set<K> requiredMissing() {
        return
            required().stream()
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

    /**
     * Adds query and path parameters from requestInfo.
     */
    public ParameterValidator<K, Q> fromRequestInfo(RequestInfo requestInfo) {
        query.setMediaType(requestInfo.getHeaders().get(ACCEPT));
        query.setNvaSearchApiUri(requestInfo.getRequestUri());
        return fromQueryParameters(requestInfo.getQueryParameters());
    }

    /**
     * Adds testParameters from query.
     *
     * @apiNote This is intended to be used when setting up tests.
     */
    public ParameterValidator<K, Q> fromQueryParameters(Map<String, String> testParameters) {
        testParameters.forEach(this::setValue);
        return this;
    }

    /**
     * Adds testParameters from query.
     *
     * @apiNote This is intended to be used when setting up tests.
     */
    public ParameterValidator<K, Q> fromQueryParameters(Collection<Map.Entry<String, String>> testParameters) {
        testParameters.forEach(this::setEntryValue);
        return this;
    }

    private void setEntryValue(Map.Entry<String, String> entry) {
        setValue(entry.getKey(), entry.getValue());
    }

    /**
     * Defines which parameters are required.
     * <p>In order to improve ease of use, you can add a default value to
     * each required parameter, and it will be used, if it is not proved by the requester. Implement default values in
     * {@link #assignDefaultValues()}</p>
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
     * When running docker tests, the current host needs to be specified.
     *
     * @param uri URI to local docker test instance
     * @apiNote This is intended to be used when setting up tests.
     */
    public final ParameterValidator<K, Q> withDockerHostUri(URI uri) {
        query.setOpenSearchUri(uri);
        return this;
    }

    protected void mergeToKey(K key, String value) {
        query.parameters().set(key, mergeWithColonOrComma(query.parameters().get(key).as(), value));
    }

    private String mergeWithColonOrComma(String oldValue, String newValue) {
        if (nonNull(oldValue)) {
            var delimiter = newValue.matches(PATTERN_IS_ASC_DESC_VALUE) ? COLON : COMMA;
            return String.join(delimiter, oldValue, newValue);
        } else {
            return newValue;
        }
    }

    protected String trimSpace(String value) {
        return value.replaceAll(PATTERN_IS_ASC_OR_DESC_GROUP, PATTERN_IS_SELECTED_GROUP);
    }

    protected String expandYearToDate(String value) {
        return value.length() == 4 ? value + JANUARY_FIRST : value;
    }

    protected String toEnumStrings(Function<String, Enum<?>> fromString, String decodedValue) {
        return
            Arrays.stream(decodedValue.split(COMMA + PIPE + SPACE))
                .map(fromString)
                .map(Enum::toString)
                .collect(Collectors.joining(COMMA));
    }


    @JacocoGenerated
    protected String ignoreInvalidFields(String value) {
        return ALL.equalsIgnoreCase(value) || isNull(value)
            ? ALL
            : Arrays.stream(value.split(COMMA))
            .filter(this::isKeyValid)           // ignoring invalid keys
            .collect(Collectors.joining(COMMA));
    }

    @JacocoGenerated
    protected boolean invalidQueryParameter(K key, String value) {
        return isNull(value) || Arrays.stream(value.split(COMMA))
            .noneMatch(singleValue -> singleValue.matches(key.valuePattern()));
    }

}