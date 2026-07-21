package agentlearning.tool;

import agentlearning.policy.PathGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class WriteFileTool implements Tool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PathGuard pathGuard;

    public WriteFileTool(PathGuard pathGuard) {
        this.pathGuard = pathGuard;
    }

    @Override public String name() { return "write_file"; }
    @Override public String description() { return "把内容写入指定路径的文件（会覆盖已有内容）"; }

    @Override public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "path": {"type": "string", "description": "要写入的文件路径（相对项目根）"},
                "content": {"type": "string", "description": "要写入的文件内容"}
              },
              "required": ["path", "content"]
            }
            """;
    }

    @Override public boolean requiresApproval() { return true; }   // ← 危险工具

    @Override public String describeForApproval(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String path = args.path("path").asText();
            String content = args.path("content").asText();
            String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
            return "写入文件 " + path + "，内容预览：" + preview;
        } catch (Exception e) {
            return argumentsJson;
        }
    }

    @Override public String execute(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String path = args.path("path").asText();
            String content = args.path("content").asText();
            if (path.isBlank()) return "错误：未提供 path 参数";

            Path p = pathGuard.resolveSafe(path);
            Files.createDirectories(p.getParent());   // 父目录不存在则创建
            Files.writeString(p, content);
            return "已写入文件: " + path + "（" + content.length() + " 字符）";
        } catch (SecurityException e) {
            return "路径被拒绝: " + e.getMessage();
        } catch (Exception e) {
            return "写入文件失败: " + e.getMessage();
        }
    }
}