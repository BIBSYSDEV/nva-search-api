package no.unit.nva.search.ticket;

import static no.unit.nva.search.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search.common.constant.Functions.decodeUTF;
import static no.unit.nva.search.common.constant.Functions.toEnumStrings;
import static no.unit.nva.search.common.constant.Functions.trimSpace;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search.common.constant.Words.COMMA;
import static no.unit.nva.search.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search.common.constant.Words.NONE;
import static no.unit.nva.search.common.constant.Words.POST_FILTER;
import static no.unit.nva.search.common.constant.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search.common.constant.Words.SEARCH;
import static no.unit.nva.search.common.constant.Words.TICKETS;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search.ticket.Constants.UNHANDLED_KEY;
import static no.unit.nva.search.ticket.Constants.facetTicketsPaths;
import static no.unit.nva.search.ticket.Constants.getTicketsAggregations;
import static no.unit.nva.search.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search.ticket.TicketParameter.ASSIGNEE;
import static no.unit.nva.search.ticket.TicketParameter.ASSIGNEE_NOT;
import static no.unit.nva.search.ticket.TicketParameter.BY_USER_PENDING;
import static no.unit.nva.search.ticket.TicketParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search.ticket.TicketParameter.FROM;
import static no.unit.nva.search.ticket.TicketParameter.NODES_EXCLUDED;
import static no.unit.nva.search.ticket.TicketParameter.NODES_INCLUDED;
import static no.unit.nva.search.ticket.TicketParameter.NODES_SEARCHED;
import static no.unit.nva.search.ticket.TicketParameter.ORGANIZATION_ID;
import static no.unit.nva.search.ticket.TicketParameter.PAGE;
import static no.unit.nva.search.ticket.TicketParameter.SEARCH_AFTER;
import static no.unit.nva.search.ticket.TicketParameter.SIZE;
import static no.unit.nva.search.ticket.TicketParameter.SORT;
import static no.unit.nva.search.ticket.TicketParameter.STATUS;
import static no.unit.nva.search.ticket.TicketParameter.STATUS_NOT;
import static no.unit.nva.search.ticket.TicketParameter.TICKET_PARAMETER_SET;
import static no.unit.nva.search.ticket.TicketStatus.NEW;
import static no.unit.nva.search.ticket.TicketStatus.PENDING;
import static nva.commons.core.paths.UriWrapper.fromUri;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import no.unit.nva.search.common.AsType;
import no.unit.nva.search.common.ParameterValidator;
import no.unit.nva.search.common.SearchQuery;
import no.unit.nva.search.common.builder.BuilderAcrossFields;
import no.unit.nva.search.common.builder.BuilderKeyword;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

/**
 * @author Stig Norland
 * @author Sondre Vestad
 */
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
        return NODES_SEARCHED;
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
    protected AsType<TicketParameter> from() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<TicketParameter> size() {
        return parameters().get(SIZE);
    }

    @Override
    public AsType<TicketParameter> sort() {
        return parameters().get(SORT);
    }

    @Override
    protected String[] exclude() {
        return parameters().get(NODES_EXCLUDED).split(COMMA);
    }

    @Override
    protected String[] include() {
        return parameters().get(NODES_INCLUDED).split(COMMA);
    }

    @Override
    public URI openSearchUri() {
        return fromUri(infrastructureApiUri).addChild(TICKETS, SEARCH).getUri();
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
    protected Stream<Entry<TicketParameter, QueryBuilder>> builderCustomQueryStream(TicketParameter key) {
        return switch (key) {
            case ASSIGNEE -> builderStreamByAssignee();
            case ORGANIZATION_ID, ORGANIZATION_ID_NOT -> builderStreamByOrganization(key);
            case STATUS, STATUS_NOT -> builderStreamByStatus(key);
            default -> throw new IllegalArgumentException(UNHANDLED_KEY + key.name());
        };
    }

    private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByStatus(TicketParameter key) {
        return hasAssigneeAndOnlyStatusNew()
            ? Stream.empty()    // we cannot query status New here, it is done together with assignee.
            : new BuilderKeyword<TicketParameter>().buildQuery(key, parameters().get(key).as());
    }


    public TicketFilter withFilter() {
        return filterBuilder;
    }

    private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByAssignee() {
        var searchByUserName = parameters().isPresent(BY_USER_PENDING) //override assignee if <user pending> is used
            ? filterBuilder.getCurrentUser()
            : parameters().get(ASSIGNEE).toString();

        var builtQuery = new BuilderAcrossFields<TicketParameter>()
            .buildQuery(ASSIGNEE, searchByUserName);

        if (hasStatusNew()) {       // we'll query assignee and status New here....
            var queryBuilder = (BoolQueryBuilder) builtQuery.findFirst().orElseThrow().getValue();
            queryBuilder.should(new TermQueryBuilder("status.keyword", NEW.toString()));
            return Functions.queryToEntry(ASSIGNEE, queryBuilder);
        }
        return builtQuery;
    }

    private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByOrganization(TicketParameter key) {
        var searchKey = parameters().get(EXCLUDE_SUBUNITS).asBoolean()
            ? EXCLUDE_SUBUNITS
            : key;

        return
            new BuilderKeyword<TicketParameter>().buildQuery(searchKey, parameters().get(key).as())
                .map(query -> Map.entry(key, query.getValue()));

    }

    private boolean hasStatusNew() {
        return parameters().get(STATUS).contains(NEW) || parameters().get(STATUS_NOT).contains(NEW);
    }

    private boolean hasAssigneeAndOnlyStatusNew() {
        return (
            parameters().get(STATUS).equalsIgnoreCase(NEW) || parameters().get(STATUS_NOT).equalsIgnoreCase(NEW)
        ) && (
            parameters().isPresent(ASSIGNEE) || parameters().isPresent(ASSIGNEE_NOT)
        );
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
                    var page = searchQuery.parameters().get(PAGE).<Number>as().longValue();
                    var perPage = searchQuery.parameters().get(SIZE).<Number>as().longValue();
                    searchQuery.parameters().set(FROM, String.valueOf(page * perPage));
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
                case NODES_SEARCHED -> searchQuery.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
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