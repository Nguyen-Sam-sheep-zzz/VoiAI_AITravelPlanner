package com.codegym.voyai.controller;

import com.codegym.voyai.entity.Trip;
import com.codegym.voyai.dto.request.TripRequest;
import com.codegym.voyai.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/guest/trips")
@RequiredArgsConstructor
public class GuestTripController {

    private final TripService tripService;

    // Tạo trip không cần đăng nhập
    // Frontend tự tạo sessionId và gửi lên qua header
    @PostMapping
    public ResponseEntity<?> createGuestTrip(
            @Valid @RequestBody TripRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        // Nếu frontend không gửi sessionId → tự tạo mới
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        try {
            Trip trip = tripService.createGuestTrip(request, sessionId);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("X-Session-Id", sessionId) // ✅ trả về để frontend lưu
                    .body(trip);

        } catch (Exception e) {
            log.error("Guest trip creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Lấy danh sách trip của guest
    @GetMapping
    public ResponseEntity<List<Trip>> getGuestTrips(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(tripService.getGuestTrips(sessionId));
    }

    // Xem chi tiết 1 trip của guest
    @GetMapping("/{id}")
    public ResponseEntity<?> getGuestTrip(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Thiếu Session ID"));
        }

        try {
            Trip trip = tripService.getGuestTripById(id, sessionId);
            return ResponseEntity.ok(trip);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
