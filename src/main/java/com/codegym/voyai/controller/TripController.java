package com.codegym.voyai.controller;

import com.codegym.voyai.model.Trip;
import com.codegym.voyai.model.dto.TripRequest;
import com.codegym.voyai.repository.ITripRepository;
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
    private final ITripRepository tripRepository;

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

    // Lấy danh sách tất cả trip của user đang đăng nhậpppp
    @GetMapping
    public ResponseEntity<?> getMyTrips(Authentication auth) {
        try {
            return ResponseEntity.ok(tripService.getMyTrips(auth.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
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

    // Bật/tắt public sharing
    @PatchMapping("/{id}/share")
    public ResponseEntity<?> toggleShare(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            Authentication auth) {

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        if (trip.getUser() == null || !trip.getUser().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Bạn không có quyền chia sẻ chuyến đi này"));
        }

        boolean isPublic = body.getOrDefault("isPublic", false);
        trip.setIsPublic(isPublic);

        if (isPublic && (trip.getShareToken() == null || trip.getShareToken().isBlank())) {
            trip.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        tripRepository.save(trip);

        return ResponseEntity.ok(Map.of(
                "isPublic", isPublic,
                "shareToken", trip.getShareToken(), // Trả về token sẽ linh hoạt hơn cho Frontend
                "message", isPublic ? "Đã bật chia sẻ công khai" : " đã tắt chia sẻ"));
    }
}