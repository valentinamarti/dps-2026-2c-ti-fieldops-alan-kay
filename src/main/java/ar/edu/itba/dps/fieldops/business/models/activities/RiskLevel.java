package ar.edu.itba.dps.fieldops.business.models.activities;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public boolean isAbove(RiskLevel other) {
        return compareTo(other) > 0;
    }
}
