package ca.yorku.eecs3311.usedcar.result;
import java.util.*;
/** Stage 1 advisory result, not a mechanical inspection or purchase guarantee. */
public record RiskAssessment(String riskLevel, List<String> strengths, List<String> risks,
                             List<String> uncertainties, String explanation) {
    public static final Set<String> LEVELS=Set.of("Low","Moderate","High","Insufficient Information");
    public RiskAssessment {
        if(!LEVELS.contains(riskLevel))throw new IllegalArgumentException("Unsupported risk category.");
        strengths=List.copyOf(strengths);risks=List.copyOf(risks);uncertainties=List.copyOf(uncertainties);
        if(explanation==null||explanation.isBlank())throw new IllegalArgumentException("Risk explanation is required.");
        if(java.util.stream.Stream.of(strengths,risks,uncertainties).flatMap(List::stream).anyMatch(String::isBlank))
            throw new IllegalArgumentException("Risk statements must not be blank.");
    }
}
