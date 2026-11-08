package ca.yorku.eecs3311.usedcar.result;
import ca.yorku.eecs3311.usedcar.domain.ClarificationField;
import java.util.*;
public record ClarificationQuestion(ClarificationField field,String question,String priority,String reason){
    public ClarificationQuestion{
        Objects.requireNonNull(field);
        if(question==null||question.isBlank()||question.length()>400||!question.endsWith("?"))throw new IllegalArgumentException("Clarification question must be concise and end with a question mark.");
        if(!Set.of("High","Medium","Low").contains(priority))throw new IllegalArgumentException("Unsupported clarification priority.");
        if(reason==null||reason.length()>300)throw new IllegalArgumentException("Clarification reason must be a bounded string.");
    }
    public String id(){return field.key();}
    public static List<String> wordings(ClarificationField field){return List.of(
            "What is the vehicle's "+field.label().toLowerCase(Locale.ROOT)+" ("+field.hint()+")?",
            "Can you confirm the vehicle's "+field.label().toLowerCase(Locale.ROOT)+" ("+field.hint()+")?");}
    public static final String REASON="Please review supporting evidence; skip or mark unknown if you do not know.";
    public static ClarificationQuestion fallback(ClarificationField field){return new ClarificationQuestion(field,"What is the vehicle's "+field.label().toLowerCase(Locale.ROOT)+" ("+field.hint()+")?",field==ClarificationField.ACCIDENT||field==ClarificationField.MAINTENANCE?"High":"Medium",REASON);}
}
