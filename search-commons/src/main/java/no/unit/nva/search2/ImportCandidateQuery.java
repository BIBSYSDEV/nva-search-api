package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_IMPORT_CANDIDATE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.ImportCandidate.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search2.constant.ImportCandidate.IMPORT_CANDIDATES_INDEX_NAME;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.SEARCH;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.enums.ImportCandidateParameter.PAGE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT;
import static no.unit.nva.search2.enums.ImportCandidateParameter.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.enums.ImportCandidateParameter.keyFromString;
import static no.unit.nva.search2.enums.ImportCandidateSort.INVALID;
import static no.unit.nva.search2.enums.ImportCandidateSort.fromSortKey;
import static no.unit.nva.search2.enums.ImportCandidateSort.validSortKeys;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.enums.ImportCandidateParameter;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ValueEncoding;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ImportCandidateQuery extends Query<ImportCandidateParameter> {

    ImportCandidateQuery() {
        super();
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    protected Integer getFrom() {
        return getValue(FROM).as();
    }

    @Override
    protected Integer getSize() {
        return getValue(SIZE).as();
    }

    @Override
    protected ImportCandidateParameter getFieldsKey() {
        return ImportCandidateParameter.FIELDS;
    }

    @Override
    protected String[] fieldsToKeyNames(String field) {
        return ALL.equals(field) || isNull(field)
            ? ASTERISK.split(COMMA)     // NONE or ALL -> ['*']
            : Arrays.stream(field.split(COMMA))
                .map(ImportCandidateParameter::keyFromString)
                .map(ParameterKey::searchFields)
                .flatMap(Collection::stream)
                .map(fieldPath -> fieldPath.replace(DOT + KEYWORD, EMPTY_STRING))
                .toArray(String[]::new);
    }

    @Override
    public String getSort() {
        return getValue(SORT).as();
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(IMPORT_CANDIDATES_INDEX_NAME, SEARCH)
                .getUri();
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream() {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : boolQuery();

        var builder = new SearchSourceBuilder().query(queryBuilder);

        var searchAfter = removeKey(SEARCH_AFTER);
        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }

        IMPORT_CANDIDATES_AGGREGATIONS.forEach(builder::aggregation);

        builder.size(getValue(SIZE).as());
        builder.from(getValue(FROM).as());
        getSortStream().forEach(entry -> builder.sort(fromSortKey(entry.getKey()).getFieldName(), entry.getValue()));

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }


    @SuppressWarnings("PMD.GodClass")
    protected static class Builder extends QueryBuilder<ImportCandidateParameter, ImportCandidateQuery> {

        Builder() {
            super(new ImportCandidateQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.fieldName(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.fieldName(), DEFAULT_IMPORT_CANDIDATE_SORT + COLON + DEFAULT_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setPagingValue(qpKey, decodedValue);
                case FIELDS -> query.setPagingValue(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToPagingKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToPagingKey(SORT, decodedValue);
                case ADDITIONAL_IDENTIFIERS, ADDITIONAL_IDENTIFIERS_NOT, ADDITIONAL_IDENTIFIERS_SHOULD,
                    CATEGORY, CATEGORY_NOT, CATEGORY_SHOULD,
                    CREATED_DATE,
                    COLLABORATION_TYPE, COLLABORATION_TYPE_NOT, COLLABORATION_TYPE_SHOULD,
                    CONTRIBUTOR, CONTRIBUTOR_NOT, CONTRIBUTOR_SHOULD,
                    DOI, DOI_NOT, DOI_SHOULD,
                    ID, ID_NOT, ID_SHOULD,
                    IMPORT_STATUS, IMPORT_STATUS_NOT, IMPORT_STATUS_SHOULD,
                    INSTANCE_TYPE, INSTANCE_TYPE_NOT, INSTANCE_TYPE_SHOULD,
                    PUBLICATION_YEAR, PUBLICATION_YEAR_BEFORE, PUBLICATION_YEAR_SINCE,
                    PUBLISHER, PUBLISHER_NOT, PUBLISHER_SHOULD,
                    SEARCH_ALL,
                    TITLE, TITLE_NOT, TITLE_SHOULD,
                    TYPE -> query.setSearchingValue(qpKey, decodedValue);
                default -> invalidKeys.add(key);
            }
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (query.isPresent(PAGE)) {
                if (query.isPresent(FROM)) {
                    var page = query.getValue(PAGE).<Number>as();
                    var perPage = query.getValue(SIZE).<Number>as();
                    query.setPagingValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
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

        @Override
        protected Collection<String> validKeys() {
            return VALID_LUCENE_PARAMETER_KEYS.stream()
                .map(ParameterKey::fieldName)
                .toList();
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return keyFromString(keyName) != ImportCandidateParameter.INVALID;
        }
    }
}