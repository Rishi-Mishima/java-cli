package com.mycliagent.plan;
import com.mycliagent.plan.Task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.llm.LlmClient.ChatResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;



public class Planner {
    // 面向接口,而不是具体实现
    private final LlmClient llmClient;
    private final ObjectMapper mapper;
    private Supplier<String> projectMemorySupplier = () -> "";

    public Planner(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.mapper = new ObjectMapper();
    }

    public void setProjectMemorySupplier(Supplier<String> projectMemorySupplier) {
        this.projectMemorySupplier = projectMemorySupplier == null ? () -> "" : projectMemorySupplier;
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

    // goal: 用户的原始目标; planJson: LLM返回的JSON字符串
    private ExecutionPlan parsePlan(String goal, String planJson) throws IOException {
        // 清理可能的 markdown 代码块
        // e.g. ```json, or \\s*正则表达式。
        // 第二行删除普通 Markdown 代码块符号
        // trim()删除字符串开头和结尾的空格、换行。
        String cleaned = planJson.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        //字符串解析成 JSON 树 (cleaned is string)
        JsonNode root = mapper.readTree(cleaned);
        //.path更安全,如果字段不存在,不会立即返回null.而是返回一个缺失节点
        String summary = root.path("summary").asText();
        // 获取tasks数组
        JsonNode tasksNode = root.path("tasks");

        // 创建一个新的执行计划对象。
        ExecutionPlan plan = new ExecutionPlan(generatePlanId(), goal);
        //设置摘要
        plan.setSummary(summary);

        // 第一遍：创建任务，不处理依赖
        //需要先确保所有任务都已经创建完成，然后才能连接它们之间的依赖关系。
        //创建 ID 映射表 - 为了防止LLM不严格遵守形成的ID
        Map<String, String> idMapping = new HashMap<>();
        int taskIndex = 1;

        // 遍历所有任务
        for (JsonNode taskNode : tasksNode) {
            //读取原始任务 ID
            String originalId = taskNode.path("id").asText();
            //生成新 ID
            String newId = "task_" + taskIndex++;
            //保存映射关系
            idMapping.put(originalId, newId);


            String description = taskNode.path("description").asText();
            String typeStr = taskNode.path("type").asText();
            Task.TaskType type = parseTaskType(typeStr);

            // 创建任务...
            Task task = new Task(newId, description, type);
            //把任务加入计划
            plan.addTask(task);
        }

        // 第二遍：处理依赖关系
        //重置 taskIndex
        taskIndex = 1;
        // 遍历所有任务JSON, taskNode是LLM返回的任务数组
        for (JsonNode taskNode : tasksNode) {
            // 当前任务
            String currentTaskId =
                    "task_" + taskIndex++;

            // 读取当前任务的dependencies
            JsonNode dependenciesNode =
                    taskNode.path("dependencies");

            //遍历多个依赖
            for (JsonNode dependencyNode : dependenciesNode) {
                //取得LLM原始依赖ID
                String originalDependencyId =
                        dependencyNode.asText();

                // 通过映射找到新的ID
                String mappedDependencyId =
                        idMapping.get(originalDependencyId);

//                // 检查映射是否存在
//                if (mappedDependencyId != null) {
//                    plan.addDependency(
//                            currentTaskId,
//                            mappedDependencyId
//                    );
//                }

                if (mappedDependencyId != null) {
                    Task currentTask = plan.getTask(currentTaskId);
                    Task dependencyTask = plan.getTask(mappedDependencyId);

                    if (currentTask != null && dependencyTask != null) {
                        currentTask.addDependency(mappedDependencyId);
                        dependencyTask.addDependent(currentTaskId);
                    }
                }
            }
        }

        // 计算执行顺序
        if (!plan.computeExecutionOrder()) {
            throw new IOException("计划中存在循环依赖");
        }

        return plan;
    }

    /**
     * 解析任务类型
     */
    private Task.TaskType parseTaskType(String typeStr) {
        return switch (typeStr.toUpperCase()) {
            case "FILE_READ" -> Task.TaskType.FILE_READ;
            case "FILE_WRITE" -> Task.TaskType.FILE_WRITE;
            case "COMMAND" -> Task.TaskType.COMMAND;
            case "ANALYSIS" -> Task.TaskType.ANALYSIS;
            case "VERIFICATION" -> Task.TaskType.VERIFICATION;
            default -> Task.TaskType.ANALYSIS;
        };
    }

    /**
     * 生成计划ID
     */
    private String generatePlanId() {
        return "plan_" + System.currentTimeMillis();
    }


    /**
     * 根据执行结果重新规划
     */

    public ExecutionPlan replan(ExecutionPlan failedPlan, String failureReason) throws IOException {
       // out.println("🔄 重新规划，原因: " + failureReason + "\n");

        // 创建一个“字符串拼接容器
        StringBuilder context = new StringBuilder();
        context.append("原任务: ").append(failedPlan.getGoal()).append("\n");
        context.append("失败原因: ").append(failureReason).append("\n");
        context.append("已完成的任务:\n");

        //遍历计划里的所有任务
        for (Task task : failedPlan.getAllTasks()) {
            //只处理已经完成的任务 - 新计划就不需要再读取一次。
            if (task.getStatus() == Task.TaskStatus.COMPLETED) {
                // 把已完成任务加入 context
                context.append("- ").append(task.getId())
                        .append(": ").append(task.getDescription())
                        .append("\n");
            }
        }

        context.append("\n请制定新的执行计划，避开之前的问题。");

        return createPlan(context.toString());
    }
}
