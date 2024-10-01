package no.unit.nva.search.resource;

import static no.unit.nva.constants.Defaults.objectMapperNoEmpty;
import static no.unit.nva.search.resource.ContributorNodeReducer.firstFewContributorsOrVerifiedOrNorwegian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;

import nva.commons.core.ioutils.IoUtils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class ContributorNodeReducerTest {

    public static final Path RESOURCE_ENTITYDESCRIPTION_CONTRIBUTORS_JSON =
            Path.of("resource_entitydescription_contributors.json");

    @Test
    void shouldReduceContributorsToThree() throws JsonProcessingException {
        var source = IoUtils.stringFromResources(RESOURCE_ENTITYDESCRIPTION_CONTRIBUTORS_JSON);
        var sourceNode = objectMapperNoEmpty.readTree(source);
        var transformed = firstFewContributorsOrVerifiedOrNorwegian().transform(sourceNode);
        var count = transformed.withArray("/entityDescription/contributors").size();

        assertThat(count, is(equalTo(8)));
    }
}
