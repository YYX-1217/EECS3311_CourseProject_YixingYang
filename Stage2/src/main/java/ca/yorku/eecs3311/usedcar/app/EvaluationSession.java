package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.Objects;
import java.util.Optional;

/** Single-user, in-memory context. Comparison and clarification memory remain planned. */
public final class EvaluationSession {
    private Vehicle currentVehicle;
    private UserPreference preference = UserPreference.limits(null, null);

    public void setCurrentVehicle(Vehicle vehicle) { currentVehicle = Objects.requireNonNull(vehicle); }
    public Optional<Vehicle> getCurrentVehicle() { return Optional.ofNullable(currentVehicle); }
    public void setPreference(UserPreference value) { preference = Objects.requireNonNull(value); }
    public UserPreference getPreference() { return preference; }
}
