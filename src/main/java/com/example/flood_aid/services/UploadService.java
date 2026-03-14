package com.example.flood_aid.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.flood_aid.models.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

import java.io.InputStream;
import java.net.URL;
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
            String publicId = objectName;
            int lastDotIndex = objectName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                publicId = objectName.substring(0, lastDotIndex);
            }

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
        try {
            String publicId = bucketName + "/" + objectName;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
        }
    }

    public void deleteImages(String bucketName, List<Image> images) {
        if (images != null) {
            for (Image image : images) {
                deleteImage(bucketName, image.getName());
            }
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

}
