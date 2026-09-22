package com.mycliagent.eval;

import com.mycliagent.rag.VectorStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    public static boolean hitAtK(
            List<VectorStore.SearchResult> results,
            List<String> expectedFiles,
            int k
    ) {
        int limit = Math.min(k, results.size());

        Set<String> normalizedExpected = new HashSet<>();

        for (String expected : expectedFiles) {
            normalizedExpected.add(normalize(expected));
        }

        for (int i = 0; i < limit; i++) {
            String actual = normalize(results.get(i).filePath());

            if (normalizedExpected.contains(actual)) {
                return true;
            }
        }

        return false;
    }

    public static double reciprocalRank(
            List<VectorStore.SearchResult> results,
            List<String> expectedFiles
    ) {
        Set<String> normalizedExpected = new HashSet<>();

        for (String expected : expectedFiles) {
            normalizedExpected.add(normalize(expected));
        }

        for (int i = 0; i < results.size(); i++) {
            String actual = normalize(results.get(i).filePath());

            if (normalizedExpected.contains(actual)) {
                return 1.0 / (i + 1);
            }
        }

        return 0.0;
    }

    private static String normalize(String path) {
        if (path == null) {
            return "";
        }

        return path
                .replace("\\", "/")
                .replaceFirst("^\\./", "");
    }
}