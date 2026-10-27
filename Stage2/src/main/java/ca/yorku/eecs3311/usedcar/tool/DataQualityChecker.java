package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import ca.yorku.eecs3311.usedcar.result.MissingInformationResult;
import java.math.BigDecimal;
import java.util.*;

/** Deterministic F04 checks on structured records; never infers facts from seller prose. */
public final class DataQualityChecker {
    // Numeric core fields are mandatory and validated by Vehicle construction.
    private static final Set<String> ESSENTIAL = Set.of("Year", "Make", "Model", "Mileage", "Asking price",
            "Accident history", "Maintenance history");
    private static final Set<String> UNKNOWN = Set.of("UNKNOWN", "UNCERTAIN", "UNSURE", "N/A", "NOT KNOWN", "NOT PROVIDED");

    public MissingInformationResult check(Vehicle vehicle) {
        var missing = new ArrayList<String>();
        var uncertain = new ArrayList<String>();
        fields(vehicle).forEach((field, value) -> {
            if (value == null || value instanceof String text && text.isBlank()) missing.add(field);
            else if (known(field, value) == null) uncertain.add(field);
        });
        boolean sufficient = missing.stream().noneMatch(ESSENTIAL::contains)
                && uncertain.stream().noneMatch(ESSENTIAL::contains);
        return new MissingInformationResult(missing, uncertain, List.of(), sufficient);
    }

    /** Reports both sources without merging them or modifying either immutable record. */
    public MissingInformationResult checkConflict(Vehicle manualVehicle, Vehicle extractedVehicle) {
        var manual = check(manualVehicle);
        var extracted = check(extractedVehicle);
        var missing = new ArrayList<String>();
        var uncertain = new ArrayList<String>();
        manual.missingFields().forEach(f -> missing.add("Manual: " + f));
        extracted.missingFields().forEach(f -> missing.add("Extracted: " + f));
        manual.uncertainFields().forEach(f -> uncertain.add("Manual: " + f));
        extracted.uncertainFields().forEach(f -> uncertain.add("Extracted: " + f));
        var conflicts = new ArrayList<String>();
        var second = fields(extractedVehicle);
        fields(manualVehicle).forEach((field, value) -> {
            Object left = known(field, value), right = known(field, second.get(field));
            if (left != null && right != null && !left.equals(right))
                conflicts.add(field + " | manual=" + value + " | extracted=" + second.get(field));
        });
        return new MissingInformationResult(missing, uncertain, conflicts,
                manual.sufficient() && extracted.sufficient() && conflicts.isEmpty());
    }

    private static Map<String, Object> fields(Vehicle v) {
        Objects.requireNonNull(v, "Vehicle is required");
        var fields = new LinkedHashMap<String, Object>();
        fields.put("Year", v.year()); fields.put("Make", v.make()); fields.put("Model", v.model());
        fields.put("Trim", v.trim()); fields.put("Mileage", v.mileage()); fields.put("Asking price", v.askingPrice());
        fields.put("Transmission", v.transmission()); fields.put("Fuel type", v.fuelType());
        fields.put("Accident history", v.accidentHistory()); fields.put("Maintenance history", v.maintenanceHistory());
        fields.put("Fuel consumption", v.fuelConsumption());
        return fields;
    }

    private static Object known(String field, Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal number) return number.stripTrailingZeros();
        if (!(value instanceof String text)) return value;
        String normalized = text.strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || UNKNOWN.contains(normalized)) return null;
        return switch (field) {
            case "Accident history" -> switch (normalized) {
                case "YES", "ACCIDENT_REPORTED", "HAS_ACCIDENTS" -> "YES";
                case "NO", "NONE", "NO_ACCIDENTS", "ACCIDENT_FREE" -> "NO";
                default -> null;
            };
            case "Maintenance history" -> switch (normalized) {
                case "DOCUMENTED", "PARTIAL", "NONE" -> normalized;
                default -> null;
            };
            case "Transmission" -> switch (normalized) {
                case "AUTOMATIC", "MANUAL", "CVT" -> normalized;
                default -> null;
            };
            default -> normalized;
        };
    }
}
