package ca.yorku.eecs3311.usedcar.ui;

import ca.yorku.eecs3311.usedcar.app.UsedCarApplicationFacade;
import ca.yorku.eecs3311.usedcar.command.PrepareEvaluationCommand;
import ca.yorku.eecs3311.usedcar.domain.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/** Shared field descriptions and input conversion, not calculation logic. */
public final class PreparationForm {
    public record Field(String key, String label, String section) { }
    public static final List<Field> FIELDS = List.of(
            new Field("year", "Year *", "Vehicle"), new Field("make", "Make *", "Vehicle"),
            new Field("model", "Model *", "Vehicle"), new Field("trim", "Trim", "Vehicle"),
            new Field("mileage", "Mileage (km) *", "Vehicle"), new Field("price", "Asking price (CAD) *", "Vehicle"),
            new Field("transmission", "Transmission (blank = missing)", "Vehicle"),
            new Field("fuelType", "Fuel type", "Vehicle"), new Field("accident", "Accident history (YES/NO/UNKNOWN)", "Vehicle"),
            new Field("maintenance", "Maintenance (DOCUMENTED/PARTIAL/NONE/UNKNOWN)", "Vehicle"),
            new Field("consumption", "Fuel consumption (L/100 km)", "Vehicle"), new Field("notes", "Notes", "Vehicle"),
            new Field("budget", "Maximum budget (CAD; blank = neutral)", "Preferences"),
            new Field("maxMileage", "Maximum mileage (km; blank = neutral)", "Preferences"),
            new Field("acceptedTransmissions", "Accepted transmissions (comma-separated; blank = any)", "Preferences"),
            new Field("accidentTolerance", "Accident tolerance (NO_ACCIDENTS/ANY; blank = neutral)", "Preferences"),
            new Field("annualKm", "Annual distance (km) *", "Cost assumptions"),
            new Field("fuelPrice", "Fuel price (CAD/L) *", "Cost assumptions"),
            new Field("taxRate", "Tax fraction (e.g. 0.13; optional)", "Cost assumptions"),
            new Field("maintenanceAllowance", "Annual maintenance allowance (CAD)", "Cost assumptions"),
            new Field("insuranceAllowance", "Annual insurance allowance (CAD)", "Cost assumptions"));

    private PreparationForm() { }

    public static Map<String, String> example() {
        Properties properties = new Properties();
        try (var stream = PreparationForm.class.getResourceAsStream("/example.properties")) {
            if (stream == null) throw new IllegalStateException("Example resource is missing; rebuild the application.");
            properties.load(stream);
        } catch (IOException e) { throw new IllegalStateException("Cannot load example input.", e); }
        var values = new LinkedHashMap<String, String>();
        FIELDS.forEach(field -> values.put(field.key(), properties.getProperty(field.key(), "")));
        return values;
    }

    public static PrepareEvaluationCommand command(UsedCarApplicationFacade facade, Map<String, String> values) {
        Vehicle vehicle = vehicle(facade, values);
        Set<String> transmissions = new HashSet<>(Arrays.asList(values.getOrDefault("acceptedTransmissions", "").split(",")));
        UserPreference preference = new UserPreference(decimal(values, "budget", false), integer(values, "maxMileage", false),
                transmissions, "", 0, 0, values.get("accidentTolerance"));
        CostAssumptions assumptions = new CostAssumptions(decimal(values, "annualKm", true), decimal(values, "fuelPrice", true),
                decimal(values, "taxRate", false), decimal(values, "maintenanceAllowance", false), decimal(values, "insuranceAllowance", false));
        return new PrepareEvaluationCommand(facade, vehicle, preference, assumptions);
    }

    public static Vehicle vehicle(UsedCarApplicationFacade facade, Map<String, String> values) {
        String id = facade.currentVehicle().map(Vehicle::id).orElseGet(() -> UUID.randomUUID().toString());
        Vehicle vehicle = new Vehicle(id, integer(values, "year", true), values.get("make"), values.get("model"),
                values.get("trim"), integer(values, "mileage", true), decimal(values, "price", true),
                values.get("transmission"), values.get("fuelType"), values.get("accident"), values.get("maintenance"),
                decimal(values, "consumption", false), values.get("notes"));
        return vehicle;
    }

    private static BigDecimal decimal(Map<String, String> values, String key, boolean required) {
        String text = values.getOrDefault(key, "").strip();
        if (text.isEmpty() && !required) return null;
        if (!text.matches("[+-]?\\d{1,12}(\\.\\d{1,6})?"))
            throw new IllegalArgumentException(label(key) + ": enter a decimal number (up to 12 integer and 6 fractional digits).");
        return new BigDecimal(text);
    }

    private static Integer integer(Map<String, String> values, String key, boolean required) {
        String text = values.getOrDefault(key, "").strip();
        if (text.isEmpty() && !required) return null;
        try { return Integer.valueOf(text); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(label(key) + ": enter a whole number within the Java integer range."); }
    }

    private static String label(String key) {
        return FIELDS.stream().filter(f -> f.key().equals(key)).findFirst().orElseThrow().label();
    }
}
