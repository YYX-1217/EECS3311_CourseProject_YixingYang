package ca.yorku.eecs3311.usedcar.result;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import java.util.*;

/** Generation-specific immutable snapshot shared by GUI/CLI. */
public record ChecklistEvaluation(String generationId, Vehicle vehicle, InspectionChecklist checklist, String status,
        String message, String knowledgeStatus, MissingInformationResult missing, Set<String> completed) {
    public ChecklistEvaluation { completed=Set.copyOf(completed); }
    public ChecklistEvaluation withCompletion(String itemId,boolean done) {
        if(checklist.items().stream().noneMatch(i->i.id().equals(itemId))) throw new IllegalArgumentException("Unknown checklist item ID; use checklist-show.");
        var next=new HashSet<>(completed); if(done)next.add(itemId);else next.remove(itemId);
        return new ChecklistEvaluation(generationId,vehicle,checklist,status,message,knowledgeStatus,missing,next);
    }
}
