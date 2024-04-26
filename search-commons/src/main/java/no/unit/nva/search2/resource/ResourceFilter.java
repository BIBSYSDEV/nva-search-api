package no.unit.nva.search2.resource;

import static no.unit.nva.search2.common.constant.Words.PUBLISHER;
import static no.unit.nva.search2.common.constant.Words.STATUS;
import no.unit.nva.search2.common.enums.PublicationStatus;
import no.unit.nva.search2.common.records.FilterBuilder;
import static no.unit.nva.search2.resource.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search2.resource.Constants.STATUS_KEYWORD;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.Arrays;

public class ResourceFilter implements FilterBuilder<ResourceQuery> {

    private final ResourceQuery resourceQuery;

    public ResourceFilter(ResourceQuery query) {
        this.resourceQuery = query;
    }

    @Override
    public ResourceQuery apply() {
        return resourceQuery;
    }

    @Override
    public ResourceQuery fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        return null;
    }

    /**
     * Filter on Required Status.
     *
     * <p>Only STATUES specified here will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     * <p>See {@link PublicationStatus} for available values.</p>
     *
     * @param publicationStatus the required statues
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter requiredStatus(PublicationStatus... publicationStatus) {
        final var values = Arrays.stream(publicationStatus)
            .map(PublicationStatus::toString)
            .toArray(String[]::new);
        final var filter = new TermsQueryBuilder(STATUS_KEYWORD, values)
            .queryName(STATUS);
        this.resourceQuery.filters.add(filter);
        return this;
    }

    /**
     * Filter on organization.
     * <P>Only documents belonging to organization specified are searchable (for the user)
     * </p>
     *
     * @param organization uri of publisher
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter organization(URI organization) {
        final var filter = new TermQueryBuilder(PUBLISHER_ID_KEYWORD, organization.toString())
            .queryName(PUBLISHER);
        this.resourceQuery.filters.add(filter);
        return this;
    }
}
