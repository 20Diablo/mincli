package agentlearning.agent;

import agentlearning.llm.DeepSeekClient;
import agentlearning.tool.ToolRegistry;
import agentlearning.hitl.ApprovalHandler;

public class SubAgent {

    private final String name;
    private final AgentRole role;
    private final DeepSeekClient client;
    private final ToolRegistry toolRegistry;
    private final ApprovalHandler approvalHandler;

    public SubAgent(String name, AgentRole role, DeepSeekClient client,
                    ToolRegistry toolRegistry, ApprovalHandler approvalHandler) {
        this.name = name;
        this.role = role;
        this.client = client;
        this.toolRegistry = toolRegistry;
        this.approvalHandler = approvalHandler;
    }

    public String name() { return name; }
    public AgentRole role() { return role; }

    /** 执行一个任务，返回结果消息 */
    public AgentMessage execute(AgentMessage task) {
        // 用角色对应的 system prompt 造一个新的 Agent
        // 关键：Planner/Reviewer 不调工具，Worker 才调工具
        Agent agent = new Agent(client, toolRegistry, approvalHandler, systemPrompt());
        try {
            String result = agent.run(task.content());
            return AgentMessage.result(name, role, result);
        } catch (Exception e) {
            return AgentMessage.error(name, role, "执行失败: " + e.getMessage());
        }
    }

    /** Reviewer 专用：审查执行结果 */
    public AgentMessage review(String originalTask, String executionResult) {
        String input = "原始任务：\n" + originalTask + "\n\n执行结果：\n" + executionResult;
        return execute(AgentMessage.task("orchestrator", input));
    }

    /** 角色对应的 system prompt */
    private String systemPrompt() {
        return switch (role) {
            case PLANNER -> """
                你是任务规划者。把用户任务拆解成带依赖关系的子任务。
                只输出 JSON，格式：{"steps":[{"id":"t1","description":"...","dependencies":[]}, ...]}
                不要输出任何解释或代码块标记。
                """;
            case WORKER -> """
                你是任务执行者。用可用的工具完成分配给你的具体任务，不要做规划，直接执行。
                """;
            case REVIEWER -> """
                你是质量检查者。审查执行结果是否正确、是否满足原始任务要求。
                只输出 JSON：{"approved": true/false, "issues": "如果不通过，说明问题"}
                不要输出任何解释或代码块标记。
                """;
        };
    }
}