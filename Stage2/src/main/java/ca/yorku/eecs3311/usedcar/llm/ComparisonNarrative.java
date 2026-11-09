package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.result.*;
import java.math.BigDecimal;
import java.util.*;

/** Evidence-bounded F09 prose. The model selects/orders facts; Java owns their wording and values. */
public final class ComparisonNarrative {
    private ComparisonNarrative() { }
    public static final String NO_PREFERENCE = "No preferred vehicle is identified; review the trade-offs and unresolved evidence.";
    public static final String LIMITED_PREFERENCE = "The preferred vehicle is a limited trade-off choice, not a verified reliability, safety or purchase verdict.";

    public static List<String> required(ComparisonEvidence evidence) {
        var lines = new ArrayList<String>();
        lines.add("Vehicle A and Vehicle B follow the requested ordered pair; seller and user statements are not independently verified.");
        for (String metric : List.of("Asking price (CAD)", "Mileage (km)", "First-year estimate (CAD)",
                "Accident history (claims)", "Maintenance history (claims)", "Consumption (L/100 km)"))
            lines.add(metric + ": " + evidence.metrics().get(metric));
        addSources(lines, "A", evidence.first());
        addSources(lines, "B", evidence.second());
        lines.add("Both vehicles use the same buyer constraints and cost assumptions; missing evidence is not a favorable result.");
        return List.copyOf(lines);
    }
    private static void addSources(List<String> lines, String label, RiskEvidence evidence) {
        var knowledge = evidence.knowledge();
        lines.add("Vehicle " + label + ": " + (knowledge == null || knowledge.equals(VehicleKnowledge.noData())
                ? "local knowledge unavailable; model-specific reliability is unknown."
                : "a local instructional record was retrieved; it is not independently verified reliability research."));
        lines.add("Vehicle " + label + ": " + (!evidence.constraintsAvailable() ? "buyer constraint checks unavailable."
                : evidence.violations().isEmpty() ? "no conflict found by implemented buyer checks; body-type compatibility is not checked."
                : "buyer checks contain conflicts or unknown results; do not treat them as satisfied."));
        // Retain mandatory gaps/limits without copying arbitrary seller notes into the approved prose.
        lines.add("Vehicle " + label + ": inspect the displayed constraint findings, missing information and cost limitations before deciding.");
    }
    public static Map<String,String> advantages(ComparisonEvidence evidence) {
        var out = new LinkedHashMap<String,String>();
        var a = evidence.first().vehicle(); var b = evidence.second().vehicle();
        lower(out, "asking price", "CAD", a.askingPrice(), b.askingPrice(), a.id(), b.id());
        lower(out, "mileage", "km", BigDecimal.valueOf(a.mileage()), BigDecimal.valueOf(b.mileage()), a.id(), b.id());
        lower(out, "fuel consumption", "L/100 km", a.fuelConsumption(), b.fuelConsumption(), a.id(), b.id());
        lower(out, "first-year estimate", "CAD", evidence.first().cost() == null ? null : evidence.first().cost().firstYearEstimate(),
                evidence.second().cost() == null ? null : evidence.second().cost().firstYearEstimate(), a.id(), b.id());
        return Collections.unmodifiableMap(out);
    }
    private static void lower(Map<String,String> out, String metric, String unit, BigDecimal a, BigDecimal b, String aid, String bid) {
        if (a == null || b == null || a.compareTo(b) == 0) return;
        boolean first = a.compareTo(b) < 0;
        out.put("Vehicle " + (first ? "A" : "B") + " has lower " + metric + " by "
                + a.subtract(b).abs().stripTrailingZeros().toPlainString() + " " + unit + "; this alone does not establish a better purchase.", first ? aid : bid);
    }
    public static void validate(VehicleComparison result, ComparisonEvidence evidence) {
        var required = required(evidence);
        var advantages = advantages(evidence);
        var lines = result.explanation().lines().toList();
        var unique = new HashSet<>(lines);
        var allowed = new HashSet<>(required); allowed.addAll(advantages.keySet());
        String decision = result.preferredVehicleId().isEmpty() ? NO_PREFERENCE : LIMITED_PREFERENCE;
        allowed.add(decision);
        if (unique.size() != lines.size() || !allowed.containsAll(lines) || !unique.containsAll(required) || !unique.contains(decision))
            throw new IllegalArgumentException("Invalid comparison JSON: explanation must contain required evidence statements and only approved trade-offs, without duplicates.");
        if (!result.preferredVehicleId().isEmpty()) {
            var selected = result.preferredVehicleId().equals(evidence.first().vehicle().id()) ? evidence.first() : evidence.second();
            boolean supported = lines.stream().anyMatch(line -> result.preferredVehicleId().equals(advantages.get(line)));
            if (!evidence.sufficient() || !selected.violations().isEmpty() || !supported)
                throw new IllegalArgumentException("Invalid comparison JSON: preferredVehicleId lacks a stated measurable advantage or has unresolved buyer constraints.");
        }
    }
}
