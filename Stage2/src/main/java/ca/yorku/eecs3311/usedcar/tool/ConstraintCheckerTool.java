package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** F07 deterministic checks. UNKNOWN findings are not confirmed violations or passes. */
public final class ConstraintCheckerTool implements Tool {
    public record Input(Vehicle vehicle, UserPreference preference) {
        public Input { Objects.requireNonNull(vehicle); Objects.requireNonNull(preference); }
    }

    @Override public List<ConstraintViolation> execute(Object input) {
        if (!(input instanceof Input request)) throw new IllegalArgumentException("Constraint tool requires ConstraintCheckerTool.Input.");
        return check(request.vehicle(), request.preference());
    }

    public List<ConstraintViolation> check(Vehicle vehicle, UserPreference preference) {
        var results = new ArrayList<ConstraintViolation>();
        if (preference.maxBudget() != null && vehicle.askingPrice().compareTo(preference.maxBudget()) > 0)
            results.add(new ConstraintViolation("Maximum budget", preference.maxBudget().toPlainString(),
                    vehicle.askingPrice().toPlainString(), "CONFLICT"));
        if (preference.maxMileage() != null && vehicle.mileage() > preference.maxMileage())
            results.add(new ConstraintViolation("Maximum mileage", preference.maxMileage().toString(),
                    Integer.toString(vehicle.mileage()), "CONFLICT"));
        if (!preference.acceptedTransmissions().isEmpty()) {
            String actual = vehicle.transmission();
            String expected = String.join(", ", new java.util.TreeSet<>(preference.acceptedTransmissions()));
            if (!UserPreference.SUPPORTED_TRANSMISSIONS.contains(actual))
                results.add(new ConstraintViolation("Transmission", expected, actual, "UNKNOWN"));
            else if (!preference.acceptedTransmissions().contains(actual))
                results.add(new ConstraintViolation("Transmission", expected, actual, "CONFLICT"));
        }
        if (!preference.accidentTolerance().equals("UNKNOWN")) {
            // Only explicit structured statements are classified; free text is not inferred.
            String history = switch (vehicle.accidentHistory()) {
                case "YES", "ACCIDENT_REPORTED", "HAS_ACCIDENTS" -> "YES";
                case "NO", "NONE", "NO_ACCIDENTS", "ACCIDENT_FREE" -> "NO";
                default -> "UNKNOWN";
            };
            if (history.equals("UNKNOWN"))
                results.add(new ConstraintViolation("Accident history", preference.accidentTolerance(),
                        vehicle.accidentHistory(), "UNKNOWN"));
            else if (history.equals("YES") && preference.accidentTolerance().equals("NO_ACCIDENTS"))
                results.add(new ConstraintViolation("Accident history", "NO_ACCIDENTS",
                        vehicle.accidentHistory(), "CONFLICT"));
        }
        return List.copyOf(results);
    }
}
