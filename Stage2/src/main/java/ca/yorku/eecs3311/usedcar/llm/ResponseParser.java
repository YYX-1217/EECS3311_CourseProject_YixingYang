package ca.yorku.eecs3311.usedcar.llm;
import ca.yorku.eecs3311.usedcar.domain.*;
import com.google.gson.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.Year;

public final class ResponseParser {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static IllegalArgumentException invalid(String field, String reason) {
        // Only trusted field paths and fixed reasons, never raw values, excerpts or parser exception messages.
        return new IllegalArgumentException("Invalid listing JSON at " + field + ": " + reason);
    }
    private static void keys(JsonObject object, Set<String> expected, String path) {
        for (String field : expected) if (!object.has(field)) throw invalid(path+"."+field,"required property missing; use null for absent values.");
        if (!expected.containsAll(object.keySet())) throw invalid(path,"unexpected properties; use the extraction schema.");
    }
    /** Partial listing result, not a confirmed Vehicle. Source is required for quotation validation. */
    public VehicleDraft parseVehicle(String response, String source) {
        if (source == null) throw invalid("source","original listing is required.");
        if (response == null || response.isBlank() || response.length()>64000) throw invalid("root","response is empty or too large.");
        String content=response.strip();
        // Tolerate one complete Markdown wrapper, never search for a JSON fragment inside arbitrary prose.
        if(content.startsWith("```json\n") || content.startsWith("```json\r\n") || content.startsWith("```\n") || content.startsWith("```\r\n")) {
            if(!content.endsWith("```")) throw invalid("root","unterminated JSON code fence.");
            content=content.substring(content.indexOf('\n')+1,content.length()-3).strip();
        }
        final JsonElement parsed;
        try { parsed=JSON.fromJson(content,JsonElement.class); }
        catch(RuntimeException e){throw invalid("root","malformed JSON; expected one complete object.");}
        if(parsed==null || !parsed.isJsonObject())throw invalid("root","expected a JSON object.");
        JsonObject root=parsed.getAsJsonObject(); keys(root,ListingExtractionSchema.keys(),"root");
        var values=new LinkedHashMap<String,String>();
        for(String key:ListingExtractionSchema.keys()) {
            if(key.equals("evidence")||key.equals("uncertainFields"))continue;
            var value=root.get(key);
            if(value.isJsonNull()){values.put(key,"");continue;}
            boolean numeric=Set.of("year","mileage","price","consumption").contains(key);
            if(!value.isJsonPrimitive() || (numeric ? !value.getAsJsonPrimitive().isNumber() : !value.getAsJsonPrimitive().isString()))
                throw invalid(key,numeric ? "expected JSON number or null, not a quoted number or object." : "expected string or null.");
            if(numeric){
                try {
                    BigDecimal n=value.getAsBigDecimal();
                    if(n.signum()<0||n.precision()>18||Math.abs(n.scale())>6)throw new ArithmeticException();
                    boolean integer=key.equals("year")||key.equals("mileage");
                    if(integer)n.intValueExact();
                    if(key.equals("year")&&(n.intValueExact()<1886||n.intValueExact()>Year.now().getValue()+1))throw new ArithmeticException();
                    values.put(key,integer?Integer.toString(n.intValueExact()):n.toPlainString());
                }catch(ArithmeticException|NumberFormatException e){throw invalid(key,"invalid numeric range, precision or integer value.");}
            }else values.put(key,value.getAsString().strip());
        }
        if(!root.get("uncertainFields").isJsonArray())throw invalid("uncertainFields","expected array, including [] when empty.");
        var uncertain=new LinkedHashSet<String>();
        for(var item:root.getAsJsonArray("uncertainFields")){
            if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isString()||!VehicleDraft.FIELDS.contains(item.getAsString()))
                throw invalid("uncertainFields","entries must be recognized vehicle field names.");
            uncertain.add(item.getAsString());
        }
        if(!root.get("evidence").isJsonObject())throw invalid("evidence","expected object containing every vehicle field.");
        var quotes=root.getAsJsonObject("evidence");keys(quotes,new LinkedHashSet<>(VehicleDraft.FIELDS),"evidence");
        var evidence=new LinkedHashMap<String,String>();
        for(String key:VehicleDraft.FIELDS){
            var quote=quotes.get(key);String q="";
            if(!quote.isJsonNull()){
                if(!quote.isJsonPrimitive()||!quote.getAsJsonPrimitive().isString())throw invalid("evidence."+key,"expected source quotation string or null.");
                q=quote.getAsString();
                if(q.isBlank()||!source.contains(q))throw invalid("evidence."+key,"quotation must be a nonblank exact substring of the original listing; do not paraphrase.");
            }
            if(!values.get(key).isBlank()&&q.isBlank())throw invalid("evidence."+key,"non-null extracted value requires a source quotation.");
            evidence.put(key,q);
        }
        if(!values.get("transmission").isBlank())values.put("transmission",Values.transmission(values.get("transmission")));
        return new VehicleDraft(source,values,evidence,uncertain);
    }
}
