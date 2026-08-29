package com.mycliagent.agent;

import com.mycliagent.llm.LlmClient;
import com.mycliagent.memory.MemoryManager;
import com.mycliagent.plan.ExecutionPlan;
import com.mycliagent.plan.Planner;
import com.mycliagent.plan.Task;
import com.mycliagent.tool.ToolRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanExecuteAgentConcurrencyTest {
    @Test
    void executesIndependentDagBatchInParallel() {
        ParallelProbeLlm llm = new ParallelProbeLlm("plan");
        PlanExecuteAgent agent = new PlanExecuteAgent(
                llm,
                new ToolRegistry(),
                new FixedParallelPlanner(llm),
                new MemoryManager(llm),
                (goal, plan) -> PlanExecuteAgent.PlanReviewDecision.execute(),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)
        );

        String result = agent.run("验证 Plan-and-Execute 并发批次");

        assertTrue(result.contains("计划执行完成"));
        assertTrue(llm.maxActive.get() >= 2, "independent DAG tasks should execute concurrently");
    }

    private static final class FixedParallelPlanner extends Planner {
        private FixedParallelPlanner(LlmClient llmClient) {
            super(llmClient);
        }

        @Override
        public ExecutionPlan createPlan(String goal) {
            ExecutionPlan plan = new ExecutionPlan("test-plan", goal);
            Task first = new Task("task_1", "并行任务 A", Task.TaskType.ANALYSIS);
            Task second = new Task("task_2", "并行任务 B", Task.TaskType.ANALYSIS);
            Task merge = new Task("task_3", "汇总结果", Task.TaskType.ANALYSIS);
            merge.addDependency("task_1");
            merge.addDependency("task_2");
            first.addDependent("task_3");
            second.addDependent("task_3");
            plan.addTask(first);
            plan.addTask(second);
            plan.addTask(merge);
            plan.computeExecutionOrder();
            return plan;
        }
    }

    private static final class ParallelProbeLlm implements LlmClient {
        private final String prefix;
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final CountDownLatch started = new CountDownLatch(2);

        private ParallelProbeLlm(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ChatResponse chat(List<Message> messages, List<Tool> tools) throws IOException {
            Message last = messages.get(messages.size() - 1);
            if (last.content().contains("并行任务 A") || last.content().contains("并行任务 B")) {
                int running = active.incrementAndGet();
                maxActive.accumulateAndGet(running, Math::max);
                started.countDown();
                try {
                    started.await(2, TimeUnit.SECONDS);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                } finally {
                    active.decrementAndGet();
                }
            }
            return new ChatResponse("assistant", prefix + " result", List.of(), 1, 1);
        }

        @Override
        public String getModelName() {
            return "parallel-probe";
        }

        @Override
        public String getProviderName() {
            return "test";
        }
    }
}
