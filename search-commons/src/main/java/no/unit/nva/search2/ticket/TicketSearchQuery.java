package no.unit.nva.search2.ticket;

import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.Functions.toEnumStrings;
import static no.unit.nva.search2.common.constant.Functions.trimSpace;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search2.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.common.constant.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search2.common.constant.Words.SEARCH;
import static no.unit.nva.search2.common.constant.Words.TICKETS;
import static no.unit.nva.search2.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.UNHANDLED_KEY;
import static no.unit.nva.search2.ticket.Constants.facetTicketsPaths;
import static no.unit.nva.search2.ticket.Constants.getTicketsAggregations;
import static no.unit.nva.search2.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search2.ticket.TicketParameter.BY_USER_PENDING;
import static no.unit.nva.search2.ticket.TicketParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search2.ticket.TicketParameter.FIELDS;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.ORGANIZATION_ID;
import static no.unit.nva.search2.ticket.TicketParameter.PAGE;
import static no.unit.nva.search2.ticket.TicketParameter.SEARCH_AFTER;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;
import static no.unit.nva.search2.ticket.TicketParameter.SORT;
import static no.unit.nva.search2.ticket.TicketParameter.SORT_ORDER;
import static no.unit.nva.search2.ticket.TicketParameter.STATUS;
import static no.unit.nva.search2.ticket.TicketParameter.TICKET_PARAMETER_SET;
import static no.unit.nva.search2.ticket.TicketStatus.PENDING;
import static nva.commons.core.paths.UriWrapper.fromUri;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.SearchQuery;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.common.builder.OpensearchQueryKeyword;
import no.unit.nva.search2.common.enums.SortKey;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class TicketSearchQuery extends SearchQuery<TicketParameter> {

    private final TicketFilter filterBuilder;

    private TicketSearchQuery() {
        super();
        applyImpossibleWhiteListFilters();
        filterBuilder = new TicketFilter(this);
    }

    public static TicketParameterValidator builder() {
        return new TicketParameterValidator();
    }


    @Override
    protected TicketParameter keyAggregation() {
        return AGGREGATION;
    }

    @Override
    protected TicketParameter keyFields() {
        return FIELDS;
    }

    @Override
    protected TicketParameter keySortOrder() {
        return SORT_ORDER;
    }

    @Override
    protected TicketParameter keySearchAfter() {
        return SEARCH_AFTER;
    }

    @Override
    protected TicketParameter toKey(String keyName) {
        return TicketParameter.keyFromString(keyName);
    }

    @Override
    protected SortKey toSortKey(String sortName) {
        return TicketSort.fromSortKey(sortName);
    }

    @Override
    protected AsType<TicketParameter> getFrom() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<TicketParameter> getSize() {
        return parameters().get(SIZE);
    }

    @Override
    public AsType<TicketParameter> getSort() {
        return parameters().get(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return fromUri(openSearchUri).addChild(TICKETS, SEARCH).getUri();
    }

    @Override
    protected String toCsvText(SwsResponse response) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    protected void setFetchSource(SearchSourceBuilder builder) {
        builder.fetchSource(true);
    }

    @Override
    protected Map<String, String> facetPaths() {
        return facetTicketsPaths;
    }

    @Override
    protected List<AggregationBuilder> builderAggregations() {
        return getTicketsAggregations(filterBuilder.getCurrentUser());
    }

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<TicketParameter, QueryBuilder>> builderStreamCustomQuery(TicketParameter key) {
        return switch (key) {
            case ASSIGNEE -> builderStreamByAssignee();
            case ORGANIZATION_ID, ORGANIZATION_ID_NOT -> builderStreamByOrganization(key);
            default -> throw new IllegalArgumentException(UNHANDLED_KEY + key.name());
        };
    }

    public TicketFilter withFilter() {
        return filterBuilder;
    }


    private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByAssignee() {
        var searchByUserName = parameters().isPresent(BY_USER_PENDING) //override assignee if <user pending> is used
            ? filterBuilder.getCurrentUser()
            : parameters().get(TicketParameter.ASSIGNEE).toString();

        return new OpensearchQueryText<TicketParameter>()
            .buildQuery(TicketParameter.ASSIGNEE, searchByUserName);
    }

    private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByOrganization(TicketParameter key) {
        var searchKey = parameters().get(EXCLUDE_SUBUNITS).asBoolean()
            ? EXCLUDE_SUBUNITS
            : key;

        return
            new OpensearchQueryKeyword<TicketParameter>().buildQuery(searchKey, parameters().get(key).as());
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the Query from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withFilter* must be set </p>
     */
    private void applyImpossibleWhiteListFilters() {
        var randomUri = URI.create("https://www.example.com/" + UUID.randomUUID());
        final var filterId =
            new TermQueryBuilder(ORGANIZATION_ID_KEYWORD, randomUri)
                .queryName(ORGANIZATION_ID.asCamelCase() + POST_FILTER);
        filters.set(filterId);
    }

    public static class TicketParameterValidator extends ParameterValidator<TicketParameter, TicketSearchQuery> {

        TicketParameterValidator() {
            super(new TicketSearchQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.name(), RELEVANCE_KEY_NAME);
                    case AGGREGATION -> setValue(key.name(), NONE);
                    default -> { /* ignore and continue */ }
                }
            });
        }

        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (searchQuery.parameters().isPresent(PAGE)) {
                if (searchQuery.parameters().isPresent(FROM)) {
                    var page = searchQuery.parameters().get(PAGE).<Number>as();
                    var perPage = searchQuery.parameters().get(SIZE).<Number>as();
                    searchQuery.parameters().set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                searchQuery.parameters().remove(PAGE);
            }
            if (searchQuery.parameters().isPresent(BY_USER_PENDING)) {
                searchQuery.parameters().set(TicketParameter.TYPE, searchQuery.parameters().get(BY_USER_PENDING).as());
                searchQuery.parameters().set(STATUS, PENDING.toString());
            }
        }

        @Override
        protected Collection<String> validKeys() {
            return TICKET_PARAMETER_SET.stream()
                .map(Enum::name)
                .toList();
        }

        @Override
        protected void validateSortKeyName(String name) {
            var nameSort = name.split(COLON_OR_SPACE);
            if (nameSort.length == NAME_AND_SORT_LENGTH) {
                SortOrder.fromString(nameSort[1]);
            } else if (nameSort.length > NAME_AND_SORT_LENGTH) {
                throw new IllegalArgumentException(TOO_MANY_ARGUMENTS + name);
            }
            if (TicketSort.fromSortKey(nameSort[0]) == TicketSort.INVALID) {
                throw new IllegalArgumentException(
                    INVALID_VALUE_WITH_SORT.formatted(name, TicketSort.validSortKeys())
                );
            }
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = TicketParameter.keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION -> searchQuery.parameters().set(qpKey, decodedValue);
                case FIELDS -> searchQuery.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case TYPE -> mergeToKey(qpKey, toEnumStrings(TicketType::fromString, decodedValue));
                case STATUS -> mergeToKey(qpKey, toEnumStrings(TicketStatus::fromString, decodedValue));
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return TicketParameter.keyFromString(keyName) != TicketParameter.INVALID;
        }

    }
}