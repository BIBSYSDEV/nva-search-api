package no.unit.nva.search.common.bibliography;

import static java.util.function.Predicate.not;
import static no.unit.nva.search.common.bibliography.SchemaOrgNodeReader.text;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_ACTOR;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_AUTHOR;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_COMPOSER;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_CONTRIBUTOR;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_DIRECTOR;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_EDITOR;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_ILLUSTRATOR;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_PRODUCER;
import static no.unit.nva.search.common.bibliography.SchemaOrgPersonBuilder.PROP_TRANSLATOR;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SchemaOrgItemTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOrgItemTransformer.class);

  private static final String ID_POINTER = "/id";
  private static final String HANDLE_POINTER = "/handle";
  private static final String INSTANCE_TYPE_POINTER =
      "/entityDescription/reference/publicationInstance/type";
  private static final String MAIN_TITLE_POINTER = "/entityDescription/mainTitle";
  private static final String ABSTRACT_POINTER = "/entityDescription/abstract";
  private static final String YEAR_POINTER = "/entityDescription/publicationDate/year";
  private static final String TAGS_POINTER = "/entityDescription/tags";
  private static final String DOI_POINTER = "/entityDescription/reference/doi";
  private static final String NVA_DOI_POINTER = "/doi";

  private SchemaOrgItemTransformer() {} // NO-OP

  static SchemaOrgItem transform(JsonNode doc) {
    var nvaType = text(doc, INSTANCE_TYPE_POINTER).orElse("");
    var id = text(doc, ID_POINTER).orElse("");
    var url = text(doc, HANDLE_POINTER).orElse(id);
    var resolvedUrl = url.isBlank() ? null : url;
    var category = SchemaOrgType.fieldCategory(nvaType);
    var contributors = SchemaOrgPersonBuilder.buildContributors(doc);

    LOGGER.debug("Transforming NVA type '{}' to schema.org item", nvaType);

    return new SchemaOrgItem(
        SchemaOrgType.toSchemaOrgType(nvaType),
        resolvedUrl,
        resolvedUrl,
        text(doc, MAIN_TITLE_POINTER).orElse(null),
        contributors.get(PROP_AUTHOR),
        contributors.get(PROP_EDITOR),
        contributors.get(PROP_TRANSLATOR),
        contributors.get(PROP_ILLUSTRATOR),
        contributors.get(PROP_PRODUCER),
        contributors.get(PROP_DIRECTOR),
        contributors.get(PROP_ACTOR),
        contributors.get(PROP_COMPOSER),
        contributors.get(PROP_CONTRIBUTOR),
        text(doc, YEAR_POINTER).orElse(null),
        text(doc, ABSTRACT_POINTER).orElse(null),
        buildKeywords(doc).orElse(null),
        buildDoi(doc).orElse(null),
        SchemaOrgPublicationContextBuilder.buildIsPartOf(doc, category),
        SchemaOrgPublicationContextBuilder.buildIsbn(doc, category),
        SchemaOrgPublicationContextBuilder.buildNumberOfPages(doc, category),
        SchemaOrgPublicationContextBuilder.buildPublisher(doc, category),
        SchemaOrgPublicationContextBuilder.buildPageStart(doc, category),
        SchemaOrgPublicationContextBuilder.buildPageEnd(doc, category));
  }

  private static Optional<String> buildKeywords(JsonNode doc) {
    var tags = doc.at(TAGS_POINTER);
    if (tags.isMissingNode() || !tags.isArray()) {
      return Optional.empty();
    }
    var joined =
        StreamSupport.stream(tags.spliterator(), false)
            .map(JsonNode::asText)
            .filter(not(String::isBlank))
            .collect(Collectors.joining(", "));
    return joined.isEmpty() ? Optional.empty() : Optional.of(joined);
  }

  private static Optional<String> buildDoi(JsonNode doc) {
    return text(doc, DOI_POINTER).or(() -> text(doc, NVA_DOI_POINTER)).filter(not(String::isBlank));
  }
}
