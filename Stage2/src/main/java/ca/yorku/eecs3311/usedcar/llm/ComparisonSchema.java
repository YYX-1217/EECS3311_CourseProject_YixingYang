package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.result.ComparisonEvidence;
import com.google.gson.*;

/** Uses the same documented structured JSON mechanism as F02/F08. */
public final class ComparisonSchema {
    private ComparisonSchema() { }
    public static String json(ComparisonEvidence evidence) {
        var properties = new JsonObject();
        properties.add("firstVehicleId",text(evidence.first().vehicle().id()));
        properties.add("secondVehicleId",text(evidence.second().vehicle().id()));
        properties.add("preferredVehicleId",text("",evidence.first().vehicle().id(),evidence.second().vehicle().id()));
        properties.add("explanation",text());
        var metrics = new JsonObject();
        evidence.metrics().forEach((key,value)->metrics.add(key,text(value)));
        properties.add("metricSummary",object(metrics));
        return object(properties).toString();
    }
    private static JsonObject text(String... values) {
        var result = new JsonObject(); result.addProperty("type","string");
        if(values.length>0) { var allowed=new JsonArray(); for(String v:values)allowed.add(v); result.add("enum",allowed); }
        return result;
    }
    private static JsonObject object(JsonObject properties) {
        var result=new JsonObject(); result.addProperty("type","object"); result.add("properties",properties);
        var required=new JsonArray(); properties.keySet().forEach(required::add); result.add("required",required);
        result.addProperty("additionalProperties",false); return result;
    }
}
