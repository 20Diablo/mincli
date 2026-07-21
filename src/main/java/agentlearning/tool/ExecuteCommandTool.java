package agentlearning.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class ExecuteCommandTool implements Tool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String workingDir;    // 命令在项目根下执行

    public ExecuteCommandTool(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override public String name() { return "execute_command"; }
    @Override public String description() { return "在项目目录下执行一条 shell 命令并返回输出"; }

    @Override public String parametersJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "command": {"type": "string", "description": "要执行的命令，如 'ls' 或 'dir'"}
              },
              "required": ["command"]
            }
            """;
    }

    @Override public boolean requiresApproval() { return true; }   // ← 危险工具

    @Override public String describeForApproval(String argumentsJson) {
        try {
            return "执行命令: " + mapper.readTree(argumentsJson).path("command").asText();
        } catch (Exception e) {
            return argumentsJson;
        }
    }

    @Override public String execute(String argumentsJson) {
        try {
            JsonNode args = mapper.readTree(argumentsJson);
            String command = args.path("command").asText();
            if (command.isBlank()) return "错误：未提供 command 参数";

            // Windows 用 cmd /c，Linux/Mac 用 sh -c
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("cmd", "/c", command)
                    : new ProcessBuilder("sh", "-c", command);
            pb.directory(new File(workingDir));
            pb.redirectErrorStream(true);   // 把 stderr 合并到 stdout

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "命令超时（30秒）";
            }
            int exitCode = process.exitValue();
            return "退出码: " + exitCode + "\n输出:\n" + output;
        } catch (Exception e) {
            return "执行命令失败: " + e.getMessage();
        }
    }
}