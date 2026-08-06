package com.mycliagent.plan;

import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Planner {
    private final GLMClient LlmClient;

    public ExecutionPlan createPlan(String goal) throws IOException {
        // 1. 构建规划提示
        List<LlmClient.Message> messages = Arrays.asList(
                Message.system(PLANNING_PROMPT),
                LlmClient.Message.user("请为以下任务制定执行计划：\n" + goal)
        );

        // 2. 调用 LLM 生成计划
        ChatResponse response = llmClient.chat(messages, null);

        // 3. 解析 JSON 计划
        return parsePlan(goal, response.content());
    }
}
