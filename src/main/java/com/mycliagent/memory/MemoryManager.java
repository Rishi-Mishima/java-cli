package com.mycliagent.memory;

import com.mycliagent.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Memory 管理器 - Memory 系统的门面类
 * 统一管理短期记忆、长期记忆、上下文压缩和检索，
 * 为 Agent 提供简洁的记忆存取接口。
 */

public class MemoryManager {
    private final ConversationMemory shortTermMemory;
    private final LongTermMemory longTermMemory;
    private final ContextCompressor compressor;
    private final MemoryRetriever retriever;
    private final TokenBudget tokenBudget;

    // 存用户消息
    public void addUserMessage(String content) {
        shortTermMemory.store(entry);
        compressIfNeeded();  // 自动检查是否需要压缩
    }

    // 检索相关记忆
    public String buildContextForQuery(String query, int maxTokens) {
        return retriever.buildContextForQuery(query, maxTokens);
    }

    // 系统状态
    public String getSystemStatus() {
        return shortTermMemory.getStatusSummary() + "\n"
                + longTermMemory.getStatusSummary() + "\n"
                + tokenBudget.getUsageReport();
    }

    // Getter
    public ConversationMemory getShortTermMemory() { return shortTermMemory; }
    public LongTermMemory getLongTermMemory() { return longTermMemory; }
    public TokenBudget getTokenBudget() { return tokenBudget; }
    public ContextProfile getContextProfile() { return contextProfile; }
}
