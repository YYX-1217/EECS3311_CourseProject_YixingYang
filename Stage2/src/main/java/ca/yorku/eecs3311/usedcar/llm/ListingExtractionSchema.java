package ca.yorku.eecs3311.usedcar.llm;
import ca.yorku.eecs3311.usedcar.domain.VehicleDraft;
import com.google.gson.Gson;
import java.util.*;

/** Shared provider-independent JSON Schema for the F02 prompt, request and parser keys. */
public final class ListingExtractionSchema {
    private ListingExtractionSchema() { }
    public static final List<String> UNITS = List.of("mileageUnit", "currency", "consumptionUnit");
    public static Set<String> keys() {
        var keys = new LinkedHashSet<>(VehicleDraft.FIELDS); keys.addAll(UNITS);
        keys.add("uncertainFields"); keys.add("evidence"); return Collections.unmodifiableSet(keys);
    }
    public static String json() {
        var properties = new LinkedHashMap<String,Object>();
        for (String key : VehicleDraft.FIELDS) {
            String type = switch(key) { case "year", "mileage" -> "integer"; case "price", "consumption" -> "number"; default -> "string"; };
            var property = new LinkedHashMap<String,Object>(); property.put("type",List.of(type,"null"));
            if (!type.equals("string")) property.put("minimum",0);
            properties.put(key,property);
        }
        UNITS.forEach(key -> properties.put(key,Map.of("type",List.of("string","null"))));
        properties.put("uncertainFields",Map.of("type","array","items",Map.of("type","string","enum",VehicleDraft.FIELDS)));
        var evidence = new LinkedHashMap<String,Object>();
        VehicleDraft.FIELDS.forEach(key -> evidence.put(key,Map.of("type",List.of("string","null"))));
        properties.put("evidence",Map.of("type","object","properties",evidence,"required",VehicleDraft.FIELDS,"additionalProperties",false));
        return new Gson().toJson(Map.of("type","object","properties",properties,"required",keys(),"additionalProperties",false));
    }
}
