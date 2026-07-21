package agentlearning.cli;

import agentlearning.agent.Agent;
import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.DeepSeekClient;
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
        Agent agent = new Agent(client, toolRegistry, approvalHandler);

        System.out.println("minicli (阶段3) 已启动，输入 exit 退出。");
        while (true) {
            System.out.print("\n你: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) break;
            String reply = agent.run(input);
            System.out.println("助手: " + reply);
        }
    }
}