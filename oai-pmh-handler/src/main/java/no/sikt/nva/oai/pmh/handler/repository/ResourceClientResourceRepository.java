package no.sikt.nva.oai.pmh.handler.repository;

import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
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
  public Set<String> fetchAvailableInstanceTypes() {
    var query = buildAggregationsQuery();
    try {
      return query
          .doSearch(resourceClient, Words.RESOURCES)
          .toPagedResponse()
          .aggregations()
          .getOrDefault(INSTANCE_TYPE_AGGREGATION_NAME, Collections.emptyList())
          .stream()
          .map(Facet::key)
          .collect(Collectors.toSet());
    } catch (RuntimeException e) {
      LOGGER.error("Failed to execute search for resources aggregations.", e);
      throw new ResourceSearchException("Error searching for resources aggregations.", e);
    }
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
    var queryBuilder =
        ResourceSearchQuery.builder()
            .withParameter(ResourceParameter.AGGREGATION, Words.NONE)
            .withParameter(ResourceParameter.FROM, ZERO)
            .withParameter(ResourceParameter.SIZE, Integer.toString(batchSize))
            .withParameter(ResourceParameter.SORT, MODIFIED_DATE_ASCENDING);
    return queryBuilder;
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
