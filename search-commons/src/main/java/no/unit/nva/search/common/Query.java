package no.unit.nva.search.common;

import static no.unit.nva.search.common.constant.Functions.readSearchInfrastructureApiUri;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;

import no.unit.nva.search.common.records.ResponseFormatter;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.records.QueryContentWrapper;

/**
 * @author Stig Norland
 */
public abstract class Query<K extends Enum<K> & ParameterKey> {
    protected transient URI infrastructureApiUri = URI.create(readSearchInfrastructureApiUri());
    protected transient QueryKeys<K> queryKeys;
    private final transient Instant startTime;

    public abstract Stream<QueryContentWrapper> assemble();

    public abstract <R, Q extends Query<K>> ResponseFormatter<K> doSearch(OpenSearchClient<R, Q> queryClient);

    protected abstract URI openSearchUri();

    protected Query() {
        startTime = Instant.now();
    }

    public QueryKeys<K> parameters() {
        return queryKeys;
    }

    public Instant getStartTime() {
        return startTime;
    }

}
