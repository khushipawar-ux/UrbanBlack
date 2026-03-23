package com.urbanblack.fleetservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanblack.fleetservice.dto.*;
import com.urbanblack.fleetservice.exception.FleetServiceException;
import com.urbanblack.fleetservice.entity.Vehicle;
import com.urbanblack.fleetservice.entity.VehicleAssignment;
import com.urbanblack.fleetservice.repository.VehicleRepository;
import com.urbanblack.fleetservice.repository.VehicleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final ObjectMapper objectMapper;

    // ===============================
    // 1️⃣ Get Assigned Vehicle
    // ===============================
    public Optional<VehicleResponse> getAssignedVehicle(String driverId) {
        Optional<VehicleAssignment> assignment = vehicleAssignmentRepository
                .findByDriverIdAndStatus(driverId, "ASSIGNED");

        if (assignment.isEmpty()) {
            return Optional.empty();
        }

        Vehicle vehicle = assignment.get().getVehicle();
        return Optional.of(mapToVehicleResponse(vehicle));
    }

    /**
     * Admin method to assign a vehicle to a driver.
     * This is called by the Traffic Operation Service when an assignment is created in T3.
     */
    @Transactional
    public VehicleAssignmentResponse adminAssignVehicle(AdminAssignVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByVehicleNumber(request.getVehicleNumber())
                .orElseThrow(() -> new FleetServiceException(
                        HttpStatus.NOT_FOUND,
                        "VEHICLE_NOT_FOUND",
                        "Vehicle not found with number: " + request.getVehicleNumber()
                ));

        // Create the assignment
        VehicleAssignment assignment = VehicleAssignment.builder()
                .driverId(request.getDriverId())
                .vehicle(vehicle)
                .status("ASSIGNED")
                .startTime(LocalDateTime.now())
                .build();

        // Update vehicle status
        vehicle.setStatus("ASSIGNED");

        vehicleRepository.save(vehicle);
        VehicleAssignment savedAssignment = vehicleAssignmentRepository.save(assignment);

        return VehicleAssignmentResponse.builder()
                .vehicleAssignmentId(savedAssignment.getId())
                .vehicleId(vehicle.getId())
                .status("assigned")
                .build();
    }

    public Optional<VehicleResponse> getVehicleByPlate(String plate) {
        return vehicleRepository.findByVehicleNumber(plate)
                .map(this::mapToVehicleResponse);
    }

    // ===============================
    // 1️⃣.1️⃣ Get All Vehicles
    // ===============================
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToVehicleResponse)
                .collect(Collectors.toList());
    }

    // ===============================
    // 2️⃣ Take Vehicle
    // ===============================
    @Transactional
    public VehicleAssignmentResponse takeVehicle(String driverId, TakeVehicleRequest request) {
        validateDriverId(driverId);
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new FleetServiceException(
                        HttpStatus.NOT_FOUND,
                        "VEHICLE_NOT_FOUND",
                        "Vehicle not found for id: " + request.getVehicleId()
                ));

        // Check if vehicle is assigned to this driver
        Optional<VehicleAssignment> existingAssignment = vehicleAssignmentRepository
                .findByDriverIdAndStatus(driverId, "ASSIGNED");

        if (existingAssignment.isEmpty() ||
                !existingAssignment.get().getVehicle().getId().equals(request.getVehicleId())) {
            throw new FleetServiceException(
                    HttpStatus.CONFLICT,
                    "VEHICLE_NOT_ASSIGNED",
                    "Vehicle is not assigned to this driver"
            );
        }

        // Check if vehicle is already in use
        Optional<VehicleAssignment> activeAssignment = vehicleAssignmentRepository
                .findByVehicle_IdAndStatus(request.getVehicleId(), "IN_USE");

        if (activeAssignment.isPresent()) {
            throw new FleetServiceException(
                    HttpStatus.CONFLICT,
                    "VEHICLE_ALREADY_IN_USE",
                    "Vehicle is already in use"
            );
        }

        VehicleAssignment assignment = existingAssignment.get();

        // Basic odometer validation
        if (request.getStartKm() == null) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "START_KM_REQUIRED",
                    "Start odometer reading is required"
            );
        }
        if (vehicle.getCurrentKm() != null && request.getStartKm() < vehicle.getCurrentKm()) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_START_KM",
                    "Start odometer reading cannot be less than current vehicle kilometers"
            );
        }

        assignment.setStartKm(request.getStartKm());
        assignment.setStartFuel(request.getStartFuel());
        assignment.setStartTime(parseTimestamp(request.getTimestamp()));

        // Convert photos and checklist to JSON strings
        try {
            assignment.setStartPhotos(objectMapper.writeValueAsString(request.getPhotos()));
            assignment.setInspectionChecklist(objectMapper.writeValueAsString(request.getInspectionChecklist()));
        } catch (JsonProcessingException e) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PHOTO_OR_CHECKLIST",
                    "Unable to process photos or inspection checklist"
            );
        }

        assignment.setStatus("IN_USE");

        vehicle.setStatus("IN_USE");
        vehicle.setCurrentKm(request.getStartKm());

        vehicleRepository.save(vehicle);
        VehicleAssignment savedAssignment = vehicleAssignmentRepository.save(assignment);

        return VehicleAssignmentResponse.builder()
                .vehicleAssignmentId(savedAssignment.getId())
                .vehicleId(vehicle.getId())
                // API spec expects: \"in-use\"
                .status("in-use")
                .build();
    }

    // ===============================
    // 3️⃣ Return Vehicle
    // ===============================
    @Transactional
    public ReturnVehicleResponse returnVehicle(String driverId, ReturnVehicleRequest request) {
        validateDriverId(driverId);
        VehicleAssignment assignment = vehicleAssignmentRepository
                .findByVehicle_IdAndStatus(request.getVehicleId(), "IN_USE")
                .orElseThrow(() -> new FleetServiceException(
                        HttpStatus.NOT_FOUND,
                        "ACTIVE_ASSIGNMENT_NOT_FOUND",
                        "No active assignment found for vehicle: " + request.getVehicleId()
                ));

        // Verify driver owns this assignment
        if (!assignment.getDriverId().equals(driverId)) {
            throw new FleetServiceException(
                    HttpStatus.FORBIDDEN,
                    "ASSIGNMENT_DRIVER_MISMATCH",
                    "This vehicle assignment does not belong to the driver"
            );
        }

        Vehicle vehicle = assignment.getVehicle();

        if (request.getEndKm() == null) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "END_KM_REQUIRED",
                    "End odometer reading is required"
            );
        }
        if (assignment.getStartKm() != null && request.getEndKm() < assignment.getStartKm()) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_END_KM",
                    "End odometer reading cannot be less than start kilometers"
            );
        }

        assignment.setEndKm(request.getEndKm());
        assignment.setEndFuel(request.getEndFuel());
        assignment.setEndTime(parseTimestamp(request.getTimestamp()));
        assignment.setDamages(request.getDamages());

        // Convert end photos to JSON string
        try {
            assignment.setEndPhotos(objectMapper.writeValueAsString(request.getPhotos()));
        } catch (JsonProcessingException e) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_END_PHOTOS",
                    "Unable to process return photos"
            );
        }

        assignment.setStatus("COMPLETED");

        vehicle.setStatus("AVAILABLE");
        vehicle.setCurrentKm(request.getEndKm());

        vehicleRepository.save(vehicle);
        VehicleAssignment savedAssignment = vehicleAssignmentRepository.save(assignment);

        if (assignment.getStartKm() == null) {
            throw new FleetServiceException(
                    HttpStatus.CONFLICT,
                    "INVALID_ASSIGNMENT_STATE",
                    "Assignment start kilometers are missing. Vehicle cannot be returned."
            );
        }

        int totalKm = request.getEndKm() - assignment.getStartKm();
        Integer fuelConsumed = null;
        if (assignment.getStartFuel() != null && request.getEndFuel() != null) {
            fuelConsumed = assignment.getStartFuel() - request.getEndFuel();
        }

        return ReturnVehicleResponse.builder()
                .vehicleAssignmentId(savedAssignment.getId())
                .totalKm(totalKm)
                .fuelConsumed(fuelConsumed)
                .build();
    }

    private VehicleResponse mapToVehicleResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehicleNumber(vehicle.getVehicleNumber())
                .model(vehicle.getModel())
                .make(vehicle.getMake())
                .year(vehicle.getYear())
                .fuelType(vehicle.getFuelType())
                .capacity(vehicle.getCapacity())
                .currentKm(vehicle.getCurrentKm())
                .lastServiceDate(vehicle.getLastServiceDate())
                .nextServiceDate(vehicle.getNextServiceDate())
                .insuranceExpiry(vehicle.getInsuranceExpiry())
                // Normalize status casing for API spec
                .status(vehicle.getStatus() != null ? vehicle.getStatus().toLowerCase() : null)
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
}
