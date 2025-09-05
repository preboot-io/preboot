package io.preboot.files.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "preboot.files.s3")
public record S3FileStorageProperties(
        @NotBlank String bucketName,
        String region,
        String endpointUrl,
        String accessKeyId,
        String secretAccessKey,
        @Positive long maxFileSize,
        boolean pathStyleAccessEnabled) {

    public S3FileStorageProperties {
        if (region == null || region.trim().isEmpty()) {
            region = "us-east-1";
        }
        if (maxFileSize <= 0) {
            maxFileSize = 52428800L; // 50MB default
        }
    }
}
