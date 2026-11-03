package ca.yorku.eecs3311.usedcar.result;

import ca.yorku.eecs3311.usedcar.domain.*;
import java.math.BigDecimal;
import java.util.*;

/** Symmetric snapshots in the application's fixed CAD/km/L-per-100-km units. No model-calculated metrics. */
public record ComparisonEvidence(RiskEvidence first, RiskEvidence second, CostAssumptions assumptions) {
    public ComparisonEvidence { Objects.requireNonNull(first); Objects.requireNonNull(second); }
    public boolean sufficient() {
        return first.missing().sufficient() && second.missing().sufficient()
                && first.constraintsAvailable() && second.constraintsAvailable();
    }
    public Map<String,String> metrics() {
        var out = new LinkedHashMap<String,String>();
        var a = first.vehicle(); var b = second.vehicle();
        pair(out,"Year",a.year(),b.year()); pair(out,"Make/model",a.make()+" "+a.model(),b.make()+" "+b.model());
        pair(out,"Mileage (km)",a.mileage(),b.mileage()); pair(out,"Asking price (CAD)",a.askingPrice(),b.askingPrice());
        pair(out,"Transmission",a.transmission(),b.transmission()); pair(out,"Fuel type",a.fuelType(),b.fuelType());
        pair(out,"Accident history (claims)",a.accidentHistory(),b.accidentHistory());
        pair(out,"Maintenance history (claims)",a.maintenanceHistory(),b.maintenanceHistory());
        pair(out,"Consumption (L/100 km)",a.fuelConsumption(),b.fuelConsumption());
        pair(out,"Annual fuel cost (CAD)",first.cost()==null?null:first.cost().annualFuelCost(),second.cost()==null?null:second.cost().annualFuelCost());
        pair(out,"Purchase tax (CAD)",first.cost()==null?null:first.cost().purchaseTax(),second.cost()==null?null:second.cost().purchaseTax());
        pair(out,"First-year estimate (CAD)",first.cost()==null?null:first.cost().firstYearEstimate(),second.cost()==null?null:second.cost().firstYearEstimate());
        out.put("Price difference A-B (CAD)",a.askingPrice().subtract(b.askingPrice()).toPlainString());
        out.put("Mileage difference A-B (km)",Long.toString((long)a.mileage()-b.mileage()));
        return Collections.unmodifiableMap(out);
    }
    private static void pair(Map<String,String> out,String key,Object a,Object b) { out.put(key,"A: "+value(a)+" | B: "+value(b)); }
    private static String value(Object x) {
        if(x==null || x.toString().isBlank()) return "UNAVAILABLE";
        return x instanceof BigDecimal n ? n.toPlainString() : x.toString();
    }
    public VehicleComparison deterministic() {
        return new VehicleComparison(first.vehicle().id(),second.vehicle().id(),metrics(),"","");
    }
}
