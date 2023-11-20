package no.unit.nva.search2.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ErrorMessages.invalidQueryParametersMessage;
import static no.unit.nva.search2.constant.ErrorMessages.requiredMissingMessage;
import static no.unit.nva.search2.constant.ErrorMessages.validQueryParameterNamesMessage;
import static no.unit.nva.search2.model.ParameterKeyResources.VALID_SEARCH_PARAMETER_KEYS;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for OpenSearchQuery.
 * @param <K> Enum of QueryParameterKeys
 * @param <Q> Instance of OpenSearchQuery
 */
public abstract class OpenSearchQueryBuilder<K extends Enum<K> & ParameterKey, Q extends OpenSearchQuery<K>> {

    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQueryBuilder.class);

    protected final transient Set<String> invalidKeys = new HashSet<>(0);
    protected final transient OpenSearchQuery<K> query;
    protected transient boolean notValidated = true;

    /**
     * Constructor of CristinQuery.Builder.
     * <p>Usage:</p>
     * <samp>new CristinQuery.Builder()<br>
     * .fromRequestInfo(requestInfo)<br>
     * .withRequiredParameters(IDENTITY,PAGE_CURRENT,PAGE_ITEMS_PER_PAGE)<br>
     * .build()
     * </samp>
     */
    public OpenSearchQueryBuilder(OpenSearchQuery<K> query) {
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
    public OpenSearchQueryBuilder<K, Q> validate() throws BadRequestException {
        assignDefaultValues();
        for (var entry : query.pageParameters.entrySet()) {
            validatesEntrySet(entry);
        }
        for (var entry : query.searchParameters.entrySet()) {
            validatesEntrySet(entry);
        }
        if (!requiredMissing().isEmpty()) {
            throw new BadRequestException(requiredMissingMessage(getMissingKeys()));
        }
        if (!invalidKeys.isEmpty()) {
            throw new BadRequestException(validQueryParameterNamesMessage(invalidKeys,validKeys()));
        }
        validateSort();
        applyRulesAfterValidation();
        notValidated = false;
        return this;
    }

    /**
     * Adds query and path parameters from requestInfo.
     */
    public final OpenSearchQueryBuilder<K, Q> fromRequestInfo(RequestInfo requestInfo) {
        query.setMediaType(requestInfo.getHeaders().get("Accept"));
        query.setNvaSearchApiUri(requestInfo.getRequestUri());
        return fromQueryParameters(requestInfo.getQueryParameters());
    }


    /**
     * Adds parameters from query.
     */
    public OpenSearchQueryBuilder<K, Q> fromQueryParameters(Collection<Map.Entry<String, String>> parameters) {
        parameters.forEach(this::setEntryValue);
        return this;
    }

    /**
     * Adds parameters from query.
     */
    public OpenSearchQueryBuilder<K, Q> fromQueryParameters(Map<String, String> parameters) {
        parameters.forEach(this::setValue);
        return this;
    }

    /**
     * Defines which parameters are required.
     * @param requiredParameters comma seperated QueryParameterKeys
     */
    @SafeVarargs
    public final OpenSearchQueryBuilder<K, Q> withRequiredParameters(K... requiredParameters) {
        var tmpSet = Set.of(requiredParameters);
        query.otherRequiredKeys.addAll(tmpSet);
        return this;
    }

    public final OpenSearchQueryBuilder<K, Q> withMediaType(String mediaType) {
        query.setMediaType(mediaType);
        return this;
    }


    /**
     * When running docker tests, the current host needs to be specified.
     * @param  uri URI to local docker test instance
     */
    public final OpenSearchQueryBuilder<K, Q> withOpensearchUri(URI uri) {
        query.setOpenSearchUri(uri);
        return this;
    }



    /**
     * Validate sort keys.
     *
     * @throws BadRequestException if sort key is invalid
     */
    protected abstract void validateSort() throws BadRequestException;

    /**
     * Sample code for assignDefaultValues.
     * <p>Usage:</p>
     * <samp>requiredMissing().forEach(key -> { <br>
     *     switch (key) {<br>
     *         case LANGUAGE:<br>
     *             query.setValue(key, DEFAULT_LANGUAGE_CODE);<br>
     *             break;<br>
     *         default:<br>
     *             break;<br>
     *     }});<br>
     * </samp>
     */
    protected abstract void assignDefaultValues();

    /**
     * Sample code for setValue.
     * <p>Usage:</p>
     * <samp>var qpKey = keyFromString(key,value);<br>
     * if(qpKey.equals(INVALID)) {<br>
     *     invalidKeys.add(key);<br>
     * } else {<br>
     *     query.setValue(qpKey, value);<br>
     * }<br>
     * </samp>
     */
    protected abstract void setValue(String key, String value);

    protected abstract void applyRulesAfterValidation();


    /**
     * returns T.VALID_LUCENE_PARAMETER_KEYS
     */
    protected Collection<String> validKeys() {
        return VALID_SEARCH_PARAMETER_KEYS.stream()
                   .map(ParameterKey::fieldName)
                   .toList();
    }

    @JacocoGenerated
    protected boolean invalidQueryParameter(K key, String value) {
        return isNull(value) || !value.matches(key.valuePattern());
    }


    protected Set<String> getMissingKeys() {
        return
            requiredMissing()
                .stream()
                .map(ParameterKey::fieldName)
                .collect(Collectors.toSet());
    }

    protected Set<K> required() {
        return query.otherRequiredKeys;

    }

    @JacocoGenerated
    protected Set<K> requiredMissing() {
        return
            required().stream()
                .filter(key -> !query.searchParameters.containsKey(key))
                .filter(key -> !query.pageParameters.containsKey(key))
                .collect(Collectors.toSet());
    }

    @JacocoGenerated
    protected void validatesEntrySet(Map.Entry<K, String> entry) throws BadRequestException {
        final var key = entry.getKey();
        final var value = entry.getValue();
        if (invalidQueryParameter(key, value)) {
            final var keyName =  key.fieldName();
            final var errorMessage = nonNull(key.errorMessage())
                ? key.errorMessage().formatted(keyName, value)
                : invalidQueryParametersMessage(keyName, value);
            throw new BadRequestException(errorMessage);
        }
    }

    private void setEntryValue(Map.Entry<String, String> entry) {
        setValue(entry.getKey(), entry.getValue());
    }

}