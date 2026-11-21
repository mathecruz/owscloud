package dev.matheuscruz;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.flow.providers.HttpClientProvider;
import io.quarkus.runtime.StartupEvent;
import io.serverlessworkflow.impl.WorkflowDefinitionId;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

/**
 * Signs the Quarkus Flow HTTP calls to AWS with Signature Version 4, which the workflow DSL does not support.
 * <p>
 * Only requests carrying the {@value #SERVICE_HEADER} header are signed; its value is the signing name of the
 * service (e.g. {@code dynamodb}) and the header is removed before the request is sent.
 * <p>
 * {@code @Provider} filters are not applied to the plain JAX-RS clients built by Quarkus Flow (only message body
 * readers/writers are global), and the client configuration cannot register providers. The AWS workflows are
 * routed to the named client {@code aws} ({@code quarkus.flow.http.client.workflow.<name>.name=aws}) and the
 * filter is registered on that client at startup.
 */
@Singleton
public class AwsSigV4ClientFilter implements ClientRequestFilter {

    static final String SERVICE_HEADER = "X-Aws-SigV4-Service";

    @Inject
    ObjectMapper mapper;

    /**
     * Workflows ({@code namespace:name:version}) routed to the named HTTP client that signs the requests.
     */
    @ConfigProperty(name = "owscloud.aws.workflows")
    List<String> workflows;

    @ConfigProperty(name = "owscloud.aws.region")
    String region;

    @ConfigProperty(name = "owscloud.aws.access-key-id")
    Optional<String> accessKeyId;

    @ConfigProperty(name = "owscloud.aws.secret-access-key")
    Optional<String> secretAccessKey;

    final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

    volatile AwsCredentialsProvider credentials;

    void registerOnFlowClient(@Observes StartupEvent ev, HttpClientProvider httpClients) {
        // HttpClientProvider has no lookup by client name: resolve the client through the workflow routing
        for (String workflow : workflows) {
            String[] id = workflow.split(":");
            Client client = httpClients.clientFor(new WorkflowDefinitionId(id[0], id[1], id[2]), null);
            if (!client.getConfiguration().isRegistered(this)) {
                client.register(this, ClientRequestFilter.class);
            }
        }
    }

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        String service = ctx.getHeaderString(SERVICE_HEADER);
        if (service == null) {
            return;
        }
        ctx.getHeaders().remove(SERVICE_HEADER);

        // The payload hash is part of the signature: serialize it now and send exactly these bytes
        byte[] body = ctx.hasEntity() ? mapper.writeValueAsBytes(ctx.getEntity()) : new byte[0];
        if (ctx.hasEntity()) {
            ctx.setEntity(body);
        }

        // Sign only host, x-amz-date and x-amz-target, so headers the client adds or changes later
        // (Content-Type charset, User-Agent, Content-Length) cannot invalidate the signature
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                .uri(ctx.getUri())
                .method(SdkHttpMethod.fromValue(ctx.getMethod()));
        String target = ctx.getHeaderString("X-Amz-Target");
        if (target != null) {
            request.putHeader("X-Amz-Target", target);
        }

        SignedRequest signed = signer.sign(r -> r
                .identity(credentialsProvider().resolveCredentials())
                .request(request.build())
                .payload(ContentStreamProvider.fromByteArray(body))
                .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, service)
                .putProperty(AwsV4HttpSigner.REGION_NAME, region));

        signed.request().headers().forEach((name, values) -> {
            if (!"Host".equalsIgnoreCase(name)) {
                ctx.getHeaders().put(name, new ArrayList<>(values));
            }
        });
    }

    AwsCredentialsProvider credentialsProvider() {
        AwsCredentialsProvider provider = credentials;
        if (provider == null) {
            // Static keys (LocalStack in dev/test), otherwise env vars, ~/.aws, IAM role...
            provider = accessKeyId.isPresent() && secretAccessKey.isPresent()
                    ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId.get(), secretAccessKey.get()))
                    : DefaultCredentialsProvider.builder().build();
            credentials = provider;
        }
        return provider;
    }
}
