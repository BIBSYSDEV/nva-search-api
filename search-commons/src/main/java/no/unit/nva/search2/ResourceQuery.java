package no.unit.nva.search2;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameter.ID;
import static no.unit.nva.search2.ResourceParameter.keyFromString;
import static no.unit.nva.search2.constants.ErrorMessages.invalidQueryParametersMessage;
import static nva.commons.apigateway.RestRequestHandler.EMPTY_STRING;
import java.util.Map;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.common.QueryBuilder;
import nva.commons.apigateway.exceptions.BadRequestException;

public class ResourceQuery extends OpenSearchQuery<ResourceParameter> {

    public static ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
    }


    @Override
    public SearchResponseDto execute(SwsOpenSearchClient queryClient) {
        return queryClient.doSearch(this.toURI());
    }

    public static class ResourceQueryBuilder extends QueryBuilder<ResourceParameter> {

        public ResourceQueryBuilder() {
            super(new ResourceQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case PAGE -> query.setValue(key, DEFAULT_VALUE_PAGE);
                    case PER_PAGE ->  query.setValue(key, DEFAULT_VALUE_PER_PAGE);
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
                         INSTITUTION,ISSN, QUERY,
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
        protected void throwInvalidParameterValue(Map.Entry<ResourceParameter, String> entry)
            throws BadRequestException {
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

}