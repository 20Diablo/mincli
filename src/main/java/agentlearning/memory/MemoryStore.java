package agentlearning.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 结构化记忆存储:
 * - 以 JSON 数组持久化到 ~/.minicli/memory.json
 * - 超出 token 阈值时自动淘汰最旧条目
 * - 启动时读回,注入 system prompt
 */
public class MemoryStore {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 记忆总 token 上限,超过就淘汰最旧的(可按需调) */
    private static final int MAX_MEMORY_TOKENS = 800;

    public MemoryStore() {
        Path dir = Path.of(System.getProperty("user.home"), ".minicli");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        this.file = dir.resolve("memory.json");
    }

    /** 读出全部记忆(按添加顺序) */
    public List<MemoryEntry> loadAll() {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            String json = Files.readString(file);
            if (json.isBlank()) return new ArrayList<>();
            return mapper.readValue(json, new TypeReference<List<MemoryEntry>>() {});
        } catch (IOException e) {
            System.out.println("读取记忆失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 保存一条记忆:追加 → 淘汰超限的 → 持久化 */
    public void save(MemoryType type, String content) {
        if (content == null || content.isBlank()) return;
        List<MemoryEntry> entries = loadAll();
        entries.add(new MemoryEntry(type, content.trim(), System.currentTimeMillis()));

        // 淘汰:总量超上限时,从最旧的开始删(entries 是添加顺序,index 0 最旧)
        int removed = evict(entries);
        persist(entries);

        if (removed > 0) {
            System.out.println("已淘汰 " + removed + " 条最旧记忆(超出 token 上限)");
        }
    }

    /**
     * 淘汰最旧条目直到总量回到阈值内。返回淘汰条数。
     */
    private int evict(List<MemoryEntry> entries) {
        int removed = 0;
        while (totalTokens(entries) > MAX_MEMORY_TOKENS && entries.size() > 1) {
            entries.remove(0);   // 删最旧的(index 0)
            removed++;
        }
        return removed;
    }

    /** 估算所有记忆的 token(复用你阶段 4 的 TokenBudget 思路) */
    private int totalTokens(List<MemoryEntry> entries) {
        int total = 0;
        for (MemoryEntry e : entries) {
            String line = e.toPromptLine();
            total += line.length() / 2 + 4;   // 粗估:2 字符≈1 token
        }
        return total;
    }

    /** 把记忆列表写回 JSON 文件 */
    private void persist(List<MemoryEntry> entries) {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entries);
            Files.writeString(file, json);
        } catch (IOException e) {
            System.out.println("保存记忆失败: " + e.getMessage());
        }
    }

    /** 清空全部记忆 */
    public void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {}
    }

    /** 拼成注入 system prompt 的一段文本 */
    public String asPromptSection() {
        List<MemoryEntry> all = loadAll();
        if (all.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n\n[关于用户的长期记忆]\n");
        for (MemoryEntry e : all) {
            sb.append(e.toPromptLine()).append("\n");
        }
        return sb.toString();
    }
}