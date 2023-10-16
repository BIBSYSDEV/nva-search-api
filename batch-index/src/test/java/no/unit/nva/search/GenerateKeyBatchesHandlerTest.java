package no.unit.nva.search;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

class GenerateKeyBatchesHandlerTest {

    public static final String OUTPUT_BUCKET = "outputBucket";
    public static final String INPUT_BUCKET_PATH = "s3://inputBucket/resources";
    public static final String RESOURCES = "resources";
    private S3Driver s3DriverInputBucket;
    private S3Driver s3DriverOutputBucket;
    private FakeS3Client outputClient;
    private FakeS3Client s3ClientInputClient;

    @BeforeEach
    void setUp() {
        s3ClientInputClient = new FakeS3Client();
        s3DriverInputBucket = new S3Driver(s3ClientInputClient, INPUT_BUCKET_PATH);
        outputClient = new FakeS3Client();
        s3DriverOutputBucket = new S3Driver(outputClient, OUTPUT_BUCKET);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 25, 100, 136})
    void shouldReadS3KeysFromPersistedBucketAndWriteToS3BatchBucket(int numberOfItemsInBucket) {
        final var expected = putObjectsInInputBucket(numberOfItemsInBucket);

        var handler = new GenerateKeyBatchesHandler(s3ClientInputClient, outputClient, INPUT_BUCKET_PATH,
                                                    OUTPUT_BUCKET);
        handler.handleRequest(null, new ByteArrayOutputStream(), new FakeContext());

        var actual = getPersistedFileFromOutputBucket();
        var expectedNumberOfFiles = getExpectedNumberOfFiles(numberOfItemsInBucket);

        assertThat(actual.size(), is(equalTo(expectedNumberOfFiles)));
        assertThat(actual.stream().collect(Collectors.joining(System.lineSeparator())), is(equalTo(expected)));
    }

    private static int getExpectedNumberOfFiles(int numberOfItemsInBucket) {
        return (int) Math.ceil(numberOfItemsInBucket < 11 ? 1 : (double) numberOfItemsInBucket / 10);
    }

    private static String getBucketPath(UriWrapper uri) {
        return Path.of(UnixPath.fromString(uri.toString()).getPathElementByIndexFromEnd(1), uri.getLastPathElement())
                   .toString();
    }

    private List<String> getPersistedFileFromOutputBucket() {
        var request = ListObjectsV2Request.builder().bucket(OUTPUT_BUCKET).maxKeys(10_000).build();
        var content = outputClient.listObjectsV2(request).contents();
        return content.stream().map(object -> s3DriverOutputBucket.getFile(UnixPath.of(object.key()))).toList();
    }

    private Object putObjectsInInputBucket(int numberOfItems) {
        return IntStream.range(0, numberOfItems)
                   .mapToObj(item -> randomString())
                   .map(this::insertFileWithKey)
                   .map(UriWrapper::fromUri)
                   .map(GenerateKeyBatchesHandlerTest::getBucketPath)
                   .collect(Collectors.joining(System.lineSeparator()));
    }

    private URI insertFileWithKey(String key) {
        return attempt(
            () -> s3DriverInputBucket.insertFile(UnixPath.of(RESOURCES, key), randomString())).orElseThrow();
    }
}