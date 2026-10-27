package ca.yorku.eecs3311.usedcar.domain;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

/** Null limits / empty sets mean unspecified, never an inferred buyer preference. */
public record UserPreference(BigDecimal maxBudget, Integer maxMileage, Set<String> acceptedTransmissions,
                             String preferredBodyType, int reliabilityPriority,
                             int fuelEconomyPriority, String accidentTolerance) {
    public static final Set<String> SUPPORTED_TRANSMISSIONS = Set.of("AUTOMATIC", "MANUAL", "CVT");

    public UserPreference {
        maxBudget = Values.optionalNonNegative(maxBudget, "Maximum budget");
        if (maxMileage != null && maxMileage < 0)
            throw new IllegalArgumentException("Maximum mileage must not be negative.");
        acceptedTransmissions = acceptedTransmissions == null ? Set.of()
                : acceptedTransmissions.stream().map(Values::transmission).filter(value -> !value.equals("UNKNOWN"))
                    .collect(Collectors.toUnmodifiableSet());
        if (!SUPPORTED_TRANSMISSIONS.containsAll(acceptedTransmissions))
            throw new IllegalArgumentException("Accepted transmissions must be AUTOMATIC, MANUAL or CVT (comma-separated).");
        preferredBodyType = Values.optionalText(preferredBodyType);
        if (reliabilityPriority < 0 || reliabilityPriority > 5 || fuelEconomyPriority < 0 || fuelEconomyPriority > 5)
            throw new IllegalArgumentException("Priority must be 0 (neutral) to 5.");
        accidentTolerance = Values.label(accidentTolerance);
        if (!Set.of("UNKNOWN", "ANY", "NO_ACCIDENTS").contains(accidentTolerance))
            throw new IllegalArgumentException("Accident tolerance must be NO_ACCIDENTS, ANY, or blank (neutral).");
    }

    public static UserPreference limits(BigDecimal budget, Integer mileage) {
        return new UserPreference(budget, mileage, Set.of(), "", 0, 0, "UNKNOWN");
    }
}
