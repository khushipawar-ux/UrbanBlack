package com.traffic.management.service;

import com.traffic.management.dto.FareSlabRequest;
import com.traffic.management.dto.FareSlabResponse;

public interface FareSlabService {
    FareSlabResponse create(FareSlabRequest request);
    FareSlabResponse update(Long id, FareSlabRequest request);
    FareSlabResponse getById(Long id);
    java.util.List<FareSlabResponse> getAll();
    void deactivate(Long id);
}
