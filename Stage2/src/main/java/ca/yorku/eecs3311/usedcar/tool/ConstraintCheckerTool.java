package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.*;
import ca.yorku.eecs3311.usedcar.result.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** F07 initial slice: budget and mileage only. Transmission/history rules remain planned. */
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
        return List.copyOf(results);
    }
}
