package com.mycliagent.agent;

import com.mycliagent.llm.LlmClient;
import com.mycliagent.memory.MemoryManager;
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

class AgentOrchestratorConcurrencyTest {
    @Test
    void executesIndependentMultiAgentStepsInParallel() {
        ParallelMultiAgentLlm llm = new ParallelMultiAgentLlm();
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llm,
                new ToolRegistry(),
                new MemoryManager(llm),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)
        );

        String result = orchestrator.run("验证 Multi-Agent 并发批次");

        assertTrue(result.contains("多 Agent 协作任务完成"));
        assertTrue(llm.maxActiveWorkers.get() >= 2, "independent steps should occupy multiple workers concurrently");
    }

    private static final class ParallelMultiAgentLlm implements LlmClient {
        private final AtomicInteger activeWorkers = new AtomicInteger();
        private final AtomicInteger maxActiveWorkers = new AtomicInteger();
        private final CountDownLatch started = new CountDownLatch(2);

        @Override
        public ChatResponse chat(List<Message> messages, List<Tool> tools) throws IOException {
            String system = messages.isEmpty() ? "" : messages.get(0).content();
            String last = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();

            if (system.contains("规划者")) {
                return new ChatResponse("assistant", """
                        {"steps":[
                          {"id":"step_1","description":"并行步骤 A","type":"ANALYSIS","dependencies":[]},
                          {"id":"step_2","description":"并行步骤 B","type":"ANALYSIS","dependencies":[]},
                          {"id":"step_3","description":"汇总结果","type":"ANALYSIS","dependencies":["step_1","step_2"]}
                        ]}
                        """, List.of(), 1, 1);
            }

            if (system.contains("审查者")) {
                return new ChatResponse("assistant",
                        "{\"approved\":true,\"issues\":[],\"suggestions\":[],\"summary\":\"ok\"}",
                        List.of(), 1, 1);
            }

            if (last.contains("并行步骤 A") || last.contains("并行步骤 B")) {
                int running = activeWorkers.incrementAndGet();
                maxActiveWorkers.accumulateAndGet(running, Math::max);
                started.countDown();
                try {
                    started.await(2, TimeUnit.SECONDS);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                } finally {
                    activeWorkers.decrementAndGet();
                }
            }

            return new ChatResponse("assistant", "worker result", List.of(), 1, 1);
        }

        @Override
        public String getModelName() {
            return "parallel-multi-agent";
        }

        @Override
        public String getProviderName() {
            return "test";
        }
    }
}
