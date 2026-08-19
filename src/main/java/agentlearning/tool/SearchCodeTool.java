package agentlearning.tool;

import agentlearning.rag.CodeChunk;
import agentlearning.rag.CodeIndex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class SearchCodeTool implements Tool {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CodeIndex index;

    public SearchCodeTool(CodeIndex index) {
        this.index = index;
    }

    @Override public String name() { return "search_code"; }
    @Override public String description() {
        return "用自然语言语义搜索代码库，返回最相关的代码片段（文件名、行号和内容）。"
                + "当用户描述的功能不确定具体文件或符号时使用。";
    }

    @Override public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "query": {"type": "string", "description": "要搜索的功能描述，如 '用户登录逻辑'"}
              },
              "required": ["query"]
            }
            """;
    }

    @Override public String execute(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String query = args.path("query").asText();
            if (query.isBlank()) return "错误：未提供 query 参数";

            List<CodeChunk> results = index.search(query, 5);
            if (results.isEmpty()) {
                return "没有找到相关代码。提示：可能还没建索引，先执行 /index";
            }

            StringBuilder sb = new StringBuilder("搜索「").append(query).append("」的结果：\n");
            for (int i = 0; i < results.size(); i++) {
                CodeChunk c = results.get(i);
                sb.append("\n【").append(i + 1).append("】").append(c.filePath())
                        .append(" 行 ").append(c.startLine()).append("-").append(c.endLine())
                        .append(" (").append(c.name()).append(")\n")
                        .append(c.content()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }
}