package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.dto.request.AdjustQuotaRequest;
import com.urbanblack.rideservice.dto.request.AssignDriverRequest;
import com.urbanblack.rideservice.entity.Ride;
import com.urbanblack.rideservice.entity.RideStatus;
import com.urbanblack.rideservice.entity.RewardTreeNode;
import com.urbanblack.rideservice.client.UserServiceClient;
import com.urbanblack.rideservice.client.WalletServiceClient;
import com.urbanblack.rideservice.repository.DriverKmLedgerRepository;
import com.urbanblack.rideservice.repository.RideRepository;
import com.urbanblack.rideservice.service.RideService;
import com.urbanblack.rideservice.client.DriverServiceClient;
import com.urbanblack.rideservice.dto.response.AdminRideResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRideController {

    private final RideRepository rideRepository;
    private final RideService rideService;
    private final DriverKmLedgerRepository ledgerRepository;
    private final com.urbanblack.rideservice.repository.RewardTreeRepository rewardTreeRepository;
    private final UserServiceClient userServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final DriverServiceClient driverServiceClient;

    @GetMapping("/rides")
    public ResponseEntity<?> getRides(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "status", required = false) RideStatus status,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        var pageable = PageRequest.of(page, size);

        if (status != null && date != null) {
            var pageRides = rideRepository.findByStatusAndRequestedAtBetweenOrderByRequestedAtDesc(
                            status,
                            date.atStartOfDay(),
                            date.plusDays(1).atStartOfDay().minusNanos(1),
                            pageable);
            var content = pageRides.getContent().stream()
                    .map(this::mapToAdminDTO)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(content, pageable, pageRides.getTotalElements()));
        } else if (date != null) {
            var pageRides = rideRepository.findByRequestedAtBetweenOrderByRequestedAtDesc(
                            date.atStartOfDay(),
                            date.plusDays(1).atStartOfDay().minusNanos(1),
                            pageable);
            var content = pageRides.getContent().stream()
                    .map(this::mapToAdminDTO)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(content, pageable, pageRides.getTotalElements()));
        } else {
            var pageRides = rideRepository.findAll(pageable);
            var content = pageRides.getContent().stream()
                    .map(this::mapToAdminDTO)
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(
                    content,
                    pageable,
                    pageRides.getTotalElements()
            ));
        }
    }

    private AdminRideResponseDTO mapToAdminDTO(Ride ride) {
        String driverName = "—";
        String driverCity = "";
        
        if (ride.getDriverId() != null) {
            try {
                var driver = driverServiceClient.getDriverSummary(ride.getDriverId());
                if (driver != null) {
                    driverName = (driver.getFirstName() != null ? driver.getFirstName() : "") + 
                                 " " + 
                                 (driver.getLastName() != null ? driver.getLastName() : "");
                    driverName = driverName.trim().isEmpty() ? "Driver-" + driver.getEmployeeId() : driverName;
                    driverCity = "Pune Hub"; // Mock city or depot indicator
                }
            } catch (Exception e) {
                // Ignore resolve failures
            }
        }

        return AdminRideResponseDTO.builder()
                .id(ride.getId())
                .rideId(ride.getId())
                .userId(ride.getUserId())
                .driverId(ride.getDriverId())
                .driverName(driverName)
                .driverCity(driverCity)
                .depotName("Pune Central") // Placeholders as per current schema
                .pickupAddress(ride.getPickupAddress())
                .dropAddress(ride.getDropAddress())
                .pickupLat(ride.getPickupLat())
                .pickupLng(ride.getPickupLng())
                .dropLat(ride.getDropLat())
                .dropLng(ride.getDropLng())
                .status(ride.getStatus() != null ? ride.getStatus().name() : null)
                .rideKm(ride.getRideKm())
                .distance(ride.getRideKm() != null ? ride.getRideKm() * 1000 : 0.0) // Frontend expects meters for distance? Wait, TripRowExpandable.jsx/L37 says meters / 1000.
                .durationMin(ride.getDurationMin())
                .duration(ride.getDurationMin() != null ? ride.getDurationMin() * 60 : 0) // Frontend expects seconds for duration? Wait, TripRowExpandable.jsx/L42 says seconds / 60.
                .fare(ride.getFare() != null ? ride.getFare().doubleValue() : 0.0)
                .requestedAt(ride.getRequestedAt())
                .createdAt(ride.getCreatedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .pickup(ride.getPickupAddress())
                .drop(ride.getDropAddress())
                .build();
    }

    @GetMapping("/rides/{rideId}")
    public ResponseEntity<Ride> getRideDetail(@PathVariable("rideId") String rideId) {
        return rideService.getRide(rideId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rides/{rideId}/force-cancel")
    public ResponseEntity<Void> forceCancelRide(@PathVariable("rideId") String rideId) {
        rideService.cancelRide(rideId, "Force cancel by admin");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rides/{rideId}/assign-driver")
    public ResponseEntity<Ride> assignDriver(
            @PathVariable("rideId") String rideId,
            @RequestBody AssignDriverRequest request) {
        Ride ride = rideService.markDriverAccepted(rideId, request.getDriverId());
        return ResponseEntity.ok(ride);
    }

    @GetMapping("/drivers/{driverId}/km-ledger")
    public ResponseEntity<?> getDriverKmLedger(
            @PathVariable("driverId") String driverId,
            @RequestParam("days") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        return ResponseEntity.ok(
                ledgerRepository.findByDriverIdAndDateBetweenOrderByDateDesc(driverId, from, to)
        );
    }

    @PutMapping("/drivers/{driverId}/km-quota")
    public ResponseEntity<Void> adjustFreeRoamingQuota(
            @PathVariable("driverId") String driverId,
            @RequestBody AdjustQuotaRequest request) {
        // For now this is a stub; actual quota adjustment would update ledger and shift defaults
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analytics/rides/today")
    public ResponseEntity<java.util.Map<String, Long>> getRidesToday() {
        long count = rideRepository.countByStatusAndCreatedAtBetween(
                com.urbanblack.rideservice.entity.RideStatus.RIDE_COMPLETED,
                java.time.LocalDate.now().atStartOfDay(),
                java.time.LocalDate.now().plusDays(1).atStartOfDay().minusNanos(1)
        );
        return ResponseEntity.ok(java.util.Map.of("totalRides", count));
    }

    @GetMapping("/analytics/revenue/today")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> getRevenueToday() {
        java.math.BigDecimal revenue = rideRepository.sumFareByStatusAndCreatedAtBetween(
                com.urbanblack.rideservice.entity.RideStatus.RIDE_COMPLETED,
                java.time.LocalDate.now().atStartOfDay(),
                java.time.LocalDate.now().plusDays(1).atStartOfDay().minusNanos(1)
        );
        return ResponseEntity.ok(java.util.Map.of("totalRevenue", revenue != null ? revenue : java.math.BigDecimal.ZERO));
    }

    @GetMapping("/reward-tree/{userId}")
    public ResponseEntity<?> getRewardTree(@PathVariable Long userId) {
        return rewardTreeRepository.findByUserId(userId)
                .map(this::buildTree)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<java.util.Map<String, Object>> dashboard() {
        long totalUsers = 0L;
        try {
            var resp = userServiceClient.getTotalUsers();
            Object v = resp.get("totalUsers");
            if (v instanceof Number) totalUsers = ((Number) v).longValue();
        } catch (Exception ignored) {}

        java.math.BigDecimal totalRevenue = rideRepository.sumFareByStatus(RideStatus.RIDE_COMPLETED);

        double adminProfit = 0.0;
        try {
            var summary = walletServiceClient.getAdminSummary();
            Object v = summary.get("totalProfit");
            if (v instanceof Number) adminProfit = ((Number) v).doubleValue();
        } catch (Exception ignored) {}

        Integer activeDepth = rewardTreeRepository.findMaxActiveDepth();
        if (activeDepth == null) activeDepth = 0;

        return ResponseEntity.ok(java.util.Map.of(
                "totalUsers", totalUsers,
                "totalRevenue", totalRevenue != null ? totalRevenue : java.math.BigDecimal.ZERO,
                "adminProfit", adminProfit,
                "activeTreeDepth", activeDepth
        ));
    }

    @GetMapping("/tree/visual")
    public ResponseEntity<java.util.Map<String, Object>> treeVisual(@RequestParam(defaultValue = "5") int level) {
        if (level < 0) level = 0;

        java.util.List<RewardTreeNode> nodes = rewardTreeRepository
                .findByDepthLevelLessThanEqualOrderByBfsPositionAsc(level);

        java.util.Map<Integer, java.util.List<java.util.Map<String, Object>>> byLevel = new java.util.LinkedHashMap<>();
        for (RewardTreeNode n : nodes) {
            byLevel.computeIfAbsent(n.getDepthLevel(), k -> new java.util.ArrayList<>())
                    .add(java.util.Map.of(
                            "nodeId", n.getNodeId(),
                            "userId", n.getUserId(),
                            "parentNodeId", n.getParentNodeId(),
                            "bfsPosition", n.getBfsPosition(),
                            "leftChildId", n.getLeftChildId(),
                            "rightChildId", n.getRightChildId(),
                            "childrenCount", n.getChildrenCount(),
                            "active", n.getActive()
                    ));
        }

        return ResponseEntity.ok(java.util.Map.of(
                "levels", level,
                "nodesCount", nodes.size(),
                "tree", byLevel
        ));
    }

    private java.util.Map<String, Object> buildTree(com.urbanblack.rideservice.entity.RewardTreeNode node) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("nodeId", node.getNodeId());
        map.put("userId", node.getUserId());
        map.put("bfsPosition", node.getBfsPosition());
        map.put("depthLevel", node.getDepthLevel());
        map.put("active", node.getActive());
        map.put("deactivatedAt", node.getDeactivatedAt());
        
        java.util.List<com.urbanblack.rideservice.entity.RewardTreeNode> children = rewardTreeRepository.findByParentNodeId(node.getNodeId());
        java.util.List<java.util.Map<String, Object>> childMaps = new java.util.ArrayList<>();
        for (com.urbanblack.rideservice.entity.RewardTreeNode child : children) {
            childMaps.add(buildTree(child));
        }
        map.put("children", childMaps);
        return map;
    }
}

