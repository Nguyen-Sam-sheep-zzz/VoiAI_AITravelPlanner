package com.codegym.voyai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserContributionRequest {

    @NotBlank
    private String destinationName;

    @NotBlank
    // Phải đúng 1 trong các category đã định nghĩa
    private String category;

    @NotNull
    @DecimalMin(value = "0.01", message = "Chi phí phải lớn hơn 0")
    private BigDecimal costUsd;
}