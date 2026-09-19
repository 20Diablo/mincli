package agentlearning.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class JsonRpcClient implements AutoCloseable {

    private final StdioTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong idCounter = new AtomicLong(1);

    /** 请求配对表：id -> 等待结果的 Future（异步配对的核心） */
    private final ConcurrentHashMap<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();

    /** 通知监听器（广播目标） */
    private volatile Consumer<JsonNode> notificationListener = n -> {};

    /** 后台读线程 */
    private final Thread readerThread;
    private volatile boolean running = true;

    public JsonRpcClient(StdioTransport transport) {
        this.transport = transport;
        this.readerThread = new Thread(this::readLoop, "jsonrpc-reader");
        this.readerThread.setDaemon(true);   // 守护线程，主程序退出跟着退
        this.readerThread.start();
    }

    /** 后台读线程：持续读 stdout，按 id 分发，通知走广播 */
    private void readLoop() {
        while (running) {
            try {
                String line = transport.receive();
                if (line == null) break;            // server 关闭
                if (line.isBlank()) continue;

                JsonNode msg = mapper.readTree(line);

                if (msg.has("id") && !msg.get("id").isNull()) {
                    // 是响应 → 按 id 配对
                    long id = msg.path("id").asLong();
                    CompletableFuture<JsonNode> future = pending.remove(id);
                    if (future != null) {
                        if (msg.has("error") && !msg.get("error").isNull()) {
                            future.completeExceptionally(
                                    new IOException("MCP 错误: " + msg.get("error")));
                        } else {
                            future.complete(msg.path("result"));
                        }
                    }
                } else {
                    // 是通知 → 广播给监听器
                    notificationListener.accept(msg);
                }
            } catch (IOException e) {
                break;   // 读失败/关闭，结束读线程
            }
        }
    }

    /** 发请求，立即返回 Future（异步）；调用方自己 get(timeout) */
    public CompletableFuture<JsonNode> requestAsync(String method, JsonNode params) throws IOException {
        long id = idCounter.getAndIncrement();

        ObjectNode req = mapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", id);
        req.put("method", method);
        req.set("params", params == null ? mapper.createObjectNode() : params);

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);          // ← 放进配对表

        transport.send(mapper.writeValueAsString(req));
        return future;
    }

    /** 同步便捷方法：requestAsync + 超时等待，超时/异常时清理 pending */
    public JsonNode request(String method, JsonNode params, long timeoutSeconds) throws IOException {
        long id = idCounter.getAndIncrement();

        ObjectNode req = mapper.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", id);
        req.put("method", method);
        req.set("params", params == null ? mapper.createObjectNode() : params);

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);
        transport.send(mapper.writeValueAsString(req));

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);   // ← 超时调度
        } catch (TimeoutException e) {
            pending.remove(id);   // 超时清理，避免残留
            throw new IOException("MCP 请求超时: " + method);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.remove(id);
            throw new IOException("MCP 请求被中断: " + method);
        } catch (ExecutionException e) {
            pending.remove(id);
            throw new IOException("MCP 请求失败: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /** 注册通知监听器（广播） */
    public void onNotification(Consumer<JsonNode> listener) {
        if (listener != null) {
            this.notificationListener = listener;
        }
    }

    /** 发通知（无 id，不等响应） */
    public void notify(String method, JsonNode params) throws IOException {
        ObjectNode note = mapper.createObjectNode();
        note.put("jsonrpc", "2.0");
        note.put("method", method);
        note.set("params", params == null ? mapper.createObjectNode() : params);
        transport.send(mapper.writeValueAsString(note));
    }

    @Override
    public void close() {
        running = false;
        readerThread.interrupt();
    }
}