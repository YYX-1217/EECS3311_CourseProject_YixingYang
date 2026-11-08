package ca.yorku.eecs3311.usedcar.result;
/** A missing report is an unavailable narrative, never an invented favorable category. */
public record ReportEvaluation(String generationId,String status,EvaluationReport report,ReportEvidence evidence,String message) {
    public EvaluationReport requireReport(){if(report==null)throw new IllegalStateException(message);return report;}
}
