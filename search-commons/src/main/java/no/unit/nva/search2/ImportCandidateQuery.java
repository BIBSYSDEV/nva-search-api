package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ImportCandidate.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search2.constant.ImportCandidate.IMPORT_CANDIDATES_INDEX_NAME;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.SEARCH;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FIELDS;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT_ORDER;
import static no.unit.nva.search2.enums.ImportCandidateSort.fromSortKey;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.enums.ImportCandidateParameter;
import no.unit.nva.search2.enums.ParameterKey;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

public final class ImportCandidateQuery extends Query<ImportCandidateParameter> {

    static ImportCandidateQueryBuilder builder() {
        return new ImportCandidateQueryBuilder();
    }

    ImportCandidateQuery() {
        super();
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
        return FIELDS;
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
    public AsType getSort() {
        return getValue(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(IMPORT_CANDIDATES_INDEX_NAME, SEARCH)
                .getUri();
    }

    @Override
    protected boolean isPagingValue(ImportCandidateParameter key) {
        return key.ordinal() >= FIELDS.ordinal() && key.ordinal() <= SORT_ORDER.ordinal();
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


}