package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.objectMapperNoEmpty;
import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.COUNTRY_CODE;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION_CONTRIBUTORS_PATH;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.NO;
import static no.unit.nva.constants.Words.VERIFICATION_STATUS;
import static no.unit.nva.constants.Words.VERIFIED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import no.unit.nva.search.common.records.JsonNodeMutator;
import no.unit.nva.search.resource.Constants;

import nva.commons.core.ioutils.IoUtils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Reduces the number of contributors in a JsonNode.
 *
 * @author Stig Norland
 */
public final class ContributorNodeReducer implements JsonNodeMutator {

    static final int MINIMUM_INCLUDED_CONTRIBUTORS = 5;
    static final JsonPointer CONTRIBUTORS_PATH_POINTER =
        JsonPointer.compile(ENTITY_DESCRIPTION_CONTRIBUTORS_PATH);

    private ContributorNodeReducer() {}

    public static ContributorNodeReducer firstFewContributorsOrVerifiedOrNorwegian() {
        return new ContributorNodeReducer();
    }

    @Override
    public JsonNode transform(JsonNode source) {
        var elements = source.at(CONTRIBUTORS_PATH_POINTER).elements();
        while (elements.hasNext()) {
            if (shouldRemoveContributor(elements.next())) {
                elements.remove();
            }
        }
        return source;
    }

    private boolean shouldRemoveContributor(JsonNode element) {
        return canRemoveAfterMinimumSequenceNo(element)
            && canRemoveWhenNotVerified(element)
            && canRemoveWhenHasNoNorwegianBasedContributor(element);
    }

    private boolean canRemoveWhenNotVerified(JsonNode element) {
        return !VERIFIED.equals(element.path(IDENTITY).path(VERIFICATION_STATUS).asText());
    }

    private boolean canRemoveWhenHasNoNorwegianBasedContributor(JsonNode element) {
        return !element.path(AFFILIATIONS).findValues(COUNTRY_CODE).contains(TextNode.valueOf(NO));
    }

    private boolean canRemoveAfterMinimumSequenceNo(JsonNode element) {
        var sequence = element.path(Constants.SEQUENCE);
        return sequence.isMissingNode() || MINIMUM_INCLUDED_CONTRIBUTORS < sequence.asInt();
    }
    class ContributorNodeReducerTest {

        public static final Path RESOURCE_ENTITYDESCRIPTION_CONTRIBUTORS_JSON =
            Path.of("resource_entitydescription_contributors.json");

        @Test
        void shouldReduceContributorsToThree() throws JsonProcessingException {
            var source = IoUtils.stringFromResources(RESOURCE_ENTITYDESCRIPTION_CONTRIBUTORS_JSON);
            var sourceNode = objectMapperNoEmpty.readTree(source);
            var transformed = firstFewContributorsOrVerifiedOrNorwegian().transform(sourceNode);
            var count = transformed.withArray("/entityDescription/contributors").size();

            assertThat(count, is(equalTo(6)));
        }
    }
}
