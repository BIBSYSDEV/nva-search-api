package no.unit.nva.search2;

import no.unit.nva.search.models.SearchResponseDto;
import org.opensearch.gateway.GatewayException;

import java.util.Map;

import static nva.commons.core.attempt.Try.attempt;

public class ResourceQuery extends OpenSearchQuery<ResourceKeys> {

    public static ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
    }


    @Override
    public SearchResponseDto execute(SwsQueryClient queryClient) throws GatewayException {
        return attempt(()-> queryClient.doSearch(this.toSearchRequest(), this.toURI())).orElseThrow();
    }

    @Override
    protected boolean ignorePathParameters(Map.Entry<ResourceKeys, String> f) {
        return false;
    }

    @Override
    protected String[] getPath() {
        return new String[]{};
    }

}