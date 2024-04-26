package no.unit.nva.search2.common;

import static no.unit.nva.search2.common.constant.Functions.readSearchInfrastructureApiUri;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.records.QueryContentWrapper;

public abstract class Query<K extends Enum<K> & ParameterKey> {
    protected transient URI openSearchUri = URI.create(readSearchInfrastructureApiUri());
    protected transient QueryKeys<K> queryKeys;

    private final transient Instant startTime;

    protected Query() {
        startTime = Instant.now();


    }
    public QueryKeys<K> parameters() {
        return queryKeys;
    }
    public Instant getStartTime() {
        return startTime;
    }

    protected abstract URI getOpenSearchUri();

    public abstract Stream<QueryContentWrapper> assemble();

}
