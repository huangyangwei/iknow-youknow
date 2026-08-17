package com.huangyangwei.iknow.module.ai.support;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地确定性 EmbeddingModel（1024 维）：字符 unigram + bigram 哈希落到 1024 维桶并归一化。
 * 共享字符/词片段的文本获得较高余弦相似度，用于在无外部 embedding API 的沙箱中
 * 验证 PgVectorStore 写入/检索链路。维度与生产锁定的 BGE-M3(1024) 对齐。
 * 仅作为链路验证桩，生产环境必须替换为真实 embedding 模型（M0 结论）。
 */
public class DeterministicEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 1024;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            embeddings.add(new Embedding(compute(instructions.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return compute(document.getText());
    }

    @Override
    public String getEmbeddingContent(Document document) {
        return document.getText();
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private float[] compute(String text) {
        float[] vector = new float[DIMENSIONS];
        String s = text.toLowerCase();
        int prev = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                prev = -1;
                continue;
            }
            vector[Math.floorMod(hashOf(c), DIMENSIONS)] += 1f;
            if (prev >= 0) {
                vector[Math.floorMod(hashOf(prev * 31 + c), DIMENSIONS)] += 2f;
            }
            prev = c;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] /= (float) norm;
            }
        }
        return vector;
    }

    private int hashOf(int v) {
        v ^= v >>> 16;
        return v;
    }
}
