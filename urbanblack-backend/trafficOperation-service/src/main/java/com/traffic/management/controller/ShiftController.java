package com.traffic.management.controller;

import com.traffic.management.dto.ShiftRequest;
import com.traffic.management.entity.ShiftMaster;
import com.traffic.management.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ShiftMaster create(@RequestBody ShiftRequest request) {
        return shiftService.createShift(request);
    }

    @GetMapping
    public List<ShiftMaster> getAll() {
        return shiftService.getAllShifts();
    }

    @PutMapping("/{id}")
    public ShiftMaster update(@PathVariable Long id, @RequestBody ShiftRequest request) {
        return shiftService.updateShift(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        shiftService.deleteShift(id);
    }
}
