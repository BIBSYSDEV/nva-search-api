package no.unit.nva.search2.common;

import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameter.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.constants.ErrorMessages.invalidQueryParametersMessage;
import static no.unit.nva.search2.constants.ErrorMessages.requiredMissingMessage;
import static no.unit.nva.search2.constants.ErrorMessages.validQueryParameterNamesMessage;
import static nva.commons.apigateway.RestRequestHandler.EMPTY_STRING;

public abstract class QueryBuilder<T extends Enum<T> & IParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);

    protected final transient Set<String> invalidKeys = new HashSet<>(0);
    protected final transient OpenSearchQuery<T> query;
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
    public QueryBuilder(OpenSearchQuery<T> query) {
        this.query = query;
    }

    /**
     * Builder of CristinQuery.
     * @throws BadRequestException if parameters are invalid or missing
     */
    public OpenSearchQuery<T>  build() throws BadRequestException {
        if (notValidated) {
            validate();
        }
        return query;
    }

    /**
     * Validator of CristinQuery.Builder.
     * @throws BadRequestException if parameters are invalid or missing
     */
    public QueryBuilder<T> validate() throws BadRequestException {
        assignDefaultValues();
        for (var entry : query.queryParameters.entrySet()) {
            throwInvalidParameterValue(entry);
        }
        for (var entry : query.luceneParameters.entrySet()) {
            throwInvalidParameterValue(entry);
        }
        if (!requiredMissing().isEmpty()) {
            throw new BadRequestException(requiredMissingMessage(getMissingKeys()));
        }
        if (!invalidKeys.isEmpty()) {
            throw new BadRequestException(validQueryParameterNamesMessage(validKeys()));
        }
        notValidated = false;
        return this;
    }

    /**
     * Adds query and path parameters from requestInfo.
     */
    public final QueryBuilder<T> fromRequestInfo(RequestInfo requestInfo) {
        return fromQueryParameters(requestInfo.getQueryParameters());
    }


    /**
     * Adds parameters from query.
     */
    public QueryBuilder<T> fromQueryParameters(Map<String, String> parameters) {
        parameters.forEach(this::setValue);
        return this;
    }


    /**
     * Defines which parameters are required.
     * @param requiredParameters comma seperated QueryParameterKeys
     */
    @SafeVarargs
    public final QueryBuilder<T> withRequiredParameters(T... requiredParameters) {
        var tmpSet = Set.of(requiredParameters);
        query.otherRequiredKeys.addAll(tmpSet);
        return this;
    }

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

    /**
     returns T.VALID_QUERY_PARAMETER_NVA_KEYS
     */
    protected Set<String> validKeys() {
        return VALID_LUCENE_PARAMETER_KEYS;
    }

    protected boolean invalidQueryParameter(T key, String value) {
        return isNull(value) || !value.matches(key.getPattern());
    }


    protected Set<String> getMissingKeys() {
        return
            requiredMissing()
                .stream()
                .map(IParameterKey::getKey)
                .collect(Collectors.toSet());
    }

    protected Set<T> required() {
        return query.otherRequiredKeys;

    }

    protected Set<T> requiredMissing() {
        return
            required().stream()
                .filter(key -> !query.luceneParameters.containsKey(key))
                .filter(key -> !query.queryParameters.containsKey(key))
                .collect(Collectors.toSet());
    }

    protected void throwInvalidParameterValue(Map.Entry<T, String> entry) throws BadRequestException {
        final var key = entry.getKey();
        if (invalidQueryParameter(key, entry.getValue())) {
            final var keyName =  key.getKey();
            String errorMessage;
            if (nonNull(key.getErrorMessage())) {
                errorMessage = String.format(key.getErrorMessage(), keyName);
            } else {
                errorMessage = invalidQueryParametersMessage(keyName, EMPTY_STRING);
            }
            throw new BadRequestException(errorMessage);
        }
    }



}