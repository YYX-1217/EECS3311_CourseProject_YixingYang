package ca.yorku.eecs3311.usedcar.domain;

import java.math.BigDecimal;
import java.util.*;

/** Closed allowlist: a model target can never name arbitrary session properties. */
public enum ClarificationField {
    ACCIDENT("accident","Accident history","YES or NO"), MAINTENANCE("maintenance","Maintenance history","DOCUMENTED, PARTIAL or NONE"),
    MILEAGE("mileage","Mileage","whole kilometres, nonnegative"), PRICE("price","Asking price","CAD amount, nonnegative"),
    TRANSMISSION("transmission","Transmission","AUTOMATIC, MANUAL or CVT"), CONSUMPTION("consumption","Fuel consumption","L/100 km, nonnegative"),
    FUEL_TYPE("fuelType","Fuel type","GASOLINE, DIESEL, HYBRID, ELECTRIC, PLUGIN_HYBRID or OTHER"),
    YEAR("year","Year","whole year"), MAKE("make","Make","manufacturer name"), MODEL("model","Model","model name"), TRIM("trim","Trim","trim name");
    private final String key,label,hint;
    ClarificationField(String key,String label,String hint){this.key=key;this.label=label;this.hint=hint;}
    public String key(){return key;} public String label(){return label;} public String hint(){return hint;}
    public static ClarificationField fromKey(String key){return Arrays.stream(values()).filter(f->f.key.equalsIgnoreCase(key)).findFirst().orElseThrow(()->new IllegalArgumentException("Unsupported clarification field."));}
    public String read(Vehicle v){return switch(this){
        case ACCIDENT->v.accidentHistory();case MAINTENANCE->v.maintenanceHistory();case MILEAGE->Integer.toString(v.mileage());case PRICE->v.askingPrice().toPlainString();
        case TRANSMISSION->v.transmission();case CONSUMPTION->v.fuelConsumption()==null?"":v.fuelConsumption().toPlainString();case FUEL_TYPE->v.fuelType();
        case YEAR->Integer.toString(v.year());case MAKE->v.make();case MODEL->v.model();case TRIM->v.trim();};}
    public Vehicle apply(Vehicle v,String answer){
        String value=Values.required(answer,"Answer");
        if(value.length()>200||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Answer must be at most 200 characters without control characters.");
        if(Set.of("UNKNOWN","UNCERTAIN","N/A","UNSURE","NOT KNOWN","NOT PROVIDED").contains(value.toUpperCase(Locale.ROOT)))throw new IllegalArgumentException("Use the Unknown action to retain uncertainty without overwriting data.");
        String normalized=value.toUpperCase(Locale.ROOT);
        Set<String> allowed=switch(this){case ACCIDENT->Set.of("YES","NO");case MAINTENANCE->Set.of("DOCUMENTED","PARTIAL","NONE");case TRANSMISSION->Set.of("AUTOMATIC","MANUAL","CVT");case FUEL_TYPE->Set.of("GASOLINE","DIESEL","HYBRID","ELECTRIC","PLUGIN_HYBRID","OTHER");default->Set.of();};
        if(this==TRANSMISSION)normalized=Values.transmission(value);
        if(!allowed.isEmpty()){if(!allowed.contains(normalized))throw new IllegalArgumentException(label+": use "+hint+".");value=normalized;}
        BigDecimal decimal=null;int integer=0;
        if(this==PRICE||this==CONSUMPTION){if(!value.matches("[+]?\\d{1,12}(\\.\\d{1,6})?"))throw new IllegalArgumentException(label+": use "+hint+" without unit symbols.");decimal=new BigDecimal(value);}
        if(this==MILEAGE||this==YEAR){try{integer=Integer.parseInt(value);if(integer<0)throw new NumberFormatException();}catch(NumberFormatException e){throw new IllegalArgumentException(label+": enter a nonnegative whole number in range.");}}
        return new Vehicle(v.id(),this==YEAR?integer:v.year(),this==MAKE?value:v.make(),this==MODEL?value:v.model(),this==TRIM?value:v.trim(),
                this==MILEAGE?integer:v.mileage(),this==PRICE?decimal:v.askingPrice(),this==TRANSMISSION?value:v.transmission(),this==FUEL_TYPE?value:v.fuelType(),
                this==ACCIDENT?value:v.accidentHistory(),this==MAINTENANCE?value:v.maintenanceHistory(),this==CONSUMPTION?decimal:v.fuelConsumption(),v.notes());
    }
}
