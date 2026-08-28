package com.mycliagent.memory;

import com.mycliagent.llm.LlmClient;

import java.util.List;

public class TokenBudget {

    private final int contextWindow;    // 模型上下文窗口大小
    private final int reservedForSystem; // 系统提示预留
    private final int reservedForTools;  // 工具定义预留
    private final int reservedForResponse; // 回复预留

    // 累计 token 消耗统计
    private int totalInputTokens;
    private int totalOutputTokens;
    private int totalCachedInputTokens;
    private int llmCallCount;


    public TokenBudget(int contextWindow) {
        this(contextWindow, 500, 800, 2000);
    }

    /**
     * @param contextWindow       模型上下文窗口（如 128K = 131072）
     * @param reservedForSystem   系统提示预留 token 数
     * @param reservedForTools    工具定义预留 token 数
     * @param reservedForResponse 回复预留 token 数
     */
    public TokenBudget(int contextWindow, int reservedForSystem, int reservedForTools, int reservedForResponse) {
        this.contextWindow = contextWindow;
        this.reservedForSystem = reservedForSystem;
        this.reservedForTools = reservedForTools;
        this.reservedForResponse = reservedForResponse;
        this.totalInputTokens = 0;
        this.totalOutputTokens = 0;
        this.totalCachedInputTokens = 0;
        this.llmCallCount = 0;
    }

    /**
     * 获取对话历史可用的 token 预算
     */
    public int getAvailableForConversation() {
        return contextWindow - reservedForSystem
                - reservedForTools - reservedForResponse;
        // 200000 - 500 - 800 - 2000 = 196700
    }

    /**
     * 检查给定的消息列表是否在预算内
     */
    public boolean isWithinBudget(List<LlmClient.Message> messages) {
        int estimatedTokens = estimateMessagesTokens(messages);
        return estimatedTokens <= getAvailableForConversation();
    }

    public static int estimateMessagesTokens(List<LlmClient.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (LlmClient.Message message : messages) {
            if (message == null) {
                continue;
            }
            total += MemoryEntry.estimateTokens(message.role());
            total += MemoryEntry.estimateTokens(message.content());
            total += MemoryEntry.estimateTokens(message.reasoningContent());
            if (message.toolCalls() != null) {
                total += MemoryEntry.estimateTokens(message.toolCalls().toString());
            }
            if (message.contentParts() != null) {
                total += MemoryEntry.estimateTokens(message.contentParts().toString());
            }
        }
        return Math.max(1, total);
    }


    public boolean needsCompression(ConversationMemory memory) {
        return memory.getTokenCount()
                > getAvailableForConversation() * 0.8;
    }

    public void recordUsage(int inputTokens, int outputTokens) {
        totalInputTokens += inputTokens;
        totalOutputTokens += outputTokens;
        llmCallCount++;
    }

    public String getUsageReport() {
        return String.format(
                "Token 统计: 调用 %d 次 | 总输入: %d | 总输出: %d",
                llmCallCount, totalInputTokens, totalOutputTokens);
    }





}
