package com.mycliagent.plan;


import java.util.*;

/**
 * 任务节点 - 表示一个可执行的任务单元
 */

public class Task {
    private final String id;              // 任务唯一标识
    private final String description;     // 任务描述
    private final TaskType type;          // 任务类型
    private TaskStatus status;            // 执行状态
    private String result;                // 执行结果
    private String error;                 // 错误信息
    private final List<String> dependencies;  // 依赖的任务ID
    private final List<String> dependents;    // 被依赖的任务ID
    private volatile long startTime;
    private volatile long endTime;

    public enum TaskType {
        PLANNING,      // 规划任务
        FILE_READ,     // 读取文件
        FILE_WRITE,    // 写入文件
        COMMAND,       // 执行命令
        ANALYSIS,      // 分析结果
        VERIFICATION   // 验证结果
    }

    public enum TaskStatus {
        PENDING,       // 等待执行
        RUNNING,       // 执行中
        COMPLETED,     // 已完成
        FAILED,        // 失败
        SKIPPED        // 跳过
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public TaskType getType() {
        return type;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public List<String> getDependents() {
        return dependents;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }



    // Setters
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setResult(String result) { this.result = result; }
    public void setError(String error) { this.error = error; }

    public void addDependent(String taskId) {
        if (!dependents.contains(taskId)) {
            dependents.add(taskId);
        }
    }

    public void addDependency(String taskId) {
        if (!dependencies.contains(taskId)) {
            dependencies.add(taskId);
        }
    }

    public Task(String id, String description, TaskType type) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.dependencies = new ArrayList<>();
        this.dependents = new ArrayList<>();
    }


    /** 生命周期 **/
    public void markStarted() {
        this.status = TaskStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
    }

    public void markCompleted(String result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.endTime = System.currentTimeMillis();
    }

    public void markFailed(String error) {
        this.status = TaskStatus.FAILED;
        this.error = error;
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 获取执行耗时（毫秒）
     */
    public long getDuration() {
        if (startTime == 0) return 0;
        if (endTime == 0) return System.currentTimeMillis() - startTime;
        return endTime - startTime;
    }

    //依赖关系 管理
    public boolean isExecutable(Map<String, Task>allTasks){
        // 只有Task是Pending, 等待执行的时候, 才能执行, 剩下都不可
        if (status != TaskStatus.PENDING) return false;
        //遍历所有依赖
        for(String depId: dependencies){
            Task dep = allTasks.get(depId);
            if(dep == null || dep.getStatus() != TaskStatus.COMPLETED){
                return false;
            }
        }
        // 可以执行
        return true;
    }



}