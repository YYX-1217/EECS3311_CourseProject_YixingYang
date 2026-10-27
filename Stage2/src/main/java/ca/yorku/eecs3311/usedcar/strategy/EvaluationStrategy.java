package ca.yorku.eecs3311.usedcar.strategy;

import ca.yorku.eecs3311.usedcar.domain.UserPreference;

/** Stage 1 prioritization policy; produces instructions, never vehicle findings. */
@FunctionalInterface
public interface EvaluationStrategy {
    String buildPriorityGuidance(UserPreference preference);
}
