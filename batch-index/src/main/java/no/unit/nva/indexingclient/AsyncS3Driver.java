package no.unit.nva.indexingclient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import no.unit.nva.indexingclient.models.IndexDocument;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;

public class AsyncS3Driver {

  private static final int MAX_CONCURRENT_S3_READS = 200;
  public static final String DECOMPRESSION_FAILURE_MESSAGE = "Failed to decompress gzipped content";
  private final S3TransferManager transferManager;

  public AsyncS3Driver(S3TransferManager transferManager) {
    this.transferManager = transferManager;
  }

  @JacocoGenerated
  public static AsyncS3Driver defaultDriver() {
    var transferManager =
        S3TransferManager.builder()
            .s3Client(
                S3AsyncClient.builder()
                    .httpClientBuilder(
                        NettyNioAsyncHttpClient.builder()
                            .maxConcurrency(MAX_CONCURRENT_S3_READS)
                            .connectionTimeout(Duration.ofSeconds(5))
                            .readTimeout(Duration.ofSeconds(60)))
                    .build())
            .build();
    return new AsyncS3Driver(transferManager);
  }

  public CompletableFuture<IndexDocument> fetchAsync(String bucket, String key) {
    var download =
        transferManager.download(
            DownloadRequest.builder()
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .responseTransformer(AsyncResponseTransformer.toBytes())
                .build());

    return download
        .completionFuture()
        .thenApply(CompletedDownload::result)
        .thenApply(response -> decompressGzipContent(response.asByteArray()))
        .thenApply(IndexDocument::fromJsonString);
  }

  private String decompressGzipContent(byte[] gzippedBytes) {
    try (var gzipStream = new GZIPInputStream(new ByteArrayInputStream(gzippedBytes));
        var reader =
            new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException e) {
      throw new UncheckedIOException(DECOMPRESSION_FAILURE_MESSAGE, e);
    }
  }
}
