package agentlearning.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 简版长期记忆：一行一条，存到 C:\Users\Aprilia\.minicli\memory.txt。
 * 启动时读回，注入 system prompt，让模型跨会话记住稳定事实。
 */
public class LongTermMemory {

    private final Path file;

    public LongTermMemory() {
        Path dir = Path.of(System.getProperty("user.home"), ".minicli");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        this.file = dir.resolve("memory.txt");
    }

    /** 保存一条记忆 */
    public void save(String content) {
        try {
            Files.writeString(file, content.trim() + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("保存记忆失败: " + e.getMessage());
        }
    }

    /** 读出所有记忆 */
    public List<String> loadAll() {
        try {
            if (!Files.exists(file)) return new ArrayList<>();
            return Files.readAllLines(file).stream()
                    .filter(s -> !s.isBlank())
                    .toList();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /** 拼成注入 system prompt 的一段文本 */
    public String asPromptSection() {
        List<String> all = loadAll();
        if (all.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n\n[关于用户的长期记忆]\n");
        for (String m : all) sb.append("- ").append(m).append("\n");
        return sb.toString();
    }
}