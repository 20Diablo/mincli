package agentlearning.tool;

import agentlearning.policy.PathGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListDirTool implements Tool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PathGuard pathGuard;

    public ListDirTool(PathGuard pathGuard) {
        this.pathGuard = pathGuard;
    }

    @Override public String name() { return "list_dir"; }
    @Override public String description() { return "列出指定目录下的文件和子目录"; }

    @Override public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "path": {"type": "string", "description": "目录路径（相对项目根，默认为项目根本身）"}
              }
            }
            """;
    }

    @Override public String execute(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String path = args.path("path").asText(".");   // 默认当前(项目根)
            if (path.isBlank()) path = ".";

            Path dir = pathGuard.resolveSafe(path);
            if (!Files.isDirectory(dir)) return "错误：不是目录: " + path;

            try (Stream<Path> entries = Files.list(dir)) {
                String listing = entries
                        .map(p -> (Files.isDirectory(p) ? "[目录] " : "[文件] ") + p.getFileName())
                        .sorted()
                        .collect(Collectors.joining("\n"));
                return listing.isBlank() ? "（空目录）" : listing;
            }
        } catch (SecurityException e) {
            return "路径被拒绝: " + e.getMessage();
        } catch (Exception e) {
            return "列目录失败: " + e.getMessage();
        }
    }
}