package agentlearning.agent;

import agentlearning.plan.ExecutionPlan;
import agentlearning.plan.Planner;
import agentlearning.plan.Task;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class PlanExecuteAgent {

    private final Planner planner;
    // 每个子任务用一个全新的 ReAct Agent 执行（避免子任务间历史互相污染）
    private final Supplier<Agent> agentFactory;

    public PlanExecuteAgent(Planner planner, Supplier<Agent> agentFactory) {
        this.planner = planner;
        this.agentFactory = agentFactory;
    }

    public String run(String goal) throws IOException {
        // 1. 规划
        System.out.println("正在规划任务...");
        ExecutionPlan plan = planner.createPlan(goal);
        System.out.println(plan.visualize());

        // 2. 校验 DAG 合法（无环）
        List<Task> order = plan.topologicalOrder();
        if (order == null) {
            return "计划存在循环依赖，无法执行。";
        }

        // 3. 按拓扑序逐个执行
        StringBuilder finalReport = new StringBuilder();
        for (Task task : order) {
            System.out.println("\n▶ 执行任务 [" + task.getId() + "]: " + task.getDescription());
            task.setStatus(Task.Status.RUNNING);

            // 把"总目标 + 前置任务结果 + 当前子任务"拼成给 ReAct 的输入
            String subInput = buildSubTaskInput(plan, task);

            try {
                Agent subAgent = agentFactory.get();   // 每个子任务用新 Agent
                String result = subAgent.run(subInput);
                task.setResult(result);
                task.setStatus(Task.Status.COMPLETED);
                finalReport.append("✅ [").append(task.getId()).append("] ").append(result).append("\n");
            } catch (Exception e) {
                task.setStatus(Task.Status.FAILED);
                finalReport.append("❌ [").append(task.getId()).append("] 失败: ").append(e.getMessage()).append("\n");
                // 简单策略：一个任务失败就停（依赖它的后续任务本来也执行不了）
                return finalReport.append("\n任务执行中断。").toString();
            }
        }

        return "全部任务完成：\n" + finalReport;
    }

    /** 给子任务拼输入：让它知道总目标和已完成的前置任务结果 */
    private String buildSubTaskInput(ExecutionPlan plan, Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("总目标: ").append(plan.getGoal()).append("\n\n");
        if (!task.getDependencies().isEmpty()) {
            sb.append("已完成的前置任务结果:\n");
            for (String depId : task.getDependencies()) {
                Task dep = plan.getTask(depId);
                if (dep != null && dep.getResult() != null) {
                    sb.append("- [").append(depId).append("] ").append(dep.getResult()).append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append("当前你要完成的子任务: ").append(task.getDescription());
        return sb.toString();
    }
}