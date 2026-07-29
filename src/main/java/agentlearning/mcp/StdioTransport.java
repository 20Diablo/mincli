package agentlearning.mcp;

import java.io.*;
import java.util.List;

public class StdioTransport implements AutoCloseable {
    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;

    /** 启动 server 子进程，比如 command="npx", args=["-y","@modelcontextprotocol/server-everything"] */
    public StdioTransport(String command, List<String> args) throws IOException {
        List<String> fullCommand = new java.util.ArrayList<>();
        fullCommand.add(command);
        fullCommand.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        // 注意：不要 redirectErrorStream(true)，否则 server 的日志会混进 stdout 破坏 JSON
        this.process = pb.start();
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
    }

    /** 发送一行 JSON 到 server 的 stdin */
    public void send(String jsonLine) throws IOException {
        stdin.write(jsonLine);
        stdin.write("\n");
        stdin.flush();
    }

    /** 从 server 的 stdout 读一行 JSON（阻塞直到有一行） */
    public String receive() throws IOException {
        return stdout.readLine();
    }

    @Override
    public void close() {
        try { stdin.close(); } catch (IOException ignored) {}
        process.destroy();
    }
}