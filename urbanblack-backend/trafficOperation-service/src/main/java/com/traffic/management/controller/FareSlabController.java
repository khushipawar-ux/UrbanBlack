package com.traffic.management.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import com.traffic.management.dto.FareSlabRequest;
import com.traffic.management.dto.FareSlabResponse;
import com.traffic.management.service.FareSlabService;

@RestController
@RequestMapping("/api/admin/fare-slabs")
public class FareSlabController {

    private final FareSlabService service;

    public FareSlabController(FareSlabService service) {
        this.service = service;
    }

    @PostMapping
    public FareSlabResponse create(@RequestBody FareSlabRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public FareSlabResponse update(@PathVariable Long id, @RequestBody FareSlabRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}")
    public FareSlabResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public java.util.List<FareSlabResponse> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
