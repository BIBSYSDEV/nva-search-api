package no.unit.nva.search2.common.records;

import static no.unit.nva.search2.common.constant.Words.HAS_PUBLIC_FILE;
import static no.unit.nva.search2.common.constant.Words.NO_PUBLIC_FILE;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JacocoGenerated
final class FacetsBuilder {

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
        if (keyName.equals(HAS_PUBLIC_FILE)) {
            return uriwrap.addQueryParameter(HAS_PUBLIC_FILE, Boolean.TRUE.toString()).getUri();
        }
        if (keyName.equals(NO_PUBLIC_FILE)) {
            return uriwrap.addQueryParameter(HAS_PUBLIC_FILE, Boolean.FALSE.toString()).getUri();
        } else {
            return uriwrap.addQueryParameter(keyName, keyValue).getUri();
        }
    }
}
