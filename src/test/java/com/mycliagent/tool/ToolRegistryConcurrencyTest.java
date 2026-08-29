package com.mycliagent.tool;

import com.mycliagent.agent.ToolExecutionResult;
import com.mycliagent.agent.ToolInvocation;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryConcurrencyTest {
    @Test
    void executesSameTurnToolCallsInParallelAndKeepsResultOrder() {
        ParallelProbeToolRegistry registry = new ParallelProbeToolRegistry();
        List<ToolInvocation> invocations = List.of(
                new ToolInvocation("call-1", "probe", "{}"),
                new ToolInvocation("call-2", "probe", "{}")
        );

        List<ToolExecutionResult> results = registry.executeTools(invocations);

        assertEquals("call-1", results.get(0).id());
        assertEquals("call-2", results.get(1).id());
        assertTrue(registry.maxActive.get() >= 2, "tool calls should overlap in the same batch");
    }

    private static final class ParallelProbeToolRegistry extends ToolRegistry {
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final CountDownLatch started = new CountDownLatch(2);

        @Override
        protected ToolOutput doExecuteTool(String name, String argumentsJson) {
            int running = active.incrementAndGet();
            maxActive.accumulateAndGet(running, Math::max);
            started.countDown();
            try {
                started.await(2, TimeUnit.SECONDS);
                Thread.sleep(50);
                return ToolOutput.text("ok-" + name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ToolOutput.text("interrupted");
            } finally {
                active.decrementAndGet();
            }
        }
    }
}
