package com.traffic.management.service;

import com.traffic.management.dto.DepotRequest;
import com.traffic.management.entity.Depot;
import com.traffic.management.repository.DepotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepotService {

    private final DepotRepository depotRepository;

    public Depot createDepot(DepotRequest request) {
        validateDepotRequest(request);
        Depot depot = Depot.builder()
                .depotCode(request.getDepotCode())
                .depotName(request.getDepotName())
                .city(request.getCity())
                .fullAddress(request.getFullAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .zone(request.getZone())
                .capacity(request.getCapacity())
                .operatingStart(request.getOperatingStart())
                .operatingEnd(request.getOperatingEnd())
                .registrationDate(request.getRegistrationDate())
                .build();
        return depotRepository.save(depot);
    }

    @Transactional
    public List<Depot> createDepots(List<DepotRequest> requests) {
        return requests.stream()
                .map(this::createDepot)
                .collect(Collectors.toList());
    }

    public Page<Depot> getAllDepots(Pageable pageable) {
        return depotRepository.findAll(pageable);
    }

    public List<Depot> getAllDepotsList() {
        return depotRepository.findAll();
    }

    public Depot getDepotById(Long id) {
        return depotRepository.findById(id).orElseThrow(() -> new RuntimeException("Depot not found"));
    }

    public Depot updateDepot(Long id, DepotRequest request) {
        validateDepotRequest(request);
        Depot depot = getDepotById(id);
        depot.setDepotCode(request.getDepotCode());
        depot.setDepotName(request.getDepotName());
        depot.setCity(request.getCity());
        depot.setFullAddress(request.getFullAddress());
        depot.setLatitude(request.getLatitude());
        depot.setLongitude(request.getLongitude());
        depot.setZone(request.getZone());
        depot.setCapacity(request.getCapacity());
        depot.setOperatingStart(request.getOperatingStart());
        depot.setOperatingEnd(request.getOperatingEnd());
        depot.setRegistrationDate(request.getRegistrationDate());
        return depotRepository.save(depot);
    }

    public void deleteDepot(Long id) {
        depotRepository.deleteById(id);
    }

    public Page<Depot> searchByCity(String city, Pageable pageable) {
        return depotRepository.findByCityContainingIgnoreCase(city, pageable);
    }

    public Page<Depot> filterByStatus(Boolean isActive, Pageable pageable) {
        return depotRepository.findByIsActive(isActive, pageable);
    }

    public Depot setDepotStatus(Long id, boolean active) {
        Depot depot = getDepotById(id);
        depot.setIsActive(active);
        return depotRepository.save(depot);
    }

    private void validateDepotRequest(DepotRequest request) {
        if (request.getCapacity() != null && request.getCapacity() <= 0) {
            throw new RuntimeException("Depot capacity must be greater than 0");
        }
    }

    public java.util.Map<String, Object> getDepotDetails(Long id) {
        Depot depot = getDepotById(id);
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("depot", depot);
        details.put("centerPoints", depot.getCenterPoints());
        // Allocation details are managed via assign_manager, allocate_vehicles, assign_driver tables
        return details;
    }
}
