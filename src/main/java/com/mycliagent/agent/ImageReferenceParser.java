package com.mycliagent.agent;

import com.mycliagent.llm.LlmClient;

import java.nio.file.Path;

final class ImageReferenceParser {
    private ImageReferenceParser() {
    }

    static LlmClient.Message userMessage(String content, Path projectPath) {
        return LlmClient.Message.user(content == null ? "" : content);
    }
}
