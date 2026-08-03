package com.mycliagent.llm;

import com.fasterxml.jackson.databind.JsonNode;

public interface LlmClient {
    String getModelName();

    String getProviderName();

    public record Message(
            String role,
            String content,
            List<ToolCall> toolcalls,
            String toolCallId
    ){
        public static Message system(String content) {
            return new Message("system", content, null, null);
        }

        public static Message user(String content) {
            return new Message("user", content, null, null);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content, null, null);
        }

        public static Message tool(String toolCallId, String content) {
            return new Message("tool", content, null, toolCallId);
        }
    }

    public record Tool(String name, String description, JsonNode parameters) {}

}
