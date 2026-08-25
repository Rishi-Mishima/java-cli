package com.mycliagent.agent;

import com.mycliagent.llm.LlmClient;
import org.slf4j.Logger;

final class LlmTraceLogger {
    private LlmTraceLogger() {
    }

    static void logReasoning(Logger log, String scope, LlmClient llmClient, String reasoningContent) {
        if (log != null && reasoningContent != null && !reasoningContent.isBlank()) {
            log.debug("{} reasoning: {}", scope, reasoningContent);
        }
    }
}
