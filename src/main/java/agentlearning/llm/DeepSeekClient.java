package agentlearning.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final String apiKey;
    private final String model;
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekClient(String apiKey) {
        this(apiKey, "deepseek-chat");   // deepseek-chat 是通用对话模型
    }

    public DeepSeekClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = new OkHttpClient();
    }

    /**
     *把消息列表发给 DeepSeek，返回模型回复的文本。
     */
    public String chat(List<Message> messages) throws IOException{
        // 1. 拼请求体：{"model": ..., "messages": [...]}
        Map<String , Object> body = Map.of(
                "model", model,
                "messages", messages
        );
        String jsonBody = mapper.writeValueAsString(body);
        //System.out.println("请求体: " + jsonBody);   // 临时加这行

        // 2. 构造请求，带上鉴权头
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization" , "Bearer " + apiKey)
                .addHeader("Content-Type" , "application/json")
                .post(RequestBody.create(jsonBody , JSON))
                .build();

        try (Response response = http.newCall(request).execute()){
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("API 调用失败，HTTP " + response.code() + "：" + respBody);
            }

            // 4. 解析 JSON，取出 choices[0].message.content
            JsonNode root = mapper.readTree(respBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            return contentNode.asText("");
        }
    }
}
