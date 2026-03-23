package com.traffic.management.service;

import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.traffic.management.dto.FareSlabRequest;
import com.traffic.management.dto.FareSlabResponse;
import com.traffic.management.entity.FareSlab;
import com.traffic.management.repository.FareSlabRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FareSlabServiceImpl implements FareSlabService {

    private final FareSlabRepository repository;

    @Override
    public FareSlabResponse create(FareSlabRequest request) {
        FareSlab slab = FareSlab.builder()
                .stageId(request.getStageId())
                .minDistance(request.getMinDistance())
                .maxDistance(request.getMaxDistance())
                .nonAcFare(request.getNonAcFare())
                .acPercentage(request.getAcPercentage())
                .isActive(true)
                .build();
        repository.save(slab);
        return mapToResponse(slab);
    }

    @Override
    public FareSlabResponse update(Long id, FareSlabRequest request) {
        FareSlab slab = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("FareSlab not found"));
        slab.setStageId(request.getStageId());
        slab.setMinDistance(request.getMinDistance());
        slab.setMaxDistance(request.getMaxDistance());
        slab.setNonAcFare(request.getNonAcFare());
        slab.setAcPercentage(request.getAcPercentage());
        repository.save(slab);
        return mapToResponse(slab);
    }

    @Override
    public FareSlabResponse getById(Long id) {
        FareSlab slab = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("FareSlab not found"));
        return mapToResponse(slab);
    }

    @Override
    public java.util.List<FareSlabResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id) {
        FareSlab slab = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("FareSlab not found"));
        slab.setIsActive(false);
        repository.save(slab);
    }

    private FareSlabResponse mapToResponse(FareSlab slab) {
        return FareSlabResponse.builder()
                .id(slab.getId())
                .stageId(slab.getStageId())
                .minDistance(slab.getMinDistance())
                .maxDistance(slab.getMaxDistance())
                .nonAcFare(slab.getNonAcFare())
                .acPercentage(slab.getAcPercentage())
                .isActive(slab.getIsActive())
                .build();
    }
}