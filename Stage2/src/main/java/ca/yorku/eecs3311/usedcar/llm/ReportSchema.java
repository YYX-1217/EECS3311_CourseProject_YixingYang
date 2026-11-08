package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.result.*;
import com.google.gson.*;
import java.util.*;

/** Small structural contract, following F08. Evidence values belong in the prompt and Java validator. */
public final class ReportSchema {
    private ReportSchema(){}
    public static String json(ReportEvidence evidence){
        Objects.requireNonNull(evidence);
        var fields=new JsonObject();
        var category=text();var categories=new JsonArray();EvaluationReport.CATEGORIES.forEach(categories::add);
        category.add("enum",categories);fields.add("category",category);
        fields.add("strengths",array());fields.add("risks",array());fields.add("missingInformation",array());
        fields.add("costSummary",text());fields.add("constraintConflicts",array());
        fields.add("inspectionPriorities",array());fields.add("nextSteps",array());fields.add("explanation",text());
        var root=new JsonObject();root.addProperty("type","object");root.add("properties",fields);
        var required=new JsonArray();fields.keySet().forEach(required::add);root.add("required",required);
        root.addProperty("additionalProperties",false);return root.toString();
    }
    private static JsonObject text(){var result=new JsonObject();result.addProperty("type","string");return result;}
    private static JsonObject array(){
        var result=new JsonObject();result.addProperty("type","array");result.add("items",text());return result;
    }
}
