package com.example.flood_aid.controllers;


import com.example.flood_aid.services.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    @Autowired
    private ImageService imageService;

    @DeleteMapping("/{id}")
    public void deleteImageById(@PathVariable Long id) {
        imageService.deleteImageById(id);
    }
}
