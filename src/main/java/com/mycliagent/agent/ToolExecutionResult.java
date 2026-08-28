package com.mycliagent.agent;

import com.mycliagent.llm.LlmClient;
import com.mycliagent.tool.ToolOutput;

import java.util.List;

public final class ToolExecutionResult {
    private final String id;
    private final String name;
    private final String argumentsJson;
    private final String result;
    private final long elapsedMillis;
    private final boolean timedOut;
    private final List<LlmClient.ContentPart> imageParts;

    public ToolExecutionResult(String id, String name, String result) {
        this(id, name, "", result, 0L, false, List.of());
    }

    public ToolExecutionResult(String id, String name, String result, List<LlmClient.ContentPart> imageParts) {
        this(id, name, "", result, 0L, false, imageParts);
    }

    public ToolExecutionResult(String id, String name, String argumentsJson, String result,
                               long elapsedMillis, boolean timedOut,
                               List<LlmClient.ContentPart> imageParts) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson == null ? "" : argumentsJson;
        this.result = result == null ? "" : result;
        this.elapsedMillis = elapsedMillis;
        this.timedOut = timedOut;
        this.imageParts = imageParts == null ? List.of() : List.copyOf(imageParts);
    }

    public static ToolExecutionResult completed(ToolInvocation invocation, ToolOutput output, long elapsedMillis) {
        return new ToolExecutionResult(
                invocation.id(),
                invocation.name(),
                invocation.argumentsJson(),
                output == null ? "" : output.text(),
                elapsedMillis,
                false,
                output == null ? List.of() : output.imageParts());
    }

    public static ToolExecutionResult failed(ToolInvocation invocation, String message) {
        return completed(invocation, ToolOutput.text("工具执行失败: " + message), 0L);
    }

    public static ToolExecutionResult timedOut(ToolInvocation invocation, long timeoutSeconds) {
        return new ToolExecutionResult(
                invocation.id(),
                invocation.name(),
                invocation.argumentsJson(),
                "工具执行超时（" + timeoutSeconds + "秒），已取消",
                timeoutSeconds * 1000L,
                true,
                List.of());
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String argumentsJson() {
        return argumentsJson;
    }

    public String result() {
        return result;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }

    public boolean timedOut() {
        return timedOut;
    }

    public List<LlmClient.ContentPart> imageParts() {
        return imageParts;
    }

    public boolean hasImageParts() {
        return !imageParts.isEmpty();
    }
}
