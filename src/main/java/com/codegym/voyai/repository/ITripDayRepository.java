package com.codegym.voyai.repository;

import com.codegym.voyai.entity.TripDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ITripDayRepository extends JpaRepository<TripDay, Long> {
    List<TripDay> findByTripIdOrderByDayNumberAsc(Long tripId);

    @Query("SELECT td FROM TripDay td " +
            "LEFT JOIN FETCH td.activities " +
            "WHERE td.trip.id = :tripId " +
            "ORDER BY td.dayNumber ASC")
    List<TripDay> findByTripIdWithActivities(@Param("tripId") Long tripId);
}