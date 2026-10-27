package ca.yorku.eecs3311.usedcar;

import org.junit.Test;

public final class FoundationTest {
    @Test public void rejectsInvalidNumericInput() { FoundationChecks.numericValidation(); }
    @Test public void validatesRequiredFieldsAndYear() { FoundationChecks.requiredFieldsAndYear(); }
    @Test public void calculatesTransparentCosts() { FoundationChecks.exactCostCalculation(); }
    @Test public void doesNotInventOptionalCosts() { FoundationChecks.omittedAssumptions(); }
    @Test public void preservesUnknownFuelConsumption() { FoundationChecks.unknownConsumption(); }
    @Test public void checksConstraintBoundaries() { FoundationChecks.constraintBoundaries(); }
    @Test public void handlesZeroAndRounding() { FoundationChecks.zeroAndRounding(); }
    @Test public void preservesSessionOnRejectedEdit() { FoundationChecks.sessionAndInvalidUpdate(); }
    @Test public void checksToolsAndUnavailableLlm() { FoundationChecks.toolContractsAndLlmBoundary(); }
    @Test public void runsInteractiveCliWorkflow() { FoundationChecks.cliFlow(); }
}
