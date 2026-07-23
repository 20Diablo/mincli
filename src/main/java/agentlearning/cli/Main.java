package agentlearning.cli;

import agentlearning.agent.Agent;
import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.DeepSeekClient;
import agentlearning.memory.LongTermMemory;
import agentlearning.policy.PathGuard;
import agentlearning.tool.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        // 项目根 = 程序运行时的当前目录
        String projectPath = System.getProperty("user.dir");
        System.out.println("项目根目录: " + projectPath);

        PathGuard pathGuard = new PathGuard(projectPath);
        Scanner scanner = new Scanner(System.in);
        ApprovalHandler approvalHandler = new ApprovalHandler(scanner);

        // 注册全部工具
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ReadFileTool(pathGuard));
        toolRegistry.register(new WriteFileTool(pathGuard));
        toolRegistry.register(new ListDirTool(pathGuard));
        toolRegistry.register(new ExecuteCommandTool(projectPath));

        DeepSeekClient client = new DeepSeekClient(apiKey);

        // 启动时创建长期记忆，把它注入 system prompt
        LongTermMemory memory = new LongTermMemory();
        String systemPrompt = "你是一个能调用工具的助手，需要时调用工具，不要编造。"
                + memory.asPromptSection();   // ← 把长期记忆拼进 system prompt
        Agent agent = new Agent(client, toolRegistry, approvalHandler, systemPrompt);

        System.out.println("minicli (阶段3) 已启动，输入 exit 退出。");
        // 主循环里加 /save 命令处理
        while (true) {
            System.out.print("\n你: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) break;

            // 斜杠命令：/save 内容
            if (input.startsWith("/save ")) {
                String content = input.substring(6).trim();
                if (!content.isBlank()) {
                    memory.save(content);
                    System.out.println("已保存到长期记忆: " + content);
                }
                continue;   // 不进 Agent
            }

            String reply = agent.run(input);
            System.out.println("助手: " + reply);
        }
    }
}