package agentlearning.llm;

import agentlearning.tool.Tool;
import agentlearning.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final String apiKey;
    private final String model;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)   // ← 关键
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekClient(String apiKey) { this(apiKey, "deepseek-chat"); }
    public DeepSeekClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public ChatResult chat(List<Message> messages, ToolRegistry toolRegistry) throws IOException {
        // 1. 用 Jackson 手动拼请求体(因为 tools 的 schema 是嵌套 JSON)
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        // messages 直接用 Jackson 序列化我们的 Message 列表
        body.set("messages", mapper.valueToTree(messages));

        // 2. 拼 tools 数组
        if (toolRegistry != null && !toolRegistry.all().isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (Tool tool : toolRegistry.all()) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode fn = toolNode.putObject("function");
                fn.put("name", tool.name());
                fn.put("description", tool.description());
                fn.set("parameters", mapper.readTree(tool.parametersJsonSchema()));
            }
        }

        String jsonBody = mapper.writeValueAsString(body);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("API 调用失败，HTTP " + response.code() + "：" + respBody);
            }
            JsonNode root = mapper.readTree(respBody);
            JsonNode message = root.path("choices").path(0).path("message");

            // 解析文本内容
            String content = message.path("content").isNull()
                    ? null : message.path("content").asText(null);

            // 解析 tool_calls
            List<ToolCall> toolCalls = null;
            JsonNode tcNode = message.path("tool_calls");
            if (tcNode.isArray() && tcNode.size() > 0) {
                toolCalls = new ArrayList<>();
                for (JsonNode tc : tcNode) {
                    ToolCall call = new ToolCall();
                    call.setId(tc.path("id").asText());
                    call.setType(tc.path("type").asText("function"));
                    ToolCall.Function fn = new ToolCall.Function();
                    fn.setName(tc.path("function").path("name").asText());
                    fn.setArguments(tc.path("function").path("arguments").asText());
                    call.setFunction(fn);
                    toolCalls.add(call);
                }
            }
            return new ChatResult(content, toolCalls);
        }
    }
}