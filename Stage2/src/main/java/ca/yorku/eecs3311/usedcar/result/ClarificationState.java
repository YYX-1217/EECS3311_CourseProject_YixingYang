package ca.yorku.eecs3311.usedcar.result;
import ca.yorku.eecs3311.usedcar.domain.*;
import java.util.*;
public record ClarificationState(String cycleId,Vehicle vehicle,ClarificationBatch batch,
        Map<ClarificationField,ClarificationResponse> responses,List<String> history){
    public ClarificationState{responses=Collections.unmodifiableMap(new LinkedHashMap<>(responses));history=List.copyOf(history);}
    public boolean settled(){return responses.values().stream().noneMatch(r->r.status().equals("PENDING")||r.status().equals("CONFLICT"));}
}
