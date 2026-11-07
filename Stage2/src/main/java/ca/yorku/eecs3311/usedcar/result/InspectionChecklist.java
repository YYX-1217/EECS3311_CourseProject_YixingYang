package ca.yorku.eecs3311.usedcar.result;

import java.util.*;

/** Stage1 grouped sections refined to typed, uniquely identified items for local completion. */
public record InspectionChecklist(Map<String,List<InspectionChecklistItem>> sections) {
    public static final List<String> SECTIONS=List.of("Documents and vehicle history","Exterior inspection","Interior inspection",
            "Engine and drivetrain inspection","Road test","Questions to ask the seller");
    public InspectionChecklist {
        if(sections==null || !sections.keySet().equals(new HashSet<>(SECTIONS))) throw new IllegalArgumentException("Checklist requires exactly the six inspection sections.");
        var copy=new LinkedHashMap<String,List<InspectionChecklistItem>>(); var ids=new HashSet<String>();
        for(String section:SECTIONS) {
            var items=sections.get(section);
            if(items==null || items.isEmpty() || items.size()>40) throw new IllegalArgumentException("Checklist sections must contain 1 to 40 items.");
            for(var item:items) if(item==null || !ids.add(item.id())) throw new IllegalArgumentException("Checklist item IDs must be unique.");
            copy.put(section,List.copyOf(items));
        }
        sections=Collections.unmodifiableMap(copy);
    }
    public List<InspectionChecklistItem> items(){return sections.values().stream().flatMap(List::stream).toList();}
}
