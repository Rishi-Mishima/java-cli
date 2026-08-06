package com.mycliagent.plan;

import java.util.*;

public class ExecutionPlan {
    private final String id;
    private final String goal;           // 计划目标



    private final Map<String, Task> tasks;  // 所有任务
    private final List<String> executionOrder;  // 执行顺序
    private PlanStatus status;
    private String summary;
    private long startTime;
    private long endTime;

    public enum PlanStatus {
        CREATED,      // 刚创建
        RUNNING,      // 执行中
        COMPLETED,    // 全部完成
        FAILED,       // 有任务失败
        CANCELLED     // 被取消
    }

    public ExecutionPlan(String id, String goal, Map<String, Task> tasks, List<String> executionOrder) {
        this.id = id;
        this.goal = goal;
        this.tasks = new LinkedHashMap<>(); // 保持插入顺序
        this.executionOrder = new ArrayList<>();
        this.status = PlanStatus.CREATED;
    }

    // Getters
    public String getId() { return id; }
    public String getGoal() { return goal; }
    public PlanStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    //setters
    public void setSummary(String summary) { this.summary = summary; }
    public void setStatus(PlanStatus status) { this.status = status; }

    // 获取任务
    public Task getTasks(String id) {
        return tasks.get(id);
    }

    //获取所有任务
    public Collection<Task> getAllTasks() {
        return tasks.values();
    }

    /**
     * 获取根任务（没有依赖的任务）
     */
    public List<Task> getRootTasks() {
        return tasks.values().stream()
                .filter(t -> t.getDependencies().isEmpty())
                .toList();
    }


    /**
     * 获取可执行的任务（依赖都已完成）
     */
    public List<Task> getExecutableTasks() {
        return tasks.values().stream()
                .filter(t -> t.isExecutable(tasks))
                .toList();
    }

    /**
     * 计算拓扑排序执行顺序
     */
    public boolean computeExecutionOrder() {
        // 每次计算之前, 都要清空之前的执行顺序
        executionOrder.clear();
        // visited表示已经彻底处理完的任务: 已经被DFS处理完, 加入了拓扑结果
        //注意，这里的“处理完成”不是说任务已经真正运行完成。 - 只是说拓扑检查了这个节点
        Set<String> visited = new HashSet<>();
        // 当前这条递归路径中，正在访问、但还没有处理完成的任务。 - 检测环
        Set<String> visiting = new HashSet<>();

        // 遍历所有任务
        for (Task task : tasks.values()) {
            // 已经处理过的就跳过 - 只有当前任务还没有被彻底处理过，才需要进行 DFS。
            if (!visited.contains(task.getId())) {
                //有环的任务无法得到合法执行顺序
                if (!topologicalSort(task, visited, visiting)) {
                    return false;  // 有环
                }
            }
        }

        return true;
    }

    // 拓扑排序
    private boolean topologicalSort(Task task, Set<String> visited, Set<String> visiting) {
       // 获取当前任务的ID
        String id = task.getId();

        if (visiting.contains(id)) {
            return false;  // 有环，排序失败
        }
        if (visited.contains(id)) {
            return true;
        }

        //标记当前任务正在访问
        visiting.add(id);

        // 递归处理所有依赖
        for (String depId : task.getDependencies()) {
            //依赖ID
            Task dep = tasks.get(depId);
            if (dep != null) {
                if (!topologicalSort(dep, visited, visiting)) {
                    return false;
                }
            }else throw new IllegalStateException(
                    "Missing dependency: " + depId
            );
        }

        // 处理完成后移除
        visiting.remove(id);
        // 标记为彻底处理完成
        visited.add(id);
        // 加入执行顺序: - 递归处理完所有依赖之后, 这个顺序可以直接执行
        executionOrder.add(id);
        return true;
    }

    /*
     * 这三个方法是在管理整个 Plan 的生命周期，以及检查计划里的任务有没有失败。
     */

    //整个计划开始执行了。
    public void markStarted() {
        // 把计划状态改成running
        this.status = PlanStatus.RUNNING;
        // 记录当前时间。 - 从 1970 年 1 月 1 日到现在经过的毫秒数。
        this.startTime = System.currentTimeMillis();
    }

    //计划已完成
    public void markCompleted() {
        this.status = PlanStatus.COMPLETED;
        //记录结束时间。
        this.endTime = System.currentTimeMillis();
    }

    //计划中的所有任务里，有没有任何一个任务失败
    // 只要有一个失败 就返回True
    public boolean hasFailed() {
        // 用stream: 让 Java 按顺序检查这一批对象
        //t 代表当前正在检查的某个任务。
        return tasks.values().stream()
                .anyMatch(t -> t.getStatus() == Task.TaskStatus.FAILED);
    }

}
