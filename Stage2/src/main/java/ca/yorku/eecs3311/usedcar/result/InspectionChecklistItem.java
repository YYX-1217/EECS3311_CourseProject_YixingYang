package ca.yorku.eecs3311.usedcar.result;

/** Immutable action; completion is separate session state, never model output. */
public record InspectionChecklistItem(String id, String action, String priority, String reason, String source) {
    public InspectionChecklistItem {
        if(id==null || !id.matches("[a-z][a-z0-9-]{0,63}")) throw new IllegalArgumentException("Invalid checklist item ID.");
        if(action==null || action.isBlank() || action.length()>2000) throw new IllegalArgumentException("Checklist action must be nonblank and bounded.");
        if(priority==null || !java.util.Set.of("High","Medium","Low").contains(priority)) throw new IllegalArgumentException("Checklist priority must be High, Medium or Low.");
        if(reason==null || reason.length()>1000) throw new IllegalArgumentException("Checklist reason must be a bounded string (empty is allowed).");
        if(source==null || source.isBlank() || source.length()>2000) throw new IllegalArgumentException("Checklist source must be nonblank and bounded.");
    }
}
