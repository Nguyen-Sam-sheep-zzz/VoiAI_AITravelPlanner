package com.codegym.voyai.controller;

import com.codegym.voyai.entity.Activity;
import com.codegym.voyai.entity.Trip;
import com.codegym.voyai.entity.TripDay;
import com.codegym.voyai.dto.request.ActivityUpdateRequest;
import com.codegym.voyai.dto.request.ReorderRequest;
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

    // Kéo thả — cập nhật sortOrder và startTime hàng loạt
    @PutMapping("/reorder")
    @Transactional
    public ResponseEntity<?> reorder(
            @RequestBody ReorderRequest request,
            Authentication auth) {

        List<Activity> activitiesToUpdate = new ArrayList<>();

        if (request.getActivities() != null) {
            for (int i = 0; i < request.getActivities().size(); i++) {
                ReorderRequest.ActivityOrderUpdate update = request.getActivities().get(i);
                if (update.getId() == null) continue;

                Activity activity = activityRepository.findById(update.getId()).orElse(null);

                if (activity != null) {
                    Trip trip = activity.getTripDay().getTrip();
                    if (trip.getUser() != null && trip.getUser().getEmail().equals(auth.getName())) {
                        activity.setSortOrder(i);
                        if (update.getStartTime() != null && !update.getStartTime().isBlank()) {
                            String timeStr = update.getStartTime();
                            if (timeStr.length() == 5) {
                                timeStr += ":00";
                            }
                            try {
                                activity.setStartTime(LocalTime.parse(timeStr));
                            } catch (Exception e) {
                                // Bỏ qua nếu thời gian lỗi định dạng
                            }
                        }
                        activitiesToUpdate.add(activity);
                    }
                }
            }
        }

        activityRepository.saveAll(activitiesToUpdate); // Lưu hàng loạt 1 lần
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật thứ tự và thời gian"));
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