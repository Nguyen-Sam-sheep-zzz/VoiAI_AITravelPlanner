package com.codegym.voyai.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderRequest {
    private List<ActivityOrderUpdate> activities;

    @Data
    public static class ActivityOrderUpdate {
        private Long id;
        private String startTime;
    }
}
