package agentlearning.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileTool implements Tool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "read_file"; }

    @Override
    public String description() {
        return "读取指定路径的文本文件内容并返回";
    }

    @Override
    public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "path": {"type": "string", "description": "要读取的文件路径"}
              },
              "required": ["path"]
            }
            """;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            // arguments 是 JSON 字符串，先解析出 path
            JsonNode args = mapper.readTree(argumentsJson);
            String path = args.path("path").asText();
            if (path.isBlank()) return "错误：未提供 path 参数";

            Path p = Path.of(path);
            if (!Files.exists(p)) return "错误：文件不存在: " + path;
            String content = Files.readString(p);
            return content.length() > 5000
                    ? content.substring(0, 5000) + "\n...(内容过长已截断)"
                    : content;
        } catch (Exception e) {
            return "读取文件失败: " + e.getMessage();
        }
    }
}