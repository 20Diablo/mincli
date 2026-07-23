package agentlearning.agent;

import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.ChatResult;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import agentlearning.llm.ToolCall;
import agentlearning.memory.ConversationHistoryCompactor;
import agentlearning.tool.Tool;
import agentlearning.tool.ToolRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Agent {
    private final DeepSeekClient client;
    private final ToolRegistry toolRegistry;
    private final ApprovalHandler approvalHandler;   // ← 新增
    private final List<Message> history = new ArrayList<>();
    // 构造函数里新增 compactor
    private final ConversationHistoryCompactor compactor;
    private final int compactionTriggerTokens;   // 压缩触发阈值
    private static final int MAX_ITERATIONS = 10;

    public Agent(DeepSeekClient client, ToolRegistry toolRegistry, ApprovalHandler approvalHandler, String systemPrompt) {
        this.client = client;
        this.toolRegistry = toolRegistry;
        this.approvalHandler = approvalHandler;
        this.compactor = new ConversationHistoryCompactor(client);
        // 触发阈值：真实项目按"窗口 - 预留"算。练手先给个小值方便测试，比如 3000。
        // 正式可设为窗口的 60-80%。
        this.compactionTriggerTokens = 3000;
        history.add(Message.system(systemPrompt));
    }

    public String run(String userInput) throws IOException {
        history.add(Message.user(userInput));
        int iteration = 0;
        while (true) {
            if (++iteration > MAX_ITERATIONS) return "已达到最大迭代次数，停止。";

            // ★ 调 LLM 前，评估并按需压缩历史
            compactor.compactIfNeeded(history, compactionTriggerTokens);

            ChatResult result = client.chat(history, toolRegistry);

            if (result.hasToolCalls()) {
                history.add(Message.assistantWithTools(result.toolCalls));

                for (ToolCall call : result.toolCalls) {
                    String toolName = call.getFunction().getName();
                    String args = call.getFunction().getArguments();
                    Tool tool = toolRegistry.get(toolName);

                    String toolResult;
                    if (tool == null) {
                        toolResult = "错误：未知工具 " + toolName;
                    } else if (tool.requiresApproval()
                            && !approvalHandler.confirm(toolName, tool.describeForApproval(args))) {
                        // 用户拒绝：把"被拒绝"作为结果回灌，让模型知道并换策略
                        toolResult = "用户拒绝了这次操作。";
                        System.out.println("  [已拒绝] " + toolName);
                    } else {
                        System.out.println("  [调用工具] " + toolName + " 参数=" + args);
                        toolResult = tool.execute(args);
                    }
                    history.add(Message.tool(call.getId(), toolResult));
                }
                continue;
            }

            history.add(Message.assistant(result.content));
            return result.content;
        }
    }
}