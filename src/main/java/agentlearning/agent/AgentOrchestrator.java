package agentlearning.agent;

import agentlearning.llm.DeepSeekClient;
import agentlearning.tool.ToolRegistry;
import agentlearning.hitl.ApprovalHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class AgentOrchestrator {

    private static final int MAX_RETRIES = 2;   // 每个步骤最多重试次数

    private final SubAgent planner;
    private final SubAgent worker;
    private final SubAgent reviewer;
    private final ObjectMapper mapper = new ObjectMapper();

    // 执行步骤
    private static class Step {
        String id;
        String description;
        List<String> dependencies;
        String result;
        boolean completed = false;

        Step(String id, String description, List<String> dependencies) {
            this.id = id;
            this.description = description;
            this.dependencies = dependencies == null ? new ArrayList<>() : dependencies;
        }
    }

    public AgentOrchestrator(DeepSeekClient client, ToolRegistry toolRegistry,
                             ApprovalHandler approvalHandler) {
        this.planner = new SubAgent("planner", AgentRole.PLANNER, client, toolRegistry, approvalHandler);
        this.worker = new SubAgent("worker", AgentRole.WORKER, client, toolRegistry, approvalHandler);
        this.reviewer = new SubAgent("reviewer", AgentRole.REVIEWER, client, toolRegistry, approvalHandler);
    }

    public String run(String userInput) {
        // 1. 规划
        System.out.println("🧑‍💼 [规划者] 正在拆解任务...");
        AgentMessage planResult = planner.execute(AgentMessage.task("orchestrator", userInput));
        if (planResult.type() == AgentMessage.Type.ERROR) {
            return "规划失败: " + planResult.content();
        }

        List<Step> steps = parsePlan(planResult.content());
        if (steps.isEmpty()) {
            return "规划失败：无法解析计划。\n原始输出:\n" + planResult.content();
        }

        // 2. 执行 + 审查（按依赖顺序）
        for (Step step : steps) {
            System.out.println("\n🛠️ [执行者] 执行 [" + step.id + "]: " + step.description);
            String context = buildContext(steps, step);

            String taskInput = context + "\n\n当前任务: " + step.description;
            AgentMessage result = worker.execute(AgentMessage.task("orchestrator", taskInput));

            if (result.type() == AgentMessage.Type.ERROR) {
                step.result = "执行失败: " + result.content();
                continue;
            }

            // 审查
            boolean approved = false;
            String issues = "";
            for (int retry = 0; retry < MAX_RETRIES && !approved; retry++) {
                System.out.println("🔍 [检查者] 审查 [" + step.id + "]...");
                AgentMessage review = reviewer.review(step.description, result.content());
                if (review.type() == AgentMessage.Type.ERROR) {
                    break;   // 审查本身出错，保守起见保留当前结果
                }
                boolean[] parsed = parseReview(review.content());
                approved = parsed[0];
                issues = review.content();

                if (!approved && retry < MAX_RETRIES - 1) {
                    System.out.println("⚠️ 审查未通过，重做 [" + step.id + "]...");
                    String retryInput = context + "\n\n当前任务: " + step.description
                            + "\n\n之前结果被拒绝，原因: " + issues;
                    result = worker.execute(AgentMessage.task("orchestrator", retryInput));
                }
            }

            if (approved) {
                step.completed = true;
                step.result = result.content();
                System.out.println("✅ [" + step.id + "] 审查通过");
            } else {
                step.result = result.content();
                System.out.println("⚠️ [" + step.id + "] 超过重试次数，保留当前结果");
            }
        }

        // 3. 汇总
        return buildFinalResult(steps);
    }

    private List<Step> parsePlan(String planJson) {
        try {
            String cleaned = planJson.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode root = mapper.readTree(cleaned);
            JsonNode stepsNode = root.path("steps");
            if (!stepsNode.isArray()) stepsNode = root.path("tasks");

            List<Step> steps = new ArrayList<>();
            for (JsonNode s : stepsNode) {
                String id = s.path("id").asText();
                String desc = s.path("description").asText();
                List<String> deps = new ArrayList<>();
                for (JsonNode d : s.path("dependencies")) deps.add(d.asText());
                steps.add(new Step(id, desc, deps));
            }
            return steps;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private boolean[] parseReview(String reviewJson) {
        try {
            String cleaned = reviewJson.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode root = mapper.readTree(cleaned);
            return new boolean[]{ root.path("approved").asBoolean(false) };
        } catch (Exception e) {
            // 解析失败：保守策略，默认不通过（别让问题结果直接放行）
            return new boolean[]{ false };
        }
    }

    private String buildContext(List<Step> steps, Step current) {
        StringBuilder sb = new StringBuilder("已完成的前置任务:\n");
        boolean hasDep = false;
        for (Step s : steps) {
            if (s.completed && current.dependencies.contains(s.id)) {
                hasDep = true;
                sb.append("- [").append(s.id).append("] ").append(s.description)
                        .append(" => ").append(s.result).append("\n");
            }
        }
        return hasDep ? sb.toString() : "（无前置任务）";
    }

    private String buildFinalResult(List<Step> steps) {
        StringBuilder sb = new StringBuilder("\n📋 多 Agent 协作总结:\n");
        for (Step s : steps) {
            sb.append("  ").append(s.completed ? "✅" : "❌")
                    .append(" [").append(s.id).append("] ").append(s.description).append("\n");
            if (s.result != null && !s.result.isBlank()) {
                String preview = s.result.length() > 120 ? s.result.substring(0, 120) + "..." : s.result;
                sb.append("       ").append(preview).append("\n");
            }
        }
        return sb.toString();
    }
}