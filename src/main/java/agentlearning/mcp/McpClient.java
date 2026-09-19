package agentlearning.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class McpClient implements AutoCloseable {
    private final String serverName;
    private final StdioTransport transport;
    private final JsonRpcClient rpc;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpClient(String serverName, StdioTransport transport) {
        this.serverName = serverName;
        this.transport = transport;
        this.rpc = new JsonRpcClient(transport);
    }

    /** initialize 握手 */
    public void initialize() throws IOException {
        ObjectNode params = mapper.createObjectNode();
        params.put("protocolVersion", "2024-11-05");     // 协议版本
        params.set("capabilities", mapper.createObjectNode());   // 我们(client)支持什么
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "mincli");
        clientInfo.put("version", "1.0");

        rpc.request("initialize", params, 60);     // 握手给长一点(首次可能要下载)
    }

    /** 发现工具：tools/list */
    public List<McpTool> listTools() throws IOException {
        JsonNode result = rpc.request("tools/list", mapper.createObjectNode(), 30);   // ← 加超时
        List<McpTool> tools = new ArrayList<>();
        for (JsonNode t : result.path("tools")) {
            String name = t.path("name").asText();
            String description = t.path("description").asText("");
            JsonNode inputSchema = t.path("inputSchema");
            tools.add(new McpTool(serverName, name, description, inputSchema));
        }
        return tools;
    }


    /** 调用工具：tools/call */
    public String callTool(String toolName, String argumentsJson) throws IOException {
        ObjectNode params = mapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", argumentsJson == null || argumentsJson.isBlank()
                ? mapper.createObjectNode()
                : mapper.readTree(argumentsJson));

        JsonNode result = rpc.request("tools/call", params, 60);;
        // MCP 返回的 content 是数组，每项有 type/text。取文本拼起来
        StringBuilder sb = new StringBuilder();
        for (JsonNode c : result.path("content")) {
            if ("text".equals(c.path("type").asText())) {
                sb.append(c.path("text").asText());
            }
        }
        return sb.toString();
    }

    @Override
    public void close() {
        transport.close();
    }
}