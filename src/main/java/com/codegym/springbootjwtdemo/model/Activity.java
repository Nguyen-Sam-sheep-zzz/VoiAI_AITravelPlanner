package com.codegym.springbootjwtdemo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", nullable = false)
    @JsonIgnore
    private TripDay tripDay;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location_name", length = 300)
    private String locationName;

    @Column(name = "location_lat", precision = 10, scale = 6)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 6)
    private BigDecimal locationLng;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost;

    // "food" | "transport" | "attraction" | "accommodation" | "other"
    @Column(length = 50)
    private String category;

    // Phút di chuyển từ activity trước tới đây (từ OSRM)
    @Column(name = "travel_duration_min")
    private Integer travelDurationMin;

    // "driving" | "walking" | "transit"
    @Column(name = "travel_mode", length = 20)
    private String travelMode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}