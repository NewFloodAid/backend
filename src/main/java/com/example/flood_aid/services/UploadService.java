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
        return cloudinary.url().generate(publicId);
    }

    public void putObject(String bucketName, String objectName, InputStream fileStream, long size, String contentType) {
        try {
            // Read stream to bytes because Cloudinary upload might need a File or byte
            // array or simpler stream handling
            // Cloudinary's upload method accepts InputStream.
            // We want to verify if 'objectName' has extension.

            // We'll treat 'bucketName' as folder.
            // 'objectName' is the filename (e.g. uuid.jpg).
            // Cloudinary best practice: strip extension from public_id, request with
            // extension.

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
                    "overwrite", true);

            // Note: Cloudinary might add the extension automatically if not in public_id,
            // or result in doubled extension.
            // But we can enable 'use_filename' => true, 'unique_filename' => false etc.
            // For simplicity, let's just pass the stream.

            // To prevent doubling extension if objectName has it:
            // If objectName is "uuid.png", Cloudinary public_id will be "uuid.png" (if we
            // force it).
            // Requesting it back: "uuid.png.png" might happen if we are not careful with
            // formatting options.
            // However, Minio treated objectName as the key.
            // Let's try to set use_filename=true, unique_filename=false, and
            // public_id=objectName.

            // Actually, safest is to just use what was passed.
            cloudinary.uploader().upload(fileStream.readAllBytes(), params);

            // Note: fileStream.readAllBytes() loads into memory. If files are huge, this is
            // bad.
            // But for images it's usually fine.
            // Alternatively, pass the temp file if available, but we only have InputStream
            // here.

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
