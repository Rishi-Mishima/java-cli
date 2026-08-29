package com.mycliagent.memory;

import com.mycliagent.context.ContextProfile;
import com.mycliagent.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Memory 管理器 - Memory 系统的门面类
 * 统一管理短期记忆、长期记忆、上下文压缩和检索，
 * 为 Agent 提供简洁的记忆存取接口。
 */

public class MemoryManager {
    private final ConversationMemory shortTermMemory;
    private final LongTermMemory longTermMemory;
    private ContextCompressor compressor;
    private MemoryRetriever retriever;
    private final TokenBudget tokenBudget;
    private volatile ContextProfile contextProfile;
    private volatile String projectPath = System.getProperty("user.dir");

    public MemoryManager(LlmClient llmClient) {
        this(createShortTermMemory(ContextProfile.from(llmClient)),
                new LongTermMemory(),
                null,
                null,
                new TokenBudget(ContextProfile.from(llmClient).maxContextWindow()),
                ContextProfile.from(llmClient));
    }

    public MemoryManager(ConversationMemory shortTermMemory, LongTermMemory longTermMemory, ContextCompressor compressor, MemoryRetriever retriever, TokenBudget tokenBudget) {
        this(shortTermMemory, longTermMemory, compressor, retriever, tokenBudget, ContextProfile.from(null));
    }

    private MemoryManager(ConversationMemory shortTermMemory, LongTermMemory longTermMemory,
                          ContextCompressor compressor, MemoryRetriever retriever,
                          TokenBudget tokenBudget, ContextProfile contextProfile) {
        this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory;
        this.compressor = compressor;
        this.retriever = retriever == null ? new MemoryRetriever(shortTermMemory, longTermMemory) : retriever;
        this.tokenBudget = tokenBudget;
        this.contextProfile = contextProfile == null ? ContextProfile.from(null) : contextProfile;
    }

    // 存用户消息
    public void addUserMessage(String content) {
        addConversationMessage("user", content);
    }

    public void addAssistantMessage(String content) {
        addConversationMessage("assistant", content);
    }

    public void addToolResult(String toolName, String content) {
        addConversationMessage("tool:" + (toolName == null ? "unknown" : toolName), content);
    }

    private synchronized void addConversationMessage(String source, String content) {
        String safeContent = content == null ? "" : content;
        MemoryEntry entry = new MemoryEntry(
                source + "-" + UUID.randomUUID().toString().substring(0, 8),
                safeContent,
                MemoryEntry.MemoryType.CONVERSATION,
                Instant.now(),             // 第 4 个参数：时间戳
                Map.of("source", source),  // 第 5 个参数：元数据 Map
                MemoryEntry.estimateTokens(safeContent)
        );
        shortTermMemory.store(entry);
        compressIfNeeded();  // 自动检查是否需要压缩
    }

    public synchronized void storeFact(String fact) {
        if (fact == null || fact.isBlank()) {
            return;
        }
        MemoryEntry entry = new MemoryEntry(
                "fact-" + UUID.randomUUID().toString().substring(0, 8),
                fact.trim(),
                MemoryEntry.MemoryType.FACT,
                Instant.now(),
                Map.of("source", "agent", "projectPath", projectPath),
                MemoryEntry.estimateTokens(fact)
        );
        longTermMemory.store(entry);
    }

    public void storeFact(String fact, String scope) {
        storeFact(fact);
    }

    public synchronized void setLlmClient(LlmClient llmClient) {
        this.contextProfile = ContextProfile.from(llmClient);
    }

    public void recordTokenUsage(int inputTokens, int outputTokens, int cachedInputTokens) {
        tokenBudget.recordUsage(inputTokens, outputTokens);
    }

    public synchronized void clearShortTerm() {
        shortTermMemory.clear();
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

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        if (projectPath != null && !projectPath.isBlank()) {
            this.projectPath = Path.of(projectPath).toAbsolutePath().normalize().toString();
        }
    }

    public boolean compressIfNeeded() {
        return true;
    }

    private static ConversationMemory createShortTermMemory(ContextProfile profile) {
        return new ConversationMemory(
                new LinkedHashMap<>(),
                profile.shortTermMemoryBudget(),
                new AtomicInteger(0),
                new ArrayList<>()
        );
    }

}
