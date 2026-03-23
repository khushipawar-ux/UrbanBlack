package com.urbanblack.rideservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanblack.rideservice.config.GoogleMapsConfig;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final GoogleMapsConfig googleMapsConfig;
    private final RestTemplate googleMapsRestTemplate;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    public static class RouteEstimate {
        private double distanceKm;
        private int durationMin;
        private String polyline;
    }

    @Data
    @Builder
    public static class PlaceSuggestion {
        private String description;
        private String placeId;
    }

    @Data
    @Builder
    public static class PlaceDetails {
        private String placeId;
        private String formattedAddress;
        private double lat;
        private double lng;
    }

    /**
     * Calls Google Directions first for route polyline, then falls back to OSRM.
     * If both fail, uses haversine estimate as the last fallback.
     */
    public RouteEstimate estimateRideRoute(double pickupLat, double pickupLng,
                                           double dropLat, double dropLng) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(googleMapsConfig.getBaseUrl() + "/maps/api/directions/json")
                    .queryParam("origin", pickupLat + "," + pickupLng)
                    .queryParam("destination", dropLat + "," + dropLng)
                    .queryParam("key", googleMapsConfig.getApiKey())
                    .build()
                    .toUri();

            String responseBody = googleMapsRestTemplate.getForObject(uri, String.class);

            if (responseBody == null) {
                log.warn("[GeoService] Null response from Directions API, trying OSRM fallback");
                return osrmRouteFallback(pickupLat, pickupLng, dropLat, dropLng);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.path("status").asText();

            if (!"OK".equals(status)) {
                log.warn("[GeoService] Directions API returned status={}, trying OSRM fallback", status);
                return osrmRouteFallback(pickupLat, pickupLng, dropLat, dropLng);
            }

            JsonNode route = root.path("routes").get(0);
            JsonNode leg = route.path("legs").get(0);

            // distance.value is in meters -> convert to km
            double distanceMeters = leg.path("distance").path("value").asDouble(0);
            double distanceKm = Math.round(distanceMeters / 10.0) / 100.0;

            // duration.value is in seconds -> convert to minutes
            int durationSeconds = leg.path("duration").path("value").asInt(0);
            int durationMin = (int) Math.ceil(durationSeconds / 60.0);

            // Encoded polyline for drawing route on the map
            String polyline = route.path("overview_polyline").path("points").asText(null);

            log.info("[GeoService] Google route: {}km, {}min, polyline={}", distanceKm, durationMin,
                    polyline != null ? polyline.length() + " chars" : "null");

            return RouteEstimate.builder()
                    .distanceKm(distanceKm)
                    .durationMin(durationMin)
                    .polyline(polyline)
                    .build();

        } catch (Exception e) {
            log.error("[GeoService] Directions API call failed: {}, trying OSRM fallback", e.getMessage());
            return osrmRouteFallback(pickupLat, pickupLng, dropLat, dropLng);
        }
    }

    /**
     * Calls Google Places Autocomplete first, then falls back to Nominatim.
     */
    public List<PlaceSuggestion> autocompletePlaces(String input, Double lat, Double lng) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(googleMapsConfig.getBaseUrl() + "/maps/api/place/autocomplete/json")
                    .queryParam("input", input)
                    .queryParam("key", googleMapsConfig.getApiKey())
                    .queryParam("types", "geocode")
                    .queryParam("components", "country:in")
                    .queryParam("language", "en");

            if (lat != null && lng != null) {
                builder.queryParam("location", lat + "," + lng)
                        .queryParam("radius", 3000);
            }

            URI uri = builder.build().toUri();

            String responseBody = googleMapsRestTemplate.getForObject(uri, String.class);
            if (responseBody == null) {
                log.warn("[GeoService] Null response from Places Autocomplete API, trying Nominatim");
                return nominatimAutocomplete(input);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.path("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("[GeoService] Places Autocomplete returned status={}, trying Nominatim", status);
                return nominatimAutocomplete(input);
            }

            List<PlaceSuggestion> results = new ArrayList<>();
            for (JsonNode prediction : root.path("predictions")) {
                String description = prediction.path("description").asText(null);
                String placeId = prediction.path("place_id").asText(null);
                if (description != null && placeId != null) {
                    results.add(PlaceSuggestion.builder()
                            .description(description)
                            .placeId(placeId)
                            .build());
                }
                if (results.size() >= 5) break;
            }

            return results;
        } catch (Exception e) {
            log.error("[GeoService] Places Autocomplete API call failed: {}, trying Nominatim", e.getMessage());
            return nominatimAutocomplete(input);
        }
    }

    /**
     * Resolves place details from Google placeId or fallback latlng-prefixed IDs.
     */
    public PlaceDetails placeDetails(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }

        // Fallback IDs produced by nominatimAutocomplete: latlng:<lat>,<lng>|<description>
        if (placeId.startsWith("latlng:")) {
            try {
                String raw = placeId.substring("latlng:".length());
                String[] parts = raw.split("\\|", 2);
                String[] latLng = parts[0].split(",", 2);
                double lat = Double.parseDouble(latLng[0]);
                double lng = Double.parseDouble(latLng[1]);
                String address = parts.length > 1 ? parts[1] : "";
                return PlaceDetails.builder()
                        .placeId(placeId)
                        .formattedAddress(address)
                        .lat(lat)
                        .lng(lng)
                        .build();
            } catch (Exception e) {
                log.warn("[GeoService] Invalid fallback placeId format: {}", placeId);
                return null;
            }
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(googleMapsConfig.getBaseUrl() + "/maps/api/place/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam("fields", "geometry,formatted_address")
                    .queryParam("key", googleMapsConfig.getApiKey())
                    .build()
                    .toUri();

            String responseBody = googleMapsRestTemplate.getForObject(uri, String.class);
            if (responseBody == null) {
                log.warn("[GeoService] Null response from Places Details API");
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("[GeoService] Places Details returned status={}", status);
                return null;
            }

            JsonNode result = root.path("result");
            JsonNode location = result.path("geometry").path("location");
            double lat = location.path("lat").asDouble(0);
            double lng = location.path("lng").asDouble(0);
            String formattedAddress = result.path("formatted_address").asText(null);

            log.info("[GeoService] Place details for {}: lat={}, lng={}", placeId, lat, lng);

            return PlaceDetails.builder()
                    .placeId(placeId)
                    .formattedAddress(formattedAddress)
                    .lat(lat)
                    .lng(lng)
                    .build();

        } catch (Exception e) {
            log.error("[GeoService] Places Details API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calls the Google Geocoding API to perform reverse geocoding
     * (converting lat/lng to a formatted address string).
     */
    public String reverseGeocode(double lat, double lng) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(googleMapsConfig.getBaseUrl() + "/maps/api/geocode/json")
                    .queryParam("latlng", lat + "," + lng)
                    .queryParam("key", googleMapsConfig.getApiKey())
                    .build()
                    .toUri();

            String responseBody = googleMapsRestTemplate.getForObject(uri, String.class);
            if (responseBody == null) {
                log.warn("[GeoService] Null response from Reverse Geocoding API");
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("[GeoService] Reverse Geocoding returned status={}", status);
                return null;
            }

            // Get the first (most specific) result
            JsonNode results = root.path("results");
            if (results.isArray() && results.size() > 0) {
                String formattedAddress = results.get(0).path("formatted_address").asText(null);
                log.info("[GeoService] Reverse geocode for {},{}: {}", lat, lng, formattedAddress);
                return formattedAddress;
            }

            return null;
        } catch (Exception e) {
            log.error("[GeoService] Reverse Geocoding API call failed: {}", e.getMessage());
            return null;
        }
    }

    private RouteEstimate osrmRouteFallback(double pickupLat, double pickupLng,
                                            double dropLat, double dropLng) {
        try {
            String osrmUrl = String.format(
                    "https://router.project-osrm.org/route/v1/driving/%s,%s;%s,%s",
                    pickupLng, pickupLat, dropLng, dropLat
            );
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(osrmUrl)
                    .queryParam("overview", "full")
                    .queryParam("geometries", "polyline")
                    .build()
                    .toUri();

            String responseBody = googleMapsRestTemplate.getForObject(uri, String.class);
            if (responseBody == null) {
                return haversineFallback(pickupLat, pickupLng, dropLat, dropLng);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode route = root.path("routes").isArray() && root.path("routes").size() > 0
                    ? root.path("routes").get(0)
                    : null;

            if (route == null || route.isMissingNode()) {
                return haversineFallback(pickupLat, pickupLng, dropLat, dropLng);
            }

            double distanceKm = Math.round((route.path("distance").asDouble(0) / 1000.0) * 100.0) / 100.0;
            int durationMin = (int) Math.ceil(route.path("duration").asDouble(0) / 60.0);
            String polyline = route.path("geometry").asText(null);

            if (polyline == null || polyline.isBlank()) {
                return haversineFallback(pickupLat, pickupLng, dropLat, dropLng);
            }

            log.info("[GeoService] OSRM route fallback: {}km, {}min, polyline={} chars",
                    distanceKm, durationMin, polyline.length());

            return RouteEstimate.builder()
                    .distanceKm(distanceKm)
                    .durationMin(durationMin)
                    .polyline(polyline)
                    .build();
        } catch (Exception e) {
            log.error("[GeoService] OSRM fallback failed: {}", e.getMessage());
            return haversineFallback(pickupLat, pickupLng, dropLat, dropLng);
        }
    }

    private List<PlaceSuggestion> nominatimAutocomplete(String input) {
        try {
            String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8);
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("q", encoded)
                    .queryParam("format", "json")
                    .queryParam("limit", 5)
                    .queryParam("addressdetails", 1)
                    .queryParam("countrycodes", "in")
                    .build(true)
                    .toUri();

            String responseBody = googleMapsRestTemplate.getForObject(uri, String.class);
            if (responseBody == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.isArray()) {
                return List.of();
            }

            List<PlaceSuggestion> results = new ArrayList<>();
            for (JsonNode node : root) {
                String description = node.path("display_name").asText("");
                double lat = node.path("lat").asDouble(Double.NaN);
                double lon = node.path("lon").asDouble(Double.NaN);
                if (description.isBlank() || Double.isNaN(lat) || Double.isNaN(lon)) {
                    continue;
                }

                String fallbackPlaceId = "latlng:" + lat + "," + lon + "|" + description;
                results.add(PlaceSuggestion.builder()
                        .description(description)
                        .placeId(fallbackPlaceId)
                        .build());
            }
            return results;
        } catch (Exception e) {
            log.error("[GeoService] Nominatim autocomplete fallback failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RouteEstimate haversineFallback(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightLine = R * c;

        // Apply road factor (~1.3x) for realistic estimate
        double roadDistance = Math.round(straightLine * 1.3 * 100.0) / 100.0;
        // Assume average 25 km/h city speed
        int estimatedMin = (int) Math.ceil(roadDistance / 25.0 * 60);

        log.info("[GeoService] Haversine fallback: {}km, {}min", roadDistance, estimatedMin);

        return RouteEstimate.builder()
                .distanceKm(roadDistance)
                .durationMin(estimatedMin)
                .polyline(null)
                .build();
    }
}


