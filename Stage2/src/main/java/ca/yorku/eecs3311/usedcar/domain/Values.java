package ca.yorku.eecs3311.usedcar.domain;

import java.math.BigDecimal;
import java.util.Locale;

/** Shared domain invariants; presentation layers must not redefine these rules. */
public final class Values {
    private Values() { }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.strip();
    }

    public static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required.");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " must not be negative.");
        return value;
    }

    public static BigDecimal optionalNonNegative(BigDecimal value, String field) {
        return value == null ? null : nonNegative(value, field);
    }

    public static String optionalText(String value) { return value == null ? "" : value.strip(); }

    public static String label(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.strip().toUpperCase(Locale.ROOT);
    }

    public static String transmission(String value) {
        return switch (label(value)) {
            case "AUTO", "AT", "AUTOMATIC" -> "AUTOMATIC";
            case "MT", "MANUAL" -> "MANUAL";
            default -> label(value);
        };
    }
}
