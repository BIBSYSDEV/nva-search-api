package no.unit.nva.search.common.records;

import java.net.URI;

/**
 * QueryContentWrapper is a class that wraps the body of a query and the URI of the query.
 *
 * @author Stig Norland
 * @author Sondre Vestad
 * @param body the body of the query
 * @param uri the URI of the query
 */
public record QueryContentWrapper(String body, URI uri) {}
