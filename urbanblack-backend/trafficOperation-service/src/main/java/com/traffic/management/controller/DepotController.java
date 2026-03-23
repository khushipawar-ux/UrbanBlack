package com.traffic.management.controller;

import com.traffic.management.dto.DepotRequest;
import com.traffic.management.entity.Depot;
import com.traffic.management.service.DepotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;


@RestController
@RequestMapping("/api/depots")
@RequiredArgsConstructor
public class DepotController {

    private final DepotService depotService;

    @PostMapping
    public Depot create(@RequestBody DepotRequest request) {
        return depotService.createDepot(request);
    }

    @PostMapping("/create-bulk")
    public List<Depot> createBulk(@RequestBody List<DepotRequest> requests) {
        return depotService.createDepots(requests);
    }

    @GetMapping
    public Page<Depot> getAll(Pageable pageable) {
        return depotService.getAllDepots(pageable);
    }

    @GetMapping("/all")
    public List<Depot> getAllList() {
        return depotService.getAllDepotsList();
    }

    @GetMapping("/{id}")
    public Depot getById(@PathVariable Long id) {
        return depotService.getDepotById(id);
    }

    @PutMapping("/{id}")
    public Depot update(@PathVariable Long id, @RequestBody DepotRequest request) {
        return depotService.updateDepot(id, request);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        depotService.deleteDepot(id);
        return "Depot deleted successfully";
    }

    @GetMapping("/search")
    public Page<Depot> search(@RequestParam String city, Pageable pageable) {
        return depotService.searchByCity(city, pageable);
    }

    @GetMapping("/status/{isActive}")
    public Page<Depot> filterByStatus(@PathVariable Boolean isActive, Pageable pageable) {
        return depotService.filterByStatus(isActive, pageable);
    }

    @PutMapping("/{id}/deactivate")
    public Depot deactivate(@PathVariable Long id) {
        return depotService.setDepotStatus(id, false);
    }

    @PutMapping("/{id}/activate")
    public Depot activate(@PathVariable Long id) {
        return depotService.setDepotStatus(id, true);
    }

    @GetMapping("/depot-details/{id}")
    public java.util.Map<String, Object> getDetails(@PathVariable Long id) {
        return depotService.getDepotDetails(id);
    }
}
