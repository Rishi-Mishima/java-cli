package com.mycliagent.agent;

import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

public class Agent {


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
}
