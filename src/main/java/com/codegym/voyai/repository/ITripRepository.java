package com.codegym.voyai.repository;

import com.codegym.voyai.model.Trip;
import com.codegym.voyai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ITripRepository extends JpaRepository<Trip, Long> {
//    List<Trip> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Cách viết sạch hơn
    List<Trip> findByUserOrderByCreatedAtDesc(User user);

    Optional<Trip> findByShareToken(String shareToken);

    @Query("SELECT t FROM Trip t " +
            "LEFT JOIN FETCH t.tripDays td " +
            "LEFT JOIN FETCH td.activities " +
            "WHERE t.id = :id")
    Optional<Trip> findByIdWithDays(@Param("id") Long id);

    List<Trip> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Modifying
    @Query("UPDATE Trip t SET t.user = :user, t.sessionId = null " +
            "WHERE t.sessionId = :sessionId AND t.user IS NULL")
    int claimTripsBySession(@Param("sessionId") String sessionId,
                            @Param("user") User user);
}