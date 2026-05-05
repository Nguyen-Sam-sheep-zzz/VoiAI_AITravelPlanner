package com.codegym.voyai.controller;

import com.codegym.voyai.model.Trip;
import com.codegym.voyai.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/trips")
@RequiredArgsConstructor
public class ShareController {

    private final TripService tripService;

    public ResponseEntity<?> getSharedTrip(@PathVariable String token) {
        try {

            Trip trip = tripService.getPublicTrip(token);

            if (trip.getUser() != null) {
                trip.getUser().setPasswordHash(null);
                trip.getUser().setRoles(null);
            }

            return ResponseEntity.ok(trip);
        } catch (RuntimeException e) {
            // Trả về lỗi 404 nếu không tìm thấy hoặc trip chưa public
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Chuyến đi không tồn tại hoặc đã bị tắt chia sẻ"));
        }
    }
}