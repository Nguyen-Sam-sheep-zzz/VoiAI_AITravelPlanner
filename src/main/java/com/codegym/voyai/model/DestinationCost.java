package com.codegym.voyai.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
// Sửa lại annotation này
@Table(name = "destination_costs",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"destination_name", "country_code", "category"})) // ✅ thêm country_code
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destination_name", nullable = false, length = 200)
    private String destinationName;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    // "meal_budget" | "meal_mid" | "meal_fine"
    // "hotel_budget" | "hotel_mid"
    // "transport_day" | "attraction_avg" | "coffee"
    @Column(nullable = false, length = 50)
    private String category;

    // Luôn lưu USD — frontend convert sang VND theo tỉ giá
    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal costUsd;

    @Column(name = "cost_local", precision = 10, scale = 2)
    private BigDecimal costLocal;

    @Column(name = "local_currency", length = 10)
    private String localCurrency;

    // "numbeo" | "budgetyourtrip" | "user_contributed"
    @Column(length = 50)
    @Builder.Default
    private String source = "numbeo";

    // Số người dùng đã xác nhận con số này sau chuyến đi thực tế
    @Column(name = "contribution_count")
    @Builder.Default
    private Integer contributionCount = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        lastUpdated = LocalDateTime.now();
    }
}