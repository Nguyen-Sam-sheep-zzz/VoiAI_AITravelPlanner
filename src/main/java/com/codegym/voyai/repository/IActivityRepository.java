package com.codegym.voyai.repository;

import com.codegym.voyai.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IActivityRepository extends JpaRepository<Activity, Long> {
    @Modifying
    @Query("UPDATE Activity a SET a.sortOrder = :order WHERE a.id = :id")
    void updateSortOrder(@Param("id") Long id, @Param("order") Integer order);

    List<Activity> findByTripDayIdOrderBySortOrderAsc(Long tripDayId);
}