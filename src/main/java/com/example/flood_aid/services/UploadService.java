package com.example.flood_aid.services;

import com.example.flood_aid.models.Image;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class UploadService {

    @Autowired
    private MinioClient minioClient;

    public String getPresignedURL(String bucketName, String objectName) {
        try {
            int duration = 60 * 60;
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(duration)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public void putObject(String bucketName, String objectName, InputStream fileStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fileStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while uploading object: " + e.getMessage());
        }
    }

    public void deleteImage(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Failed to delete image from MinIO: " + e.getMessage());
        }
    }

    public void deleteImages(String bucketName, List<Image> images) {
        if(images != null){
            for (Image image : images) {
                deleteImage(bucketName, image.getName());
            }
        }
    }

    public byte[] getObject(String bucketName, String objectName) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        ); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while downloading object: " + e.getMessage());
        }
    }

}
