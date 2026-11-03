package ca.yorku.eecs3311.usedcar.result;

import java.util.*;

/** Stage 1 result. Empty preferred ID means no defensible preference; empty explanation means no AI narrative. */
public record VehicleComparison(String firstVehicleId, String secondVehicleId, Map<String,String> metricSummary,
                                String preferredVehicleId, String explanation) {
    public VehicleComparison {
        if (firstVehicleId == null || firstVehicleId.isBlank() || secondVehicleId == null || secondVehicleId.isBlank()
                || firstVehicleId.equals(secondVehicleId)) throw new IllegalArgumentException("Comparison requires two distinct vehicle IDs.");
        if (preferredVehicleId == null || !(preferredVehicleId.isEmpty() || preferredVehicleId.equals(firstVehicleId)
                || preferredVehicleId.equals(secondVehicleId))) throw new IllegalArgumentException("Preferred vehicle must be A, B, or an empty ID for no preference.");
        if (explanation == null || (!preferredVehicleId.isEmpty() && explanation.isBlank()))
            throw new IllegalArgumentException("A preferred vehicle requires an explanation.");
        Objects.requireNonNull(metricSummary);
        metricSummary.forEach((k,v) -> { if (k == null || k.isBlank() || v == null || v.isBlank())
            throw new IllegalArgumentException("Comparison metrics require nonblank names and values."); });
        metricSummary = Collections.unmodifiableMap(new LinkedHashMap<>(metricSummary));
    }
}
