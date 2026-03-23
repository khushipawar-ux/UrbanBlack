package com.traffic.management.service;

import com.traffic.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final DepotRepository depotRepository;

    public Map<String, Object> globalSearch(String keyword) {
        Map<String, Object> results = new HashMap<>();
        org.springframework.data.domain.Pageable unpaged = org.springframework.data.domain.Pageable.unpaged();
        results.put("depots", depotRepository.findByDepotNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrDepotCodeContainingIgnoreCase(keyword, keyword, keyword, unpaged));
        // Employee and Vehicle search now proxied to their external services
        return results;
    }
}
