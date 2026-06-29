package com.gstcompliance.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${aws.s3.bucket:gst-invoices}")
    private String bucket;

    public FileStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileKey = generateFileKey(file.getOriginalFilename());
            log.info("📤 Uploading file: {} to bucket: {}", fileKey, bucket);

            // Check if bucket exists, create if not
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("✅ Bucket '{}' created", bucket);
            }

            // ✅ REAL UPLOAD TO MINIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("✅ File uploaded to MinIO: {}", fileKey);
            return fileKey;

        } catch (Exception e) {
            log.error("❌ Failed to upload file to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    public byte[] downloadFile(String fileKey) {
        try {
            log.info("📥 Downloading file from MinIO: {}", fileKey);

            // ✅ REAL DOWNLOAD FROM MINIO
            try (var stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build()
            )) {
                byte[] content = stream.readAllBytes();
                log.info("📄 Downloaded {} bytes from MinIO", content.length);

                // ✅ Log first 200 characters for debugging
                String preview = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                preview = preview.length() > 200 ? preview.substring(0, 200) + "..." : preview;
                log.info("📄 File preview: {}", preview);

                return content;
            }

        } catch (Exception e) {
            log.error("❌ Failed to download file from MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("File download failed", e);
        }
    }

    public void deleteFile(String fileKey) {
        try {
            log.info("🗑️ Deleting file from MinIO: {}", fileKey);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Failed to delete file from MinIO: {}", e.getMessage(), e);
        }
    }

    public String generateFileKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "invoices/" + UUID.randomUUID() + extension;
    }

    public boolean fileExists(String fileKey) {
        try {
            minioClient.statObject(
                    io.minio.StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}