package com.codegym.voyai.repository;

import com.codegym.voyai.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByTripDayIdOrderBySortOrderAsc(Long tripDayId);
}