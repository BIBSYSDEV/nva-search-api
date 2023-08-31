package no.unit.nva.search2;

import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.common.QueryBuilder;

import static no.unit.nva.search2.ResourceParameter.keyFromString;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_PAGE;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_SORT_ORDER;

public class ResourceQuery extends OpenSearchQuery<ResourceParameter> {

    public static ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
    }


    @Override
    public SearchResponseDto apply(SwsOpenSearchClient queryClient) {
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
                    case PAGE -> setValue(key.getKey(), DEFAULT_VALUE_PAGE);
                    case PER_PAGE -> setValue(key.getKey(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.getKey(), DEFAULT_VALUE_SORT);
                    case SORT_ORDER -> setValue(key.getKey(), DEFAULT_VALUE_SORT_ORDER);
                    default -> { }
                }
            });
        }


        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key,value);
            switch (qpKey) {
                case FIELDS,
                         SORT, SORT_ORDER,
                         PER_PAGE, PAGE -> query.setQValue(qpKey, value);
                case CATEGORY,CONTRIBUTOR,
                         CREATED_BEFORE,CREATED_SINCE,
                         DOI,FUNDING,FUNDING_SOURCE,ID,
                         INSTITUTION,ISSN, QUERY,
                         MODIFIED_BEFORE,MODIFIED_SINCE,
                         PROJECT_CODE,PUBLISHED_BEFORE,
                         PUBLISHED_SINCE,TITLE,
                         UNIT,USER, YEAR_REPORTED -> query.setValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }
    }

}