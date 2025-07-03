package com.example.flood_aid.services;

import com.example.flood_aid.models.Image;
import com.example.flood_aid.repositories.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UploadService uploadService;

    public void deleteImageById(Long id) {
        System.out.println("Attempting to delete image with ID: " + id);
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image with ID " + id + " not found."));
        System.out.println("Found image: " + image.getName());

        try {
            uploadService.deleteImage("images", image.getName());
            System.out.println("Image deleted from storage: " + image.getName());
        } catch (Exception e) {
            System.err.println("Failed to delete image from storage: " + e.getMessage());
            throw new RuntimeException("Failed to delete image from storage: " + e.getMessage(), e);
        }

        try {
            imageRepository.deleteById(id);
            System.out.println("Image deleted from database with ID: " + id);
        } catch (Exception e) {
            System.err.println("Failed to delete image from database: " + e.getMessage());
            throw new RuntimeException("Failed to delete image from database: " + e.getMessage(), e);
        }
    }


}
