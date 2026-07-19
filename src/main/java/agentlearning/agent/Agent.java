package agentlearning.agent;

import agentlearning.llm.ChatResult;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import agentlearning.llm.ToolCall;
import agentlearning.tool.ToolRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Agent {
    private final DeepSeekClient client;
    private final ToolRegistry toolRegistry;
    private final List<Message> history = new ArrayList<>();

    private static final int MAX_ITERATIONS = 10;   // 兜底：防止死循环

    public Agent(DeepSeekClient client, ToolRegistry toolRegistry) {
        this.client = client;
        this.toolRegistry = toolRegistry;
        history.add(Message.system(
                "你是一个能调用工具的助手。需要读取文件等操作时，调用提供的工具，不要编造内容。"));
    }

    public String run(String userInput) throws IOException {
        history.add(Message.user(userInput));

        int iteration = 0;
        while (true) {
            if (++iteration > MAX_ITERATIONS) {
                return "已达到最大迭代次数，停止。";
            }

            // 1. 把 history + 工具清单发给模型
            ChatResult result = client.chat(history, toolRegistry);

            // 2. 模型要调工具？
            if (result.hasToolCalls()) {
                // 2a. 把模型的决定(带 tool_calls 的 assistant 消息)加进 history —— 必须！
                history.add(Message.assistantWithTools(result.toolCalls));

                // 2b. 逐个执行工具，把结果作为 tool 消息加进 history
                for (ToolCall call : result.toolCalls) {
                    String toolName = call.getFunction().getName();
                    String args = call.getFunction().getArguments();
                    System.out.println("  [调用工具] " + toolName + " 参数=" + args);

                    String toolResult = toolRegistry.execute(toolName, args);
                    history.add(Message.tool(call.getId(), toolResult));
                }
                // 2c. 回到循环顶，带着工具结果再问模型
                continue;
            }

            // 3. 没有工具调用 → 这是最终答案
            history.add(Message.assistant(result.content));
            return result.content;
        }
    }
}