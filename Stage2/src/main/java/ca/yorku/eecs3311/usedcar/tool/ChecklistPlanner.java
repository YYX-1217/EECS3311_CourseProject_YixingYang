package ca.yorku.eecs3311.usedcar.tool;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import ca.yorku.eecs3311.usedcar.result.*;
import java.util.*;

/** Production fallback and mandatory action plan derived from actual inputs, not a demonstration response. */
public final class ChecklistPlanner {
    public InspectionChecklist build(Vehicle vehicle, VehicleKnowledge knowledge, MissingInformationResult missing) {
        var sections=new LinkedHashMap<String,List<InspectionChecklistItem>>();InspectionChecklist.SECTIONS.forEach(s->sections.put(s,new ArrayList<>()));
        add(sections,0,"documents","Review ownership documents and match the vehicle identity to the seller's records.","High","Confirm the documents refer to the vehicle being offered.","General inspection guidance");
        boolean maintenanceUnknown=missing.missingFields().contains("Maintenance history")||missing.uncertainFields().contains("Maintenance history");
        add(sections,0,"maintenance-records","Request and review maintenance records; identify gaps and ask which scheduled work has evidence.",
                maintenanceUnknown||Set.of("NONE","PARTIAL").contains(vehicle.maintenanceHistory())?"High":"Medium",
                maintenanceUnknown?"Maintenance history is unknown; do not assume records exist.":"A history statement is a claim; verify the supporting records.","Structured maintenance claim / F04 check");
        boolean accidentUnknown=missing.missingFields().contains("Accident history")||missing.uncertainFields().contains("Accident history");
        add(sections,0,"accident-disclosures","Ask for accident disclosures and supporting repair or inspection evidence; compare them with the seller's statements.",
                accidentUnknown||Set.of("YES","ACCIDENT_REPORTED","HAS_ACCIDENTS").contains(vehicle.accidentHistory())?"High":"Medium",
                accidentUnknown?"Accident history is unknown; absence of disclosure is not evidence of absence.":"Verify the disclosed history rather than treating a seller statement as an inspection.","Structured accident claim / F04 check");
        add(sections,1,"exterior","Inspect tires, lights, glass, body panels and visible corrosion; ask a professional about concerning findings.","Medium","This is a general observation task, not a diagnosis.","General inspection guidance");
        add(sections,2,"interior","Check seat belts, seats, controls, warning indicators and included keys.","Medium","Record what works and what requires further inspection.","General inspection guidance");
        add(sections,3,"professional-inspection","Arrange an independent professional pre-purchase inspection of the engine or propulsion system and drivetrain.","High","This checklist does not replace a professional inspection; do not inspect hazardous components yourself.","General inspection guidance");
        String transmission=switch(vehicle.transmission()) {
            case "AUTOMATIC" -> "During an authorized safe test, check automatic engagement and shifting; have unusual behavior assessed professionally.";
            case "MANUAL" -> "During an authorized safe test, check clutch operation and manual gear selection; have unusual behavior assessed professionally.";
            case "CVT" -> "During an authorized safe test, observe CVT engagement and acceleration; have unusual behavior assessed professionally.";
            default -> "Confirm the transmission type and ask a qualified inspector which operation checks apply.";
        };
        add(sections,3,"transmission-check",transmission,"Medium","Transmission-specific guidance does not imply a defect.","Structured transmission claim");
        add(sections,4,"road-test","With permission and only when safe and lawful, observe braking, steering and unusual sounds on a road test; stop if unsafe.","High","An observation is not proof of mechanical condition.","General inspection guidance");
        add(sections,5,"seller-questions","Ask the seller about ownership, reason for sale, unresolved repairs and permission for an independent inspection.","High","Request evidence for answers; unanswered questions remain unresolved.","General inspection guidance");
        var gaps=new LinkedHashSet<>(missing.missingFields());gaps.addAll(missing.uncertainFields());
        for(String field:gaps) {
            if(field.equals("Accident history")||field.equals("Maintenance history"))continue;
            add(sections,5,"missing-"+field.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-"),"Ask the seller to clarify "+field+" and provide supporting evidence.","Medium","F04 identifies this field as missing or uncertain.","F04 missing/uncertain field");
        }
        if(knowledge!=null&&!knowledge.equals(VehicleKnowledge.noData())) {
            int n=0;
            for(String note:knowledge.inspectionNotes())add(sections,5,"local-note-"+(++n),"Discuss this local instructional inspection note with a qualified inspector: "+note,"Medium",
                    "This is a retrieved teaching note, not a verified defect or real-world reliability statistic.",knowledge.sourceLabel());
        }
        return new InspectionChecklist(sections);
    }
    private static void add(Map<String,List<InspectionChecklistItem>> sections,int section,String id,String action,String priority,String reason,String source) {
        sections.get(InspectionChecklist.SECTIONS.get(section)).add(new InspectionChecklistItem(id,action,priority,reason,source));
    }
}
