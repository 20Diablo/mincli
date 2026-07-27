package agentlearning.plan;

import agentlearning.llm.ChatResult;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Planner {

    private final DeepSeekClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String PLAN_PROMPT = """
            你是一个任务规划助手。请把用户的需求拆解成若干个可执行的子任务，并标出它们之间的依赖关系。

            要求：
            1. 每个子任务是一个具体、独立可执行的步骤
            2. 用 id 标识任务（t1, t2, t3...），dependencies 里写它依赖的任务 id
            3. 没有依赖的任务 dependencies 为空数组
            4. 只输出 JSON，不要任何解释或 markdown 代码块标记

            输出格式（严格遵守）：
            {
              "tasks": [
                {"id": "t1", "description": "具体任务描述", "dependencies": []},
                {"id": "t2", "description": "具体任务描述", "dependencies": ["t1"]}
              ]
            }

            用户需求：%s
            """;

    public Planner(DeepSeekClient client) {
        this.client = client;
    }

    public ExecutionPlan createPlan(String goal) throws IOException {
        String prompt = String.format(PLAN_PROMPT, goal);

        List<Message> req = new ArrayList<>();
        req.add(Message.system("你是任务规划助手，只输出规定格式的 JSON。"));
        req.add(Message.user(prompt));
        ChatResult result = client.chat(req, null);   // 不带工具，纯规划

        String json = extractJson(result.content);   // 容错：剥掉可能的 markdown 包裹
        JsonNode root = mapper.readTree(json);

        ExecutionPlan plan = new ExecutionPlan(goal);
        for (JsonNode taskNode : root.path("tasks")) {
            String id = taskNode.path("id").asText();
            String desc = taskNode.path("description").asText();
            List<String> deps = new ArrayList<>();
            for (JsonNode dep : taskNode.path("dependencies")) {
                deps.add(dep.asText());
            }
            plan.addTask(new Task(id, desc, deps));
        }
        return plan;
    }

    /** 容错：模型有时会用 ```json ``` 包裹，剥掉它 */
    private String extractJson(String content) {
        if (content == null) return "{}";
        String s = content.trim();
        if (s.startsWith("```")) {
            int start = s.indexOf('{');
            int end = s.lastIndexOf('}');
            if (start >= 0 && end > start) return s.substring(start, end + 1);
        }
        return s;
    }
}