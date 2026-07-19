package agentlearning.llm;

import java.util.List;

public class ChatResult {
    public final String content;              // 模型的文本回复(可能为 null)
    public final List<ToolCall> toolCalls;    // 模型要调的工具(可能为 null/空)

    public ChatResult(String content, List<ToolCall> toolCalls) {
        this.content = content;
        this.toolCalls = toolCalls;
    }
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}