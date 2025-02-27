package no.unit.nva.search.ticket;

import static java.util.function.Predicate.not;
import static no.unit.nva.constants.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.constants.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.constants.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.EXCLUDE_KEYWORD;
import static no.unit.nva.constants.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.constants.Words.NONE;
import static no.unit.nva.constants.Words.POST_FILTER;
import static no.unit.nva.constants.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.constants.Words.SEARCH;
import static no.unit.nva.constants.Words.TICKETS;
import static no.unit.nva.search.common.constant.Functions.toEnumStrings;
import static no.unit.nva.search.common.constant.Functions.trimSpace;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.ticket.Constants.UNHANDLED_KEY;
import static no.unit.nva.search.ticket.Constants.facetTicketsPaths;
import static no.unit.nva.search.ticket.Constants.getTicketsAggregations;
import static no.unit.nva.search.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search.ticket.TicketParameter.ASSIGNEE;
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
import static no.unit.nva.search.ticket.TicketParameter.TICKET_PARAMETER_SET;
import static no.unit.nva.search.ticket.TicketParameter.TYPE;
import static no.unit.nva.search.ticket.TicketStatus.NEW;
import static no.unit.nva.search.ticket.TicketStatus.PENDING;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.index.query.QueryBuilders.multiMatchQuery;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.common.AsType;
import no.unit.nva.search.common.ParameterValidator;
import no.unit.nva.search.common.SearchQuery;
import no.unit.nva.search.common.builder.AcrossFieldsQuery;
import no.unit.nva.search.common.builder.KeywordQuery;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.SortKey;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

/**
 * TicketSearchQuery is a class that searches for tickets.
 *
 * @author Stig Norland
 * @author Sondre Vestad
 */
public final class TicketSearchQuery extends SearchQuery<TicketParameter> {

  private final TicketAccessFilter accessFilter;

  private TicketSearchQuery() {
    super();
    applyImpossibleWhiteListFilters();
    accessFilter = new TicketAccessFilter(this);
  }

  public static TicketParameterValidator builder() {
    return new TicketParameterValidator();
  }

  public boolean hasOrganization(URI organizationId) {
    return accessFilter.hasOrganization(organizationId);
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
    var denySet =
        Arrays.stream(TicketType.values())
            .filter(item -> !accessFilter.getAccessRightAsTicketTypes().contains(item))
            .map(Enum::toString)
            .toArray(String[]::new);
    return getTicketsAggregations(accessFilter.getCurrentUser(), denySet);
  }

  @JacocoGenerated // default value shouldn't happen, (developer have forgotten to handle a key)
  @Override
  protected Stream<Entry<TicketParameter, QueryBuilder>> builderCustomQueryStream(
      TicketParameter key) {
    return switch (key) {
      case ASSIGNEE -> builderStreamByAssignee();
      case ORGANIZATION_ID, ORGANIZATION_ID_NOT -> builderStreamByOrganization(key);
      case STATUS, STATUS_NOT -> builderStreamByStatus(key);
      default -> throw new IllegalArgumentException(UNHANDLED_KEY + key.name());
    };
  }

  private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByStatus(TicketParameter key) {
    // we cannot query status New here, it is done together with assignee.
    return hasAssigneeAndStatusNew()
        ? Stream.empty()
        : new KeywordQuery<TicketParameter>().buildQuery(key, parameters().get(key).toString());
  }

  public TicketAccessFilter withFilter() {
    return accessFilter;
  }

  private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByAssignee() {
    var searchByUserName = // override assignee if <user pending> is used
        parameters().isPresent(BY_USER_PENDING)
            ? accessFilter.getCurrentUser()
            : parameters().get(ASSIGNEE).toString();

    var returnedQuery =
        new BoolQueryBuilder().minimumShouldMatch(1).queryName("builderStreamByAssignee");

    if (hasStatusNew()) { // we'll query assignee and status New here....
      returnedQuery.should(new TermQueryBuilder(STATUS_KEYWORD, NEW.toString()));
    }
    var assigneeWithStatus = new BoolQueryBuilder().must(assigneeQuery(searchByUserName));
    if (hasMoreThanOneStatus()) {
      assigneeWithStatus.must(new TermsQueryBuilder(STATUS_KEYWORD, statusWithoutNew()));
    }

    returnedQuery.should(assigneeWithStatus);

    return Functions.queryToEntry(ASSIGNEE, returnedQuery);
  }

