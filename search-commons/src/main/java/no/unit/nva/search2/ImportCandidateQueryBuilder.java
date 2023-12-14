package no.unit.nva.search2;

import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.enums.ImportCandidateParameter;
import no.unit.nva.search2.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.sort.SortOrder;

import java.util.Collection;
import java.util.Map;

import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_IMPORT_CANDIDATE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.enums.ImportCandidateParameter.PAGE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT;
import static no.unit.nva.search2.enums.ImportCandidateParameter.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.enums.ImportCandidateParameter.keyFromString;
import static no.unit.nva.search2.enums.ImportCandidateSort.INVALID;
import static no.unit.nva.search2.enums.ImportCandidateSort.fromSortKey;
import static no.unit.nva.search2.enums.ImportCandidateSort.validSortKeys;
import static nva.commons.core.attempt.Try.attempt;

public class ImportCandidateQueryBuilder extends QueryBuilder<ImportCandidateParameter, ImportCandidateQuery> {

    ImportCandidateQueryBuilder() {
        super(new ImportCandidateQuery());
    }

    @Override
    protected void assignDefaultValues() {
        requiredMissing().forEach(key -> {
            switch (key) {
                case FROM -> setKeyValue(key.fieldName(), DEFAULT_OFFSET);
                case SIZE -> setKeyValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                case SORT -> setKeyValue(key.fieldName(), DEFAULT_IMPORT_CANDIDATE_SORT + COLON + DEFAULT_SORT_ORDER);
                default -> {
                }
            }
        });
    }

    @Override
    protected void setKeyValue(String key, String value) {
        if (isKeyFormatUnset()) {
            assignFormatByKey(key);
        }
        var qpKey = keyFromString(key);
        var decodedValue = qpKey.valueEncoding() != ParameterKey.ValueEncoding.NONE
            ? decodeUTF(value)
            : value;
        switch (qpKey) {
            case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setValue(qpKey, decodedValue);
            case FIELDS -> query.setValue(qpKey, ignoreInvalidFields(decodedValue));
            case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
            case SORT_ORDER -> mergeToKey(SORT, decodedValue);
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
                TYPE -> mergeToKey(qpKey, decodedValue);
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
                query.setValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
            }
            query.removeKey(PAGE);
        }
    }

    @Override
    protected void validateSortEntry(Map.Entry<String, SortOrder> entry) {
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
