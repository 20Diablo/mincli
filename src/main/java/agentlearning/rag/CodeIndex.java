package agentlearning.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 代码索引：扫描项目 → 分块 → 向量化 → 存内存。
 * 检索时直接余弦相似度找 TopK。
 */
public class CodeIndex {

    private final CodeChunker chunker = new CodeChunker();
    private final SimpleEmbedder embedder = new SimpleEmbedder();

    /** 索引项：块 + 它的向量 */
    private static class IndexedChunk {
        CodeChunk chunk;
        float[] vector;
        IndexedChunk(CodeChunk c, float[] v) { chunk = c; vector = v; }
    }

    private final List<IndexedChunk> entries = new ArrayList<>();

    /** 扫描并索引一个目录（递归） */
    public int indexDirectory(String dirPath) throws IOException {
        entries.clear();
        Path root = Path.of(dirPath);
        List<Path> files = new ArrayList<>();

        // 收集代码文件（递归），跳过 .git、target 等
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> isCodeFile(p))
                    .filter(p -> !p.toString().contains(".git")
                            && !p.toString().contains("target")
                            && !p.toString().contains("node_modules"))
                    .forEach(files::add);
        }

        for (Path file : files) {
            List<CodeChunk> chunks = chunker.chunkFile(file);
            for (CodeChunk c : chunks) {
                entries.add(new IndexedChunk(c, embedder.embed(c.content())));
            }
        }
        return entries.size();
    }

    /** 语义检索：返回最相似的 TopK 个块 */
    public List<CodeChunk> search(String query, int topK) {
        float[] qv = embedder.embed(query);
        return entries.stream()
                .map(e -> new Object[]{ e, cosine(qv, e.vector) })
                .sorted((a, b) -> Double.compare((double) b[1], (double) a[1]))
                .limit(topK)
                .map(e -> ((IndexedChunk) e[0]).chunk)
                .toList();
    }

    private static boolean isCodeFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".py") || name.endsWith(".js")
                || name.endsWith(".ts") || name.endsWith(".md") || name.endsWith(".txt")
                || name.endsWith(".json");
    }

    private static double cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}