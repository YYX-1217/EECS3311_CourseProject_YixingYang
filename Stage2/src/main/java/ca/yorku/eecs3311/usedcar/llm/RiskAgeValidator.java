package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.result.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Targeted English quantified-age checks, not a general natural-language truth verifier. */
public final class RiskAgeValidator {
    private RiskAgeValidator(){}
    private static final String ONES="one|two|three|four|five|six|seven|eight|nine";
    private static final String NUMBER="(?:-?\\d+(?:\\.\\d+)?|(?:twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)(?:[- ](?:"+ONES+"))?|zero|"+ONES+"|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|an?)";
    private static final String LINK="\\s*(?:(?:of|is|equals|:)\\s*)?(?:(?:about|approximately|around)\\s*)?";
    private static final Pattern YEARS_OLD=Pattern.compile("\\b"+NUMBER+"[-\\s]+years?[-\\s]+old\\b");
    private static final Pattern AGE=Pattern.compile("\\b(?:age|aged)"+LINK+NUMBER+"[-\\s]+years?\\b");
    private static final Pattern SERVICE=Pattern.compile("\\b(?:in service|on the road|in use)\\s+(?:for\\s+)?"+NUMBER+"\\s+years?\\b|\\b"+NUMBER+"\\s+years?\\s+(?:in service|on the road|of service|of use)\\b");
    private static final Pattern DIFFERENCE=Pattern.compile("\\bmodel[- ]year (?:difference|gap)"+LINK+"("+NUMBER+")\\s+years?\\b");
    private static final Pattern MODEL=Pattern.compile("\\bmodel[- ]year"+LINK+"(\\d{4})\\b");
    private static final Pattern REFERENCE=Pattern.compile("\\breference[- ]year"+LINK+"(\\d{4})\\b");
    private static final Map<String,Integer> WORDS;
    static {
        var words=new HashMap<String,Integer>();String[] small={"zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen"};
        for(int i=0;i<small.length;i++)words.put(small[i],i);
        String[] tens={"twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety"};
        for(int i=0;i<tens.length;i++)words.put(tens[i],20+10*i);
        words.put("a",1);words.put("an",1);WORDS=Map.copyOf(words);
    }
    /** Fixed field names and reasons only; never include the rejected narrative. */
    public static final class Rejected extends IllegalArgumentException {
        private Rejected(String field,String reason){super("[RISK_AGE_EVIDENCE] "+field+": "+reason);}
    }
    public static void validate(RiskAssessment result,ModelYearContext context){
        checkItems(result.strengths(),"strengths",context);checkItems(result.risks(),"risks",context);
        checkItems(result.uncertainties(),"uncertainties",context);check(result.explanation(),"explanation",context);
    }
    private static void checkItems(List<String> items,String field,ModelYearContext context){
        for(int i=0;i<items.size();i++)check(items.get(i),field+"["+i+"]",context);
    }
    private static void check(String text,String field,ModelYearContext context){
        String normalized=Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\u2010-\\u2015\\u2212]","-").replaceAll("\\s+"," ");
        if(YEARS_OLD.matcher(normalized).find()||AGE.matcher(normalized).find()||SERVICE.matcher(normalized).find())
            throw new Rejected(field,"Numerical vehicle age/time in service is unsupported; model-year difference is not exact age.");
        Integer difference=context==null?null:context.difference();
        verify(DIFFERENCE,normalized,difference,field);
        verify(MODEL,normalized,difference==null?null:context.modelYear(),field);
        verify(REFERENCE,normalized,difference==null?null:context.referenceYear(),field);
    }
    private static void verify(Pattern pattern,String text,Integer expected,String field){
        var matcher=pattern.matcher(text);
        while(matcher.find()){
            if(expected==null||number(matcher.group(1)).compareTo(BigDecimal.valueOf(expected))!=0)
                throw new Rejected(field,"Model/reference-year statement is inconsistent with or unsupported by the Java year context.");
        }
    }
    private static BigDecimal number(String text){
        if(text.matches("-?\\d+(?:\\.\\d+)?"))return new BigDecimal(text);
        int total=0;for(String word:text.split("[- ]"))total+=WORDS.get(word);return BigDecimal.valueOf(total);
    }
}
