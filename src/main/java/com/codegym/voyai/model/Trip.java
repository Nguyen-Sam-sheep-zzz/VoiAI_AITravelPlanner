package com.codegym.voyai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "destination_name", nullable = false, length = 300)
    private String destinationName;

    @Column(name = "dest_lat", precision = 10, scale = 6)
    private BigDecimal destLat;

    @Column(name = "dest_lng", precision = 10, scale = 6)
    private BigDecimal destLng;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "num_days", nullable = false)
    private Integer numDays;

    @Column(name = "budget_total", precision = 15, scale = 2)
    private BigDecimal budgetTotal;

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(columnDefinition = "TEXT")
    private String notes;

    // UUID để tạo link chia sẻ public
    @Column(name = "share_token", unique = true, length = 100)
    private String shareToken;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TripDay> tripDays = new LinkedHashSet<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<BudgetEntry> budgetEntries = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<WeatherCache> weatherCaches = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (shareToken == null) {
            shareToken = UUID.randomUUID().toString().replace("-", "");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
