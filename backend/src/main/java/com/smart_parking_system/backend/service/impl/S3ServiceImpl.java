package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.service.IS3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class S3ServiceImpl implements IS3Service {

    @Value("${app.s3.bucketName:}")
    private String bucketName;

    @Value("${app.s3.region:}")
    private String region;

    @Value("${app.s3.presignExpireSeconds:300}")
    private long presignExpireSeconds;

    @Override
    public String presignGetUrl(String key) {
        if (key == null || key.isBlank()) return null;
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("app.s3.bucketName is not configured");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalStateException("app.s3.region is not configured");
        }

        // Uses AWS default credential provider chain:
        // env vars, system props, ~/.aws/credentials, EC2/ECS role, etc.
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignExpireSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}

