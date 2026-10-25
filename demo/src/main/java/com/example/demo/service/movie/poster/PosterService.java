package com.example.demo.service.movie.poster;

import com.example.demo.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class PosterService {

    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;
    private final RestClient restClient;

    public PosterService(MinioClient minioClient,
                         MinIOConfig minIOConfig,
                         @Qualifier("fileRestClient")
                         RestClient restClient) {
        this.minioClient = minioClient;
        this.minIOConfig = minIOConfig;
        this.restClient = restClient;
    }

    public String uploadPosterFromUrl(String imageUrl, String fileName) {
        byte[] posterBytes = download(imageUrl);
        upload(posterBytes, fileName);
        return getPublicUrl(fileName);
    }

    public byte[] download(String imageUrl) {
        byte[] posterBytes = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);

        if (posterBytes == null || posterBytes.length == 0) {
            throw new IllegalStateException("Не удалось скачать постер: " + imageUrl);
        }
        return posterBytes;
    }

    public void upload(byte[] posterBytes, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(posterBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, posterBytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException("Не удалось загрузить постер в MinIO: " + fileName, exception);
        }
    }

    public String getPublicUrl(String fileName) {
        String endpoint = minIOConfig.getExternalEndpoint().replaceAll("/+$", "");
        return endpoint + "/" + minIOConfig.getBucketName() + "/" + fileName;
    }
}