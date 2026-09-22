package com.mycliagent.benchmark;

import com.mycliagent.agent.ToolExecutionResult;
import com.mycliagent.agent.ToolInvocation;
import com.mycliagent.tool.ToolOutput;
import com.mycliagent.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ToolExecutionBenchmark {

    // Simulated latency of one I/O-bound tool call
    private static final int TOOL_DELAY_MS = 200;

    // Same concurrency limit as ToolRegistry
    private static final int MAX_PARALLEL_TOOLS = 4;

    // Number of measured runs for each tool count
    private static final int RUNS = 20;

    // Scalability test points
    private static final int[] TOOL_COUNTS = {1, 2, 4, 8};

    public static void main(String[] args) throws Exception {

        System.out.println("=== Tool Execution Scalability Benchmark ===");
        System.out.println("Synthetic tool delay: " + TOOL_DELAY_MS + " ms");
        System.out.println("Maximum concurrency: " + MAX_PARALLEL_TOOLS);
        System.out.println("Runs per configuration: " + RUNS);
        System.out.println();

        System.out.printf(
                "%-8s %-15s %-15s %-15s %-12s %-12s%n",
                "Tools",
                "Sequential",
                "Parallel",
                "ToolRegistry",
                "Speedup",
                "P95"
        );

        System.out.println(
                "----------------------------------------------------------------------------"
        );

        for (int toolCount : TOOL_COUNTS) {

            List<Long> sequentialResults =
                    runMultipleTimes(
                            RUNS,
                            () -> runSequential(toolCount)
                    );

            List<Long> parallelResults =
                    runMultipleTimes(
                            RUNS,
                            () -> runParallel(toolCount)
                    );

            List<Long> registryResults =
                    runMultipleTimes(
                            RUNS,
                            () -> runToolRegistry(toolCount)
                    );

            double sequentialMean = mean(sequentialResults);
            double parallelMean = mean(parallelResults);
            double registryMean = mean(registryResults);

            double registrySpeedup =
                    sequentialMean / registryMean;

            long registryP95 =
                    percentile(registryResults, 0.95);

            System.out.printf(
                    "%-8d %-15.2f %-15.2f %-15.2f %-12.2fx %-12d%n",
                    toolCount,
                    sequentialMean,
                    parallelMean,
                    registryMean,
                    registrySpeedup,
                    registryP95
            );

            printDetailedResults(
                    toolCount,
                    sequentialResults,
                    parallelResults,
                    registryResults
            );
        }
    }

    // =========================================================
    // Sequential baseline
    // =========================================================

    private static long runSequential(
            int toolCount
    ) throws InterruptedException {

        long start = System.nanoTime();

        for (int i = 0; i < toolCount; i++) {
            simulateTool();
        }

        long end = System.nanoTime();

        return TimeUnit.NANOSECONDS.toMillis(
                end - start
        );
    }

    // =========================================================
    // Raw bounded parallel baseline
    // =========================================================

    private static long runParallel(
            int toolCount
    ) throws Exception {

        /*
         * Important:
         *
         * Do NOT use:
         *
         * newFixedThreadPool(toolCount)
         *
         * because ToolRegistry only supports up to
         * 4 concurrent tools.
         *
         * Both implementations should therefore use
         * the same concurrency limit.
         */

        int poolSize =
                Math.min(
                        toolCount,
                        MAX_PARALLEL_TOOLS
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(poolSize);

        try {

            long start = System.nanoTime();

            List<Future<?>> futures =
                    new ArrayList<>();

            for (int i = 0; i < toolCount; i++) {

                Future<?> future =
                        executor.submit(() -> {

                            simulateTool();

                            return null;
                        });

                futures.add(future);
            }

            // Wait until every task finishes
            for (Future<?> future : futures) {
                future.get();
            }

            long end = System.nanoTime();

            return TimeUnit.NANOSECONDS.toMillis(
                    end - start
            );

        } finally {

            executor.shutdown();
        }
    }

    // =========================================================
    // Synthetic tool workload
    // =========================================================

    private static void simulateTool()
            throws InterruptedException {

        Thread.sleep(TOOL_DELAY_MS);
    }

    // =========================================================
    // ToolRegistry benchmark implementation
    // =========================================================

    private static final class BenchmarkToolRegistry
            extends ToolRegistry {

        @Override
        protected ToolOutput doExecuteTool(
                String name,
                String argumentsJson
        ) {

            try {

                Thread.sleep(TOOL_DELAY_MS);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new RuntimeException(e);
            }

            return ToolOutput.text("ok");
        }
    }

    // =========================================================
    // Run ToolRegistry
    // =========================================================

    private static long runToolRegistry(
            int toolCount
    ) {

        ToolRegistry registry =
                new BenchmarkToolRegistry();

        List<ToolInvocation> invocations =
                new ArrayList<>();

        for (int i = 0; i < toolCount; i++) {

            invocations.add(
                    new ToolInvocation(
                            "call-" + i,
                            "benchmark-tool",
                            "{}"
                    )
            );
        }

        long start = System.nanoTime();

        List<ToolExecutionResult> results =
                registry.executeTools(invocations);

        long end = System.nanoTime();

        if (results.size() != toolCount) {

            throw new IllegalStateException(
                    "Expected "
                            + toolCount
                            + " results, got "
                            + results.size()
            );
        }

        return TimeUnit.NANOSECONDS.toMillis(
                end - start
        );
    }

    // =========================================================
    // Benchmark runner
    // =========================================================

    @FunctionalInterface
    private interface BenchmarkRunner {

        long run() throws Exception;
    }

    private static List<Long> runMultipleTimes(
            int runs,
            BenchmarkRunner runner
    ) throws Exception {

        List<Long> results =
                new ArrayList<>();

        for (int i = 0; i < runs; i++) {

            long latency =
                    runner.run();

            results.add(latency);
        }

        return results;
    }

    // =========================================================
    // Statistics
    // =========================================================

    private static double mean(
            List<Long> values
    ) {

        return values.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    private static double median(
            List<Long> values
    ) {

        List<Long> sorted =
                new ArrayList<>(values);

        sorted.sort(Long::compareTo);

        int size = sorted.size();

        if (size % 2 == 0) {

            return (
                    sorted.get(size / 2 - 1)
                            +
                            sorted.get(size / 2)
            ) / 2.0;
        }

        return sorted.get(size / 2);
    }

    private static long percentile(
            List<Long> values,
            double percentile
    ) {

        if (values.isEmpty()) {

            throw new IllegalArgumentException(
                    "Values must not be empty"
            );
        }

        if (percentile <= 0
                || percentile > 1) {

            throw new IllegalArgumentException(
                    "Percentile must be between 0 and 1"
            );
        }

        List<Long> sorted =
                new ArrayList<>(values);

        sorted.sort(Long::compareTo);

        int index =
                (int) Math.ceil(
                        percentile * sorted.size()
                ) - 1;

        return sorted.get(index);
    }

    // =========================================================
    // Detailed output
    // =========================================================

    private static void printDetailedResults(
            int toolCount,
            List<Long> sequential,
            List<Long> parallel,
            List<Long> registry
    ) {

        double sequentialMean =
                mean(sequential);

        double parallelMean =
                mean(parallel);

        double registryMean =
                mean(registry);

        double parallelSpeedup =
                sequentialMean / parallelMean;

        double registrySpeedup =
                sequentialMean / registryMean;

        double registryReduction =
                (
                        1.0
                                - registryMean
                                / sequentialMean
                ) * 100;

        double registryOverhead =
                (
                        (
                                registryMean
                                        - parallelMean
                        )
                                / parallelMean
                ) * 100;

        System.out.println();

        System.out.println(
                "--- "
                        + toolCount
                        + " Tool(s) ---"
        );

        System.out.println(
                "Sequential raw results: "
                        + sequential
        );

        System.out.println(
                "Parallel raw results:   "
                        + parallel
        );

        System.out.println(
                "Registry raw results:   "
                        + registry
        );

        System.out.println();

        System.out.printf(
                "Sequential:   mean=%.2f ms, median=%.2f ms, p95=%d ms%n",
                sequentialMean,
                median(sequential),
                percentile(sequential, 0.95)
        );

        System.out.printf(
                "Parallel:     mean=%.2f ms, median=%.2f ms, p95=%d ms%n",
                parallelMean,
                median(parallel),
                percentile(parallel, 0.95)
        );

        System.out.printf(
                "ToolRegistry: mean=%.2f ms, median=%.2f ms, p95=%d ms%n",
                registryMean,
                median(registry),
                percentile(registry, 0.95)
        );

        System.out.printf(
                "Raw parallel speedup:   %.2fx%n",
                parallelSpeedup
        );

        System.out.printf(
                "ToolRegistry speedup:   %.2fx%n",
                registrySpeedup
        );

        System.out.printf(
                "Latency reduction:      %.2f%%%n",
                registryReduction
        );

        System.out.printf(
                "Registry vs raw parallel difference: %.2f%%%n",
                registryOverhead
        );

        System.out.println();
    }
}