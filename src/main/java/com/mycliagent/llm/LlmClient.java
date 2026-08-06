package com.mycliagent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;

public interface LlmClient {
    String getModelName();

    String getProviderName();

    ChatResponse chat(List<Message> messages, List<Tool> tools)
            throws IOException;

    public record ChatResponse(
            String content,
            List<ToolCall> toolCalls
    ) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    public record Message(
            String role,
            String content,
            List<ToolCall> toolCalls,
            String toolCallId
    ) {
        public static Message system(String content) {
            return new Message("system", content, null, null);
        }

        public static Message user(String content) {
            return new Message("user", content, null, null);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content, null, null);
        }

        public static Message assistant(String content, List<ToolCall> toolCalls) {
            return new Message("assistant", content, toolCalls, null);
        }

        public static Message tool(String toolCallId, String content) {
            return new Message("tool", content, null, toolCallId);
        }

    }

    public record Tool(String name, String description, JsonNode parameters) {
    }

    public record ToolCall(String id, FunctionCall function) {
    }

    public record FunctionCall(String name, String arguments) {
    }

}




