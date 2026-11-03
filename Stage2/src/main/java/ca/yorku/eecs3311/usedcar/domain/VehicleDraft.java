package ca.yorku.eecs3311.usedcar.domain;

import java.util.*;

/** Unconfirmed seller claims, with source text and nullable values represented by empty form cells. */
public record VehicleDraft(String source, Map<String,String> values, Map<String,String> evidence,
                           Set<String> uncertainFields) {
    public static final List<String> FIELDS = List.of("year", "make", "model", "trim", "mileage", "price",
            "transmission", "fuelType", "accident", "maintenance", "consumption", "notes");
    public VehicleDraft {
        Objects.requireNonNull(source);
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        evidence = Collections.unmodifiableMap(new LinkedHashMap<>(evidence));
        uncertainFields = Set.copyOf(uncertainFields);
    }
    public boolean unresolved(String field) {
        return uncertainFields.contains(field) || switch (field) {
            case "mileage" -> !"km".equalsIgnoreCase(values.get("mileageUnit"));
            case "price" -> !"CAD".equalsIgnoreCase(values.get("currency"));
            case "consumption" -> !"L/100km".equalsIgnoreCase(values.get("consumptionUnit"));
            default -> false;
        };
    }
    /** Uncertain/noncanonical quantities must be supplied or corrected explicitly, never silently converted. */
    public Map<String,String> formValues() {
        var result = new LinkedHashMap<String,String>();
        for (String key : FIELDS) result.put(key, unresolved(key) ? "" : values.get(key));
        return result;
    }
    public String describe() {
        var out = new StringBuilder("UNCONFIRMED SELLER CLAIMS - not independently verified\n");
        values.forEach((k,v) -> out.append(k).append(": ").append(v.isBlank() ? "MISSING" : v).append("\n"));
        out.append("Uncertain fields: ").append(uncertainFields).append("\nSource excerpts: ").append(evidence);
        return out.append("\nOriginal listing:\n").append(source).toString();
    }
}
