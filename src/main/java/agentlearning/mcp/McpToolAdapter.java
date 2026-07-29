package agentlearning.mcp;

import agentlearning.tool.Tool;

public class McpToolAdapter implements Tool {
    private final McpClient client;
    private final McpTool mcpTool;

    public McpToolAdapter(McpClient client, McpTool mcpTool) {
        this.client = client;
        this.mcpTool = mcpTool;
    }

    @Override public String name() { return mcpTool.namespacedName(); }
    @Override public String description() { return mcpTool.description(); }

    @Override public String parametersJsonSchema() {
        // MCP 的 inputSchema 本就是 JSON Schema，直接转字符串
        return mcpTool.inputSchema() == null || mcpTool.inputSchema().isMissingNode()
                ? "{\"type\":\"object\",\"properties\":{}}"
                : mcpTool.inputSchema().toString();
    }

    @Override public String execute(String argumentsJson) {
        try {
            // 真正调用时，用原始 toolName（不带前缀）发给 server
            return client.callTool(mcpTool.toolName(), argumentsJson);
        } catch (Exception e) {
            return "MCP 工具调用失败: " + e.getMessage();
        }
    }

    // MCP 工具默认需要审批（它们可能有副作用），复用阶段3的 HITL
    @Override public boolean requiresApproval() { return true; }
    @Override public String describeForApproval(String argumentsJson) {
        return "调用 MCP 工具 " + mcpTool.namespacedName() + " 参数=" + argumentsJson;
    }
}