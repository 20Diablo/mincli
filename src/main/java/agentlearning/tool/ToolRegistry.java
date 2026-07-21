package agentlearning.tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

//管理所有工具：按名字找、执行、以及给模型看的清单。
public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public boolean has(String name) { return tools.containsKey(name); }

    // 根据工具名执行
    public String execute(String name, String argumentsJson) {
        Tool tool = tools.get(name);
        if (tool == null) return "错误：未知工具 " + name;
        return tool.execute(argumentsJson);
    }

    public Collection<Tool> all() { return tools.values(); }

    // 按名字拿tool
    public Tool get(String name) {
        return tools.get(name);
    }
}