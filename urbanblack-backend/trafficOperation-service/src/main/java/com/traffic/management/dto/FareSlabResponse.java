package com.traffic.management.dto;

public class FareSlabResponse {
    private Long id;
    private Integer stageId;
    private Double minDistance;
    private Double maxDistance;
    private Double nonAcFare;
    private Double acPercentage;
    private Double acFare;
    private Boolean isActive;

    public FareSlabResponse() {}

    public FareSlabResponse(Long id, Integer stageId, Double minDistance, Double maxDistance, Double nonAcFare, Double acPercentage, Boolean isActive) {
        this.id = id;
        this.stageId = stageId;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.nonAcFare = nonAcFare;
        this.acPercentage = acPercentage;
        this.isActive = isActive;
        this.acFare = calculateAcFare(nonAcFare, acPercentage);
    }

    private Double calculateAcFare(Double nonAcFare, Double acPercentage) {
        if (nonAcFare == null || acPercentage == null) return null;
        return nonAcFare + (nonAcFare * acPercentage / 100.0);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getStageId() { return stageId; }
    public void setStageId(Integer stageId) { this.stageId = stageId; }
    public Double getMinDistance() { return minDistance; }
    public void setMinDistance(Double minDistance) { this.minDistance = minDistance; }
    public Double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }
    public Double getNonAcFare() { 
        return nonAcFare; 
    }
    public void setNonAcFare(Double nonAcFare) { 
        this.nonAcFare = nonAcFare; 
        this.acFare = calculateAcFare(this.nonAcFare, this.acPercentage);
    }
    public Double getAcPercentage() { return acPercentage; }
    public void setAcPercentage(Double acPercentage) { 
        this.acPercentage = acPercentage; 
        this.acFare = calculateAcFare(this.nonAcFare, this.acPercentage);
    }
    public Double getAcFare() { return acFare; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public static FareSlabResponseBuilder builder() {
        return new FareSlabResponseBuilder();
    }

    public static class FareSlabResponseBuilder {
        private Long id;
        private Integer stageId;
        private Double minDistance;
        private Double maxDistance;
        private Double nonAcFare;
        private Double acPercentage;
        private Boolean isActive;

        public FareSlabResponseBuilder id(Long id) { this.id = id; return this; }
        public FareSlabResponseBuilder stageId(Integer stageId) { this.stageId = stageId; return this; }
        public FareSlabResponseBuilder minDistance(Double minDistance) { this.minDistance = minDistance; return this; }
        public FareSlabResponseBuilder maxDistance(Double maxDistance) { this.maxDistance = maxDistance; return this; }
        public FareSlabResponseBuilder nonAcFare(Double nonAcFare) { this.nonAcFare = nonAcFare; return this; }
        public FareSlabResponseBuilder acPercentage(Double acPercentage) { this.acPercentage = acPercentage; return this; }
        public FareSlabResponseBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public FareSlabResponse build() {
            return new FareSlabResponse(id, stageId, minDistance, maxDistance, nonAcFare, acPercentage, isActive);
        }
    }

}
