package no.unit.nva.search.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.yaml.snakeyaml.LoaderOptions;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;
import org.testcontainers.shaded.org.yaml.snakeyaml.constructor.Constructor;

public class ParametersTest {

    @Test
    public void whenLoadMultipleYAMLDocuments_thenLoadCorrectJavaObjects() {
        Yaml yaml = new Yaml(new Constructor(ParameterCollection.class, new LoaderOptions()));
        var inputStream = this.getClass().getClassLoader().getResourceAsStream("tickets.yaml");

        int count = 0;
        for (Object object : yaml.loadAll(inputStream)) {
            count++;
            assertTrue(object instanceof ParameterCollection);
        }
        assertEquals(2, count);
    }
}
