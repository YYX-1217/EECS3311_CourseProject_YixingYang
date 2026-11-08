package ca.yorku.eecs3311.usedcar.result;

import java.util.List;

/** One readable UTF-8 representation shared by GUI, CLI and explicit local export. */
public final class ReportText {
    private ReportText(){}
    public static String format(ReportEvaluation value){
        var e=value.evidence();var d=e.deterministic();var v=d.vehicle();
        var text=new StringBuilder("F12 PERSONALIZED RECOMMENDATION / REPORT - ").append(value.status()).append("\n").append(value.message()).append("\n");
        text.append("Vehicle: ").append(v.year()).append(" ").append(v.make()).append(" ").append(v.model()).append(" ").append(v.trim()).append("\n");
        text.append("Advisory category: ").append(value.report()==null?"UNAVAILABLE":value.report().category()).append(value.status().equals("AI_SUCCESS")?" (Gemini interpretation)":" (AI narrative unavailable)").append("\n");
        if(value.report()!=null)text.append("Summary: ").append(value.report().explanation()).append("\n");
        text.append("\nA. DETERMINISTIC EVIDENCE (Java; entered claims are not independently verified)\n");
        text.append("Mileage (km): ").append(v.mileage()).append("; asking price (CAD): ").append(v.askingPrice().toPlainString()).append("\n");
        text.append("Transmission: ").append(v.transmission()).append("; fuel type: ").append(v.fuelType()).append("; fuel consumption (L/100 km): ").append(v.fuelConsumption()==null?"UNAVAILABLE":v.fuelConsumption()).append("\n");
        text.append("Accident-history claim: ").append(v.accidentHistory()).append("; maintenance-history claim: ").append(v.maintenanceHistory()).append("\n");
        text.append(new ModelYearContext(v.year(),e.referenceYear()).description()).append("\n");
        var profile=d.preference();
        text.append("Buyer limits: budget CAD ").append(profile.maxBudget()==null?"unspecified":profile.maxBudget()).append("; mileage km ").append(profile.maxMileage()==null?"unspecified":profile.maxMileage()).append("; accepted transmissions ").append(profile.acceptedTransmissions()).append("\n");
        text.append("Buyer priorities: reliability ").append(profile.reliabilityPriority()).append("/5; fuel economy ").append(profile.fuelEconomyPriority()).append("/5; accident tolerance ").append(profile.accidentTolerance()).append("; preferred body type ").append(profile.preferredBodyType().isBlank()?"unspecified":profile.preferredBodyType()).append("\n");
        String heading="PRIORITY GUIDANCE PREVIEW - ";
        text.append("Selected focus: ").append(d.priorityGuidance().lines().filter(line->line.startsWith(heading)).map(line->line.substring(heading.length())).findFirst().orElse("Saved evaluation strategy")).append("\n");
        text.append("Ownership costs: ").append(e.costSummary()).append("\n");
        var a=e.assumptions();
        if(a==null)text.append("Cost assumptions: UNAVAILABLE\n");
        else text.append("Supplied assumptions: annual km ").append(a.annualKm()).append("; fuel CAD/L ").append(a.fuelPrice()).append("; tax fraction ").append(a.taxRate()==null?"unspecified":a.taxRate()).append("; annual maintenance CAD ").append(a.annualMaintenance()==null?"unspecified":a.annualMaintenance()).append("; annual insurance CAD ").append(a.annualInsurance()==null?"unspecified":a.annualInsurance()).append("\n");
        section(text,"Buyer-constraint conflicts",e.conflicts());
        text.append("Constraint checks available: ").append(d.constraintsAvailable()).append("\n");
        d.violations().stream().filter(finding->!finding.severity().equals("CONFLICT")).forEach(finding->text.append(finding.severity()).append(": ").append(finding.constraint()).append("; expected ").append(finding.expected()).append("; entered ").append(finding.actual()).append("\n"));
        if(d.knowledge()==null)text.append("Local knowledge: UNAVAILABLE\n");
        else text.append("Local knowledge: ").append(d.knowledge().summary()).append(" Source: ").append(d.knowledge().sourceLabel()).append("\n");
        text.append("Prior risk category: ").append(e.risk()==null?"UNAVAILABLE":e.risk().riskLevel()).append(" (AI interpretation, not deterministic fact). ").append(e.riskStatus()).append("\n");
        section(text,"Missing information and source limitations",e.uncertainties());
        section(text,"Inspection priorities (general guidance / attributed local notes)",e.priorities());
        section(text,"Clarification history (user claims/choices; session only)",e.clarificationHistory());
        text.append("\nB. GEMINI-ASSISTED INTERPRETATION / RECOMMENDATION\n");
        var r=value.report();
        if(r==null){text.append("Advisory category: UNAVAILABLE. No AI recommendation or explanation was fabricated.\n");section(text,"Suggested verification steps (Java guidance)",e.nextSteps());}
        else{
            text.append("Advisory category: ").append(r.category()).append(value.status().equals("INSUFFICIENT_INFORMATION")?" (Java sufficiency gate, not AI)":" (AI advisory, not a purchase guarantee)").append("\n");
            section(text,"Main strengths",r.strengths());section(text,"Main risks",r.risks());section(text,"Missing / uncertain information",r.missingInformation());section(text,"Explicit buyer-constraint conflicts",r.constraintConflicts());
            text.append("Cost summary (preserved Java values): ").append(r.costSummary()).append("\n");section(text,"Inspection priorities",r.inspectionPriorities());section(text,"Recommended next steps",r.nextSteps());text.append("Explanation: ").append(r.explanation()).append("\n");
        }
        return text.append("\nAdvisory only. Seller claims and checked checklist boxes do not establish safety, roadworthiness or mechanical condition. Obtain an independent professional inspection.\n").toString();
    }
    private static void section(StringBuilder text,String title,List<String> values){text.append(title).append(":\n");if(values.isEmpty())text.append("- None recorded; not proof of absence.\n");else values.forEach(v->text.append("- ").append(v).append("\n"));}
}
