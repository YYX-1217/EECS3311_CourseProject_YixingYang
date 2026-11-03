package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import ca.yorku.eecs3311.usedcar.domain.VehicleDraft;
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

    /** F04 rules applied to partial draft fields without inventing a confirmed Vehicle. */
    public MissingInformationResult check(VehicleDraft draft, Vehicle baseline) {
        var labels = List.of("Year","Make","Model","Trim","Mileage","Asking price","Transmission","Fuel type","Accident history","Maintenance history","Fuel consumption");
        var missing = new ArrayList<String>(); var uncertain = new ArrayList<String>(); var conflicts = new ArrayList<String>();
        Map<String,Object> previous = baseline == null ? Map.of() : fields(baseline);
        for (int i=0; i<labels.size(); i++) {
            String key = VehicleDraft.FIELDS.get(i), label = labels.get(i), raw = draft.values().get(key);
            if (raw.isBlank()) { missing.add(label); if (draft.uncertainFields().contains(key)) uncertain.add(label); continue; }
            Object value = switch(key) {
                case "year", "mileage" -> Integer.valueOf(raw);
                case "price", "consumption" -> new BigDecimal(raw);
                default -> raw;
            };
            Object normalized = known(label,value);
            if (draft.unresolved(key) || normalized == null) { uncertain.add(label); continue; }
            Object prior = known(label,previous.get(label));
            if (prior != null && !prior.equals(normalized)) conflicts.add(label + " | manual=" + previous.get(label) + " | extracted=" + raw);
        }
        return new MissingInformationResult(missing,uncertain,conflicts,
                missing.stream().noneMatch(ESSENTIAL::contains) && uncertain.stream().noneMatch(ESSENTIAL::contains) && conflicts.isEmpty());
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
