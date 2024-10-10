package no.unit.nva.indexingclient;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.model.IndexingClient;
import no.unit.nva.search.model.records.IndexDocument;
import no.unit.nva.s3.ListingResult;
import no.unit.nva.s3.S3Driver;

import nva.commons.core.paths.UnixPath;

import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkItemResponse.Failure;
import org.opensearch.action.bulk.BulkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BatchIndexer implements IndexingResult<SortableIdentifier> {

    private static final Logger logger = LoggerFactory.getLogger(BatchIndexer.class);
    private final ImportDataRequestEvent importDataRequest;
    private final S3Driver s3Driver;
    private final IndexingClient openSearchRestClient;
    private final int numberOfFilesPerEvent;
    private IndexingResultRecord<SortableIdentifier> processingResult;

    public BatchIndexer(
            ImportDataRequestEvent importDataRequestEvent,
            S3Client s3Client,
            IndexingClient openSearchRestClient,
            int numberOfFilesPerEvent) {
        this.importDataRequest = importDataRequestEvent;
        this.openSearchRestClient = openSearchRestClient;
        this.s3Driver = new S3Driver(s3Client, importDataRequestEvent.getBucket());
        this.numberOfFilesPerEvent = numberOfFilesPerEvent;
    }

    public IndexingResult<SortableIdentifier> processRequest() {

        ListingResult listFilesResult = fetchNextPageOfFilenames();
        List<IndexDocument> contents =
                fileContents(listFilesResult.getFiles()).collect(Collectors.toList());
        List<SortableIdentifier> failedResults = indexFileContents(contents);
        this.processingResult =
                new IndexingResultRecord<>(
                        failedResults,
                        listFilesResult.getListingStartingPoint(),
                        listFilesResult.isTruncated());

        return this;
    }

    private Stream<IndexDocument> fileContents(List<UnixPath> files) {
        return files.stream().map(s3Driver::getFile).map(IndexDocument::fromJsonString);
    }

    @Override
    public List<SortableIdentifier> failedResults() {
        return this.processingResult.failedResults();
    }

    @Override
    public String nextStartMarker() {
        return processingResult.nextStartMarker();
    }

    @Override
    public boolean truncated() {
        return this.processingResult.truncated();
    }

    private ListingResult fetchNextPageOfFilenames() {
        return s3Driver.listFiles(
                UnixPath.of(importDataRequest.getS3Path()),
                importDataRequest.getStartMarker(),
                numberOfFilesPerEvent);
    }

    private List<SortableIdentifier> indexFileContents(List<IndexDocument> contents) {

        Stream<BulkResponse> result = openSearchRestClient.batchInsert(contents.stream());
        List<SortableIdentifier> failures = collectFailures(result).collect(Collectors.toList());
        failures.forEach(this::logFailure);
        return failures;
    }

    private <T> void logFailure(T failureMessage) {
        logger.warn("Failed to index resource:{}", failureMessage);
    }

    private Stream<SortableIdentifier> collectFailures(Stream<BulkResponse> indexActions) {
        return indexActions
                .filter(BulkResponse::hasFailures)
                .map(BulkResponse::getItems)
                .flatMap(Arrays::stream)
                .filter(BulkItemResponse::isFailed)
                .map(BulkItemResponse::getFailure)
                .map(Failure::getId)
                .map(SortableIdentifier::new);
    }
}
