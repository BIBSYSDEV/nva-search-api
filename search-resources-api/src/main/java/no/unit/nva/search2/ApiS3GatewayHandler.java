package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Map;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

public abstract class ApiS3GatewayHandler<I> extends ApiGatewayHandler<I, String> {

    public static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    public static final String BUCKET_NAME = new Environment().readEnv("LARGE_API_RESPONSES_BUCKET");
    private final S3Client s3client;

    private static Logger logger = LoggerFactory.getLogger(ApiS3GatewayHandler.class);
    private final S3Presigner s3presigner;

    public ApiS3GatewayHandler(Class<I> iclass, Environment environment, S3Client s3client, S3Presigner s3Presigner) {
        super(iclass, environment);
        this.s3client = s3client;
        this.s3presigner = s3Presigner;
    }

    @Override
    protected Integer getSuccessStatusCode(I input, String output) {
        return HttpURLConnection.HTTP_MOVED_TEMP;
    }

    @Override
    protected String processInput(I input, RequestInfo requestInfo, Context context) throws BadRequestException {
        var data = processS3Input(input, requestInfo, context);

        logger.info(BUCKET_NAME);
        var request = PutObjectRequest.builder()
                          .bucket(BUCKET_NAME)
                          .contentType("text/csv")
                          .key(context.getAwsRequestId())
                          .build();
        var requestBody = RequestBody.fromString(data);
        this.s3client.putObject(request, requestBody);

        var presignGetRequest = GetObjectPresignRequest.builder()
                                    .signatureDuration(Duration.ofMinutes(10))
                                    .getObjectRequest(
                                        GetObjectRequest.builder()
                                            .bucket(BUCKET_NAME)
                                            .key(context.getAwsRequestId())
                                            .build()
                                    )
                                    .build();


        var response = s3presigner.presignGetObject(presignGetRequest);
        logger.info(response.toString());
        logger.info(String.valueOf(response.url()));

        addAdditionalHeaders(() -> Map.of("Location", response.url().toString()));
        return null;
    }

    abstract String processS3Input(I input, RequestInfo requestInfo, Context context) throws BadRequestException;

    @JacocoGenerated
    public static S3Client defaultS3Client() {
        return S3Client.builder()
                   .region(Region.of(AWS_REGION))
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    @JacocoGenerated
    public static S3Presigner defaultS3Presigner() {
        return S3Presigner.builder()
                   .region(Region.of(AWS_REGION))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .build();
    }
}
