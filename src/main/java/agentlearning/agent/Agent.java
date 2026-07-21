package agentlearning.agent;

import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.ChatResult;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import agentlearning.llm.ToolCall;
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
    private static final int MAX_ITERATIONS = 10;

    public Agent(DeepSeekClient client, ToolRegistry toolRegistry, ApprovalHandler approvalHandler) {
        this.client = client;
        this.toolRegistry = toolRegistry;
        this.approvalHandler = approvalHandler;
        history.add(Message.system(
                "你是一个能调用工具的助手。需要读文件、写文件、列目录、执行命令时，调用对应工具，不要编造。"));
    }

    public String run(String userInput) throws IOException {
        history.add(Message.user(userInput));
        int iteration = 0;
        while (true) {
            if (++iteration > MAX_ITERATIONS) return "已达到最大迭代次数，停止。";

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