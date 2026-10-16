package com.example.demo.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Getter
@Slf4j
@Configuration
public class MinIOConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.external-endpoint:${minio.endpoint}}")
    private String externalEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("classpath:minio/public-read-policy.json")
    private Resource publicReadPolicy;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            initBucket(minioClient);
        } catch (Exception exception) {
            log.warn("Не удалось инициализировать бакет MinIO '{}'. " +
                            "Загрузка постеров будет недоступна. Причина: {}",
                    bucketName, exception.getMessage());
        }
        return minioClient;
    }

    private void initBucket(MinioClient minioClient) throws Exception {
        boolean found = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!found) {
            minioClient.makeBucket(
                    io.minio.MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }
        minioClient.setBucketPolicy(
                io.minio.SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(buildPolicy(bucketName))
                        .build()
        );
    }

    private String buildPolicy(String bucketName) throws IOException {
        String template;
        try (var inputStream = publicReadPolicy.getInputStream()) {
            template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        return template.replace("__BUCKET__", bucketName);
    }
}