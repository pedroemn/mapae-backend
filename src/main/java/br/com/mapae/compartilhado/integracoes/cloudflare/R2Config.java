package br.com.mapae.compartilhado.integracoes.cloudflare;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class R2Config {
    @Value("${CLOUDFLARE_ACCOUNT_ID}")
    private String accountId;

    @Value("${CLOUDFLARE_ACCESS_KEY_ID}")
    private String accessKeyId;

    @Value("${CLOUDFLARE_SECRET_ACCESS_KEY}")
    private String secretAccessKey;

    @Value("${CLOUDFLARE_ENDPOINT}")
    private String endpoint;

    private URI getEndpointUri() {
        return URI.create(String.format("https://%s.r2.cloudflarestorage.com", accountId));
    }

    private StaticCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint)) 
            .credentialsProvider(getCredentialsProvider())
            .region(Region.of("auto"))
            .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build())
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(getEndpointUri())
                .credentialsProvider(getCredentialsProvider())
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}