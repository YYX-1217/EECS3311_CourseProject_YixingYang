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
    @Test public void transmissionConflict() { FoundationChecks.transmissionConflict(); }
    @Test public void transmissionMatch() { FoundationChecks.transmissionMatch(); }
    @Test public void multipleTransmissions() { FoundationChecks.multipleTransmissions(); }
    @Test public void neutralTransmission() { FoundationChecks.neutralTransmission(); }
    @Test public void unknownTransmission() { FoundationChecks.unknownTransmission(); }
    @Test public void accidentConflict() { FoundationChecks.accidentConflict(); }
    @Test public void acceptableHistory() { FoundationChecks.acceptableHistory(); }
    @Test public void neutralAccidentTolerance() { FoundationChecks.neutralAccidentTolerance(); }
    @Test public void unknownAccidentHistory() { FoundationChecks.unknownAccidentHistory(); }
    @Test public void invalidConstraintPreferences() { FoundationChecks.invalidConstraintPreferences(); }
}
