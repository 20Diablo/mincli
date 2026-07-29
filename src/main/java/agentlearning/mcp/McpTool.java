package agentlearning.mcp;

import com.fasterxml.jackson.databind.JsonNode;

public record McpTool(String serverName, String toolName, String description, JsonNode inputSchema) {
    /** 加命名空间前缀，避免不同 server 工具重名：mcp__<server>__<tool> */
    public String namespacedName() {
        return "mcp__" + serverName + "__" + toolName;
    }
}