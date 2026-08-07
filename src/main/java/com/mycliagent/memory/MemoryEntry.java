package com.mycliagent.memory;

import java.time.Instant;
import java.util.Map;

public class MemoryEntry {
    private final String id;
    private final String content;
    private final MemoryType type;
    private final Instant timestamp;
    private final Map<String, String> metadata;
    private final int tokenCount;

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public MemoryType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public MemoryEntry(String id, String content, MemoryType type, Instant timestamp, Map<String, String> metadata, int tokenCount) {
        this.id = id;
        this.content = content;
        this.type = type;
        // If 'timestamp' is passed as non-null, use it; otherwise, default to the current time.
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        // If 'metadata' is passed as non-null, use it; otherwise, default to an immutable empty Map.
        this.metadata = metadata != null ? metadata : Map.of();
        this.tokenCount = tokenCount;
    }

    public enum MemoryType{
        CONVERSATION,  // 对话记忆
        FACT,          // 事实记忆（用户偏好、项目信息）
        SUMMARY,       // 摘要记忆
        TOOL_RESULT    // 工具执行结果
    }


    /**
     * 粗略估算 token 数（中文约 1.5 字/token，英文约 4 字符/token）
     */

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;

        // Expand range to include CJK Unified Ideographs & Full-width Punctuation
        long chineseChars = text.codePoints()
                .filter(c -> (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0xFF00 && c <= 0xFFEF))
                .count();

        // Total code points rather than raw UTF-16 char length
        long totalCodePoints = text.codePointCount(0, text.length());
        long otherChars = totalCodePoints - chineseChars;

        return (int) Math.ceil((chineseChars / 1.5) + (otherChars / 4.0));
    }
}
