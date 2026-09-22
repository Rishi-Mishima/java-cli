package com.mycliagent.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycliagent.rag.CodeRetriever;
import com.mycliagent.rag.VectorStore;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class RetrievalEval {

    private static final int TOP_K = 5;

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    public static void main(String[] args) throws Exception {

        String projectPath =
                Path.of(".")
                        .toAbsolutePath()
                        .normalize()
                        .toString();

        List<RetrievalEvalCase> cases =
                loadCases();

        System.out.println(
                "Loaded " + cases.size() + " evaluation queries."
        );

        try (CodeRetriever retriever =
                     new CodeRetriever(projectPath)) {

            EvalResult keyword =
                    evaluate(
                            "Keyword",
                            cases,
                            retriever,
                            false
                    );

            EvalResult hybrid =
                    evaluate(
                            "Hybrid",
                            cases,
                            retriever,
                            true
                    );

            printSummary(keyword, hybrid);
        }
    }

    private static List<RetrievalEvalCase> loadCases()
            throws Exception {

        try (InputStream input =
                     RetrievalEval.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     "rag-eval/queries.json"
                             )) {

            if (input == null) {
                throw new IllegalStateException(
                        "Could not find rag-eval/queries.json"
                );
            }

            return MAPPER.readValue(
                    input,
                    new TypeReference<
                            List<RetrievalEvalCase>>() {}
            );
        }
    }

    private static EvalResult evaluate(
            String method,
            List<RetrievalEvalCase> cases,
            CodeRetriever retriever,
            boolean hybrid
    ) throws Exception {

        int hit1 = 0;
        int hit3 = 0;
        int hit5 = 0;

        double reciprocalRankSum = 0.0;

        System.out.println(
                "\n=== " + method + " Retrieval ==="
        );

        for (RetrievalEvalCase evalCase : cases) {

            List<VectorStore.SearchResult> results;

            if (hybrid) {
                results =
                        retriever.hybridSearch(
                                evalCase.query(),
                                TOP_K
                        );
            } else {
                results =
                        retriever.keywordOnlySearch(
                                evalCase.query(),
                                TOP_K
                        );
            }

            boolean h1 =
                    RetrievalMetrics.hitAtK(
                            results,
                            evalCase.expectedFiles(),
                            1
                    );

            boolean h3 =
                    RetrievalMetrics.hitAtK(
                            results,
                            evalCase.expectedFiles(),
                            3
                    );

            boolean h5 =
                    RetrievalMetrics.hitAtK(
                            results,
                            evalCase.expectedFiles(),
                            5
                    );

            double rr =
                    RetrievalMetrics.reciprocalRank(
                            results,
                            evalCase.expectedFiles()
                    );

            if (h1) hit1++;
            if (h3) hit3++;
            if (h5) hit5++;

            reciprocalRankSum += rr;

            printQueryResult(
                    evalCase,
                    results,
                    h5,
                    rr
            );
        }

        int total = cases.size();

        return new EvalResult(
                method,
                hit1 / (double) total,
                hit3 / (double) total,
                hit5 / (double) total,
                reciprocalRankSum / total
        );
    }

    private static void printQueryResult(
            RetrievalEvalCase evalCase,
            List<VectorStore.SearchResult> results,
            boolean hit5,
            double reciprocalRank
    ) {

        System.out.printf(
                "%-4s %-5s RR=%.3f  %s%n",
                evalCase.id(),
                hit5 ? "HIT" : "MISS",
                reciprocalRank,
                evalCase.query()
        );

        for (int i = 0; i < results.size(); i++) {

            VectorStore.SearchResult result =
                    results.get(i);

            System.out.printf(
                    "     #%d %-45s %s%n",
                    i + 1,
                    shorten(result.filePath(), 45),
                    result.name()
            );
        }
    }

    private static void printSummary(
            EvalResult keyword,
            EvalResult hybrid
    ) {

        System.out.println(
                "\n=== Retrieval Evaluation Summary ==="
        );

        System.out.printf(
                "%-12s %10s %10s %10s %10s%n",
                "Method",
                "Recall@1",
                "Recall@3",
                "Recall@5",
                "MRR"
        );

        printResult(keyword);
        printResult(hybrid);

        double improvement =
                hybrid.recall5()
                        - keyword.recall5();

        System.out.printf(
                "%nRecall@5 improvement: %+.1f percentage points%n",
                improvement * 100
        );
    }

    private static void printResult(
            EvalResult result
    ) {

        System.out.printf(
                "%-12s %9.1f%% %9.1f%% %9.1f%% %10.3f%n",
                result.method(),
                result.recall1() * 100,
                result.recall3() * 100,
                result.recall5() * 100,
                result.mrr()
        );
    }

    private static String shorten(
            String value,
            int max
    ) {

        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return "..."
                + value.substring(
                value.length()
                        - max
                        + 3
        );
    }

    private record EvalResult(
            String method,
            double recall1,
            double recall3,
            double recall5,
            double mrr
    ) {}
}