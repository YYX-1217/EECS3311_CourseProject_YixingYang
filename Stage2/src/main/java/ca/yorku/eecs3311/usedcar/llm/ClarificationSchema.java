package ca.yorku.eecs3311.usedcar.llm;
import ca.yorku.eecs3311.usedcar.domain.ClarificationField;
import com.google.gson.*;
import java.util.*;
public final class ClarificationSchema {
    private ClarificationSchema(){}
    public static String json(List<ClarificationField> allowed){
        var fields=new JsonObject();fields.add("field",text(allowed.stream().map(ClarificationField::key).toList()));fields.add("question",text(allowed.stream().flatMap(f->ca.yorku.eecs3311.usedcar.result.ClarificationQuestion.wordings(f).stream()).toList()));fields.add("priority",text(List.of("High","Medium","Low")));fields.add("reason",text(List.of(ca.yorku.eecs3311.usedcar.result.ClarificationQuestion.REASON)));
        var item=new JsonObject();item.addProperty("type","object");item.add("properties",fields);var required=new JsonArray();fields.keySet().forEach(required::add);item.add("required",required);item.addProperty("additionalProperties",false);
        var array=new JsonObject();array.addProperty("type","array");array.add("items",item);array.addProperty("minItems",1);array.addProperty("maxItems",3);
        var properties=new JsonObject();properties.add("questions",array);var root=new JsonObject();root.addProperty("type","object");root.add("properties",properties);var keys=new JsonArray();keys.add("questions");root.add("required",keys);root.addProperty("additionalProperties",false);return root.toString();
    }
    private static JsonObject text(List<String> allowed){var value=new JsonObject();value.addProperty("type","string");if(!allowed.isEmpty()){var values=new JsonArray();allowed.forEach(values::add);value.add("enum",values);}return value;}
}
