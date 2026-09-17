package agentlearning.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一记忆层:按类型管理记忆,并对不同类型施加不同策略。
 * - 会话内(CONVERSATION/TOOL_RESULT/SUMMARY):存内存,不持久化
 * - 跨会话(FACT):持久化到 JSON,注入 system prompt
 */
public class MemoryManager {

    /** 会话内记忆(不落盘) */
    private final List<MemoryEntry> shortTerm = new ArrayList<>();

    /** 跨会话记忆(JSON 持久化) */
    private final MemoryStore longTerm = new MemoryStore();

    /** 工具结果在记忆里的最大长度(完整结果在 history 里,记忆只留摘要) */
    private static final int MAX_TOOL_RESULT_CHARS = 500;

    // ---------- 会话内记忆 ----------

    public void recordUserMessage(String content) {
        shortTerm.add(withSource(new MemoryEntry(MemoryType.CONVERSATION, content, now()), "user"));
    }

    public void recordAssistantMessage(String content) {
        shortTerm.add(withSource(new MemoryEntry(MemoryType.CONVERSATION, content, now()), "assistant"));
    }

    /** 记录工具结果:超过 MAX 字符则截断 */
    public void recordToolResult(String toolName, String result) {
        String truncated = result.length() > MAX_TOOL_RESULT_CHARS
                ? result.substring(0, MAX_TOOL_RESULT_CHARS) + "...(已截断)"
                : result;
        MemoryEntry e = new MemoryEntry(MemoryType.TOOL_RESULT,
                "[" + toolName + "] " + truncated, now());
        e.getMetadata().put("source", "tool");
        e.getMetadata().put("toolName", toolName);
        shortTerm.add(e);
    }

    /** 记录压缩摘要 */
    public void recordSummary(String summary) {
        shortTerm.add(withSource(new MemoryEntry(MemoryType.SUMMARY, summary, now()), "compactor"));
    }

    // ---------- 跨会话记忆 ----------

    /** 存一条长期事实(持久化) */
    public void storeFact(String fact) {
        longTerm.save(MemoryType.FACT, fact);
    }

    // ---------- 注入与查询 ----------

    /**
     * 注入 system prompt 的内容:只取 FACT(长期)。
     * 会话内的对话/工具结果不进 prompt —— 它们已经在 history 里了,重复注入只会占 token。
     */
    public String promptSection() {
        return longTerm.asPromptSection();
    }

    /** 会话内记忆(供调试/展示) */
    public List<MemoryEntry> shortTermEntries() {
        return new ArrayList<>(shortTerm);
    }

    /** 长期记忆(供展示/管理) */
    public List<MemoryEntry> longTermEntries() {
        return longTerm.loadAll();
    }

    public void clearLongTerm() {
        longTerm.clear();
    }

    /** 按类型统计数量 */
    public java.util.Map<MemoryType, Integer> countByType() {
        java.util.Map<MemoryType, Integer> counts = new java.util.EnumMap<>(MemoryType.class);
        for (MemoryEntry e : shortTerm) counts.merge(e.getType(), 1, Integer::sum);
        for (MemoryEntry e : longTerm.loadAll()) counts.merge(e.getType(), 1, Integer::sum);
        return counts;
    }

    private MemoryEntry withSource(MemoryEntry e, String source) {
        e.getMetadata().put("source", source);
        return e;
    }

    private long now() { return System.currentTimeMillis(); }
}