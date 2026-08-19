package agentlearning.rag;

import java.util.HashSet;
import java.util.Set;

/**
 * 简易 embedding：基于字符 n-gram 特征的哈希向量。
 * 生产环境应替换为真实 embedding 模型（如 OpenAI text-embedding 或本地 ollama）。
 */
public class SimpleEmbedder {

    private static final int DIM = 256;   // 向量维度
    private static final int NGRAM = 2;   // 字符 n-gram 大小

    public float[] embed(String text) {
        float[] vec = new float[DIM];
        if (text == null || text.isBlank()) return vec;

        // 1. 取字符 n-gram（连续 n 个字符的组合）
        Set<String> grams = new HashSet<>();
        String norm = text.toLowerCase();
        for (int i = 0; i <= norm.length() - NGRAM; i++) {
            grams.add(norm.substring(i, i + NGRAM));
        }

        // 2. 每个 gram 哈希到向量某一位，累加
        for (String g : grams) {
            int idx = Math.floorMod(g.hashCode(), DIM);
            vec[idx] += 1.0f;
        }

        // 3. 归一化（让向量长度统一为 1，余弦相似度才有意义）
        return normalize(vec);
    }

    private float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0) return v;
        for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
        return v;
    }
}