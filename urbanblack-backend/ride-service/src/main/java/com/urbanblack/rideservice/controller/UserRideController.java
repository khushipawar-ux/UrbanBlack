        package com.urbanblack.rideservice.controller;

        import com.urbanblack.rideservice.dto.request.CancelRideRequest;
        import com.urbanblack.rideservice.dto.request.FeedbackRequest;
        import com.urbanblack.rideservice.dto.request.RideRequestDto;
        import com.urbanblack.rideservice.dto.response.*;
        import com.urbanblack.rideservice.entity.Ride;
        import com.urbanblack.rideservice.service.FareService;
        import com.urbanblack.rideservice.service.GeoService;
        import com.urbanblack.rideservice.service.NearestDriverService;
        import com.urbanblack.rideservice.client.DriverServiceClient;
        import com.urbanblack.rideservice.client.UserServiceClient;
        import com.urbanblack.rideservice.service.DriverLocationService;
        import com.urbanblack.rideservice.service.RideService;
        import jakarta.servlet.http.HttpServletRequest;
        import lombok.RequiredArgsConstructor;
        import org.springframework.data.domain.Page;
        import org.springframework.data.domain.PageRequest;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.*;

        import java.time.format.DateTimeFormatter;
        import java.util.Collections;
        import java.util.List;
        import java.util.Map;
        import java.util.stream.Collectors;

        @RestController
        @RequestMapping("/api/v1/rides")
        @RequiredArgsConstructor
        public class UserRideController {

        private final RideService rideService;
        private final GeoService geoService;
        private final FareService fareService;
        private final NearestDriverService nearestDriverService;
        private final DriverLocationService driverLocationService;
        private final UserServiceClient userServiceClient;
        private final DriverServiceClient driverServiceClient;

        @GetMapping("/estimate")
        public ResponseEntity<EstimateRideResponse> estimateRide(
                @RequestParam("pickup_lat") double pickupLat,
                @RequestParam("pickup_lng") double pickupLng,
                @RequestParam("drop_lat") double dropLat,
                @RequestParam("drop_lng") double dropLng) {

                var route = geoService.estimateRideRoute(pickupLat, pickupLng, dropLat, dropLng);
                var fare = fareService.calculateFare(route.getDistanceKm());

                // Count nearby available drivers via Redis GeoSearch — free, instant
                // For estimate, no vehicleType filter (shows total nearby)
                List<NearestDriverService.NearestDriverResult> nearby =
                        nearestDriverService.findNearestAvailableDrivers(pickupLat, pickupLng, 5.0, 5);

                EstimateRideResponse response = EstimateRideResponse.builder()
                        .rideKm(route.getDistanceKm())
                        .durationMin(route.getDurationMin())
                        .pickupAddress(null)
                        .dropAddress(null)
                        .fare(fare.doubleValue())
                        .polyline(route.getPolyline())
                        .nearbyDriversCount(nearby.size())
                        .build();

                return ResponseEntity.ok(response);
        }

        @GetMapping("/places/autocomplete")
        public ResponseEntity<List<Map<String, Object>>> autocompletePlaces(
                @RequestParam("input") String input,
                @RequestParam(value = "lat", required = false) Double lat,
                @RequestParam(value = "lng", required = false) Double lng) {

                List<GeoService.PlaceSuggestion> suggestions =
                        geoService.autocompletePlaces(input, lat, lng);

                List<Map<String, Object>> payload = suggestions.stream()
                        .map(p -> Map.<String, Object>of(
                                "description", p.getDescription(),
                                "placeId", p.getPlaceId()
                        ))
                        .collect(Collectors.toList());

                return ResponseEntity.ok(payload);
        }

        @GetMapping("/places/details")
        public ResponseEntity<?> getPlaceDetails(@RequestParam("placeId") String placeId) {
                GeoService.PlaceDetails details = geoService.placeDetails(placeId);
                if (details == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Could not resolve place details"));
                }
                return ResponseEntity.ok(Map.of(
                        "placeId", details.getPlaceId(),
                        "formattedAddress", details.getFormattedAddress() != null ? details.getFormattedAddress() : "",
                        "lat", details.getLat(),
                        "lng", details.getLng()
                ));
        }

        @GetMapping("/places/reverse-geocode")
        public ResponseEntity<?> reverseGeocode(
                @RequestParam("lat") double lat,
                @RequestParam("lng") double lng) {
                String address = geoService.reverseGeocode(lat, lng);
                if (address == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Could not reverse geocode location"));
                }
                return ResponseEntity.ok(Map.of("formattedAddress", address));
        }

        @PostMapping("/request")
        public ResponseEntity<RideRequestResponse> requestRide(
                @RequestBody RideRequestDto request,
                HttpServletRequest httpRequest) {
                String userId = httpRequest.getHeader("X-User-Id");

                Ride ride = rideService.createRideRequest(
                        userId,
                        request.getPickupLat(),
                        request.getPickupLng(),
                        request.getDropLat(),
                        request.getDropLng(),
                        request.getPickupAddress(),
                        request.getDropAddress(),
                        request.getNotes(),
                        request.getVehicleType()
                );

                // Map NearestDriverResult list into response-friendly Map<String, Object> objects
                List<Map<String, Object>> nearbyDriverMaps = ride.getNearbyDrivers() == null
                        ? Collections.emptyList()
                        : ride.getNearbyDrivers().stream()
                                .map(d -> Map.<String, Object>of(
                                        "driverId", d.getDriverId(),
                                        "distanceKm", d.getDistanceKm(),
                                        "lat", d.getLat(),
                                        "lng", d.getLng()
                                ))
                                .collect(Collectors.toList());

                RideRequestResponse response = RideRequestResponse.builder()
                        .rideId(ride.getId())
                        .status(ride.getStatus().name())
                        .rideKm(ride.getRideKm() != null ? ride.getRideKm() : 0d)
                        .fare(ride.getFare() != null ? ride.getFare().doubleValue() : 0d)
                        .estimatedPickupMin(0)
                        .nearbyDrivers(nearbyDriverMaps)
                        .build();

                return ResponseEntity.status(201).body(response);
        }

        @GetMapping("/{rideId}/status")
        public ResponseEntity<RideStatusResponse> getRideStatus(@PathVariable("rideId") String rideId) {
                Ride ride = rideService.getRide(rideId)
                        .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

                Map<String, Object> driver;
                if (ride.getDriverId() == null || ride.getDriverId().isBlank()) {
                        driver = Map.of();
                } else {
                        driver = new java.util.HashMap<>(Map.of(
                                "id", ride.getDriverId(),
                                "driverId", ride.getDriverId(),
                                "otp", ride.getOtp() != null ? ride.getOtp() : ""
                        ));
                        try {
                                var summary = driverServiceClient.getDriverSummary(ride.getDriverId());
                                if (summary != null) {
                                        String first = summary.getFirstName() != null ? summary.getFirstName().trim() : "";
                                        String last = summary.getLastName() != null ? summary.getLastName().trim() : "";
                                        String fullName = (first + " " + last).trim();
                                        if (!fullName.isBlank()) {
                                                driver.put("name", fullName);
                                                driver.put("driverName", fullName);
                                        }
                                        if (summary.getEmployeeId() != null && !summary.getEmployeeId().isBlank()) {
                                                driver.put("employeeId", summary.getEmployeeId());
                                                driver.put("employee_id", summary.getEmployeeId());
                                        }
                                }
                        } catch (Exception ignored) {
                                try {
                                        Map<String, Object> userResponse = userServiceClient.getUserById(ride.getDriverId());
                                        if (userResponse != null) {
                                                Object data = userResponse.get("data");
                                                if (data instanceof Map) {
                                                        Object nameObj = ((Map<?, ?>) data).get("name");
                                                        if (nameObj != null && !nameObj.toString().isBlank()) {
                                                                driver.put("name", nameObj.toString());
                                                                driver.put("driverName", nameObj.toString());
                                                        }
                                                }
                                        }
                                } catch (Exception ignored2) {
                                        // Driver name unavailable; keep id, driverId, otp only
                                }
                        }
                }

                RideStatusResponse response = RideStatusResponse.builder()
                        .rideId(ride.getId())
                        .status(ride.getStatus().name())
                        .driver(driver)
                        .build();

                return ResponseEntity.ok(response);
        }

        @GetMapping("/active")
        public ResponseEntity<Ride> getActiveRide(HttpServletRequest httpRequest) {
                String userId = httpRequest.getHeader("X-User-Id");
                return rideService.getActiveRideForUser(userId)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.noContent().build());
        }

        @GetMapping("/history")
        public ResponseEntity<RideHistoryResponse> getRideHistory(
                @RequestParam("page") int page,
                @RequestParam("size") int size,
                HttpServletRequest httpRequest) {
                String userId = httpRequest.getHeader("X-User-Id");
                Page<Ride> rides = rideService.getUserRideHistory(userId, PageRequest.of(page, size));

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                List<RideHistoryItemResponse> items = rides.stream()
                        .map(r -> RideHistoryItemResponse.builder()
                                .rideId(r.getId())
                                .date(r.getRequestedAt() != null ? r.getRequestedAt().toLocalDate().format(formatter) : null)
                                .pickup(r.getPickupAddress())
                                .drop(r.getDropAddress())
                                .status(r.getStatus().name())
                                .rideKm(r.getRideKm() != null ? r.getRideKm() : 0d)
                                .fare(r.getFare() != null ? r.getFare().doubleValue() : 0d)
                                .durationMin(r.getDurationMin() != null ? r.getDurationMin() : 0)
                                .driverName(null)
                                .build())
                        .collect(Collectors.toList());

                RideHistoryResponse response = RideHistoryResponse.builder()
                        .rides(items)
                        .build();

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{rideId}/cancel")
        public ResponseEntity<Void> cancelRide(
                @PathVariable("rideId") String rideId,
                @RequestBody CancelRideRequest request) {
                rideService.cancelRide(rideId, request.getReason());
                return ResponseEntity.ok().build();
        }

        @PostMapping("/{rideId}/feedback")
        public ResponseEntity<Void> submitFeedback(
                @PathVariable("rideId") String rideId,
                @RequestBody FeedbackRequest request) {
                return ResponseEntity.ok().build();
        }

        @GetMapping("/{rideId}/receipt")
        public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable("rideId") String rideId) {
                Ride ride = rideService.getRide(rideId)
                        .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

                ReceiptResponse response = ReceiptResponse.builder()
                        .rideId(ride.getId())
                        .pickup(ride.getPickupAddress())
                        .drop(ride.getDropAddress())
                        .distanceKm(ride.getRideKm() != null ? ride.getRideKm() : 0d)
                        .fare(ride.getFare() != null ? ride.getFare().doubleValue() : 0d)
                        .ridePolyline(null)
                        .date(ride.getCompletedAt() != null ? ride.getCompletedAt().toString() : null)
                        .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Returns the driver's latest cached GPS location for an active ride.
         * Used by the passenger app as a REST fallback when the WebSocket is unavailable.
         */
        @GetMapping("/{rideId}/driver-location")
        public ResponseEntity<?> getDriverLocation(@PathVariable("rideId") String rideId) {
                Ride ride = rideService.getRide(rideId)
                        .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

                if (ride.getDriverId() == null || ride.getDriverId().isBlank()) {
                return ResponseEntity.noContent().build();
                }

                Map<String, Object> location = driverLocationService.getLatestLocation(ride.getDriverId());
                if (location.isEmpty()) {
                return ResponseEntity.noContent().build();
                }

                return ResponseEntity.ok(location);
        }
        }
