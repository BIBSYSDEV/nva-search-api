package no.unit.nva.search.common.records;

import no.unit.nva.search.common.Query;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public interface FilterBuilder<Q extends Query<?>> {
    Q apply();

    Q fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException;

}
