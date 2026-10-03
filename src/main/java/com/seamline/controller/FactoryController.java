package com.seamline.controller;

import com.seamline.dto.CreateLineRequest;
import com.seamline.dto.FactoryResponse;
import com.seamline.service.FactoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Factory-wide screens: every line, and the order book. */
@RestController
@RequestMapping("/api")
public class FactoryController {

    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    @GetMapping("/lines")
    public ResponseEntity<List<FactoryResponse.Line>> lines() {
        return ResponseEntity.ok(factoryService.allLines());
    }

    @PostMapping("/lines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FactoryResponse.Line> createLine(@Valid @RequestBody CreateLineRequest body) {
        return ResponseEntity.ok(factoryService.createLine(body));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<FactoryResponse.OrderRow>> orders() {
        return ResponseEntity.ok(factoryService.allOrders());
    }
}
