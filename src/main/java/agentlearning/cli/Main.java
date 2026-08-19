package agentlearning.cli;

import agentlearning.agent.Agent;
import agentlearning.agent.PlanExecuteAgent;
import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.DeepSeekClient;
import agentlearning.mcp.McpClient;
import agentlearning.mcp.McpTool;
import agentlearning.mcp.McpToolAdapter;
import agentlearning.mcp.StdioTransport;
import agentlearning.memory.LongTermMemory;
import agentlearning.plan.Planner;
import agentlearning.policy.PathGuard;
import agentlearning.tool.*;
import agentlearning.agent.AgentOrchestrator;
import agentlearning.rag.CodeIndex;
import agentlearning.tool.SearchCodeTool;

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

        CodeIndex codeIndex = new CodeIndex();
        toolRegistry.register(new SearchCodeTool(codeIndex));

        DeepSeekClient client = new DeepSeekClient(apiKey);

        // 启动一个 MCP server（以官方 everything server 为例，它有很多示例工具）
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            StdioTransport transport = new StdioTransport(
                    isWindows ? "npx.cmd" : "npx",
                    java.util.List.of("-y", "@modelcontextprotocol/server-everything"));
            McpClient mcpClient = new McpClient("everything", transport);
            mcpClient.initialize();                       // 握手
            for (McpTool t : mcpClient.listTools()) {     // 发现工具
                toolRegistry.register(new McpToolAdapter(mcpClient, t));   // 注册进 ToolRegistry
                System.out.println("已注册 MCP 工具: " + t.namespacedName());
            }
            // 注意：mcpClient 要在程序退出时 close()，练手可以先不管，或用 Runtime.addShutdownHook
        } catch (Exception e) {
            System.out.println("MCP server 启动失败（不影响其他功能）: " + e.getMessage());
        }

        // 启动时创建长期记忆，把它注入 system prompt
        LongTermMemory memory = new LongTermMemory();
        String basePrompt = """
        你是一个能调用工具的编程助手。

        工具使用规则：
        - 只有当任务确实需要读写用户项目里的文件、列目录、或执行命令时，才调用工具。
        - 纯知识性问题（讲解概念、原理、语法、算法等）直接用你已有的知识回答，不要调用任何工具。
        - 不确定文件是否存在时，不要凭空猜测去读；先问用户或说明你需要什么。
        - 不要编造工具返回的内容。
        """;
        // system prompt = 基础规则 + 长期记忆
        Agent agent = new Agent(client, toolRegistry, approvalHandler,
                basePrompt + memory.asPromptSection());

        // ↓↓↓ 新增：Planner + PlanExecuteAgent
        Planner planner = new Planner(client);
        PlanExecuteAgent planAgent = new PlanExecuteAgent(
                planner,
                // 工厂：每个子任务 new 一个全新 Agent，并带上最新的长期记忆
                () -> new Agent(client, toolRegistry, approvalHandler,
                        basePrompt + memory.asPromptSection())
        );
        // 组装 AgentOrchestrator
        AgentOrchestrator teamAgent = new AgentOrchestrator(client, toolRegistry, approvalHandler);

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
                    // 重新拼 system prompt 并刷新到运行中的 Agent
                    agent.refreshSystemPrompt(basePrompt + memory.asPromptSection());
                    System.out.println("已保存到长期记忆: " + content);
                }
                continue;
            }

            // 斜杠命令：/memory list —— 查看全部长期记忆
            if ("/memory list".equals(input.trim())) {
                java.util.List<String> all = memory.loadAll();
                if (all.isEmpty()) {
                    System.out.println("长期记忆为空。");
                } else {
                    System.out.println("长期记忆（共 " + all.size() + " 条）:");
                    for (int i = 0; i < all.size(); i++) {
                        System.out.println("  " + (i + 1) + ". " + all.get(i));
                    }
                }
                continue;
            }

            // 斜杠命令：/memory clear —— 清空长期记忆，并让当前会话立即生效
            if ("/memory clear".equals(input.trim())) {
                memory.clear();
                // 刷新 system prompt：此时 asPromptSection() 返回空，记忆段被移除
                agent.refreshSystemPrompt(basePrompt + memory.asPromptSection());
                System.out.println("已清空全部长期记忆。");
                continue;
            }

            if (input.startsWith("/plan ")) {
                String goal = input.substring(6).trim();   // "/plan " 正好 6 个字符
                if (!goal.isBlank()) {
                    String report = planAgent.run(goal);
                    System.out.println(report);
                }
                continue;
            }

            if (input.equals("/index")) {
                int count = codeIndex.indexDirectory(projectPath);
                System.out.println("已索引 " + count + " 个代码块");
                continue;
            }
            if (input.startsWith("/index ")) {
                int count = codeIndex.indexDirectory(input.substring(7).trim());
                System.out.println("已索引 " + count + " 个代码块");
                continue;
            }

            if (input.startsWith("/team ")) {
                String goal = input.substring(6).trim();
                if (!goal.isBlank()) {
                    String report = teamAgent.run(goal);
                    System.out.println(report);
                }
                continue;
            }

            String reply = agent.run(input);
            System.out.println("助手: " + reply);
        }
    }
}