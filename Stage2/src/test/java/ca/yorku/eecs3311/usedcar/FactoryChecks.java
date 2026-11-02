package ca.yorku.eecs3311.usedcar;

import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.app.*;
import ca.yorku.eecs3311.usedcar.domain.UserPreference;
import ca.yorku.eecs3311.usedcar.llm.GeminiAdapter;
import ca.yorku.eecs3311.usedcar.strategy.*;
import ca.yorku.eecs3311.usedcar.tool.*;
import ca.yorku.eecs3311.usedcar.ui.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import static ca.yorku.eecs3311.usedcar.FoundationChecks.check;

/** Factory behavior checks shared by JUnit and the offline runner. */
public final class FactoryChecks {
    private FactoryChecks() { }
    private static UserPreference preference(int reliability, int economy) {
        return new UserPreference(null, null, Set.of(), "", reliability, economy, "UNKNOWN");
    }
    private static UsedCarAgent agent() {
        return new UsedCarAgent(new ToolManager(), new GeminiAdapter(), new DataQualityChecker());
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid manual selection was accepted.");
    }
    public static void neutral() {
        var factory = new EvaluationStrategyFactory();
        check(factory.createStrategy(null) instanceof BalancedStrategy, "Absent profile must choose Balanced.");
        check(factory.createStrategy(UserPreference.limits(null, null)) instanceof BalancedStrategy, "Neutral profile must choose Balanced.");
        check(factory.createStrategy(preference(5, 0)) instanceof BalancedStrategy
                && factory.createStrategy(preference(0, 5)) instanceof BalancedStrategy, "An unspecified priority is not a lower ranking.");
    }
    public static void ties() {
        var factory = new EvaluationStrategyFactory();
        for (int priority = 1; priority <= 5; priority++) {
            var p = preference(priority, priority);
            check(factory.createStrategy(p) instanceof BalancedStrategy, "Equal specified priorities must balance.");
            check(factory.createStrategy(p).buildPriorityGuidance(p).contains("equal priority values"), "Balanced behavior must preserve a tie.");
        }
    }
    public static void reliability() {
        var p = preference(5, 2);
        EvaluationStrategy selected = new EvaluationStrategyFactory().createStrategy(p);
        check(selected instanceof ReliabilityFocusedStrategy, "Explicit higher reliability priority must specialize.");
        String guidance = selected.buildPriorityGuidance(p);
        check(guidance.contains("inspection concerns and vehicle risks before cost advantages")
                && guidance.contains("reliability importance 5/5") && guidance.contains("not a mechanical-condition score"),
                "Factory product must deliver real reliability guidance, not just its name.");
    }
    public static void priorityMatrix() {
        var factory = new EvaluationStrategyFactory();
        for (int reliability = 0; reliability <= 5; reliability++) {
            for (int economy = 0; economy <= 5; economy++) {
                var selected = factory.createStrategy(preference(reliability, economy));
                Class<?> expected = economy > 0 && reliability > economy ? ReliabilityFocusedStrategy.class : BalancedStrategy.class;
                check(selected.getClass() == expected, "Unexpected policy for priorities " + reliability + "/" + economy);
            }
        }
    }
    public static void budgetNotInferred() {
        var factory = new EvaluationStrategyFactory();
        for (BigDecimal budget : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("1"), new BigDecimal("50000")}) {
            var economyLed = new UserPreference(budget, 80000, Set.of("MANUAL"), "SUV", 1, 5, "NO_ACCIDENTS");
            EvaluationStrategy selected = factory.createStrategy(economyLed);
            check(selected instanceof BalancedStrategy, "A budget limit plus economy emphasis does not establish budget-first intent.");
            String guidance = selected.buildPriorityGuidance(economyLed);
            check(guidance.contains("fuel economy more emphasis") && guidance.contains("80000 km")
                    && guidance.contains("NO_ACCIDENTS") && guidance.contains("retain safety and reliability warnings"),
                    "Conservative selection must still honor actual preferences and safeguards.");
        }
    }
    public static void manualFactory() {
        var factory = new EvaluationStrategyFactory();
        check(factory.createManualStrategy("balanced") instanceof BalancedStrategy, "Manual balanced selection failed.");
        check(factory.createManualStrategy(" BUDGET ") instanceof BudgetFocusedStrategy, "Explicit cost emphasis must create Budget Focused.");
        check(factory.createManualStrategy("reliability") instanceof ReliabilityFocusedStrategy, "Manual reliability selection failed.");
        rejects(() -> factory.createManualStrategy(null));
        rejects(() -> factory.createManualStrategy("cheap"));
        check(factory.createManualStrategy("budget").buildPriorityGuidance(preference(5, 1)).contains("purchase price and ownership costs first"),
                "Explicit budget choice must retain the existing budget policy.");
    }
    public static void agentDispatch() {
        var agent = agent(); var factory = new EvaluationStrategyFactory();
        agent.useAutomaticStrategy();
        for (var p : new UserPreference[]{preference(5, 1), preference(2, 2), preference(1, 5)}) {
            EvaluationStrategy expected = factory.createStrategy(p);
            check(agent.buildPriorityGuidance(p).equals(factory.selectionBasis(p) + expected.buildPriorityGuidance(p)),
                    "Agent must invoke the selected policy with the current profile.");
        }
        String absent = agent.buildPriorityGuidance(null);
        check(absent.contains("Balanced") && absent.contains("Budget is unspecified") && absent.contains("reliability=0/5"),
                "Automatic guidance must handle an absent profile without inventing preferences.");
    }
    public static void agentOverride() {
        var agent = agent(); agent.useAutomaticStrategy();
        var first = preference(5, 1); var changed = preference(1, 5);
        agent.buildPriorityGuidance(first);
        agent.setStrategy(p -> "Explicit custom strategy " + p.reliabilityPriority());
        check(agent.buildPriorityGuidance(changed).equals("Explicit custom strategy 1"), "Manual interface override must receive updated preferences.");
        agent.selectManualStrategy("budget");
        check(agent.buildPriorityGuidance(first).contains("Budget Focused"), "Automatic reliability must not overwrite a manual choice.");
        agent.useAutomaticStrategy();
        check(agent.buildPriorityGuidance(first).contains("Reliability Focused"), "Returning to auto must discard the manual override.");
    }
    public static void facadeModes() {
        var facade = ApplicationFactory.create();
        var vehicle = PreparationForm.command(facade, PreparationForm.example()).execute().vehicle();
        facade.savePreferences(preference(5, 1));
        facade.selectStrategy(" AUTO ");
        check(facade.previewPriorityGuidance().contains("Reliability Focused"), "Facade auto must reach the factory.");
        facade.savePreferences(preference(1, 5));
        check(facade.previewPriorityGuidance().contains("Balanced"), "Saved preference changes must reselect in auto mode.");
        rejects(() -> facade.selectStrategy("invalid")); rejects(() -> facade.selectStrategy(null));
        facade.savePreferences(preference(4, 2));
        check(facade.previewPriorityGuidance().contains("Reliability Focused"), "Invalid selection must preserve automatic mode, not freeze its last product.");
        facade.selectStrategy("budget"); facade.savePreferences(preference(5, 1));
        check(facade.previewPriorityGuidance().contains("Budget Focused"), "Manual override must survive profile updates.");
        facade.selectStrategy("auto");
        check(facade.previewPriorityGuidance().contains("Reliability Focused") && facade.currentVehicle().orElseThrow().equals(vehicle)
                && facade.preferences().equals(preference(5, 1)), "Mode changes must preserve unrelated session data.");
    }
    public static void cliAuto() {
        var facade = ApplicationFactory.create(); var bytes = new ByteArrayOutputStream();
        String script = "strategy auto\nprofile-set\n20000\n90000\nauto\nNO_ACCIDENTS\nSUV\n5\n1\nstrategy\n"
                + "strategy budget\nprofile-set\n:keep\n:keep\n:keep\n:keep\n:keep\n1\n5\nstrategy\nstrategy auto\nquit\n";
        new UsedCarCLI(facade, new Scanner(script), new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        String output = bytes.toString(StandardCharsets.UTF_8);
        check(output.contains("Selection mode: automatic") && output.contains("reliability=5/5, fuel economy=1/5")
                && output.contains("Reliability Focused") && output.contains("Budget Focused")
                && output.contains("reliability=1/5, fuel economy=5/5"), "CLI must expose real selection and its basis.");
        check(facade.previewPriorityGuidance().contains("Balanced") && facade.currentVehicle().isEmpty()
                && facade.preferences().reliabilityPriority() == 1, "CLI auto must use the saved updated profile without requiring a vehicle.");
    }
    public static void runAll() {
        neutral(); ties(); reliability(); priorityMatrix(); budgetNotInferred(); manualFactory();
        agentDispatch(); agentOverride(); facadeModes(); cliAuto();
    }
    public static void main(String[] args) {
        runAll(); System.out.println("PASS: 10 targeted factory verification groups.");
    }
}
