package com.urbanblack.fleetservice.service;

import com.urbanblack.fleetservice.client.CabFeignClient;
import com.urbanblack.fleetservice.dto.FuelEntryRequest;
import com.urbanblack.fleetservice.dto.FuelEntryResponse;
import com.urbanblack.fleetservice.dto.FuelHistoryResponse;
import com.urbanblack.fleetservice.entity.FuelEntry;
import com.urbanblack.fleetservice.entity.Vehicle;
import com.urbanblack.fleetservice.exception.FleetServiceException;
import com.urbanblack.fleetservice.repository.FuelEntryRepository;
import com.urbanblack.fleetservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuelService {

    private final FuelEntryRepository fuelEntryRepository;
    private final VehicleRepository vehicleRepository;
    private final CabFeignClient cabFeignClient;

    // ===============================
    // 1️⃣ Submit Fuel Entry
    // ===============================
    @Transactional
    public FuelEntryResponse submitFuelEntry(String driverId, FuelEntryRequest request) {
        validateDriverId(driverId);

        // Sanity check for quantity (prevent typos like odometer in quantity field)
        if (request.getQuantity() != null && request.getQuantity() > 1000.0) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_QUANTITY",
                    "Fuel quantity cannot exceed 1000 liters. Please check for mistyped values."
            );
        }

        Vehicle vehicle = resolveVehicle(request.getVehicleId());

        if (request.getOdometerReading() == null) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "ODOMETER_REQUIRED",
                    "odometerReading is required"
            );
        }
        if (request.getOdometerReading() < 0) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ODOMETER_READING",
                    "Odometer reading cannot be negative"
            );
        }
        if (vehicle.getCurrentKm() != null && request.getOdometerReading() < vehicle.getCurrentKm()) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ODOMETER_READING",
                    "Odometer reading cannot be less than current vehicle kilometers"
            );
        }

        FuelEntry fuelEntry = FuelEntry.builder()
                .vehicle(vehicle)
                .tripId(request.getTripId())
                .fuelType(request.getFuelType())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .odometerReading(request.getOdometerReading())
                .stationName(request.getStationName())
                .stationAddress(request.getStationAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .receiptImage(request.getReceiptImage())
                .timestamp(parseTimestamp(request.getTimestamp()))
                .status("PENDING")
                .driverId(driverId)
                .build();

        FuelEntry savedEntry = fuelEntryRepository.save(fuelEntry);

        // Update vehicle odometer if provided reading is higher
        if (request.getOdometerReading() != null &&
                (vehicle.getCurrentKm() == null || request.getOdometerReading() > vehicle.getCurrentKm())) {
            vehicle.setCurrentKm(request.getOdometerReading());
            vehicleRepository.save(vehicle);
        }

        return FuelEntryResponse.builder()
                .id(savedEntry.getId())
                // API spec shows lower-case status values
                .status(savedEntry.getStatus() != null ? savedEntry.getStatus().toLowerCase() : null)
                .build();
    }

    // ===============================
    // 2️⃣ Get Fuel History
    // ===============================
    public List<FuelHistoryResponse> getFuelHistory(String driverId, int page, int limit) {
        validateDriverId(driverId);
        if (page < 1) {
            throw new FleetServiceException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "page must be >= 1");
        }
        if (limit < 1 || limit > 100) {
            throw new FleetServiceException(HttpStatus.BAD_REQUEST, "INVALID_LIMIT", "limit must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<FuelEntry> fuelEntries = fuelEntryRepository.findByDriverIdOrderByTimestampDesc(driverId, pageable);

        return fuelEntries.getContent().stream()
                .map(this::mapToFuelHistoryResponse)
                .collect(Collectors.toList());
    }

    private FuelHistoryResponse mapToFuelHistoryResponse(FuelEntry entry) {
        return FuelHistoryResponse.builder()
                .id(entry.getId())
                .date(entry.getTimestamp())
                .fuelType(entry.getFuelType() != null ? entry.getFuelType().toValue() : null)
                .quantity(entry.getQuantity())
                .amount(entry.getAmount())
                .stationName(entry.getStationName())
                // Normalize to lower-case for API
                .status(entry.getStatus() != null ? entry.getStatus().toLowerCase() : null)
                .build();
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private void validateDriverId(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "DRIVER_ID_REQUIRED",
                    "X-Driver-Id header is required"
            );
        }
    }

    /**
     * Resolve a vehicleId that may be:
     *   - A fleet-service UUID → direct lookup
     *   - A numeric cab_id from cab-registration-service → Feign lookup → find/create by numberPlate
     */
    private Vehicle resolveVehicle(String vehicleId) {
        // 1) Try direct UUID lookup first
        Optional<Vehicle> directHit = vehicleRepository.findById(vehicleId);
        if (directHit.isPresent()) {
            return directHit.get();
        }

        // 2) If it looks like a numeric cab_id, resolve via cab-registration-service
        if (vehicleId.matches("\\d+")) {
            try {
                Long cabId = Long.parseLong(vehicleId);
                Map<String, Object> cabData = cabFeignClient.getCabById(cabId);
                log.info("[FuelService] Resolved cab_id={} → cabData: {}", cabId, cabData);

                String numberPlate = (String) cabData.get("numberPlate");
                if (numberPlate == null || numberPlate.isBlank()) {
                    throw new FleetServiceException(
                            HttpStatus.NOT_FOUND,
                            "CAB_NO_PLATE",
                            "Cab " + cabId + " has no numberPlate registered"
                    );
                }

                // Find existing fleet vehicle by plate, or create one
                Optional<Vehicle> byPlate = vehicleRepository.findByVehicleNumber(numberPlate);
                if (byPlate.isPresent()) {
                    return byPlate.get();
                }

                // Auto-create a fleet Vehicle from cab data
                Vehicle newVehicle = Vehicle.builder()
                        .vehicleNumber(numberPlate)
                        .model((String) cabData.getOrDefault("cabModel", "Unknown"))
                        .make((String) cabData.getOrDefault("carName", "Unknown"))
                        .year(cabData.get("vehicleYear") != null ? ((Number) cabData.get("vehicleYear")).intValue() : 0)
                        .fuelType((String) cabData.getOrDefault("fuelType", "Diesel"))
                        .capacity(4)
                        .currentKm(cabData.get("kms") != null ? ((Number) cabData.get("kms")).intValue() : 0)
                        .status("AVAILABLE")
                        .build();
                Vehicle saved = vehicleRepository.save(newVehicle);
                log.info("[FuelService] Auto-created fleet Vehicle {} for cab_id={}", saved.getId(), cabId);
                return saved;

            } catch (FleetServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("[FuelService] Failed to resolve cab_id={}: {}", vehicleId, e.getMessage());
                throw new FleetServiceException(
                        HttpStatus.NOT_FOUND,
                        "VEHICLE_NOT_FOUND",
                        "Vehicle not found for id: " + vehicleId + " (cab lookup failed: " + e.getMessage() + ")"
                );
            }
        }

        // 3) Not a UUID match and not numeric → unknown
        throw new FleetServiceException(
                HttpStatus.NOT_FOUND,
                "VEHICLE_NOT_FOUND",
                "Vehicle not found for id: " + vehicleId
        );
    }
}