  private boolean hasMoreThanOneStatus() {
    return parameters().get(STATUS).asSplitStream(COMMA).count() > 1;
  }

  private String[] statusWithoutNew() {
    return parameters()
        .get(STATUS)
        .asSplitStream(COMMA)
        .filter(s -> !NEW.toString().equals(s))
        .toArray(String[]::new);
  }

  private MultiMatchQueryBuilder assigneeQuery(String searchByUserName) {
    return multiMatchQuery(
            searchByUserName, ASSIGNEE.searchFields(EXCLUDE_KEYWORD).toArray(String[]::new))
        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
        .autoGenerateSynonymsPhraseQuery(false)
        .fuzzyTranspositions(false)
        .operator(Operator.AND);
  }

  private Stream<Entry<TicketParameter, QueryBuilder>> builderStreamByOrganization(
      TicketParameter key) {
    if (parameters().get(EXCLUDE_SUBUNITS).asBoolean()) {
      return new KeywordQuery<TicketParameter>()
          .buildQuery(EXCLUDE_SUBUNITS, parameters().get(key).toString())
          .map(query -> Map.entry(key, query.getValue()));
    } else {
      return new AcrossFieldsQuery<TicketParameter>()
          .buildQuery(key, parameters().get(key).toString());
    }
  }

  private boolean hasStatusNew() {
    return parameters().get(STATUS).contains(NEW);
  }

  private boolean hasAssigneeAndStatusNew() {
    return hasStatusNew() && parameters().isPresent(ASSIGNEE);
  }

  /**
   * Add a (default) filter to the query that will never match any document.
   *
   * <p>This whitelist the Query from any forgetful developer (me)
   *
   * <p>i.e.In order to return any results, withFilter* must be set
   */
  private void applyImpossibleWhiteListFilters() {
    var randomUri = URI.create("https://www.example.com/" + UUID.randomUUID());
    final var filterId =
        new TermQueryBuilder(ORGANIZATION_ID_KEYWORD, randomUri)
            .queryName(ORGANIZATION_ID.asCamelCase() + POST_FILTER);
    filters().set(filterId);
  }

  public static class TicketParameterValidator
      extends ParameterValidator<TicketParameter, TicketSearchQuery> {

    TicketParameterValidator() {
      super(new TicketSearchQuery());
    }

    @Override
    protected void assignDefaultValues() {
      requiredMissing()
          .forEach(
              key -> {
                switch (key) {
                  case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                  case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                  case SORT -> setValue(key.name(), RELEVANCE_KEY_NAME);
                  case AGGREGATION -> setValue(key.name(), NONE);
                  default -> {
                    /* ignore and continue */
                  }
                }
              });
    }

    @Override
    protected void applyRulesAfterValidation() {
      // convert page to offset if offset is not set
      if (query.parameters().isPresent(PAGE)) {
        if (query.parameters().isPresent(FROM)) {
          var page = query.parameters().get(PAGE).<Number>as().longValue();
          var perPage = query.parameters().get(SIZE).<Number>as().longValue();
          query.parameters().set(FROM, String.valueOf(page * perPage));
        }
        query.parameters().remove(PAGE);
      }
      if (query.parameters().isPresent(BY_USER_PENDING)) {
        query.parameters().set(STATUS, PENDING.toString());

        if (!query.parameters().get(BY_USER_PENDING).asBoolean()) {
          query.parameters().set(TYPE, query.parameters().get(BY_USER_PENDING).toString());
        }
        if (!query.parameters().isPresent(ASSIGNEE)) {
          // business rule: need to set assignee to search by user pending
          // (value is ignored)
          query.parameters().set(ASSIGNEE, "👁");
        }
      }
    }

    @Override
    protected Collection<String> validKeys() {
      return TICKET_PARAMETER_SET.stream().map(Enum::name).toList();
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
            INVALID_VALUE_WITH_SORT.formatted(name, TicketSort.validSortKeys()));
      }
    }

    @Override
    protected void setValue(String key, String value) {
      var qpKey = TicketParameter.keyFromString(key);
      var decodedValue = getDecodedValue(qpKey, value);
      switch (qpKey) {
        case INVALID -> invalidKeys.add(key);
        case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION ->
            query.parameters().set(qpKey, decodedValue);
        case NODES_SEARCHED -> query.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
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
