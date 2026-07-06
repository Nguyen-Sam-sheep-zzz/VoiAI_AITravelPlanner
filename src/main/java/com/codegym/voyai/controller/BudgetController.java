package com.codegym.voyai.controller;

import com.codegym.voyai.entity.BudgetEntry;
import com.codegym.voyai.entity.BudgetEntryRequest;
import com.codegym.voyai.entity.Trip;
import com.codegym.voyai.repository.IBudgetEntryRepository;
import com.codegym.voyai.repository.ITripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips/{tripId}/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final IBudgetEntryRepository budgetRepository;
    private final ITripRepository tripRepository;

    // Lấy toàn bộ budget + tổng kết
    @GetMapping
    public ResponseEntity<?> getBudget(
            @PathVariable Long tripId,
            Authentication auth) {

        Trip trip = getAndVerifyTrip(tripId, auth.getName());
        if (trip == null) return ResponseEntity.status(403)
                .body(Map.of("message", "Không có quyền"));

        List<BudgetEntry> entries =
                budgetRepository.findByTripIdOrderByEntryDateAsc(tripId);

        BigDecimal actualTotal =
                budgetRepository.sumActualByTripId(tripId);
        BigDecimal estimatedTotal =
                budgetRepository.sumEstimatedByTripId(tripId);

        // Chart data — group by category
        List<Object[]> chartData =
                budgetRepository.sumByCategory(tripId, true);

        Map<String, Object> chartMap = new LinkedHashMap<>();
        for (Object[] row : chartData) {
            chartMap.put((String) row[0], row[1]);
        }

        return ResponseEntity.ok(Map.of(
                "entries", entries,
                "actualTotal", actualTotal != null ? actualTotal : BigDecimal.ZERO,
                "estimatedTotal", estimatedTotal != null ? estimatedTotal : BigDecimal.ZERO,
                "budgetTotal", trip.getBudgetTotal(),
                "remaining", trip.getBudgetTotal() != null && actualTotal != null
                        ? trip.getBudgetTotal().subtract(actualTotal)
                        : trip.getBudgetTotal(),
                "chartByCategory", chartMap
        ));
    }

    // Thêm chi tiêu thực tế
    @PostMapping
    public ResponseEntity<?> addEntry(
            @PathVariable Long tripId,
            @RequestBody BudgetEntryRequest request,
            Authentication auth) {

        Trip trip = getAndVerifyTrip(tripId, auth.getName());
        if (trip == null) return ResponseEntity.status(403)
                .body(Map.of("message", "Không có quyền"));

        BudgetEntry entry = BudgetEntry.builder()
                .trip(trip)
                .label(request.getLabel())
                .amount(request.getAmount())
                .category(request.getCategory())
                .isActual(true) // người dùng nhập = thực tế
                .entryDate(request.getEntryDate() != null
                        ? request.getEntryDate() : LocalDate.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetRepository.save(entry));
    }

    // Xóa 1 entry
    @DeleteMapping("/{entryId}")
    public ResponseEntity<?> deleteEntry(
            @PathVariable Long tripId,
            @PathVariable Long entryId,
            Authentication auth) {

        getAndVerifyTrip(tripId, auth.getName());
        budgetRepository.deleteById(entryId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa"));
    }

    private Trip getAndVerifyTrip(Long tripId, String email) {
        return tripRepository.findById(tripId)
                .filter(t -> t.getUser() != null &&
                        t.getUser().getEmail().equals(email))
                .orElse(null);
    }
}
