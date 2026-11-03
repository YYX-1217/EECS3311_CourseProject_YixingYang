package ca.yorku.eecs3311.usedcar.result;

/** Status/evidence wrapper retains the original comparison result on insufficient/provider/stale paths. */
public record ComparisonEvaluation(String status, VehicleComparison comparison, ComparisonEvidence evidence, String message) { }
