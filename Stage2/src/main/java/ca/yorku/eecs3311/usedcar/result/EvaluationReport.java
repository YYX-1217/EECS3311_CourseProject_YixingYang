package ca.yorku.eecs3311.usedcar.result;

import java.util.*;

/** Stage 1 report sections; deterministic constraints are explicit rather than buried in prose. */
public record EvaluationReport(String category,List<String> strengths,List<String> risks,
        List<String> missingInformation,String costSummary,List<String> constraintConflicts,
        List<String> inspectionPriorities,List<String> nextSteps,String explanation) {
    public static final List<String> CATEGORIES=List.of("Strong Candidate","Consider with Caution","High Risk","Insufficient Information");
    public EvaluationReport {
        if(!CATEGORIES.contains(category))throw new IllegalArgumentException("Unsupported report category.");
        strengths=items(strengths);risks=items(risks);missingInformation=items(missingInformation);
        constraintConflicts=items(constraintConflicts);inspectionPriorities=items(inspectionPriorities);nextSteps=items(nextSteps);
        if(costSummary==null||costSummary.isBlank()||costSummary.length()>6000)throw new IllegalArgumentException("Report cost summary is required and bounded.");
        if(explanation==null||explanation.isBlank()||explanation.length()>1800||explanation.chars().anyMatch(c->Character.isISOControl(c)&&c!='\n'))throw new IllegalArgumentException("Report explanation is required and bounded.");
        if(inspectionPriorities.isEmpty()||nextSteps.isEmpty())throw new IllegalArgumentException("Report inspection priorities and next steps are required.");
    }
    private static List<String> items(List<String> values){
        if(values==null||values.size()>64)throw new IllegalArgumentException("Report section must be an array of at most sixty-four items.");
        for(var value:values)if(value==null||value.isBlank()||value.length()>2500)throw new IllegalArgumentException("Report section items must be nonblank bounded strings.");
        if(new HashSet<>(values).size()!=values.size())throw new IllegalArgumentException("Duplicate report section item.");return List.copyOf(values);
    }
}
