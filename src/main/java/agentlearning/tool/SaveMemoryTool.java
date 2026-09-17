package agentlearning.tool;

import agentlearning.memory.MemoryStore;
import agentlearning.memory.MemoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SaveMemoryTool implements Tool {

    private final ObjectMapper mapper = new ObjectMapper();   // ← 补上这个
    private final MemoryStore memory;
    private final Runnable afterSave;

    public SaveMemoryTool(MemoryStore memory, Runnable afterSave) {
        this.memory = memory;
        this.afterSave = afterSave;
    }

    @Override public String name() { return "save_memory"; }

    @Override public String description() {
        return "当且仅当用户明确说\"记一下\"\"记住\"\"以后记得\"，或要求保存长期偏好、稳定事实时调用，"
                + "把精炼的事实写入长期记忆。不要在普通对话、一次性任务请求、临时文件名或你自己猜测时调用。";
    }

    @Override public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "fact": {
                  "type": "string",
                  "description": "要长期保存的稳定事实或用户偏好，必须精炼、可跨会话复用"
                },
                "type": {
                  "type": "string",
                  "description": "记忆类型：fact（稳定事实/偏好）或 conversation（对话记录），默认 fact"
                }
              },
              "required": ["fact"]
            }
            """;
    }

    @Override public String execute(String argumentsJson) {   // ← 只保留这一个
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String fact = args.path("fact").asText();
            if (fact.isBlank()) {
                return "保存失败：fact 不能为空";
            }
            MemoryType type = "conversation".equalsIgnoreCase(args.path("type").asText())
                    ? MemoryType.CONVERSATION
                    : MemoryType.FACT;

            memory.save(type, fact.trim());
            if (afterSave != null) afterSave.run();
            return "已保存到长期记忆(" + type.label() + "): " + fact.trim();
        } catch (Exception e) {
            return "保存记忆失败: " + e.getMessage();
        }
    }
}
