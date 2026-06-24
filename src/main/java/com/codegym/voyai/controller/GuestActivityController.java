package com.codegym.voyai.controller;

import com.codegym.voyai.model.Activity;
import com.codegym.voyai.model.Trip;
import com.codegym.voyai.model.dto.ReorderRequest;
import com.codegym.voyai.repository.IActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/guest/activities")
@RequiredArgsConstructor
public class GuestActivityController {

    private final IActivityRepository activityRepository;

    @PutMapping("/reorder")
    @Transactional
    public ResponseEntity<?> reorderForGuest(
            @RequestBody ReorderRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Thieu Session ID"));
        }

        if (request.getActivities() == null || request.getActivities().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Danh sach activities khong duoc rong"));
        }

        List<Activity> activitiesToUpdate = new ArrayList<>();

        for (int i = 0; i < request.getActivities().size(); i++) {
            ReorderRequest.ActivityOrderUpdate update = request.getActivities().get(i);
            if (update.getId() == null) continue;

            Activity activity = activityRepository.findById(update.getId()).orElse(null);

            if (activity == null) continue;

            // Xac nhan ownership bang sessionId (khong can JWT)
            Trip trip = activity.getTripDay().getTrip();
            if (sessionId.equals(trip.getSessionId())) {
                activity.setSortOrder(i);
                if (update.getStartTime() != null && !update.getStartTime().isBlank()) {
                    String timeStr = update.getStartTime();
                    if (timeStr.length() == 5) {
                        timeStr += ":00";
                    }
                    try {
                        activity.setStartTime(java.time.LocalTime.parse(timeStr));
                    } catch (Exception e) {
                        // ignore
                    }
                }
                activitiesToUpdate.add(activity);
            } else {
                log.warn("Guest reorder denied: sessionId={} khong khop voi trip.sessionId={}",
                        sessionId, trip.getSessionId());
            }
        }

        if (activitiesToUpdate.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Khong co activity nao duoc cap quyen. Kiem tra Session ID."));
        }

        activityRepository.saveAll(activitiesToUpdate);
        log.info("Guest reorder: {} activities updated for session {}", activitiesToUpdate.size(), sessionId);

        return ResponseEntity.ok(Map.of(
                "message", "Da cap nhat thu tu va thoi gian",
                "updated", activitiesToUpdate.size()
        ));
    }
}
