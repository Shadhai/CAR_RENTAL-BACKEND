package com.example.demo.controllers;

import com.example.demo.models.Car;
import com.example.demo.payload.MessageResponse;
import com.example.demo.repositories.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admin/cars")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCarController {

    @Autowired
    private CarRepository carRepository;

    private final String uploadDir = "src/main/resources/static/images/cars/";

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addCar(@RequestParam("make") String make,
                                    @RequestParam("model") String model,
                                    @RequestParam("type") String type,
                                    @RequestParam("pricePerDay") Double pricePerDay,
                                    @RequestParam(value = "image", required = false) MultipartFile image) {
        Car car = new Car();
        car.setMake(make);
        car.setModel(model);
        car.setType(type);
        car.setPricePerDay(pricePerDay);
        car.setAvailable(true);

        if (image != null && !image.isEmpty()) {
            try {
                String filename = StringUtils.cleanPath(image.getOriginalFilename());
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(filename);
                image.transferTo(filePath.toFile());
                car.setImageUrl("/images/cars/" + filename);
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(new MessageResponse("Failed to upload image"));
            }
        }

        carRepository.save(car);
        return ResponseEntity.ok(car);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCar(@PathVariable Long id,
                                       @RequestParam("make") String make,
                                       @RequestParam("model") String model,
                                       @RequestParam("type") String type,
                                       @RequestParam("pricePerDay") Double pricePerDay,
                                       @RequestParam(value = "image", required = false) MultipartFile image) {
        return carRepository.findById(id).map(car -> {
            car.setMake(make);
            car.setModel(model);
            car.setType(type);
            car.setPricePerDay(pricePerDay);

            if (image != null && !image.isEmpty()) {
                try {
                    String filename = StringUtils.cleanPath(image.getOriginalFilename());
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    Path filePath = uploadPath.resolve(filename);
                    image.transferTo(filePath.toFile());
                    car.setImageUrl("/images/cars/" + filename);
                } catch (IOException e) {
                    return ResponseEntity.badRequest().body(new MessageResponse("Failed to upload image"));
                }
            }

            carRepository.save(car);
            return ResponseEntity.ok(car);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCar(@PathVariable Long id) {
        return carRepository.findById(id).map(car -> {
            carRepository.delete(car);
            return ResponseEntity.ok(new MessageResponse("Car deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }
}