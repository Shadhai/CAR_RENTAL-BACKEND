package com.example.demo.controllers;

import com.example.demo.models.Booking;
import com.example.demo.models.Car;
import com.example.demo.models.User;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.CarRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Create new booking with validation
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestParam Long carId,
                                           @RequestParam String startDate,
                                           @RequestParam String endDate,
                                           Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));

        if (!car.isAvailable()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Car is not available"));
        }

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        LocalDate today = LocalDate.now();

        // ✅ Date validations
        if (start.isBefore(today)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start date cannot be in the past"));
        }

        if (end.isBefore(start) || end.isEqual(start)) {
            return ResponseEntity.badRequest().body(Map.of("error", "End date must be after start date"));
        }

        Booking booking = new Booking(start, end, user, car);
        bookingRepository.save(booking);

        // Mark car unavailable
        car.setAvailable(false);
        carRepository.save(car);

        return ResponseEntity.ok(Map.of(
                "message", "Booking created successfully",
                "data", booking
        ));
    }

    // ✅ Get current user's bookings
    @GetMapping("/me")
    public ResponseEntity<?> getMyBookings(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingRepository.findByUser(user);
        return ResponseEntity.ok(Map.of(
                "message", "User bookings fetched",
                "data", bookings
        ));
    }

    // ✅ Cancel a booking (user)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findById(id).map(booking -> {
            if (!booking.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "You cannot cancel this booking"));
            }

            // Make car available again
            Car car = booking.getCar();
            car.setAvailable(true);
            carRepository.save(car);

            bookingRepository.delete(booking);
            return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "Booking not found")));
    }

    // ✅ Admin: Get all bookings
    @GetMapping("/all")
    public ResponseEntity<?> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return ResponseEntity.ok(Map.of(
                "message", "All bookings fetched (admin)",
                "data", bookings
        ));
    }
}
