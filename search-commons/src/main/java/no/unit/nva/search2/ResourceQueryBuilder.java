package no.unit.nva.search2;

import nva.commons.apigateway.exceptions.BadRequestException;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceKeys.*;
import static nva.commons.apigateway.RestRequestHandler.EMPTY_STRING;

@SuppressWarnings({"PMD.GodClass"})
public class ResourceQueryBuilder extends QueryBuilder<ResourceKeys> {

    public ResourceQueryBuilder() {
        super(new ResourceQuery());
    }

    @Override
    protected void assignDefaultValues() {
        requiredMissing().forEach(key -> {
            switch (key) {
                case ID -> query.setPath(key, query.getValue(ID));
                case PAGE -> query.setValue(key, PARAMETER_PAGE_DEFAULT_VALUE);
                case PER_PAGE ->  query.setValue(key, PARAMETER_PER_PAGE_DEFAULT_VALUE);
                default -> { }
            }
        });
    }

    @Override
    protected void setPath(String key, String value) {
        final var nonNullValue = nonNull(value) ? value : EMPTY_STRING;

        if (key.equals(ID.getKey())) {
            setValue(key,nonNullValue);
        } else {
            invalidKeys.add(key);
        }
    }

    @Override
    protected void setValue(String key, String value) {
        var qpKey = keyFromString(key,value);
        switch (qpKey) {
            case CATEGORY,CONTRIBUTOR,
             CREATED_BEFORE,CREATED_SINCE,
             DOI,FUNDING,FUNDING_SOURCE,ID,
             INSTITUTION,ISSN,
             MODIFIED_BEFORE,MODIFIED_SINCE,
             PROJECT_CODE,PUBLISHED_BEFORE,
             PUBLISHED_SINCE,TITLE,
             UNIT,USER, YEAR_REPORTED, SORT, FIELDS, PER_PAGE, PAGE -> query.setValue(qpKey, value);
            case LANG -> {
                // ignore and continue
            }
            default -> invalidKeys.add(key);
        }
    }

    @Override
    protected Set<String> validKeys() {
        return VALID_QUERY_PARAMETER_KEYS;
    }

    @Override
    protected void throwInvalidParamererValue(Map.Entry<ResourceKeys, String> entry) throws BadRequestException {
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
