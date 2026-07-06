package com.codegym.voyai.repository;

import com.codegym.voyai.entity.BudgetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface IBudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {

    List<BudgetEntry> findByTripIdOrderByEntryDateAsc(Long tripId);

    // Tổng chi tiêu thực tế
    @Query("SELECT SUM(b.amount) FROM BudgetEntry b " +
            "WHERE b.trip.id = :tripId AND b.isActual = true")
    BigDecimal sumActualByTripId(@Param("tripId") Long tripId);

    // Tổng AI ước tính
    @Query("SELECT SUM(b.amount) FROM BudgetEntry b " +
            "WHERE b.trip.id = :tripId AND b.isActual = false")
    BigDecimal sumEstimatedByTripId(@Param("tripId") Long tripId);

    // Group by category cho Doughnut Chart
    @Query("SELECT b.category, SUM(b.amount) FROM BudgetEntry b " +
            "WHERE b.trip.id = :tripId AND b.isActual = :isActual " +
            "GROUP BY b.category")
    List<Object[]> sumByCategory(@Param("tripId") Long tripId,
                                 @Param("isActual") boolean isActual);
}
