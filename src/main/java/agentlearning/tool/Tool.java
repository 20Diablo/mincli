package agentlearning.tool;

public interface Tool {
    String name();                     // 工具名，如 "read_file"
    String description();              // 干什么用，给模型看
    String parametersJsonSchema();     // 参数的 JSON Schema（字符串形式，简单起见）
    String execute(String argumentsJson);  // 执行，入参是模型传来的 arguments(JSON字符串)
}