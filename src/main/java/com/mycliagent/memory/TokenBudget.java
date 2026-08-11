package com.mycliagent.memory;

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
