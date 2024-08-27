package no.unit.nva.search.common;

import static no.unit.nva.search.common.constant.Functions.readSearchInfrastructureApiUri;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;

import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.records.QueryContentWrapper;
import no.unit.nva.search.common.records.ResponseFormatter;

/**
 * @author Stig Norland
 */
public abstract class Query<K extends Enum<K> & ParameterKey<K>> {
    protected transient URI infrastructureApiUri = URI.create(readSearchInfrastructureApiUri());
    protected transient QueryKeys<K> queryKeys;
    private final transient Instant startTime;

    public abstract Stream<QueryContentWrapper> assemble();

    /**
     * Method to mimic Domain driven design.
     *
     * @param queryClient simple service to do i/o (http)
     * @return ResponseFormatter<ParameterKey>
     */
    public abstract ResponseFormatter<K> doSearch(OpenSearchClient<?, Query<K>> queryClient);

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
