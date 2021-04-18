package no.unit.nva.search;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.dataimport.S3IonReader;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.DynamoEntry;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.exception.SearchException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class ImportToSearchIndexHandler implements RequestStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(ImportToSearchIndexHandler.class);
    private final ElasticSearchHighLevelRestClient elasticSearchRestClient;
    private S3IonReader ionReader;
    private S3Driver s3Driver;
    private Environment environment;

    @JacocoGenerated
    public ImportToSearchIndexHandler() {
        this(new Environment());
    }

    @JacocoGenerated
    public ImportToSearchIndexHandler(Environment environment) {
        this(null, null, defaultEsClient(environment));
        this.environment = environment;
    }

    public ImportToSearchIndexHandler(S3Driver s3Driver, S3IonReader ionReader,
                                      ElasticSearchHighLevelRestClient elasticSearchRestClient) {
        this.s3Driver = s3Driver;
        this.ionReader = ionReader;
        this.elasticSearchRestClient = elasticSearchRestClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String inputString = IoUtils.streamToString(input);
        ImportDataRequest request = JsonUtils.objectMapper.readValue(inputString, ImportDataRequest.class);
        setupS3Access(request.getBucket());
        Stream<Publication> publishedPublications = fetchPublishedPublicationsFromDynamoDbExportInS3(request);

        List<Try<SortableIdentifier>> indexActions = insertToIndex(publishedPublications)
                                                         .collect(Collectors.toList());

        List<String> failures = collectFailures(indexActions.stream());
        failures.forEach(this::logFailure);
        writeOutput(output, failures);
    }

    protected void writeOutput(OutputStream outputStream, List<String> failures)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputJson = JsonUtils.objectMapperWithEmpty.writeValueAsString(failures);
            writer.write(outputJson);
        }
    }

    private static ElasticSearchHighLevelRestClient defaultEsClient(Environment environment) {
        return new ElasticSearchHighLevelRestClient(environment);
    }

    // This method is necessary due to the fact that S3Driver needs the bucket for initialization which is
    // known only in query time.
    @JacocoGenerated
    private void setupS3Access(String bucketName) {
        s3Driver = new S3Driver(defaultS3Client(), bucketName);
        ionReader = new S3IonReader(s3Driver);
    }

    private S3Client defaultS3Client() {
        String awsRegion = environment
                               .readEnvOpt("AWS_REGION")
                               .orElse(Regions.EU_WEST_1.getName());
        return S3Client.builder().region(Region.of(awsRegion)).build();
    }

    private void logFailure(String failureMessage) {
        logger.warn("Failed to index resource:" + failureMessage);
    }

    private Stream<Publication> fetchPublishedPublicationsFromDynamoDbExportInS3(ImportDataRequest request) {
        List<String> allFiles = s3Driver.listFiles(Path.of(request.getS3Path()));
        List<JsonNode> allContent = fetchAllContentFromDataExport(allFiles);
        return keepOnlyPublishedPublications(allContent);
    }

    private List<String> collectFailures(Stream<Try<SortableIdentifier>> indexActions) {
        return indexActions
                   .filter(Try::isFailure)
                   .map(f -> ExceptionUtils.stackTraceInSingleLine(f.getException()))
                   .collect(Collectors.toList());
    }

    private Stream<Try<SortableIdentifier>> insertToIndex(Stream<Publication> publishedPublications) {
        return publishedPublications
                   .map(IndexDocument::fromPublication)
                   .map(attempt(this::indexDocument));
    }

    private SortableIdentifier indexDocument(IndexDocument doc) throws SearchException {
        elasticSearchRestClient.addDocumentToIndex(doc);
        return doc.getId();
    }

    private Stream<Publication> keepOnlyPublishedPublications(List<JsonNode> allContent) {
        Stream<DynamoEntry> dynamoEntries = allContent.stream().map(this::toDynamoEntry);
        Stream<Publication> allPublications = dynamoEntries
                                                  .filter(entry -> entry instanceof ResourceDao)
                                                  .map(dao -> (ResourceDao) dao)
                                                  .map(ResourceDao::getData)
                                                  .map(Resource::toPublication);
        return allPublications
                   .filter(publication -> PublicationStatus.PUBLISHED.equals(publication.getStatus()));
    }

    private DynamoEntry toDynamoEntry(JsonNode jsonNode) {
        return JsonUtils.objectMapperNoEmpty.convertValue(jsonNode, DynamoEntry.class);
    }

    private List<JsonNode> fetchAllContentFromDataExport(List<String> allFiles) {
        return allFiles.stream()
                   .map(attempt(ionReader::extractJsonNodeStreamFromS3File))
                   .map(Try::toOptional)
                   .flatMap(Optional::stream)
                   .flatMap(Function.identity())
                   .collect(Collectors.toList());
    }
}
