package com.huangyangwei.iknow.module.ai.support;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块（技术方案 §5.2）：按句子边界切分，贪心打包成约 maxTokens 的块，
 * 块间携带 overlapTokens 的尾部重叠，保证语义连续片段不被截断。
 * token 估算：中文/全角字符按 1 token、ASCII 按 1/4 token（≈1.6 字符/token 的经验比）。
 */
@Component
public class ChunkTextSplitter {

    public static final int DEFAULT_MAX_TOKENS = 512;
    public static final int DEFAULT_OVERLAP_TOKENS = 64;

    public List<String> split(String text) {
        return split(text, DEFAULT_MAX_TOKENS, DEFAULT_OVERLAP_TOKENS);
    }

    public List<String> split(String text, int maxTokens, int overlapTokens) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (estimateTokens(normalized) <= maxTokens) {
            return List.of(normalized);
        }

        List<String> sentences = splitSentences(normalized);
        List<String> chunks = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        int bufferTokens = 0;

        for (String sentence : sentences) {
            int tokens = estimateTokens(sentence);
            if (bufferTokens + tokens > maxTokens && !buffer.isEmpty()) {
                chunks.add(String.join("", buffer));
                buffer = overlapTail(buffer, overlapTokens);
                bufferTokens = estimateTokens(String.join("", buffer));
            }
            buffer.add(sentence);
            bufferTokens += tokens;
        }
        if (!buffer.isEmpty()) {
            chunks.add(String.join("", buffer));
        }
        return chunks;
    }

    /** 取尾部句子作为下一块的重叠种子（至少保留一句，总额不超过 overlapTokens）。 */
    private List<String> overlapTail(List<String> sentences, int overlapTokens) {
        List<String> tail = new ArrayList<>();
        int acc = 0;
        for (int i = sentences.size() - 1; i >= 0; i--) {
            int tokens = estimateTokens(sentences.get(i));
            if (!tail.isEmpty() && acc + tokens > overlapTokens) {
                break;
            }
            tail.add(0, sentences.get(i));
            acc += tokens;
        }
        return tail;
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '!' || c == '?' || c == ';' || c == '\n') {
                if (i > start) {
                    sentences.add(text.substring(start, i + 1));
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            sentences.add(text.substring(start));
        }
        if (sentences.isEmpty()) {
            sentences.add(text);
        }
        return sentences;
    }

    /** 经验 token 估算：中文按 1，ASCII 按 1/4。 */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int ascii = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c <= 0x7F) {
                ascii++;
            } else {
                cjk++;
            }
        }
        return cjk + (int) Math.ceil(ascii / 4.0);
    }
}
