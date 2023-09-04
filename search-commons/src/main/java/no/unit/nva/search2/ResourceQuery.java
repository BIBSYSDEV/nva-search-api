package no.unit.nva.search2;


import static no.unit.nva.search2.ResourceParameter.keyFromString;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_PAGE;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_VALUE_SORT_ORDER;
import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.common.PagedSearchResponseDto;
import no.unit.nva.search2.common.QueryBuilder;

public class ResourceQuery extends OpenSearchQuery<ResourceParameter> {

    public static ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
    }


    @Override
    public PagedSearchResponseDto doSearch(SwsOpenSearchClient queryClient) {
        var requestUri = this.toURI();
        return queryClient.doSearch(requestUri).body();
    }


    public static class ResourceQueryBuilder extends QueryBuilder<ResourceParameter> {

        public ResourceQueryBuilder() {
            super(new ResourceQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case PAGE -> setValue(key.key(), DEFAULT_VALUE_PAGE);
                    case PER_PAGE -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.key(), DEFAULT_VALUE_SORT);
                    case SORT_ORDER -> setValue(key.key(), DEFAULT_VALUE_SORT_ORDER);
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
                         INSTITUTION,ISSN,
                         MODIFIED_BEFORE,MODIFIED_SINCE,
                         PROJECT_CODE,PUBLISHED_BEFORE,
                         PUBLISHED_SINCE, SEARCH_ALL, TITLE,
                         UNIT,USER, YEAR_REPORTED -> query.setValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }
    }

}