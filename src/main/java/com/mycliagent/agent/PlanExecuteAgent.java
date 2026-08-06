package com.mycliagent.agent;

import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.plan.ExecutionPlan;
import com.mycliagent.plan.Planner;
import com.mycliagent.plan.Task;
import com.mycliagent.tool.ToolRegistry;

import java.io.IOException;

public class PlanExecuteAgent {
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final Planner planner;

    public PlanExecuteAgent(LlmClient llmClient, ToolRegistry toolRegistry, Planner planner) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.planner = planner;
    }

    public String run(String userInput) throws IOException {
        // 1. 创建执行计划
        ExecutionPlan plan = planner.createPlan(userInput);

        // 2. 计算执行顺序
        boolean success = plan.computeExecutionOrder();

        if (!success) {
            throw new IllegalStateException("计划中存在循环依赖");
        }

        // 3. 显示计划
        System.out.println(plan.visualize());

        // 4. 执行计划
        for (String taskId : plan.getExecutionOrder()) {
            Task task = plan.getTask(taskId);

            if (task != null) {
                executeTask(task);
            }
        }

        // 4. 返回结果
        return buildResult(plan);
    }

    private String buildResult(ExecutionPlan plan) {
        StringBuilder result = new StringBuilder();

        result.append("执行结果：\n");

        for (Task task : plan.getAllTasks()) {
            result.append("- ")
                    .append(task.getDescription())
                    .append("\n");

            if (task.getResult() != null) {
                result.append("  结果：")
                        .append(task.getResult())
                        .append("\n");
            }

            if (task.getError() != null) {
                result.append("  错误：")
                        .append(task.getError())
                        .append("\n");
            }
        }

        return result.toString();
    }

    private void executeTask(Task task) {
        System.out.println("正在执行任务：" + task.getDescription());
    }


}
