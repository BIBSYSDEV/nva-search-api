package no.sikt.nva.oai.pmh.handler.repository;

import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.constants.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.ParameterValidator;
import no.unit.nva.search.common.records.Facet;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.resource.SimplifiedMutator;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceClientResourceRepository implements ResourceRepository {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ResourceClientResourceRepository.class);
  private static final String ASCENDING = "asc";
  private static final String COLON = ":";
  private static final String MODIFIED_DATE_ASCENDING =
      ResourceSort.MODIFIED_DATE.asCamelCase() + COLON + ASCENDING;
  private static final String INSTANCE_TYPE_AGGREGATION_NAME = "type";

  private final ResourceClient resourceClient;

  public ResourceClientResourceRepository(final ResourceClient resourceClient) {
    this.resourceClient = resourceClient;
  }

  @Override
  public PagedResponse fetchInitialPage(
      final OaiPmhDateTime from, final OaiPmhDateTime until, final SetSpec setSpec, int pageSize) {
    var query = buildPageQuery(from.orElse(null), until, setSpec, pageSize);
    return doQuery(query);
  }

  @Override
  public PagedResponse fetchNextPage(
      String from, OaiPmhDateTime until, SetSpec setSpec, int pageSize) {
    var query = buildPageQuery(from, until, setSpec, pageSize);
    return doQuery(query);
  }

  @Override
  public Sets fetchSetsFromAggregations() {
    var query = buildAggregationsQuery();
    try {
      var aggregations =
          query.doSearch(resourceClient, Words.RESOURCES).toPagedResponse().aggregations();

      var instanceTypes =
          aggregations
              .getOrDefault(INSTANCE_TYPE_AGGREGATION_NAME, Collections.emptyList())
              .stream()
              .map(Facet::key)
              .collect(Collectors.toSet());

      var institutionIdentifiers =
          aggregations.getOrDefault(TOP_LEVEL_ORGANIZATION, Collections.emptyList()).stream()
              .map(Facet::key)
              .map(UriWrapper::fromUri)
              .map(UriWrapper::getLastPathElement)
              .collect(Collectors.toSet());

      return new Sets(instanceTypes, institutionIdentifiers);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to execute search for resources aggregations.", e);
      throw new ResourceSearchException("Error searching for resources aggregations.", e);
    }
  }

  @Override
  public Optional<ResourceSearchResponse> fetchByIdentifier(String identifier) {
    var query = buildFetchResourceByIdentifierQuery(identifier);
    var pagedResponse = doQuery(query);
    return pagedResponse.hits().isEmpty()
        ? Optional.empty()
        : Optional.of(pagedResponse.hits().getFirst());
  }

  private ResourceSearchQuery buildFetchResourceByIdentifierQuery(String identifier) {
    var builder = buildQueryWithMandatoryParameters(1);
    applyIdentifierParameter(builder, identifier);
    return applyFilterAndBuild(builder);
  }

  private void applyIdentifierParameter(
      ParameterValidator<ResourceParameter, ResourceSearchQuery> builder, String identifier) {
    builder.withParameter(ResourceParameter.ID, identifier);
  }

  private ResourceSearchQuery buildAggregationsQuery() {
    try {
      return ResourceSearchQuery.builder()
          .withParameter(ResourceParameter.AGGREGATION, ALL)
          .withParameter(ResourceParameter.FROM, ZERO)
          .withParameter(ResourceParameter.SIZE, ZERO)
          .build()
          .withFilter()
          .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
          .apply();
    } catch (BadRequestException e) {
      LOGGER.error("Failed to build aggregation query.", e);
      throw new ResourceSearchException("Building query for aggregation failed", e);
    }
  }

  private PagedResponse doQuery(final ResourceSearchQuery query) {
    try {
      var response =
          query.doSearch(resourceClient, Words.RESOURCES).withMutators(new SimplifiedMutator());
      var mutatedHits = response.toMutatedHits();
      var hits =
          mutatedHits.stream()
              .map(
                  node ->
                      JsonUtils.dtoObjectMapper.convertValue(node, ResourceSearchResponse.class))
              .toList();
      return new PagedResponse(response.swsResponse().getTotalSize(), hits);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to execute search for resources.", e);
      throw new ResourceSearchException("Error searching for resources.", e);
    }
  }

  private static boolean isResourceTypeGeneralSpecWithChild(SetSpec setSpec) {
    return SetRoot.RESOURCE_TYPE_GENERAL.equals(setSpec.root()) && setSpec.children().length > 0;
  }

  private static ResourceSearchQuery buildPageQuery(
      final String from, final OaiPmhDateTime until, final SetSpec setSpec, final int batchSize) {
    var queryBuilder = buildQueryWithMandatoryParameters(batchSize);
    applyOptionalParameters(from, until, setSpec, queryBuilder);
    return applyFilterAndBuild(queryBuilder);
  }

  private static void applyOptionalParameters(
      String from,
      OaiPmhDateTime until,
      SetSpec setSpec,
      ParameterValidator<ResourceParameter, ResourceSearchQuery> queryBuilder) {
    Optional.ofNullable(from)
        .ifPresent(
            fromValue -> queryBuilder.withParameter(ResourceParameter.MODIFIED_SINCE, fromValue));
    until.ifPresent(
        untilValue -> queryBuilder.withParameter(ResourceParameter.MODIFIED_BEFORE, untilValue));
    setSpec.ifPresent(
        ignored -> {
          if (isResourceTypeGeneralSpecWithChild(setSpec)) {
            queryBuilder.withParameter(ResourceParameter.INSTANCE_TYPE, setSpec.children()[0]);
          }
        });
  }

  private static ParameterValidator<ResourceParameter, ResourceSearchQuery>
      buildQueryWithMandatoryParameters(int batchSize) {
    return ResourceSearchQuery.builder()
        .withParameter(ResourceParameter.AGGREGATION, Words.NONE)
        .withParameter(ResourceParameter.FROM, ZERO)
        .withParameter(ResourceParameter.SIZE, Integer.toString(batchSize))
        .withParameter(ResourceParameter.SORT, MODIFIED_DATE_ASCENDING);
  }

  private static ResourceSearchQuery applyFilterAndBuild(
      ParameterValidator<ResourceParameter, ResourceSearchQuery> queryBuilder) {
    try {
      return queryBuilder
          .withAlwaysIncludedFields(SimplifiedMutator.getIncludedFields())
          .build()
          .withFilter()
          .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
          .apply();
    } catch (BadRequestException e) {
      // should never happen unless query validation code is changed!
      LOGGER.error("Failed to build page query.", e);
      throw new ResourceSearchException("Building query for page query failed", e);
    }
  }
}
