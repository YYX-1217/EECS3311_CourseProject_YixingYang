package ca.yorku.eecs3311.usedcar.result;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.*;
import java.math.BigDecimal;

/** Evidence snapshot owned by Java. Provider output can never replace these records. */
public record ReportEvidence(RiskEvidence deterministic,CostAssumptions assumptions,RiskAssessment risk,
        String riskStatus,InspectionChecklist inspection,List<String> clarificationHistory,
        List<String> unresolvedConflicts,int referenceYear) {
    public static final String INSPECTION="Arrange an independent professional pre-purchase inspection before deciding.";
    public static final String VERIFY="Verify seller claims and supporting accident and maintenance records.";
    public static final String REVIEW="Review outstanding uncertainties, buyer constraints and the full ownership-cost assumptions.";
    public ReportEvidence {clarificationHistory=List.copyOf(clarificationHistory);unresolvedConflicts=List.copyOf(unresolvedConflicts);}
    public boolean sufficient(){return deterministic.missing().sufficient()&&deterministic.constraintsAvailable()&&unresolvedConflicts.isEmpty()&&(risk==null||!risk.riskLevel().equals("Insufficient Information"));}
    public boolean strongAllowed(){return sufficient()&&conflicts().isEmpty()&&deterministic.violations().isEmpty()&&risk!=null&&risk.riskLevel().equals("Low")&&deterministic.cost()!=null&&deterministic.cost().firstYearEstimate()!=null;}
    public List<String> conflicts(){return deterministic.requiredRisks();}
    public List<String> strengths(){
        var v=deterministic.vehicle();var p=deterministic.preference();var list=new ArrayList<String>();
        if(p.maxBudget()!=null&&v.askingPrice().compareTo(p.maxBudget())<=0)list.add("The entered asking price is within the saved purchase budget; this is not a market valuation.");
        if(p.maxMileage()!=null&&v.mileage()<=p.maxMileage())list.add("The entered mileage is within the saved mileage limit; this does not establish mechanical condition.");
        if(!p.acceptedTransmissions().isEmpty()&&p.acceptedTransmissions().contains(v.transmission()))list.add("The entered transmission matches a saved accepted transmission.");
        if(v.accidentHistory().equals("NO"))list.add("The saved input reports no accidents; the claim still requires independent evidence.");
        if(v.maintenanceHistory().equals("DOCUMENTED"))list.add("The saved input reports documented maintenance; the records still require review.");
        return List.copyOf(list);
    }
    public List<String> risks(){
        var list=new ArrayList<>(conflicts());var v=deterministic.vehicle();
        if(Set.of("YES","ACCIDENT_REPORTED","HAS_ACCIDENTS").contains(v.accidentHistory()))list.add("The saved input reports an accident history; review repair evidence with an independent inspector.");
        if(Set.of("NONE","PARTIAL").contains(v.maintenanceHistory()))list.add("The saved maintenance information indicates missing or partial records; do not assume complete service history.");
        if(risk!=null&&risk.riskLevel().equals("High"))list.add("The current saved risk assessment is High; this is an AI assessment, not a verified defect.");
        list.add("Mechanical condition has not been independently verified by this application.");return List.copyOf(list);
    }
    public List<String> uncertainties(){
        var list=new LinkedHashSet<>(deterministic.requiredUncertainties());list.addAll(unresolvedConflicts);
        if(!deterministic.preference().preferredBodyType().isBlank())list.add("Preferred body type has not been verified against a structured vehicle body-type field.");
        list.add(riskStatus);list.add("Checklist completion records user progress only, not a professional inspection result.");
        return List.copyOf(list);
    }
    public List<String> priorities(){return inspection.items().stream().map(i->i.priority()+": "+i.action()).distinct().toList();}
    public List<String> nextSteps(){return List.of(INSPECTION,VERIFY,REVIEW);}
    public String costSummary(){
        var c=deterministic.cost();if(c==null)return "Ownership-cost estimate unavailable; no cost values were invented.";
        return "Annual fuel use (L): "+amount(c.annualFuelUse())+"; annual fuel cost (CAD): "+amount(c.annualFuelCost())
            +"; purchase tax (CAD): "+amount(c.purchaseTax())+"; first-year estimate (CAD): "+amount(c.firstYearEstimate())
            +". Uses supplied assumptions; excludes financing, depreciation, registration and other unlisted costs.";
    }
    private static String amount(BigDecimal value){return value==null?"UNAVAILABLE":value.toPlainString();}
    public EvaluationReport insufficient(){return new EvaluationReport("Insufficient Information",List.of(),risks(),uncertainties(),costSummary(),conflicts(),priorities(),nextSteps(),
            "Essential evidence, mandatory checks or conflict resolution is missing, or the saved risk assessment requests more information. No recommendation request was made. Review the unresolved information before deciding.");}
}
