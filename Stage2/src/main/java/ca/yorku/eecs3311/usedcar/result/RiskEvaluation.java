package ca.yorku.eecs3311.usedcar.result;
/** Null assessment means no usable AI result, never a fabricated category. */
public record RiskEvaluation(String status, RiskAssessment assessment, RiskEvidence evidence, String message) {
    public RiskAssessment requireAssessment(){
        if(assessment==null)throw new IllegalStateException(message);return assessment;
    }
}
