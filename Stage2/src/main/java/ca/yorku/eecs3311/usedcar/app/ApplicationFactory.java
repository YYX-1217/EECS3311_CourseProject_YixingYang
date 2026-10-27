package ca.yorku.eecs3311.usedcar.app;

import ca.yorku.eecs3311.usedcar.agent.UsedCarAgent;
import ca.yorku.eecs3311.usedcar.llm.GeminiAdapter;
import ca.yorku.eecs3311.usedcar.tool.*;

/** Composition root, not a claim of the GoF Factory Method pattern. */
public final class ApplicationFactory {
    private ApplicationFactory() { }
    public static UsedCarApplicationFacade create() {
        var tools = new ToolManager();
        tools.registerTool("cost", new CostCalculatorTool());
        tools.registerTool("constraints", new ConstraintCheckerTool());
        return new UsedCarApplicationFacade(new UsedCarAgent(tools, new GeminiAdapter(), new DataQualityChecker()), new EvaluationSession());
    }
}
