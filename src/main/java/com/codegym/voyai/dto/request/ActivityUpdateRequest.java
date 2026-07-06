package com.codegym.voyai.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivityUpdateRequest {
    private String title;
    private String description;
    private String startTime;       // "08:00"
    private BigDecimal estimatedCost;
    private Double lat;
    private Double lng;
}
