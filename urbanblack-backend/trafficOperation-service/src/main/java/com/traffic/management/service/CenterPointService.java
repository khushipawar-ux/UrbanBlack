package com.traffic.management.service;

import com.traffic.management.dto.CenterPointRequest;
import com.traffic.management.entity.CenterPoint;
import com.traffic.management.entity.Depot;
import com.traffic.management.repository.CenterPointRepository;
import com.traffic.management.repository.DepotRepository;
import com.traffic.management.repository.DriverDailyAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CenterPointService {

    private final CenterPointRepository centerPointRepository;
    private final DepotRepository depotRepository;
    private final DriverDailyAssignmentRepository driverDailyAssignmentRepository;

    @org.springframework.transaction.annotation.Transactional
    public CenterPoint addCenterPoint(Long depotId, CenterPointRequest request) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Depot not found"));
        
        CenterPoint centerPoint = CenterPoint.builder()
                .pointName(request.getPointName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .depot(depot)
                .build();
        return centerPointRepository.save(centerPoint);
    }

    public List<CenterPoint> getCenterPointsByDepot(Long depotId) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Depot not found"));
        return centerPointRepository.findByDepot(depot);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteCenterPoint(Long id) {
        if (driverDailyAssignmentRepository.existsByCenterPointId(id)) {
            throw new RuntimeException("Cannot delete center point as it is currently assigned to one or more drivers.");
        }
        centerPointRepository.deleteById(id);
    }

    public List<CenterPoint> getAllCenterPoints() {
        return centerPointRepository.findAll();
    }

    public CenterPoint updateCenterPoint(Long id, CenterPointRequest request) {
        CenterPoint cp = centerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Center Point not found"));
        cp.setPointName(request.getPointName());
        cp.setLatitude(request.getLatitude());
        cp.setLongitude(request.getLongitude());
        return centerPointRepository.save(cp);
    }
}
