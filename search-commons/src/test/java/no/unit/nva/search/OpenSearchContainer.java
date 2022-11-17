package no.unit.nva.search;

import org.testcontainers.containers.GenericContainer;

import java.util.List;

public class OpenSearchContainer extends GenericContainer<OpenSearchContainer> {
    private static final int CONTAINER_PORT = 9200;
    private static final String SECURITY_DISABLED = "true";
    private static final String DISCOVERY_TYPE = "single-node";


    public OpenSearchContainer() {
        super("opensearchproject/opensearch:latest");
        this.withEnv("plugins.security.disabled", SECURITY_DISABLED)
            .withEnv("discovery.type", DISCOVERY_TYPE)
            .setExposedPorts(List.of(CONTAINER_PORT));
    }

    public String getHttpHostAddress() {
        return this.getHost() + ":" + this.getMappedPort(CONTAINER_PORT);
    }


}
