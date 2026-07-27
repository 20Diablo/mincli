package agentlearning.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Task {
    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }

    private final String id;
    private final String description;
    private final List<String> dependencies;   // 依赖的其他任务 id
    private Status status = Status.PENDING;
    private String result;                       // 执行结果

    public Task(String id, String description, List<String> dependencies) {
        this.id = id;
        this.description = description;
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>(dependencies);
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public List<String> getDependencies() { return dependencies; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    /** 是否可以执行：自己还没执行，且所有依赖都已完成 */
    public boolean isExecutable(Map<String, Task> allTasks) {
        if (status != Status.PENDING) return false;
        for (String depId : dependencies) {
            Task dep = allTasks.get(depId);
            if (dep == null || dep.getStatus() != Status.COMPLETED) {
                return false;   // 有依赖没完成，暂时不能执行
            }
        }
        return true;
    }
}