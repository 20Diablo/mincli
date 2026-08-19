package agentlearning.agent;

public enum AgentRole {
    PLANNER("规划者", "负责把任务拆解成带依赖关系的子任务"),
    WORKER("执行者", "负责执行具体子任务，调用工具完成任务"),
    REVIEWER("检查者", "负责审查执行结果的质量，判断是否通过");

    private final String displayName;
    private final String description;

    AgentRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    public String displayName() { return displayName; }
    public String description() { return description; }
}