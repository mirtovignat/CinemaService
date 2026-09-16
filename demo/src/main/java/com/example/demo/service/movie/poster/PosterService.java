package com.example.demo.service.movie.poster;

import com.example.demo.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PosterService {

    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;

    public String uploadPosterFromUrl(String imageUrl, String fileName) throws Exception {
        RestClient restClient = RestClient.create();
        byte[] bytes = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Не удалось скачать постер: " + imageUrl);
        }

        try (java.io.ByteArrayInputStream inputStream =
                     new java.io.ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, bytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );
        }
        return getPublicUrl(fileName);
    }

    public String getPublicUrl(String fileName) {
        return minIOConfig.getExternalEndpoint()
                + "/" + minIOConfig.getBucketName()
                + "/" + fileName;
    }
}