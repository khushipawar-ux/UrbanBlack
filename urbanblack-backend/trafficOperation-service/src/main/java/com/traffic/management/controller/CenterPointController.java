package com.traffic.management.controller;

import com.traffic.management.dto.CenterPointRequest;
import com.traffic.management.entity.CenterPoint;
import com.traffic.management.service.CenterPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/centerpoints")
@RequiredArgsConstructor
public class CenterPointController {

    private final CenterPointService centerPointService;

    @PostMapping("/{depotId}")
    public CenterPoint add(@PathVariable Long depotId, @RequestBody CenterPointRequest request) {
        return centerPointService.addCenterPoint(depotId, request);
    }

    @GetMapping("/depot/{depotId}")
    public List<CenterPoint> getByDepot(@PathVariable Long depotId) {
        return centerPointService.getCenterPointsByDepot(depotId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            centerPointService.deleteCenterPoint(id);
            response.put("message", "Center Point deleted successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public List<CenterPoint> getAll() {
        return centerPointService.getAllCenterPoints();
    }

    @PutMapping("/{id}")
    public CenterPoint update(@PathVariable Long id, @RequestBody CenterPointRequest request) {
        return centerPointService.updateCenterPoint(id, request);
    }
}
