package ca.yorku.eecs3311.usedcar.result;

/** Model-year arithmetic only: registration/manufacture dates are not available. */
public record ModelYearContext(Integer modelYear,Integer referenceYear) {
    public Integer difference(){
        if(modelYear==null||referenceYear==null||modelYear<1886||referenceYear<1886||referenceYear>9999||modelYear>referenceYear+1)return null;
        return referenceYear-modelYear;
    }
    public String description(){
        var difference=difference();
        return difference==null ? "Model-year difference: UNAVAILABLE; model/reference year is missing or invalid. Exact in-service age is unknown."
            : "Model-year difference: "+difference+" years (reference year "+referenceYear+" minus model year "+modelYear+"); not the exact in-service age.";
    }
}
