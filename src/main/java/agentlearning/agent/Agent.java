package agentlearning.agent;

import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.ChatResult;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import agentlearning.llm.ToolCall;
import agentlearning.memory.ConversationHistoryCompactor;
import agentlearning.memory.MemoryManager;
import agentlearning.tool.Tool;
import agentlearning.tool.ToolRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Agent {

    private final DeepSeekClient client;
    private final ToolRegistry toolRegistry;
    private final ApprovalHandler approvalHandler;
    private final MemoryManager memoryManager;            // 可为 null（子 Agent 不记录记忆）
    private final List<Message> history = new ArrayList<>();
    private final ConversationHistoryCompactor compactor;
    private final int compactionTriggerTokens;

    /** 迭代兜底：防止模型陷入死循环，正常退出由模型自决（不再返回 tool_calls） */
    private static final int MAX_ITERATIONS = 10;

    /** 压缩触发阈值：实战按"窗口 - 预留"算；测试压缩时可临时调小（如 300） */
    private static final int COMPACTION_TRIGGER_TOKENS = 3000;

    /** 不带记忆的构造（子 Agent / Plan 子任务用） */
    public Agent(DeepSeekClient client, ToolRegistry toolRegistry,
                 ApprovalHandler approvalHandler, String systemPrompt) {
        this(client, toolRegistry, approvalHandler, systemPrompt, null);
    }

    /** 带记忆的构造（主 Agent 用） */
    public Agent(DeepSeekClient client, ToolRegistry toolRegistry,
                 ApprovalHandler approvalHandler, String systemPrompt,
                 MemoryManager memoryManager) {
        this.client = client;
        this.toolRegistry = toolRegistry;
        this.approvalHandler = approvalHandler;
        this.memoryManager = memoryManager;
        this.compactor = new ConversationHistoryCompactor(client);
        this.compactionTriggerTokens = COMPACTION_TRIGGER_TOKENS;
        history.add(Message.system(systemPrompt));
    }

    public String run(String userInput) throws IOException {
        history.add(Message.user(userInput));
        if (memoryManager != null) {
            memoryManager.recordUserMessage(userInput);       // CONVERSATION
        }

        int iteration = 0;
        while (true) {
            if (++iteration > MAX_ITERATIONS) {
                return "已达到最大迭代次数，停止。";
            }

            // 调 LLM 前，评估并按需压缩历史
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
                    if (memoryManager != null) {
                        memoryManager.recordToolResult(toolName, toolResult);   // TOOL_RESULT
                    }
                }
                continue;
            }

            history.add(Message.assistant(result.content));
            if (memoryManager != null) {
                memoryManager.recordAssistantMessage(result.content);          // CONVERSATION
            }
            return result.content;
        }
    }

    /** 刷新 system prompt（history 第 0 条） */
    public void refreshSystemPrompt(String newSystemPrompt) {
        history.set(0, Message.system(newSystemPrompt));
    }
}
