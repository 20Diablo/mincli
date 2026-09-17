package agentlearning.cli;

import agentlearning.agent.Agent;
import agentlearning.agent.AgentOrchestrator;
import agentlearning.agent.PlanExecuteAgent;
import agentlearning.hitl.ApprovalHandler;
import agentlearning.llm.DeepSeekClient;
import agentlearning.mcp.McpClient;
import agentlearning.mcp.McpTool;
import agentlearning.mcp.McpToolAdapter;
import agentlearning.mcp.StdioTransport;
import agentlearning.memory.*;
import agentlearning.plan.Planner;
import agentlearning.policy.PathGuard;
import agentlearning.rag.CodeIndex;
import agentlearning.tool.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        String projectPath = System.getProperty("user.dir");
        System.out.println("项目根目录: " + projectPath);

        PathGuard pathGuard = new PathGuard(projectPath);
        Scanner scanner = new Scanner(System.in);
        ApprovalHandler approvalHandler = new ApprovalHandler(scanner);

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ReadFileTool(pathGuard));
        toolRegistry.register(new WriteFileTool(pathGuard));
        toolRegistry.register(new ListDirTool(pathGuard));
        toolRegistry.register(new ExecuteCommandTool(projectPath));

        CodeIndex codeIndex = new CodeIndex();
        toolRegistry.register(new SearchCodeTool(codeIndex));

        DeepSeekClient client = new DeepSeekClient(apiKey);

        // ---- MCP ----
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            StdioTransport transport = new StdioTransport(
                    isWindows ? "npx.cmd" : "npx",
                    java.util.List.of("-y", "@modelcontextprotocol/server-everything"));
            McpClient mcpClient = new McpClient("everything", transport);
            mcpClient.initialize();
            for (McpTool t : mcpClient.listTools()) {
                toolRegistry.register(new McpToolAdapter(mcpClient, t));
                System.out.println("已注册 MCP 工具: " + t.namespacedName());
            }
        } catch (Exception e) {
            System.out.println("MCP server 启动失败（不影响其他功能）: " + e.getMessage());
        }

        // ---- 记忆 ----
        MemoryManager memory = new MemoryManager();
        String basePrompt = """
            你是一个能调用工具的编程助手。

            工具使用规则：
            - 只有当任务确实需要读写用户项目里的文件、列目录、或执行命令时，才调用工具。
            - 纯知识性问题（讲解概念、原理、语法、算法等）直接用你已有的知识回答，不要调用任何工具。
            - 不确定文件是否存在时，不要凭空猜测去读；先问用户或说明你需要什么。
            - 不要编造工具返回的内容。

            记忆规则：
            - 当用户说"记一下""记住""以后记得"时，调用 save_memory 工具保存这条稳定事实/偏好。
            - 只保存跨会话可复用的稳定信息（偏好、约定），不要保存一次性任务或临时内容。
            """;

        Agent agent = new Agent(client, toolRegistry, approvalHandler,
                basePrompt + memory.promptSection(), memory);

        // save_memory：保存后刷新当前会话的 system prompt
        toolRegistry.register(new SaveMemoryTool(memory,
                () -> agent.refreshSystemPrompt(basePrompt + memory.promptSection())));

        Planner planner = new Planner(client);
        PlanExecuteAgent planAgent = new PlanExecuteAgent(planner,
                () -> new Agent(client, toolRegistry, approvalHandler,
                        basePrompt + memory.promptSection(), memory));

        AgentOrchestrator teamAgent = new AgentOrchestrator(client, toolRegistry, approvalHandler);

        System.out.println("minicli 已启动，输入 exit 退出。");
        while (true) {
            System.out.print("\n你: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) break;

            // /save：存长期事实（FACT）
            if (input.startsWith("/save ")) {
                String content = input.substring(6).trim();
                if (!content.isBlank()) {
                    memory.storeFact(content);
                    agent.refreshSystemPrompt(basePrompt + memory.promptSection());
                    System.out.println("已保存到长期记忆: " + content);
                }
                continue;
            }

            // /memory：按类型统计 + 列出长期记忆
            if ("/memory".equals(input.trim())) {
                var counts = memory.countByType();
                System.out.println("记忆统计:");
                for (MemoryType t : MemoryType.values()) {
                    System.out.println("  " + t.label() + ": " + counts.getOrDefault(t, 0) + " 条");
                }
                var facts = memory.longTermEntries();
                System.out.println("长期记忆(FACT):");
                if (facts.isEmpty()) System.out.println("  (空)");
                else for (int i = 0; i < facts.size(); i++)
                    System.out.println("  " + (i + 1) + ". " + facts.get(i).getContent());
                continue;
            }

            if ("/memory clear".equals(input.trim())) {
                memory.clearLongTerm();
                agent.refreshSystemPrompt(basePrompt + memory.promptSection());
                System.out.println("已清空长期记忆。");
                continue;
            }

            if (input.startsWith("/plan ")) {
                String goal = input.substring(6).trim();
                if (!goal.isBlank()) System.out.println(planAgent.run(goal));
                continue;
            }

            if (input.equals("/index")) {
                System.out.println("已索引 " + codeIndex.indexDirectory(projectPath) + " 个代码块");
                continue;
            }
            if (input.startsWith("/index ")) {
                String dir = input.substring(7).trim();
                if (!java.nio.file.Files.isDirectory(java.nio.file.Path.of(dir))) {
                    System.out.println("路径不是目录: " + dir);
                    continue;
                }
                System.out.println("已索引 " + codeIndex.indexDirectory(dir) + " 个代码块");
                continue;
            }

            if (input.startsWith("/team ")) {
                String goal = input.substring(6).trim();
                if (!goal.isBlank()) System.out.println(teamAgent.run(goal));
                continue;
            }

            System.out.println("助手: " + agent.run(input));
        }
    }
}
