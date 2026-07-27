package agentlearning.plan;

import java.util.*;

public class ExecutionPlan {
    private final String goal;                          // 总目标
    private final Map<String, Task> tasks = new LinkedHashMap<>();  // id -> Task

    public ExecutionPlan(String goal) {
        this.goal = goal;
    }

    public String getGoal() { return goal; }
    public Collection<Task> getAllTasks() { return tasks.values(); }
    public Task getTask(String id) { return tasks.get(id); }

    public void addTask(Task task) {
        tasks.put(task.getId(), task);
    }

    /** 当前可执行的任务（依赖都已完成的） */
    public List<Task> getExecutableTasks() {
        return tasks.values().stream()
                .filter(t -> t.isExecutable(tasks))
                .toList();
    }

    /** 是否全部完成 */
    public boolean isAllCompleted() {
        return tasks.values().stream()
                .allMatch(t -> t.getStatus() == Task.Status.COMPLETED);
    }

    /**
     * 拓扑排序：算出一个合法的执行顺序。
     * 返回 null 表示存在环（非法的 DAG）。
     */
    public List<Task> topologicalOrder() {
        List<Task> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();    // 已完成排序的
        Set<String> visiting = new HashSet<>();   // 正在访问的（用来检测环）

        for (Task task : tasks.values()) {
            if (!visited.contains(task.getId())) {
                if (!dfs(task, visited, visiting, order)) {
                    return null;   // 检测到环
                }
            }
        }
        return order;
    }

    private boolean dfs(Task task, Set<String> visited, Set<String> visiting, List<Task> order) {
        String id = task.getId();
        if (visiting.contains(id)) return false;   // 正在访问的又碰到了 → 有环
        if (visited.contains(id)) return true;

        visiting.add(id);
        for (String depId : task.getDependencies()) {
            Task dep = tasks.get(depId);
            if (dep != null && !dfs(dep, visited, visiting, order)) {
                return false;
            }
        }
        visiting.remove(id);
        visited.add(id);
        order.add(task);   // 依赖都排完了，才轮到自己 → 保证依赖在前
        return true;
    }

    /** 打印计划给用户看 */
    public String visualize() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 执行计划：").append(goal).append("\n");
        List<Task> order = topologicalOrder();
        if (order == null) {
            return sb.append("  ⚠️ 计划存在循环依赖，无法执行").toString();
        }
        int i = 1;
        for (Task t : order) {
            String deps = t.getDependencies().isEmpty() ? "无" : String.join(",", t.getDependencies());
            sb.append(String.format("  %d. [%s] %s（依赖: %s）%n",
                    i++, t.getId(), t.getDescription(), deps));
        }
        return sb.toString();
    }
}