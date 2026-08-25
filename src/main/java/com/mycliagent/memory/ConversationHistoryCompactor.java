package com.mycliagent.memory;

import com.mycliagent.llm.LlmClient;

import java.util.List;

public class ConversationHistoryCompactor {
    public ConversationHistoryCompactor() {
    }

    public ConversationHistoryCompactor(LlmClient llmClient) {
    }

    public boolean compactIfNeeded(List<LlmClient.Message> conversationHistory, int i) {
        return false;
    }
}
