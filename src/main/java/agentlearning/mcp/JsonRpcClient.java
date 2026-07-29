package agentlearning.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class JsonRpcClient {
    private final StdioTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong idCounter = new AtomicLong(1);

    public JsonRpcClient(StdioTransport transport) {
        this.transport = transport;
    }

    /** 发一个请求，返回 result 节点 */
    public JsonNode request(String method, JsonNode params) throws IOException {
        long id = idCounter.getAndIncrement();

        ObjectNode req = mapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", id);
        req.put("method", method);
        req.set("params", params == null ? mapper.createObjectNode() : params);

        transport.send(mapper.writeValueAsString(req));

        // 同步读，直到读到 id 匹配的响应（跳过 server 主动发来的通知）
        while (true) {
            String line = transport.receive();
            if (line == null) throw new IOException("MCP server 关闭了连接");
            if (line.isBlank()) continue;

            JsonNode msg = mapper.readTree(line);
            // 没有 id 的是通知，跳过
            if (!msg.has("id") || msg.get("id").isNull()) continue;
            if (msg.path("id").asLong() != id) continue;   // 不是我等的响应

            if (msg.has("error") && !msg.get("error").isNull()) {
                throw new IOException("MCP 错误: " + msg.get("error").toString());
            }
            return msg.path("result");
        }
    }

    /** 发一个通知（不等响应） */
    public void notify(String method, JsonNode params) throws IOException {
        ObjectNode note = mapper.createObjectNode();
        note.put("jsonrpc", "2.0");
        note.put("method", method);
        note.set("params", params == null ? mapper.createObjectNode() : params);
        transport.send(mapper.writeValueAsString(note));
    }
}