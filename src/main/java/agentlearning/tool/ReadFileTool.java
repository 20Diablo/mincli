package agentlearning.tool;

import agentlearning.policy.PathGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileTool implements Tool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PathGuard pathGuard;

    public ReadFileTool(PathGuard pathGuard) {
        this.pathGuard = pathGuard;
    }

    @Override public String name() { return "read_file"; }
    @Override public String description() { return "读取指定路径的文本文件内容并返回"; }

    @Override public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "path": {"type": "string", "description": "要读取的文件路径（相对项目根）"}
              },
              "required": ["path"]
            }
            """;
    }

    @Override public String execute(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String path = args.path("path").asText();
            if (path.isBlank()) return "错误：未提供 path 参数";

            Path p = pathGuard.resolveSafe(path);   // ← 路径校验 + 解析成基于项目根的绝对路径
            if (!Files.exists(p)) return "错误：文件不存在: " + path;
            String content = Files.readString(p);
            return content.length() > 5000
                    ? content.substring(0, 5000) + "\n...(内容过长已截断)"
                    : content;
        } catch (SecurityException e) {
            return "路径被拒绝: " + e.getMessage();   // PathGuard 拒绝时的提示
        } catch (Exception e) {
            return "读取文件失败: " + e.getMessage();
        }
    }
}