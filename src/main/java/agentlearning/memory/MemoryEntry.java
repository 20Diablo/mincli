package agentlearning.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)   // 反序列化时忽略不认识的字段,便于以后加字段
public class MemoryEntry {

    private MemoryType type;    // 记忆类型
    private String content;     // 记忆内容
    private long createdAt;     // 创建时间(毫秒时间戳)

    public MemoryEntry() {}     // Jackson 反序列化需要无参构造

    public MemoryEntry(MemoryType type, String content, long createdAt) {
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Jackson 靠 getter 发现字段,所以必须有 getter/setter
    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** 给 system prompt 用的展示格式 */
    public String toPromptLine() {
        return "- [" + (type == null ? "记忆" : type.label()) + "] " + content;
    }
}