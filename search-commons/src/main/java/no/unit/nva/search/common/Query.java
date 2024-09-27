package no.unit.nva.search.common;

import static no.unit.nva.search.common.constant.Functions.readSearchInfrastructureApiUri;

import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.common.records.QueryContentWrapper;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Query is a class that represents a query to the search service.
 *
 * @author Stig Norland
 */
public abstract class Query<K extends Enum<K> & ParameterKey<K>> {
    private final transient Instant startTime;
    protected transient URI infrastructureApiUri = URI.create(readSearchInfrastructureApiUri());
    protected transient QueryKeys<K> queryKeys;

    protected Query() {
        startTime = Instant.now();
    }

    public abstract Stream<QueryContentWrapper> assemble();

    /**
     * Method to mimic Domain driven design.
     *
     * @param queryClient simple service to do i/o (http)
     * @return ResponseFormatter<ParameterKey>
     */
    public abstract <R, Q extends Query<K>> HttpResponseFormatter<K> doSearch(
            OpenSearchClient<R, Q> queryClient);

    protected abstract URI openSearchUri();

    public QueryKeys<K> parameters() {
        return queryKeys;
    }

    public Instant getStartTime() {
        return startTime;
    }
}
