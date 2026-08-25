package com.mycliagent.agent;

import com.mycliagent.llm.LlmClient;

import java.util.List;

public record ToolExecutionResult(String id, String name, String result, List<LlmClient.ContentPart> imageParts) {
    public ToolExecutionResult(String id, String name, String result) {
        this(id, name, result, List.of());
    }

    public boolean hasImageParts() {
        return imageParts != null && !imageParts.isEmpty();
    }
}
