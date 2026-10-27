package ca.yorku.eecs3311.usedcar.result;

public record ConstraintViolation(String constraint, String expected, String actual, String severity) { }
