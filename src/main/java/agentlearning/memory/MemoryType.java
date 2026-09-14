package agentlearning.memory;

/**
 * 记忆类型:区分不同来源的记忆,便于分类管理和展示。
 */
public enum MemoryType {
    CONVERSATION("对话"),   // 对话中产生的记忆
    FACT("事实"),          // 跨会话稳定的事实(用户偏好、项目信息)
    SUMMARY("摘要"),       // 压缩后的历史摘要
    TOOL_RESULT("工具结果"); // 工具执行的关键结果

    private final String label;

    MemoryType(String label) { this.label = label; }
    public String label() { return label; }
}