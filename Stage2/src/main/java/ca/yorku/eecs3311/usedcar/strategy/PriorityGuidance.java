package ca.yorku.eecs3311.usedcar.strategy;

import ca.yorku.eecs3311.usedcar.domain.UserPreference;
import java.util.Objects;
import java.util.TreeSet;

/** Shared factual preference rendering and safeguards, not strategy selection. */
final class PriorityGuidance {
    private PriorityGuidance() { }
    static String budget(UserPreference p) {
        return p.maxBudget() == null ? "Budget is unspecified; do not assume an affordability ceiling."
                : "Compare purchase price with the explicit maximum budget of CAD " + p.maxBudget().toPlainString() + ".";
    }
    static String reliability(UserPreference p) {
        return p.reliabilityPriority() == 0 ? "Reliability importance is unspecified (neutral)."
                : "Apply the stated reliability importance " + p.reliabilityPriority() + "/5 without treating it as vehicle evidence.";
    }
    static String economy(UserPreference p) {
        return p.fuelEconomyPriority() == 0 ? "Fuel-economy importance is unspecified (neutral)."
                : "Apply the stated fuel-economy importance " + p.fuelEconomyPriority() + "/5 when discussing consumption and operating costs.";
    }
    static String finish(UserPreference p) {
        Objects.requireNonNull(p, "Buyer preferences are required");
        return "\n" + (p.maxMileage() == null ? "Mileage preference is unspecified; do not invent a limit."
                : "Retain the maximum mileage constraint of " + p.maxMileage() + " km.")
                + "\n" + (p.acceptedTransmissions().isEmpty() ? "Transmission preference is unspecified (neutral)."
                : "Retain accepted transmissions: " + new TreeSet<>(p.acceptedTransmissions()) + ".")
                + "\n" + (p.preferredBodyType().isEmpty() ? "Body-type preference is unspecified (neutral)."
                : "Retain the stated body-type preference: " + p.preferredBodyType() + "; do not infer a vehicle's body type.")
                + "\n" + switch (p.accidentTolerance()) {
                    case "NO_ACCIDENTS" -> "Preserve the explicit NO_ACCIDENTS constraint; unknown accident history is not a pass.";
                    case "ANY" -> "ANY permits reported accidents as a preference; it does not establish safety or remove risk warnings.";
                    default -> "Accident tolerance is unspecified (neutral); do not infer acceptance or rejection.";
                }
                + "\nFor every mode: retain safety and reliability warnings, maintenance uncertainty and inspection concerns. "
                + "Do not let low cost override known risks or explicit constraints. "
                + "Unknown information remains unknown; do not invent vehicle history, reliability evidence or risk ratings.\n";
    }
    static String heading(String mode) {
        return "PRIORITY GUIDANCE PREVIEW - " + mode + "\n"
                + "Instructions based on saved buyer preferences only; not an AI risk assessment or purchase recommendation.\n";
    }
}
