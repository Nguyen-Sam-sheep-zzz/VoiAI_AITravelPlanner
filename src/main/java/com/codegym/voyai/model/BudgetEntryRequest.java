package com.codegym.voyai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// BudgetEntryRequest.java
@Data
public class BudgetEntryRequest {
    @NotBlank
    private String label;

    @NotNull
    private BigDecimal amount;

    private String category;
    private LocalDate entryDate;
}