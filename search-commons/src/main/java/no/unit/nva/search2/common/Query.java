package no.unit.nva.search2.common;

import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.records.QueryContentWrapper;

public abstract class Query<K extends Enum<K> & ParameterKey> {

    private final transient Instant startTime;
    protected QueryKeys<K> queryKeys;

    protected Query() {
        startTime = Instant.now();


    }

    public Instant getStartTime() {
        return startTime;
    }

    public QueryKeys<K> parameters() {
        return queryKeys;
    }

    public abstract Stream<QueryContentWrapper> assemble();

}
