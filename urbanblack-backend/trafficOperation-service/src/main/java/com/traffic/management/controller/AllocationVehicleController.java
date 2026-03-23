package com.traffic.management.controller;

import com.traffic.management.dto.AssignVehicleRequest;
import com.traffic.management.entity.AllocationVehicle;
import com.traffic.management.service.AssignVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depot/assign-vehicle")
@RequiredArgsConstructor
@Tag(name = "Assign Vehicle to Depot", description = "Endpoints for assigning multiple Vehicles to a Depot (allocate_vehicles table)")
public class AllocationVehicleController {

    private final AssignVehicleService assignVehicleService;

    @PostMapping
    @Operation(summary = "Assign Vehicles to Depot",
               description = "Creates a batch vehicle-to-depot assignment. Multiple vehicles can be selected at once.")
    public ResponseEntity<AllocationVehicle> assignVehicles(@RequestBody AssignVehicleRequest request) {
        return ResponseEntity.ok(assignVehicleService.assignVehicles(request));
    }

    @GetMapping
    @Operation(summary = "List All Vehicle Assignments",
               description = "Returns all records from the allocate_vehicles table.")
    public ResponseEntity<List<AllocationVehicle>> getAll() {
        return ResponseEntity.ok(assignVehicleService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Vehicle Assignment by ID")
    public ResponseEntity<AllocationVehicle> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assignVehicleService.getById(id));
    }

    @GetMapping("/depot/{depotId}")
    @Operation(summary = "Get Vehicle Assignments by Depot",
               description = "Returns all vehicle assignment records for a specific depot.")
    public ResponseEntity<List<AllocationVehicle>> getByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(assignVehicleService.getByDepot(depotId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Vehicle Assignment",
               description = "Updates depot, date or vehicle list for an existing assignment.")
    public ResponseEntity<AllocationVehicle> update(@PathVariable Long id,
                                                     @RequestBody AssignVehicleRequest request) {
        return ResponseEntity.ok(assignVehicleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Vehicle Assignment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignVehicleService.delete(id);
        return ResponseEntity.ok().build();
    }
}
