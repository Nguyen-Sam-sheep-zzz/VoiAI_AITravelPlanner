package com.codegym.voyai.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderRequest {
    private List<Long> activityIds; // thứ tự mới từ frontend
}
