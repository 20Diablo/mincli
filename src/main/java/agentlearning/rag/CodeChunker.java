package agentlearning.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CodeChunker {

    private static final int MAX_CHUNK_CHARS = 2000;

    /** 对一个文件分块 */
    public List<CodeChunk> chunkFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        String path = filePath.toString();

        // 小文件：整体一块
        if (content.length() <= MAX_CHUNK_CHARS) {
            return List.of(new CodeChunk(path, filePath.getFileName().toString(), content, 1,
                    content.split("\r?\n").length));
        }
        return chunkByLines(path, content);
    }

    /** 大文件按行分段 */
    private List<CodeChunk> chunkByLines(String filePath, String content) {
        List<CodeChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\r?\n");
        StringBuilder seg = new StringBuilder();
        int startLine = 1;

        for (int i = 0; i < lines.length; i++) {
            if (seg.length() + lines[i].length() + 1 > MAX_CHUNK_CHARS && !seg.isEmpty()) {
                chunks.add(new CodeChunk(filePath, "chunk#" + chunks.size(), seg.toString().trim(),
                        startLine, i));
                seg.setLength(0);
                startLine = i + 1;
            }
            seg.append(lines[i]).append("\n");
        }
        if (!seg.isEmpty()) {
            chunks.add(new CodeChunk(filePath, "chunk#" + chunks.size(), seg.toString().trim(),
                    startLine, lines.length));
        }
        return chunks;
    }
}