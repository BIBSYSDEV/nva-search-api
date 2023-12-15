package no.unit.nva.search2.dto;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;

public final class FacetsBuilder {

    public static Map<String, List<Facet>> build(String aggregations, URI id) {
        return toMapOfFacets(aggregations)
            .entrySet().stream()
            .map((entry) -> addIdToFacets(entry, id))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, List<Facet>> toMapOfFacets(String aggregations) {
        final var typeReference = new TypeReference<Map<String, List<Facet>>>() {
        };
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(aggregations, typeReference))
            .orElseThrow();
    }

    private static Map.Entry<String, List<Facet>> addIdToFacets(Map.Entry<String, List<Facet>> entry, URI id) {
        final var uriwrap = UriWrapper.fromUri(id);
        var facets = entry.getValue().stream()
            .map(facet -> new Facet(
                createUriFilterByKey(entry.getKey(), facet.key(), uriwrap),
                facet.key(),
                facet.count(),
                facet.labels())
            ).toList();
        return Map.entry(entry.getKey(), facets);
    }

    private static URI createUriFilterByKey(String keyName, String keyValue, UriWrapper uriwrap) {
        return uriwrap.addQueryParameter(keyName, keyValue).getUri();
    }
}
