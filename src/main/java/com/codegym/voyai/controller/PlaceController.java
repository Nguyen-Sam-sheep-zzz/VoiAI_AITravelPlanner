package com.codegym.voyai.controller;

import com.codegym.voyai.service.NominatimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/places")
@RequiredArgsConstructor
public class PlaceController {
    private final NominatimService nominatimService;

    @GetMapping("/search")
    public ResponseEntity<?> searchPlace(@RequestParam String q) {
        return ResponseEntity.ok(nominatimService.search(q));
    }
}