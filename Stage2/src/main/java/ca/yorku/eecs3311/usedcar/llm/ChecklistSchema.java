package ca.yorku.eecs3311.usedcar.llm;

import com.google.gson.*;
import ca.yorku.eecs3311.usedcar.result.InspectionChecklist;

public final class ChecklistSchema {
    private ChecklistSchema() { }
    public static String json() {
        var fields=new JsonObject();
        for(String field:new String[]{"id","action","priority","reason","source"}) {
            var value=new JsonObject();value.addProperty("type","string");
            if(field.equals("priority")){var values=new JsonArray();for(String p:new String[]{"High","Medium","Low"})values.add(p);value.add("enum",values);}
            fields.add(field,value);
        }
        var sections=new JsonObject();
        for(String section:InspectionChecklist.SECTIONS){var array=new JsonObject();array.addProperty("type","array");array.add("items",object(fields));array.addProperty("minItems",1);array.addProperty("maxItems",40);sections.add(section,array);}
        var root=new JsonObject();root.add("sections",object(sections));return object(root).toString();
    }
    private static JsonObject object(JsonObject fields){
        var object=new JsonObject();object.addProperty("type","object");object.add("properties",fields);
        var required=new JsonArray();fields.keySet().forEach(required::add);object.add("required",required);object.addProperty("additionalProperties",false);return object;
    }
}
