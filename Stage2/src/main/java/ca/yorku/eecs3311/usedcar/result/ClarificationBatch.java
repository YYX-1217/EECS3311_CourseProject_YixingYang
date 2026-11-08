package ca.yorku.eecs3311.usedcar.result;
import java.util.*;
public record ClarificationBatch(String status,String message,List<ClarificationQuestion> questions){
    public ClarificationBatch{questions=List.copyOf(questions);if(questions.size()>3||questions.stream().map(ClarificationQuestion::field).distinct().count()!=questions.size())throw new IllegalArgumentException("Clarification requires at most three distinct targets.");}
}
