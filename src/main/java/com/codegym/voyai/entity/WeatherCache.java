package com.codegym.voyai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "forecast_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    // Mã thời tiết WMO từ Open-Meteo (51=mưa nhỏ, 95=giông...)
    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "precipitation_mm", precision = 6, scale = 2)
    private BigDecimal precipitationMm;

    // true nếu weather_code >= 51 (có mưa) — để query nhanh
    @Column(name = "is_rainy")
    private Boolean isRainy;

    // Thời điểm fetch để kiểm tra cache còn mới không
    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        fetchedAt = LocalDateTime.now();
        isRainy = weatherCode != null && weatherCode >= 51;
    }
}
