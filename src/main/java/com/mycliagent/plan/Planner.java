package com.mycliagent.plan;

import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.llm.LlmClient.ChatResponse;
import com.mycliagent.plan.Task.TaskType;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;



public class Planner {
    // 面向接口,而不是具体实现
    private final LlmClient llmClient;

    public Planner(LlmClient llmClient) {
        this.llmClient = llmClient;
    }


    // 规划提示词
    private static final String PLANNING_PROMPT = """
        你是一个任务规划器。

        请根据用户给出的目标，将目标拆分成可以执行的子任务。

        请严格按照以下 JSON 格式输出执行计划，不要输出 Markdown，
        不要输出代码块，不要添加其他解释：

        {
          "summary": "任务摘要",
          "tasks": [
            {
              "id": "task_1",
              "description": "任务描述",
              "type": "FILE_READ",
              "dependencies": []
            }
          ]
        }

        可用任务类型：

        - FILE_READ：读取文件内容，用于获取文件中的信息
        - FILE_WRITE：写入文件内容，用于创建或修改文件
        - COMMAND：执行 Shell 命令，用于编译、运行或测试程序
        - ANALYSIS：分析已有信息，用于推理和做出中间决策
        - VERIFICATION：验证执行结果，用于检查任务是否正确完成

        规划规则：

            1. 每个任务必须有唯一 id，格式为 task_1、task_2、task_3。
            2. dependencies 必须是任务 id 数组。
            3. 没有依赖的任务使用空数组 []。
            4. 不能依赖不存在的任务。
            5. 不能产生循环依赖。
            6. tasks 按照合理的执行顺序排列。
            7. 每个任务描述必须具体、明确、可以执行。
            8. 一个任务只负责一个主要动作。
            9. 简单任务可以拆分成 1 到 4 个任务。
            10. 复杂任务应拆分成 5 到 10 个任务。
            11. 任务类型只能使用给定的五种类型。
            12. 最终只输出合法 JSON。
            13. 不要输出 Markdown 代码块。
            14. 不要添加任何解释、前言或结尾。
        """;


    public ExecutionPlan createPlan(String goal) throws IOException {
        // 1. 构建规划提示prompt
        List<LlmClient.Message> messages = Arrays.asList(
                // 给LLM发送一个系统指令
                LlmClient.Message.system(PLANNING_PROMPT),
                //user message e.g. 请为以下任务制定执行计划：创建一个 React 网站
                LlmClient.Message.user("请为以下任务制定执行计划：\n" + goal)
        );

        // 2. 调用 LLM 生成计划
        // tools传null, 因为Planner只制定计划, 不是执行任务
        ChatResponse response = llmClient.chat(messages, null);

        // 3. 解析 JSON 计划 - 负责把这段 JSON 转成 Java 的 ExecutionPlan 对象
        return parsePlan(goal, response.content());
    }
}
