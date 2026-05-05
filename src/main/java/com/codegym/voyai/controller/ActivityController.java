package com.codegym.voyai.controller;

import com.codegym.voyai.model.Activity;
import com.codegym.voyai.model.Trip;
import com.codegym.voyai.model.TripDay;
import com.codegym.voyai.model.dto.ActivityUpdateRequest;
import com.codegym.voyai.model.dto.ReorderRequest;
import com.codegym.voyai.repository.IActivityRepository;
import com.codegym.voyai.repository.ITripDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final IActivityRepository activityRepository;
    private final ITripDayRepository tripDayRepository;

    // Sửa nội dung 1 activity
    @PutMapping("/{id}")
    public ResponseEntity<?> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityUpdateRequest request,
            Authentication auth) {

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity không tồn tại"));

        // Kiểm tra ownership qua tripDay → trip → user
        Trip trip = activity.getTripDay().getTrip();
        if (trip.getUser() == null ||
                !trip.getUser().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Không có quyền"));
        }

        if (request.getTitle() != null)
            activity.setTitle(request.getTitle());
        if (request.getDescription() != null)
            activity.setDescription(request.getDescription());
        if (request.getStartTime() != null)
            activity.setStartTime(LocalTime.parse(request.getStartTime()));
        if (request.getEstimatedCost() != null)
            activity.setEstimatedCost(request.getEstimatedCost());

        return ResponseEntity.ok(activityRepository.save(activity));
    }

    // Kéo thả — cập nhật sortOrder hàng loạt
    @PutMapping("/reorder")
    @Transactional
    public ResponseEntity<?> reorder(
            @RequestBody ReorderRequest request,
            Authentication auth) {

        List<Activity> activitiesToUpdate = new ArrayList<>();

        for (int i = 0; i < request.getActivityIds().size(); i++) {
            Long actId = request.getActivityIds().get(i);
            Activity activity = activityRepository.findById(actId).orElse(null);

            if (activity != null) {
                // Kiểm tra quyền (Nếu muốn hỗ trợ Guest, hãy check sessionId ở đây)
                Trip trip = activity.getTripDay().getTrip();
                if (trip.getUser() != null && trip.getUser().getEmail().equals(auth.getName())) {
                    activity.setSortOrder(i);
                    activitiesToUpdate.add(activity);
                }
            }
        }

        activityRepository.saveAll(activitiesToUpdate); // Lưu hàng loạt 1 lần
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật thứ tự"));
    }

    // Xóa 1 activity
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivity(
            @PathVariable Long id,
            Authentication auth) {

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tồn tại"));

        Trip trip = activity.getTripDay().getTrip();
        if (trip.getUser() == null ||
                !trip.getUser().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Không có quyền"));
        }

        activityRepository.delete(activity);
        return ResponseEntity.ok(Map.of("message", "Đã xóa"));
    }

    // Thêm activity mới vào 1 ngày
    @PostMapping("/trip-day/{tripDayId}")
    public ResponseEntity<?> addActivity(
            @PathVariable Long tripDayId,
            @RequestBody ActivityUpdateRequest request,
            Authentication auth) {

        TripDay tripDay = tripDayRepository.findById(tripDayId)
                .orElseThrow(() -> new RuntimeException("Ngày không tồn tại"));

        Trip trip = tripDay.getTrip();
        if (trip.getUser() == null ||
                !trip.getUser().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Không có quyền"));
        }

        // SortOrder = cuối danh sách
        int maxOrder = tripDay.getActivities().size();

        Activity activity = Activity.builder()
                .tripDay(tripDay)
                .sortOrder(maxOrder)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime() != null
                        ? LocalTime.parse(request.getStartTime()) : null)
                .estimatedCost(request.getEstimatedCost())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityRepository.save(activity));
    }
}