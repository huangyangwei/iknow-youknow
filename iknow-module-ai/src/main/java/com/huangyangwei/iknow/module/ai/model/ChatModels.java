package com.huangyangwei.iknow.module.ai.model;

/**
 * 多模型业务键（技术方案 ModelRouter）：与 ModelConfig 注册表一致。
 */
public final class ChatModels {

    private ChatModels() {
    }

    public static final String CLAUDE_OPUS_5 = "claude-opus-5";
    public static final String GPT_4O = "gpt-4o";
    public static final String GEMINI_2_5_PRO = "gemini-2.5-pro";
    public static final String DEEPSEEK_V3 = "deepseek-v3";
    /** 本地确定性桩，沙箱无密钥时唯一可调用的模型。 */
    public static final String DETERMINISTIC = "deterministic";

    public static final String DEFAULT_MODEL = CLAUDE_OPUS_5;
}
