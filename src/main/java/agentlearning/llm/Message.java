package agentlearning.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Message {
    private String role;
    private String content;
    private List<ToolCall> toolCalls;   // 只有 assistant 调工具时才有
    private String toolCallId;          // 只有 tool 消息才有

    public Message(){}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // 关键：JSON 字段名是 tool_calls（下划线），Java 里是 toolCalls（驼峰），
    // 用 @JsonProperty 显式指定，避免 Jackson 按驼峰序列化成 "toolCalls" 导致 API 不认。
    @JsonProperty("tool_calls")
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    @JsonProperty("tool_call_id")
    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    // 几个方便的工厂方法，有哪些角色
    public static Message system(String content) { return new Message("system", content); }
    public static Message user(String content)   { return new Message("user", content); }
    public static Message assistant(String content) { return new Message("assistant", content); }

    // 带工具调用的 assistant 消息
    public static Message assistantWithTools(List<ToolCall> toolCalls) {
        Message m = new Message("assistant", null);
        m.toolCalls = toolCalls;
        return m;
    }

    // 工具执行结果消息
    public static Message tool(String toolCallId, String content) {
        Message m = new Message("tool", content);
        m.toolCallId = toolCallId;
        return m;
    }
}
