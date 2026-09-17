package agentlearning.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryEntry {

    private MemoryType type;
    private String content;
    private long createdAt;
    private Map<String, String> metadata = new HashMap<>();   // 来源、工具名等

    public MemoryEntry() {}

    public MemoryEntry(MemoryType type, String content, long createdAt) {
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String toPromptLine() {
        return "- [" + (type == null ? "记忆" : type.label()) + "] " + content;
    }
}