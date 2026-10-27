package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import ca.yorku.eecs3311.usedcar.result.MissingInformationResult;
import java.util.ArrayList;
import java.util.List;

/** F04 initial slice. No free-text interpretation, draft handling, or source conflict resolution yet. */
public final class DataQualityChecker {
    public MissingInformationResult check(Vehicle vehicle) {
        var missing = new ArrayList<String>();
        if (vehicle.accidentHistory().equals("UNKNOWN")) missing.add("Accident history");
        if (vehicle.maintenanceHistory().equals("UNKNOWN")) missing.add("Maintenance history");
        if (vehicle.transmission().equals("UNKNOWN")) missing.add("Transmission");
        if (vehicle.fuelType().equals("UNKNOWN")) missing.add("Fuel type");
        if (vehicle.fuelConsumption() == null) missing.add("Fuel consumption");
        // Conservative readiness flag only. Even true is not an AI or mechanical assessment.
        return new MissingInformationResult(missing, List.of(), List.of(), missing.isEmpty());
    }
}
