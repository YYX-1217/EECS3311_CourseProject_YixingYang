package ca.yorku.eecs3311.usedcar.domain;

import java.math.BigDecimal;
import java.time.Year;

/** Confirmed manual record. Missing listing drafts are a separate, planned concern. */
public record Vehicle(String id, int year, String make, String model, String trim,
                      int mileage, BigDecimal askingPrice, String transmission, String fuelType,
                      String accidentHistory, String maintenanceHistory,
                      BigDecimal fuelConsumption, String notes) {
    public Vehicle {
        id = Values.required(id, "Vehicle ID");
        if (year < 1886 || year > Year.now().getValue() + 1)
            throw new IllegalArgumentException("Year must be between 1886 and next calendar year.");
        make = Values.required(make, "Make");
        model = Values.required(model, "Model");
        trim = Values.optionalText(trim);
        if (mileage < 0) throw new IllegalArgumentException("Mileage must not be negative.");
        askingPrice = Values.nonNegative(askingPrice, "Asking price");
        transmission = Values.optionalText(transmission).isEmpty() ? "" : Values.transmission(transmission);
        fuelType = Values.optionalText(fuelType).isEmpty() ? "" : Values.label(fuelType);
        accidentHistory = Values.optionalText(accidentHistory).isEmpty() ? "" : Values.label(accidentHistory);
        maintenanceHistory = Values.optionalText(maintenanceHistory).isEmpty() ? "" : Values.label(maintenanceHistory);
        fuelConsumption = Values.optionalNonNegative(fuelConsumption, "Fuel consumption");
        notes = Values.optionalText(notes);
    }
}
