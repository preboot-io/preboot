package io.preboot.files.s3;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.api.FileStorageService;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "preboot.files.s3.bucket-name")
@EnableConfigurationProperties(S3FileStorageProperties.class)
public class S3FileStorageAutoConfiguration {

    @Bean
    public S3Client s3Client(S3FileStorageProperties properties) {
        log.info("Configuring S3 client for bucket: {}, region: {}", properties.bucketName(), properties.region());

        var clientBuilder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(createCredentialsProvider(properties));

        if (properties.endpointUrl() != null && !properties.endpointUrl().trim().isEmpty()) {
            log.info("Using custom S3 endpoint: {}", properties.endpointUrl());
            clientBuilder.endpointOverride(URI.create(properties.endpointUrl()));
        }

        if (properties.pathStyleAccessEnabled()) {
            log.info("Enabling path-style access for S3-compatible services");
            clientBuilder.forcePathStyle(true);
        }

        return clientBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService s3FileStorageService(
            S3Client s3Client, EventPublisher eventPublisher, S3FileStorageProperties properties) {
        return new S3FileStorageService(s3Client, eventPublisher, properties);
    }

    private AwsCredentialsProvider createCredentialsProvider(S3FileStorageProperties properties) {
        if (properties.accessKeyId() != null
                && !properties.accessKeyId().trim().isEmpty()
                && properties.secretAccessKey() != null
                && !properties.secretAccessKey().trim().isEmpty()) {

            log.info("Using explicit AWS credentials for S3 access");
            AwsBasicCredentials credentials =
                    AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey());
            return StaticCredentialsProvider.create(credentials);
        } else {
            log.info("Using AWS default credential provider chain for S3 access");
            return DefaultCredentialsProvider.create();
        }
    }
}
