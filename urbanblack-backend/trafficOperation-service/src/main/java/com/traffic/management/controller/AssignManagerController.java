package com.traffic.management.controller;

import com.traffic.management.dto.AssignManagerRequest;
import com.traffic.management.entity.AssignManager;
import com.traffic.management.service.AssignManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depot/assign-manager")
@RequiredArgsConstructor
@Tag(name = "Assign Manager to Depot", description = "Endpoints for assigning a Depot Manager to a Depot (assign_manager table)")
public class AssignManagerController {

    private final AssignManagerService assignManagerService;

    @PostMapping
    @Operation(summary = "Assign Manager to Depot",
               description = "Creates a new assignment record linking a Depot Manager to a Depot.")
    public ResponseEntity<AssignManager> assignManager(@RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(assignManagerService.assignManager(request));
    }

    @GetMapping
    @Operation(summary = "List All Manager Assignments",
               description = "Returns all records from the assign_manager table.")
    public ResponseEntity<List<AssignManager>> getAll() {
        return ResponseEntity.ok(assignManagerService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Manager Assignment by ID")
    public ResponseEntity<AssignManager> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assignManagerService.getById(id));
    }

    @GetMapping("/depot/{depotId}")
    @Operation(summary = "Get Manager Assignments by Depot",
               description = "Returns all manager assignments for a specific depot.")
    public ResponseEntity<List<AssignManager>> getByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(assignManagerService.getByDepot(depotId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Manager Assignment",
               description = "Updates an existing manager assignment record.")
    public ResponseEntity<AssignManager> update(@PathVariable Long id,
                                                 @RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(assignManagerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Manager Assignment",
               description = "Deletes a manager-to-depot assignment record.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignManagerService.delete(id);
        return ResponseEntity.ok().build();
    }
}
