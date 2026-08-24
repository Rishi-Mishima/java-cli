package com.mycliagent.llm;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MockLlmClient implements LlmClient {
    @Override
    public String getModelName() {
        return "mock-agent";
    }

    @Override
    public String getProviderName() {
        return "Mock";
    }

    @Override
    public ChatResponse chat(List<Message> messages, List<Tool> tools) throws IOException {
        Message lastMessage = messages.get(messages.size() - 1);
        if ("tool".equals(lastMessage.role())) {
            return new ChatResponse(formatToolObservation(messages, lastMessage.content()), List.of());
        }

        String input = lastMessage.content();
        String normalized = input.toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "readme", "读取", "读文件", "read_file")) {
            return callTool("read_file", "{\"path\":\"README.md\"}");
        }
        if (containsAny(normalized, "目录", "列出", "list", "src/main/java")) {
            return callTool("list_dir", "{\"path\":\"src/main/java\"}");
        }
        if (containsAny(normalized, "编译", "package", "mvn")) {
            return callTool("execute_command", "{\"command\":\"mvn -q package\"}");
        }
        if (containsAny(normalized, "创建文件", "写文件", "temp-agent-test")) {
            return callTool("write_file", "{\"path\":\"temp-agent-test.txt\",\"content\":\"hello from mock agent\"}");
        }

        String response = """
                你好，我是 MyCliAgent 的 Mock LLM。
                当前不需要 API key，也不会请求真实模型；你可以让我读取 README、列目录、执行 mvn package，或创建 temp-agent-test.txt 来演示工具调用流程。
                """;
        return new ChatResponse(response, List.of());
    }

    private ChatResponse callTool(String toolName, String arguments) {
        ToolCall toolCall = new ToolCall(
                "mock-call-" + UUID.randomUUID().toString().substring(0, 8),
                new FunctionCall(toolName, arguments)
        );
        return new ChatResponse("", List.of(toolCall));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String formatToolObservation(List<Message> messages, String observation) {
        String lastUserMessage = findLastUserMessage(messages);
        String normalizedUserMessage = lastUserMessage.toLowerCase(Locale.ROOT);

        if (observation.startsWith("文件内容:") && containsAny(normalizedUserMessage, "总结", "介绍", "做什么")) {
            return """
                    Mock 模式已完成 read_file 工具调用。
                    项目摘要：MyCliAgent 是一个 Java CLI AI Agent，用于演示 ReAct 工具调用、LLM provider 抽象、短期/长期记忆，以及 Plan-and-Execute 任务规划原型。
                    """;
        }

        if (containsAny(normalizedUserMessage, "目录", "列出", "list")) {
            return "Mock 模式已完成 list_dir 工具调用，目录结果如下：\n" + observation;
        }

        if (containsAny(normalizedUserMessage, "编译", "package", "mvn")) {
            return "Mock 模式已完成 execute_command 工具调用，命令结果如下：\n" + observation;
        }

        return "Mock 模式已收到工具结果：\n" + observation;
    }

    private String findLastUserMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if ("user".equals(message.role())) {
                return message.content();
            }
        }
        return "";
    }
}
