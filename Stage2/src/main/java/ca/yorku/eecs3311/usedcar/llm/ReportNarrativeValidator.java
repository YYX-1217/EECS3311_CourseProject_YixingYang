package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.domain.Vehicle;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** F12-only checks for explicit history assertions, not a general natural-language fact checker.
 * The other narrative sections are constrained to the actual Java evidence by ResponseParser.
 * A saved DOCUMENTED claim is not independent verification or proof of complete records.
 */
public final class ReportNarrativeValidator {
    private ReportNarrativeValidator() { }
    private static final String HISTORY="(?:maintenance|service) (?:history|records?|documentation)";
    private static final String COPULA="(?:is|are|was|were|remains?|has been|have been)(?: reported as| claimed as)?";
    private static final Pattern NO_ACCIDENT=Pattern.compile(
        "\\bno (?:prior |previous |reported )?accidents?\\b|\\bno accident history\\b|\\bnever (?:been )?(?:in|involved in|had) (?:an? )?(?:accident|collision)\\b|\\b(?:accident|collision)[ -]free\\b");
    private static final Pattern HAS_ACCIDENT=Pattern.compile(
        "\\b(?:reports?|reported|claims?|had|experienced|sustained) (?:an? |any |prior |previous |past )*(?:accidents?|collisions?|accident history)\\b"
        +"|\\b(?:has|have|with) (?:an? )?(?:history of|prior|previous|past) (?:accidents?|collisions?)\\b"
        +"|\\b(?:was|were|been) involved in (?:an? )?(?:accident|collision)\\b"
        +"|\\b(?:prior |previous |past )?(?:accidents?|collisions?) (?:are|were|have been) reported\\b");
    private static final Pattern UNKNOWN_ACCIDENT=Pattern.compile("\\baccident history "+COPULA+" (?:unknown|uncertain|unspecified)\\b");
    private static final Pattern NO_RECORDS=Pattern.compile(
        "\\b(?:undocumented|unrecorded|missing|absent|unavailable|no) "+HISTORY+"\\b"
        +"|\\b"+HISTORY+" "+COPULA+" (?:undocumented|unrecorded|missing|absent|unavailable|not available|not documented)\\b");
    private static final Pattern PARTIAL_RECORDS=Pattern.compile(
        "\\b(?:partial|incomplete|limited) "+HISTORY+"\\b|\\b"+HISTORY+" "+COPULA+" (?:partial|incomplete|limited)\\b");
    private static final Pattern DOCUMENTED_RECORDS=Pattern.compile(
        "\\bdocumented "+HISTORY+"\\b|\\b"+HISTORY+" "+COPULA+" (?:documented|available)\\b");
    private static final Pattern COMPLETE_RECORDS=Pattern.compile(
        "\\b(?:full|complete|comprehensive) "+HISTORY+"\\b|\\b"+HISTORY+" "+COPULA+" (?:full|complete|comprehensive)\\b");
    private static final Pattern UNKNOWN_RECORDS=Pattern.compile(
        "\\b(?:unknown|unspecified|uncertain) "+HISTORY+"\\b|\\b"+HISTORY+" "+COPULA+" (?:unknown|unspecified|uncertain)\\b");
    public static void validate(String explanation,Vehicle vehicle){
        String normalized=Normalizer.normalize(explanation,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
            .replace('\u2019','\'').replaceAll("\\s+"," ");
        String accident=switch(vehicle.accidentHistory()){
            case "NO","NONE","NO_ACCIDENTS","ACCIDENT_FREE"->"NO";
            case "YES","ACCIDENT_REPORTED","HAS_ACCIDENTS"->"YES";
            default->"UNKNOWN";
        };
        String maintenance=vehicle.maintenanceHistory();
        // Scope exceptions to one clause: a verification recommendation cannot excuse a later claim.
        for(String raw:normalized.split("[.!?;,]|\\b(?:but|however|yet)\\b|\\band (?=(?:the |this |it |seller |owner ))")){
            String clause=raw.strip();if(guidance(clause))continue;
            boolean negative=NO_ACCIDENT.matcher(clause).find();
            if(negative&&!accident.equals("NO"))reject("accident history");
            // Remove the negative phrase before checking positive claims (e.g. 'reports no prior accidents').
            if(HAS_ACCIDENT.matcher(NO_ACCIDENT.matcher(clause).replaceAll(" ")).find()&&!accident.equals("YES"))reject("accident history");
            if(UNKNOWN_ACCIDENT.matcher(clause).find()&&!accident.equals("UNKNOWN"))reject("accident history");
            if(NO_RECORDS.matcher(clause).find()&&!maintenance.equals("NONE"))reject("maintenance history");
            if(PARTIAL_RECORDS.matcher(clause).find()&&!maintenance.equals("PARTIAL"))reject("maintenance history");
            if(UNKNOWN_RECORDS.matcher(clause).find()&&!Set.of("UNKNOWN","").contains(maintenance))reject("maintenance history");
            // Strip qualified partial/absent/unknown phrases before looking for 'records are available'.
            String remaining=PARTIAL_RECORDS.matcher(clause).replaceAll(" ");
            remaining=NO_RECORDS.matcher(remaining).replaceAll(" ");remaining=UNKNOWN_RECORDS.matcher(remaining).replaceAll(" ");
            if(DOCUMENTED_RECORDS.matcher(remaining).find()&&!maintenance.equals("DOCUMENTED"))reject("maintenance history");
            // DOCUMENTED does not establish completeness; no saved state justifies this stronger claim.
            if(COMPLETE_RECORDS.matcher(clause).find())reject("maintenance completeness");
        }
    }
    private static boolean guidance(String clause){
        if(clause.matches("^(?:if|unless|whether)\\b.*"))return true;
        if(clause.matches("^(?:ask|check|verify|confirm|determine)\\b.{0,80}\\b(?:whether|if)\\b.*"))return true;
        if(clause.matches("^do not (?:assume|infer)\\b.*"))return true;
        // Merely asking to inspect records is not an assertion that records or accidents exist.
        return clause.matches("^(?:inspect|review|request|obtain|verify|check|ask for)\\b.*")
            &&!clause.matches(".*\\b(?:is|are|was|were|has|have|reports?|claims?|because|since)\\b.*");
    }
    private static void reject(String field){
        throw new IllegalArgumentException("Report explanation contradicts or overstates saved "+field+" claims. Unverified claims must remain distinct from missing evidence.");
    }
}
