package com.mycliagent.agent;

import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.tool.ToolRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Agent {


    private static final int MAX_RETRIES = 3;
    private final GLMClient llmClient;
    //保存所有工具。
    private final ToolRegistry toolRegistry;
    //  保存所有聊天记录
    private final List<Message> conversationHistory;
    //限制 Agent 最多循环 10 次。 - 是一种保险措施,不然可能永远停不下来
    private static final int MAX_ITERATIONS = 10;

    // 系统提示词
    private static final String SYSTEM_PROMPT = """
        你是一个智能编程助手，可以帮助用户完成各种任务。

        你可以使用以下工具来完成任务：
        1. read_file - 读取文件内容
        2. write_file - 写入文件内容
        3. list_dir - 列出目录内容
        4. execute_command - 执行Shell命令
        5. create_project - 创建新项目结构

        当需要操作文件、执行命令或创建项目时，请使用工具调用。
        使用工具后，根据工具返回的结果继续思考下一步行动。

        请用中文回复用户。
        """;

    public Agent(String apiKey) {
        this.llmClient = new GLMClient(apiKey);
        this.toolRegistry = new ToolRegistry();
        this.conversationHistory = new ArrayList<>();

        // 添加系统提示
        // 创建AGENT后, 聊天记录并不是空的 而是[System Prompt]
        conversationHistory.add(LlmClient.Message.system(SYSTEM_PROMPT));
    }

    public String run(String userInput) {
        // 添加用户输入 - 加入聊天历史
        conversationHistory.add(Message.user(userInput));

        // 初始化迭代次数
        int iteration = 0;
        int retryCount = 0;
        //开始Agent循环, 小于Max
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            ChatResponse response = null;

            try {
                // 调用 LLM: 发送当前完整聊天记录, 以及可以使用的工具定义
                response = llmClient.chat(
                        conversationHistory,
                        toolRegistry.getToolDefinitions()
                );
            } catch (IOException e) {
                // 网络错误，可以重试
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    continue;
                }
                return "网络错误: " + e.getMessage();
            }
            catch (Exception e) {
                // 其他错误，返回错误信息
                return "执行错误: " + e.getMessage();
            }

            // 如果有工具调用 - 检查大模型返回的响应中，是否包含工具调用请求。
            if (response.hasToolCalls()) {
                // 记录助手消息
                conversationHistory.add(
                        Message.assistant(response.content(), response.toolCalls())
                );

                // 遍历执行每个工具调用 - 因为一条AI恢复可能包含多个工具调用
                for (ToolCall toolCall : response.toolCalls()) {
                    //执行工具
                    String result = toolRegistry.executeTool(
                            toolCall.function().name(),
                            toolCall.function().arguments()
                    );

                    // 记录工具结果
                    conversationHistory.add(
                            // toolCall.id()表示工具调用的唯一ID
                            Message.tool(toolCall.id(), result)
                    );
                }
                // 继续循环，让 LLM 根据结果继续思考
                continue;
            } else {
                // 没有工具调用，任务完成
                conversationHistory.add(
                        Message.assistant(response.content())
                );
                return response.content();
            }
        }

        return "达到最大迭代次数限制";
    }
}
