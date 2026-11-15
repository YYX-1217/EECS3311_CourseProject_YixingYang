package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.result.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** F08-only guard for specific unsupported assertion families, not a general English fact checker.
 * Current tool evidence supplies no verified reliability statistics or mechanical diagnosis.
 * Exact source/user quotations remain unverified; a source label alone is never evidence.
 */
public final class RiskGroundingValidator {
    private RiskGroundingValidator(){}
    public static final class Rejected extends IllegalArgumentException {
        private Rejected(String field,String reason){
            super("[RISK_GROUNDING] "+field+": "+reason+". Use inspection guidance or an exact, explicitly unverified source quotation.");
        }
    }
    private static final Pattern STATISTICAL=Pattern.compile(
            "\\bstatistically (?:more|less|highly|very) likely\\b|\\bstatistical(?:ly)? (?:failure )?(?:probability|likelihood|risk)\\b");
    private static final String FAILURE="(?:(?:engine|transmission|mechanical) )?(?:failure|breakdown)";
    private static final String PROBABILITY="(?:probability|chance|risk|likelihood) of "+FAILURE;
    private static final Pattern QUANTIFIED=Pattern.compile(
            "\\b(?:failure (?:rate|probability|risk)|breakdown (?:rate|probability|risk)|reliability(?: rating)?|"+PROBABILITY+")"
            +"\\s*(?:(?:is |of |at |:|=)|(?:is )?(?:rated|estimated) at )?\\s*\\d+(?:\\.\\d+)?"
            +"|\\b\\d+(?:\\.\\d+)?\\s*(?:%|percent)\\s+(?:"+PROBABILITY+"|"+FAILURE+"|reliability)\\b");
    private static final Pattern DOCUMENTED=Pattern.compile(
            "\\b(?:documented|known|proven|confirmed)\\b.{0,80}\\b(?:defects?|failures?|mechanical problems?|engine problems?|transmission problems?)\\b");
    private static final Pattern BREAKDOWN=Pattern.compile(
            "\\b(?:engine|transmission|vehicle|car)\\b.{0,60}\\bwill (?:fail|break down)\\b|\\b(?:imminent|inevitable) (?:mechanical )?(?:failure|breakdown)\\b");
    private static final Pattern SOURCE=Pattern.compile(
            "\\b(?:csv|local (?:knowledge|record|dataset))\\b.{0,60}\\b(?:confirms?|shows?|proves?|documents?|establishes?|reports?|indicates?)\\b|\\baccording to\\b.{0,50}\\b(?:csv|local (?:knowledge|record|dataset))\\b");
    // A narrowly scoped denial, not a blanket exemption for words such as 'not' or 'unknown'.
    // Contrast/conjunction boundaries are checked separately so one denial cannot excuse another assertion.
    private static final Pattern DENIAL=Pattern.compile(
            "^(?:(?:the )?available (?:information|data|evidence) (?:does not|cannot|can't) (?:establish|support|confirm|prove)\\b.*"
            +"|(?:there is )?no (?:statistical )?(?:evidence|source|data|statistics)\\b.*"
            +"|(?:the )?(?:statistical )?(?:failure probability|failure rate|reliability statistics|probability|likelihood|risk) (?:is|are|remains?) (?:unknown|unavailable|not established))$");
    public static void validate(RiskAssessment assessment,RiskEvidence evidence){
        checkItems("strengths",assessment.strengths(),evidence);
        checkItems("risks",assessment.risks(),evidence);
        checkItems("uncertainties",assessment.uncertainties(),evidence);
        check("explanation",assessment.explanation(),evidence);
    }
    private static void checkItems(String field,List<String> items,RiskEvidence evidence){
        for(int i=0;i<items.size();i++)check(field+"["+i+"]",items.get(i),evidence);
    }
    private static void check(String field,String text,RiskEvidence evidence){
        String value=text.strip();
        if(value.startsWith("Local knowledge (unverified; source: ")){
            var knowledge=evidence.knowledge();
            if(knowledge!=null){
                var quotes=new ArrayList<>(knowledge.commonIssues());quotes.addAll(knowledge.inspectionNotes());quotes.add(knowledge.summary());
                String prefix="Local knowledge (unverified; source: "+knowledge.sourceLabel()+"): ";
                if(quotes.stream().anyMatch(q->value.equals(prefix+q)))return;
            }
            throw new Rejected(field,"local-source quotation does not match supplied evidence");
        }
        if(value.startsWith("User-provided unverified claim: ")){
            if(evidence.vehicle()!=null&&!evidence.vehicle().notes().isBlank()
                    &&value.equals("User-provided unverified claim: "+evidence.vehicle().notes()))return;
            throw new Rejected(field,"user quotation does not match saved notes");
        }
        String normalized=Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
        // Do not split decimal percentages at the decimal point.
        for(String clause:normalized.split("[;!?]|(?<!\\d)\\.|\\.(?!\\d)|\\b(?:but|however|yet|and|nevertheless)\\b")){
            String part=clause.strip().replaceAll("[, :]+$","");
            if(DENIAL.matcher(part).matches())continue;
            if(STATISTICAL.matcher(part).find()||QUANTIFIED.matcher(part).find())
                throw new Rejected(field,"unsupported statistical or numerical reliability assertion");
            if(DOCUMENTED.matcher(part).find()||BREAKDOWN.matcher(part).find())
                throw new Rejected(field,"unsupported mechanical finding or breakdown prediction");
            if(SOURCE.matcher(part).find())
                throw new Rejected(field,"factual CSV attribution requires a matching unverified quotation");
        }
    }
}
