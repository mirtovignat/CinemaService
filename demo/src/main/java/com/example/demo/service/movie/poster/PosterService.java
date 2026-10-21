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
        byte[] bytes = download(imageUrl);
        upload(bytes, fileName);
        return getPublicUrl(fileName);
    }

    public byte[] download(String imageUrl) {
        byte[] bytes = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Не удалось скачать постер: " + imageUrl);
        }
        return bytes;
    }

    public void upload(byte[] bytes, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, bytes.length, -1)
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