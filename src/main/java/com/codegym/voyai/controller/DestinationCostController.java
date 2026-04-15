package com.codegym.voyai.controller;

import com.codegym.voyai.model.dto.DestinationCostDTO;
import com.codegym.voyai.model.dto.UserContributionRequest;
import com.codegym.voyai.service.DestinationCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/destinations")
@RequiredArgsConstructor
public class DestinationCostController {

    private final DestinationCostService service;

    // GET /api/public/destinations/suggest?keyword=sing
    // → ["Singapore", "Singburi"]
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(@RequestParam String keyword) {
        return ResponseEntity.ok(service.suggest(keyword));
    }

    // GET /api/public/destinations/costs?name=Singapore
    // → Toàn bộ chi phí của Singapore
    @GetMapping("/costs")
    public ResponseEntity<List<DestinationCostDTO>> getCosts(
            @RequestParam String name) {
        return ResponseEntity.ok(service.getByDestination(name));
    }

    // POST /api/public/destinations/contribute
    // → Người dùng đóng góp chi phí thực tế

    @PostMapping("/contribute")
    public ResponseEntity<?> contribute(@Valid @RequestBody UserContributionRequest request) {
        String result = service.contribute(request);

        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Cảm ơn bạn đã đóng góp!"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", result));
    }
}