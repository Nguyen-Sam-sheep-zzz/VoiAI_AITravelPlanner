package com.codegym.voyai.controller;

import com.codegym.voyai.model.Trip;
import com.codegym.voyai.model.dto.TripRequest;
import com.codegym.voyai.service.NominatimService;
import com.codegym.voyai.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final NominatimService nominatimService;

    // Tạo trip — AI sinh lịch trình
    @PostMapping
    public ResponseEntity<?> createTrip(
            @Valid @RequestBody TripRequest request,
            Authentication auth) {
        try {
            Trip trip = tripService.createTrip(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(trip);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Lấy danh sách trip của user
    @GetMapping
    public ResponseEntity<?> getMyTrips(Authentication auth) {
        return ResponseEntity.ok(tripService.getMyTrips(auth.getName()));
    }

    // Lấy 1 trip theo id
    @GetMapping("/{id}")
    public ResponseEntity<?> getTrip(
            @PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(tripService.getTripById(id, auth.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Xóa trip
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrip(
            @PathVariable Long id, Authentication auth) {
        tripService.deleteTrip(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Đã xóa chuyến đi"));
    }

    // Tìm kiếm địa điểm — gọi Nominatim
    @GetMapping("/search-place")
    public ResponseEntity<?> searchPlace(@RequestParam String q) {
        return ResponseEntity.ok(nominatimService.search(q));
    }
}