package agentlearning.cli;

import agentlearning.agent.Agent;
import agentlearning.llm.DeepSeekClient;
import agentlearning.llm.Message;
import agentlearning.tool.ReadFileTool;
import agentlearning.tool.ToolRegistry;
import okio.JvmSystemFileSystem;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class Main {

    public static void main(String[] args) throws Exception {
        // 从环境变量读 key
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        // 组装：客户端 + 工具注册表 + Agent
        DeepSeekClient client = new DeepSeekClient(apiKey);
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ReadFileTool());   // 注册工具
        Agent agent = new Agent(client, toolRegistry);

        Scanner scanner = new Scanner(System.in);
        System.out.println("minicli (阶段2) 已启动，输入 exit 退出。");
        while (true) {
            System.out.print("\n你: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) break;
            String reply = agent.run(input);
            System.out.println("助手: " + reply);
        }
    }

}
