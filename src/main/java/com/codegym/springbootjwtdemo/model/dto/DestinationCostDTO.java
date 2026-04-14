package com.codegym.springbootjwtdemo.model.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DestinationCostDTO {
    private String destinationName;
    private String category;
    private BigDecimal costUsd;
    private BigDecimal costLocal;
    private String localCurrency;
    private Integer contributionCount;
    private String source;
}