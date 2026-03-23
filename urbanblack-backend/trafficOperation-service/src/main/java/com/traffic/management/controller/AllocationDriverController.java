package com.traffic.management.controller;

import com.traffic.management.dto.AssignDriverRequest;
import com.traffic.management.entity.AllocationDriver;
import com.traffic.management.service.AssignDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depot/assign-driver")
@RequiredArgsConstructor
@Tag(name = "Assign Drivers to Depot", description = "Endpoints for assigning multiple Drivers to a Depot (assign_driver table)")
public class AllocationDriverController {

    private final AssignDriverService assignDriverService;

    @PostMapping
    @Operation(summary = "Assign Drivers to Depot",
               description = "Creates a batch driver-to-depot assignment. Multiple employees can be selected at once.")
    public ResponseEntity<AllocationDriver> assignDrivers(@RequestBody AssignDriverRequest request) {
        return ResponseEntity.ok(assignDriverService.assignDrivers(request));
    }

    @GetMapping
    @Operation(summary = "List All Driver Assignments",
               description = "Returns all records from the assign_driver table.")
    public ResponseEntity<List<AllocationDriver>> getAll() {
        return ResponseEntity.ok(assignDriverService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Driver Assignment by ID")
    public ResponseEntity<AllocationDriver> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assignDriverService.getById(id));
    }

    @GetMapping("/depot/{depotId}")
    @Operation(summary = "Get Driver Assignments by Depot",
               description = "Returns all driver assignment records for a specific depot.")
    public ResponseEntity<List<AllocationDriver>> getByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(assignDriverService.getByDepot(depotId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Driver Assignment",
               description = "Updates depot, date or employee list for an existing assignment.")
    public ResponseEntity<AllocationDriver> update(@PathVariable Long id,
                                                    @RequestBody AssignDriverRequest request) {
        return ResponseEntity.ok(assignDriverService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Driver Assignment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignDriverService.delete(id);
        return ResponseEntity.ok().build();
    }
}
