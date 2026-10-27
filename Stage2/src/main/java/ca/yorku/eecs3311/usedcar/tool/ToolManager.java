package ca.yorku.eecs3311.usedcar.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ca.yorku.eecs3311.usedcar.domain.Values;

public final class ToolManager {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void registerTool(String name, Tool tool) {
        name = Values.required(name, "Tool name");
        if (tools.putIfAbsent(name, Objects.requireNonNull(tool)) != null)
            throw new IllegalArgumentException("Tool already registered: " + name);
    }

    public Object execute(String name, Object input) {
        Tool tool = tools.get(name);
        if (tool == null) throw new IllegalArgumentException("No tool registered: " + name);
        return tool.execute(input);
    }
}
