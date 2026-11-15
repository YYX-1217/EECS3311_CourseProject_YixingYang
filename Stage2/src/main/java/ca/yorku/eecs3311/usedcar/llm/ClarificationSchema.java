package ca.yorku.eecs3311.usedcar.llm;

import ca.yorku.eecs3311.usedcar.domain.ClarificationField;
import ca.yorku.eecs3311.usedcar.result.ClarificationQuestion;
import com.google.gson.*;
import java.util.*;

/** Bounded Gemini JSON Schema; Java still verifies distinct targets and evidence. */
public final class ClarificationSchema {
    private ClarificationSchema() {}
    public static String json(List<ClarificationField> allowed) {
        if (allowed.isEmpty() || allowed.size() > 3 || new HashSet<>(allowed).size() != allowed.size())
            throw new IllegalArgumentException("Clarification requires 1-3 distinct targets.");
        var alternatives = new JsonArray();
        for (var field : allowed) {
            var fields = new JsonObject();
            fields.add("field", text(List.of(field.key())));
            fields.add("question", text(ClarificationQuestion.wordings(field)));
            fields.add("priority", text(field == ClarificationField.ACCIDENT || field == ClarificationField.MAINTENANCE
                    ? List.of("High") : List.of("High", "Medium", "Low")));
            fields.add("reason", text(List.of(ClarificationQuestion.REASON)));
            alternatives.add(object(fields));
        }
        // anyOf ties each question and priority to its field, rather than independent cross-product enums.
        var item = new JsonObject(); item.add("anyOf", alternatives);
        var array = new JsonObject(); array.addProperty("type", "array"); array.add("items", item);
        array.addProperty("minItems", allowed.size()); array.addProperty("maxItems", allowed.size());
        var properties = new JsonObject(); properties.add("questions", array);
        return object(properties).toString();
    }
    private static JsonObject object(JsonObject properties) {
        var value = new JsonObject(); value.addProperty("type", "object"); value.add("properties", properties);
        var required = new JsonArray(); properties.keySet().forEach(required::add);
        value.add("required", required); value.addProperty("additionalProperties", false); return value;
    }
    private static JsonObject text(List<String> allowed) {
        var value = new JsonObject(); value.addProperty("type", "string");
        var values = new JsonArray(); allowed.forEach(values::add); value.add("enum", values); return value;
    }
}
