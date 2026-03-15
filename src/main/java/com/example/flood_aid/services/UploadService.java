package com.example.flood_aid.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.flood_aid.models.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UploadService {

    @Autowired
    private Cloudinary cloudinary;

    public String getPresignedURL(String bucketName, String objectName) {
        // Cloudinary URLs are public by default unless restricted.
        // Assuming we want a standard URL.
        // If we treat bucketName as folder:
        String publicId = bucketName + "/" + objectName;
        // Sanitize '%' to prevent URLDecoder: Incomplete trailing escape (%) pattern
        // exception
        publicId = publicId.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return cloudinary.url().generate(publicId);
    }

    public void putObject(String bucketName, String objectName, InputStream fileStream, long size, String contentType) {
        try {
            String publicId = stripFileExtension(objectName);

            @SuppressWarnings("unchecked")
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", bucketName,
                    "public_id", publicId,
                    "resource_type", "auto",
                    "overwrite", true,
                    "use_filename", false);

            // Cloudinary Java SDK is most stable with byte[] uploads for multipart input.
            cloudinary.uploader().upload(fileStream.readAllBytes(), params);

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while uploading object to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteImage(String bucketName, String objectName) {
        String publicId = buildPublicId(bucketName, objectName);
        try {
            Map<?, ?> destroyResponse = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            Object resultValue = destroyResponse.get("result");
            String result = resultValue == null ? "" : resultValue.toString();
            if (!"ok".equalsIgnoreCase(result) && !"not found".equalsIgnoreCase(result)) {
                throw new RuntimeException("Unexpected Cloudinary delete result for " + publicId + ": " + destroyResponse);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image from Cloudinary for " + publicId + ": " + e.getMessage(), e);
        }
    }

    public void deleteImages(String bucketName, List<Image> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        List<String> failedDeletes = new ArrayList<>();
        for (Image image : images) {
            if (image == null || image.getName() == null || image.getName().isBlank()) {
                continue;
            }
            try {
                deleteImage(bucketName, image.getName());
            } catch (RuntimeException e) {
                failedDeletes.add(image.getName() + " (" + e.getMessage() + ")");
            }
        }
        if (!failedDeletes.isEmpty()) {
            throw new RuntimeException("Cloudinary cleanup incomplete: " + String.join(", ", failedDeletes));
        }
    }

    public byte[] getObject(String bucketName, String objectName) {
        try {
            String urlString = getPresignedURL(bucketName, objectName);
            URL url = new URL(urlString);
            try (InputStream is = url.openStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] data = new byte[8192];
                int nRead;
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                return buffer.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while downloading object from Cloudinary: " + e.getMessage());
        }
    }

    private String buildPublicId(String bucketName, String objectName) {
        return bucketName + "/" + stripFileExtension(objectName);
    }

    private String stripFileExtension(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name is required");
        }
        int lastDotIndex = objectName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return objectName.substring(0, lastDotIndex);
        }
        return objectName;
    }

}
