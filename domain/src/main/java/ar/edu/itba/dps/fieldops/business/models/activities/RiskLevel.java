package ar.edu.itba.dps.fieldops.business.models.activities;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME;

    public boolean isAbove(RiskLevel other) {
        return compareTo(other) > 0;
    }

    public RiskLevel next() {
        final var levels = values();
        return levels[Math.min(ordinal() + 1, levels.length - 1)];
    }
}
