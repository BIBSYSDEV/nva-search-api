package no.unit.nva.search.resource;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Defaults.BIBTEX_UTF_8;
import static nva.commons.apigateway.MediaType.CSV_UTF_8;

import java.util.Collection;
import no.unit.nva.search.common.bibtex.ResourceBibTexTransformer;
import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import nva.commons.apigateway.MediaType;

/**
 * Resolves the OpenSearch {@code _source} include fields for a resource search.
 *
 * <p>BibTeX and CSV exports return many hits, and the full document (contributor affiliations,
 * files, funding) can exceed the search infrastructure response-size limit. For those media types
 * the source is slimmed to the fields the transformer actually reads. JSON requests are unaffected
 * and keep their configured include set.
 */
final class ResourceSourceFields {

  private ResourceSourceFields() {}

  static String[] forMediaType(MediaType mediaType, Collection<String> defaultIncludedFields) {
    final Collection<String> includedFields;
    if (matches(mediaType, BIBTEX_UTF_8)) {
      includedFields = ResourceBibTexTransformer.getBibTexFields();
    } else if (matches(mediaType, CSV_UTF_8)) {
      includedFields = ResourceCsvTransformer.getJsonFields();
    } else {
      includedFields = defaultIncludedFields;
    }
    return includedFields.toArray(String[]::new);
  }

  private static boolean matches(MediaType actualMediaType, MediaType expectedMediaType) {
    return nonNull(actualMediaType) && expectedMediaType.matches(actualMediaType);
  }
}
