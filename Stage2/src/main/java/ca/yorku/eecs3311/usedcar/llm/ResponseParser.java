package ca.yorku.eecs3311.usedcar.llm;
import ca.yorku.eecs3311.usedcar.domain.*;
import com.google.gson.*;
import ca.yorku.eecs3311.usedcar.result.*;
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
    public RiskAssessment parseRiskAssessment(String response) {
        final JsonObject root;
        try {
            if(response==null||response.isBlank()||response.length()>64000)throw new IllegalArgumentException();
            root=JSON.fromJson(response,JsonObject.class);
            if(root==null||!root.keySet().equals(Set.of("riskLevel","strengths","risks","uncertainties","explanation")))throw new IllegalArgumentException();
        }catch(RuntimeException e){throw new IllegalArgumentException("Invalid risk JSON: expected exactly the five risk-assessment properties.");}
        String level=riskText(root.get("riskLevel"),"riskLevel");
        String explanation=riskText(root.get("explanation"),"explanation");
        var lists=new ArrayList<List<String>>();
        for(String field:List.of("strengths","risks","uncertainties")){
            if(!root.get(field).isJsonArray())throw new IllegalArgumentException("Invalid risk JSON: "+field+" must be an array.");
            var items=new ArrayList<String>();for(var item:root.getAsJsonArray(field))items.add(riskText(item,field));lists.add(items);
        }
        return new RiskAssessment(level,lists.get(0),lists.get(1),lists.get(2),explanation);
    }
    private static String riskText(JsonElement value,String field){
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||value.getAsString().isBlank())
            throw new IllegalArgumentException("Invalid risk JSON: "+field+" must contain nonblank text.");
        return value.getAsString();
    }
    public RiskAssessment validateRiskAssessment(RiskAssessment assessment,RiskEvidence evidence){
        if(!assessment.risks().containsAll(evidence.requiredRisks()))throw new IllegalArgumentException("Risk response omitted or altered deterministic conflicts.");
        if(!assessment.uncertainties().containsAll(evidence.requiredUncertainties()))throw new IllegalArgumentException("Risk response omitted or altered required uncertainties/source limitations.");
        if(assessment.riskLevel().equals("Low")&&!evidence.requiredRisks().isEmpty())throw new IllegalArgumentException("Low risk contradicts unresolved buyer constraints.");
        if(!evidence.missing().sufficient()&&!assessment.riskLevel().equals("Insufficient Information"))throw new IllegalArgumentException("Risk category contradicts insufficient essential evidence.");
        String narrative=(String.join(" ",assessment.strengths())+" "+assessment.explanation()).toLowerCase(Locale.ROOT);
        if(!evidence.requiredRisks().isEmpty()&&(narrative.contains("no constraint conflicts")||narrative.contains("all constraints satisfied")))
            throw new IllegalArgumentException("Risk narrative contradicts deterministic constraints.");
        if(Set.of("YES","ACCIDENT_REPORTED","HAS_ACCIDENTS").contains(evidence.vehicle().accidentHistory())
                &&(narrative.contains("accident-free")||narrative.contains("no accidents")))
            throw new IllegalArgumentException("Risk narrative contradicts reported accident history.");
        if(Set.of("NONE","PARTIAL").contains(evidence.vehicle().maintenanceHistory())
                &&(narrative.contains("complete service history")||narrative.contains("full maintenance records")))
            throw new IllegalArgumentException("Risk narrative contradicts maintenance evidence.");
        return assessment;
    }
    public VehicleComparison parseComparison(String response) {
        final JsonObject root;
        try {
            if(response==null || response.isBlank() || response.length()>64000) throw new IllegalArgumentException();
            try(var reader=JSON.newJsonReader(new java.io.StringReader(response))) {
                root=readComparisonObject(reader,0);
                if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException();
            }
            if(root==null || !root.keySet().equals(Set.of("firstVehicleId","secondVehicleId","metricSummary","preferredVehicleId","explanation")))
                throw new IllegalArgumentException();
        } catch(RuntimeException | java.io.IOException e) { throw comparisonError("root must be one JSON object with exactly the five comparison properties; duplicate properties are forbidden."); }
        String first=comparisonText(root.get("firstVehicleId"),"firstVehicleId",false);
        String second=comparisonText(root.get("secondVehicleId"),"secondVehicleId",false);
        String preferred=comparisonText(root.get("preferredVehicleId"),"preferredVehicleId",true);
        String explanation=comparisonText(root.get("explanation"),"explanation",false);
        if(first.equals(second)) throw comparisonError("firstVehicleId and secondVehicleId must differ.");
        if(!preferred.isEmpty() && !preferred.equals(first) && !preferred.equals(second))
            throw comparisonError("preferredVehicleId must match one of the two IDs or be empty.");
        if(!root.get("metricSummary").isJsonObject() || root.getAsJsonObject("metricSummary").isEmpty())
            throw comparisonError("metricSummary must be a nonempty object of strings.");
        var metrics=new LinkedHashMap<String,String>();
        for(var entry:root.getAsJsonObject("metricSummary").entrySet()) {
            if(entry.getKey().isBlank()) throw comparisonError("metricSummary contains an empty metric name.");
            metrics.put(entry.getKey(),comparisonText(entry.getValue(),"metricSummary value",false));
        }
        return new VehicleComparison(first,second,metrics,preferred,explanation);
    }
    private static JsonObject readComparisonObject(com.google.gson.stream.JsonReader reader,int depth) throws java.io.IOException {
        if(depth>1) throw comparisonError("nested metric objects are unsupported.");
        var object=new JsonObject(); reader.beginObject();
        while(reader.hasNext()) {
            String key=reader.nextName();
            if(object.has(key)) throw comparisonError("duplicate properties are forbidden.");
            var token=reader.peek();
            if(token==com.google.gson.stream.JsonToken.BEGIN_ARRAY) throw comparisonError("arrays are unsupported.");
            object.add(key,token==com.google.gson.stream.JsonToken.BEGIN_OBJECT ? readComparisonObject(reader,depth+1) : JSON.fromJson(reader,JsonElement.class));
        }
        reader.endObject();return object;
    }
    public VehicleComparison validateComparison(VehicleComparison result, ComparisonEvidence evidence) {
        if(!result.firstVehicleId().equals(evidence.first().vehicle().id()) || !result.secondVehicleId().equals(evidence.second().vehicle().id()))
            throw comparisonError("vehicle IDs do not match the requested ordered pair.");
        if(!result.metricSummary().equals(evidence.metrics())) throw comparisonError("metricSummary changed, omitted or added deterministic metrics.");
        if(!evidence.sufficient() && !result.preferredVehicleId().isEmpty())
            throw comparisonError("preferredVehicleId is unsupported by insufficient evidence.");
        String narrative=result.explanation().toLowerCase(Locale.ROOT);
        boolean conflicts=!evidence.first().requiredRisks().isEmpty() || !evidence.second().requiredRisks().isEmpty();
        if(conflicts && (narrative.contains("both vehicles meet all constraints") || narrative.contains("no constraint conflicts")))
            throw comparisonError("explanation contradicts known buyer constraints.");
        if((reportedAccident(evidence.first()) || reportedAccident(evidence.second()))
                && (narrative.contains("both vehicles are accident-free") || narrative.contains("neither vehicle has reported accidents")))
            throw comparisonError("explanation contradicts reported accident history.");
        if((missingRecords(evidence.first()) || missingRecords(evidence.second())) && narrative.contains("both vehicles have complete service history"))
            throw comparisonError("explanation contradicts maintenance records.");
        return result;
    }
    private static boolean reportedAccident(RiskEvidence e) { return Set.of("YES","ACCIDENT_REPORTED","HAS_ACCIDENTS").contains(e.vehicle().accidentHistory()); }
    private static boolean missingRecords(RiskEvidence e) { return Set.of("NONE","PARTIAL").contains(e.vehicle().maintenanceHistory()); }
    private static String comparisonText(JsonElement value,String field,boolean emptyAllowed) {
        if(value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || (!emptyAllowed && value.getAsString().isBlank())) throw comparisonError(field+" must be "+(emptyAllowed?"a string.":"nonblank text."));
        return value.getAsString();
    }
    private static IllegalArgumentException comparisonError(String reason) {
        return new IllegalArgumentException("Invalid comparison JSON: "+reason); // Fixed diagnostics; no model text/values.
    }
    public InspectionChecklist parseChecklist(String response) {
        try {
            if(response==null||response.isBlank()||response.length()>64000)throw new IllegalArgumentException();
            JsonObject root;
            try(var reader=JSON.newJsonReader(new java.io.StringReader(response))){
                root=readChecklistJson(reader,0).getAsJsonObject();
                if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT)throw new IllegalArgumentException();
            }
            if(!root.keySet().equals(Set.of("sections")))throw new IllegalArgumentException();
            var sections=root.getAsJsonObject("sections");
            if(!sections.keySet().equals(new HashSet<>(InspectionChecklist.SECTIONS)))throw new IllegalArgumentException();
            var result=new LinkedHashMap<String,List<InspectionChecklistItem>>();
            for(String section:InspectionChecklist.SECTIONS){
                var items=new ArrayList<InspectionChecklistItem>();
                for(var value:sections.getAsJsonArray(section)){
                    var item=value.getAsJsonObject();
                    if(!item.keySet().equals(Set.of("id","action","priority","reason","source")))throw new IllegalArgumentException();
                    for(var field:item.entrySet())if(!field.getValue().isJsonPrimitive()||!field.getValue().getAsJsonPrimitive().isString())throw new IllegalArgumentException();
                    items.add(new InspectionChecklistItem(item.get("id").getAsString(),item.get("action").getAsString(),item.get("priority").getAsString(),item.get("reason").getAsString(),item.get("source").getAsString()));
                }
                result.put(section,items);
            }
            return new InspectionChecklist(result);
        }catch(RuntimeException|java.io.IOException e){throw new IllegalArgumentException("Invalid checklist JSON: require six nonempty sections, unique IDs and exactly five typed item fields with supported priorities; duplicate properties are forbidden.");}
    }
    private static JsonElement readChecklistJson(com.google.gson.stream.JsonReader reader,int depth)throws java.io.IOException {
        if(depth>5)throw new IllegalArgumentException();
        switch(reader.peek()){
            case BEGIN_OBJECT: {
                var object=new JsonObject();reader.beginObject();
                while(reader.hasNext()){String key=reader.nextName();if(object.has(key))throw new IllegalArgumentException();object.add(key,readChecklistJson(reader,depth+1));}
                reader.endObject();return object;
            }
            case BEGIN_ARRAY: {
                var array=new JsonArray();reader.beginArray();while(reader.hasNext()){if(array.size()>=40)throw new IllegalArgumentException();array.add(readChecklistJson(reader,depth+1));}reader.endArray();return array;
            }
            case STRING: return new JsonPrimitive(reader.nextString());
            default: throw new IllegalArgumentException();
        }
    }
    public InspectionChecklist validateChecklist(InspectionChecklist result,InspectionChecklist required){
        for(String section:InspectionChecklist.SECTIONS){
            var expected=new LinkedHashMap<String,InspectionChecklistItem>();required.sections().get(section).forEach(i->expected.put(i.id(),i));
            if(result.sections().get(section).size()!=expected.size())throw new IllegalArgumentException("Checklist omitted or added required inspection actions.");
            for(var item:result.sections().get(section)){
                var original=expected.get(item.id());
                if(original==null||!item.action().equals(original.action())||!item.source().equals(original.source())
                        ||priorityRank(item.priority())<priorityRank(original.priority()))
                    throw new IllegalArgumentException("Checklist changed an evidence-grounded action, source, section or minimum priority.");
                String text=item.reason().toLowerCase(Locale.ROOT);
                for(String claim:List.of("accident-free","no accidents","complete service history","full maintenance records","mechanically sound","is reliable","is defective","guaranteed","confirmed defect"))
                    if(text.contains(claim))throw new IllegalArgumentException("Checklist reason contains an unsupported condition/history claim.");
            }
        }
        return result;
    }
    private static int priorityRank(String priority){return switch(priority){case "High"->3;case "Medium"->2;default->1;};}
}
