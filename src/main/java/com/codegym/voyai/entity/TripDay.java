package com.codegym.voyai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "trip_days")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "trip_date")
    private LocalDate tripDate;

    @Column(name = "day_note", columnDefinition = "TEXT")
    private String dayNote;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, startTime ASC")
    @EqualsAndHashCode.Exclude
    private Set<Activity> activities = new LinkedHashSet<>();
}
