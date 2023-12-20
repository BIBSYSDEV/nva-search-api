package no.unit.nva.search2;

import static no.unit.nva.search2.common.QueryTools.valueToBoolean;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.PAGE;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static no.unit.nva.search2.enums.ResourceParameter.keyFromString;
import static no.unit.nva.search2.enums.ResourceSort.INVALID;
import static no.unit.nva.search2.enums.ResourceSort.fromSortKey;
import static no.unit.nva.search2.enums.ResourceSort.validSortKeys;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Map.Entry;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.enums.ResourceParameter;
import org.opensearch.search.sort.SortOrder;

@SuppressWarnings({"PMD.GodClass"})
public class ResourceQueryBuilder extends QueryBuilder<ResourceParameter, ResourceQuery> {

    /**
     * Constructor of QueryBuilder.
     * <p>Usage:</p>
     * <samp>Query.builder()<br>
     * .fromRequestInfo(requestInfo)<br> .withRequiredParameters(FROM, SIZE)<br> .build()
     * </samp>
     */
    public ResourceQueryBuilder() {
        super(new ResourceQuery());
    }

    @Override
    protected boolean isKeyValid(String keyName) {
        return keyFromString(keyName) != ResourceParameter.INVALID;
    }

    @Override
    protected void assignDefaultValues() {
        requiredMissing().forEach(key -> {
            switch (key) {
                case FROM -> setKeyValue(key.fieldName(), DEFAULT_OFFSET);
                case SIZE -> setKeyValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                case SORT -> setKeyValue(key.fieldName(), DEFAULT_RESOURCE_SORT + COLON + DEFAULT_SORT_ORDER);
                default -> {
                }
            }
        });
    }

    @Override
    protected void setKeyValue(String key, String value) {
        assignFormatByKey(key);
        var qpKey = keyFromString(key);

        switch (qpKey) {
            case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setValue(qpKey, value);
            case FIELDS -> query.setValue(qpKey, ignoreInvalidFields(value));
            case SORT -> mergeToKey(SORT, trimSpace(value));
            case SORT_ORDER -> mergeToKey(SORT, value);
            case CREATED_BEFORE, CREATED_SINCE,
                MODIFIED_BEFORE, MODIFIED_SINCE,
                PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setValue(qpKey, expandYearToDate(value));
            case HAS_FILE -> query.setValue(qpKey, valueToBoolean(value).toString());
            case CONTEXT_TYPE, CONTEXT_TYPE_NOT, CONTEXT_TYPE_SHOULD,
                CONTRIBUTOR, CONTRIBUTOR_NOT, CONTRIBUTOR_SHOULD,
                CONTRIBUTOR_NAME, CONTRIBUTOR_NAME_NOT, CONTRIBUTOR_NAME_SHOULD,
                DOI, DOI_NOT, DOI_SHOULD,
                FUNDING, FUNDING_SOURCE, FUNDING_SOURCE_NOT, FUNDING_SOURCE_SHOULD,
                ID, ID_NOT, ID_SHOULD,
                INSTANCE_TYPE, INSTANCE_TYPE_NOT, INSTANCE_TYPE_SHOULD,
                INSTITUTION, INSTITUTION_NOT, INSTITUTION_SHOULD,
                ISBN, ISBN_NOT, ISBN_SHOULD, ISSN, ISSN_NOT, ISSN_SHOULD,
                ORCID, ORCID_NOT, ORCID_SHOULD,
                PARENT_PUBLICATION, PARENT_PUBLICATION_SHOULD,
                PROJECT, PROJECT_NOT, PROJECT_SHOULD,
                PUBLICATION_YEAR, PUBLICATION_YEAR_SHOULD,
                SEARCH_ALL,
                TITLE, TITLE_NOT, TITLE_SHOULD,
                TOP_LEVEL_ORGANIZATION,
                UNIT, UNIT_NOT, UNIT_SHOULD,
                USER, USER_NOT, USER_SHOULD,
                USER_AFFILIATION, USER_AFFILIATION_NOT, USER_AFFILIATION_SHOULD -> mergeToKey(qpKey, value);
            case LANG -> { /* ignore and continue */ }
            default -> invalidKeys.add(key);
        }
    }

    @Override
    protected void applyRulesAfterValidation() {
        // convert page to offset if offset is not set
        if (query.isPresent(PAGE)) {
            if (query.isPresent(FROM)) {
                var page = query.getValue(PAGE).<Number>as();
                var perPage = query.getValue(SIZE).<Number>as();
                query.setValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
            }
            query.removeKey(PAGE);
        }
    }

    @Override
    protected void validateSortEntry(Entry<String, SortOrder> entry) {
        if (fromSortKey(entry.getKey()) == INVALID) {
            throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(entry.getKey(), validSortKeys()));
        }
        attempt(entry::getValue)
            .orElseThrow(e -> new IllegalArgumentException(e.getException().getMessage()));
    }
}

