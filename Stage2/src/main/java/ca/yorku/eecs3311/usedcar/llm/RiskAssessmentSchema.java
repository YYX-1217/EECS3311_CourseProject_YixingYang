package ca.yorku.eecs3311.usedcar.llm;
import ca.yorku.eecs3311.usedcar.result.RiskAssessment;
import com.google.gson.Gson;
import java.util.*;
public final class RiskAssessmentSchema {
    private RiskAssessmentSchema(){}
    public static String json(){
        var p=new LinkedHashMap<String,Object>();p.put("riskLevel",Map.of("type","string","enum",RiskAssessment.LEVELS));
        for(String k:List.of("strengths","risks","uncertainties"))p.put(k,Map.of("type","array","items",Map.of("type","string")));
        p.put("explanation",Map.of("type","string"));
        return new Gson().toJson(Map.of("type","object","properties",p,"required",p.keySet(),"additionalProperties",false));
    }
}
