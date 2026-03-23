package com.traffic.management.dto;

public class FareSlabRequest {
    private Integer stageId;
    private Double minDistance;
    private Double maxDistance;
    private Double nonAcFare;
    private Double acPercentage;

    public FareSlabRequest() {}

    public FareSlabRequest(Integer stageId, Double minDistance, Double maxDistance, Double nonAcFare, Double acPercentage) {
        this.stageId = stageId;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.nonAcFare = nonAcFare;
        this.acPercentage = acPercentage;
    }

    public Integer getStageId() { return stageId; }
    public void setStageId(Integer stageId) { this.stageId = stageId; }
    public Double getMinDistance() { return minDistance; }
    public void setMinDistance(Double minDistance) { this.minDistance = minDistance; }
    public Double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }
    public Double getNonAcFare() { return nonAcFare; }
    public void setNonAcFare(Double nonAcFare) { this.nonAcFare = nonAcFare; }
    public Double getAcPercentage() { return acPercentage; }
    public void setAcPercentage(Double acPercentage) { this.acPercentage = acPercentage; }
}
