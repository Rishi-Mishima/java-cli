package com.mycliagent.memory;

import com.mycliagent.llm.LlmClient;

import java.util.List;

public class ConversationHistoryCompactor {
    private LlmClient llmClient;

    public ConversationHistoryCompactor() {
    }

    public ConversationHistoryCompactor(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void setLlmClient(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public boolean compactIfNeeded(List<LlmClient.Message> conversationHistory, int i) {
        return false;
    }

    public boolean compactNow(List<LlmClient.Message> conversationHistory) {
        return false;
    }
}
