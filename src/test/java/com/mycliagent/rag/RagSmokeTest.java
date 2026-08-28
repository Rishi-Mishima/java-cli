package com.mycliagent.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycliagent.llm.LlmClient;
import com.mycliagent.tool.ToolRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RagSmokeTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("mycliagent.rag.dir");
    }

    @Test
    void indexesAndSearchesCodeWithoutExternalEmbeddingService() throws Exception {
        Path project = tempDir.resolve("project");
        Files.createDirectories(project.resolve("src"));
        Files.writeString(project.resolve("src/MemoryManager.java"), """
                package demo;

                public class MemoryManager {
                    public String buildContextForQuery(String query) {
                        return "memory context " + query;
                    }
                }
                """);

        System.setProperty("mycliagent.rag.dir", tempDir.resolve("rag-db").toString());
        EmbeddingClient embeddingClient = new FakeEmbeddingClient();

        CodeIndex.IndexResult indexResult = new CodeIndex(embeddingClient).index(project.toString());
        assertTrue(indexResult.chunkCount() > 0);
        assertTrue(indexResult.relationCount() > 0);

        try (CodeRetriever retriever = new CodeRetriever(project.toString(), embeddingClient)) {
            List<VectorStore.SearchResult> results = retriever.hybridSearch("buildContextForQuery memory", 3);
            assertFalse(results.isEmpty());
            assertTrue(results.stream().anyMatch(result -> result.name().contains("buildContextForQuery")));
        }
    }

    @Test
    void registersSearchCodeTool() {
        ToolRegistry registry = new ToolRegistry();

        assertTrue(registry.getToolDefinitions().stream()
                .map(LlmClient.Tool::name)
                .anyMatch("search_code"::equals));
    }

    private static class FakeEmbeddingClient extends EmbeddingClient {
        @Override
        public float[] embed(String text) throws IOException {
            String lower = text == null ? "" : text.toLowerCase();
            return new float[] {
                    lower.contains("memory") ? 1.0f : 0.0f,
                    lower.contains("context") ? 1.0f : 0.0f,
                    lower.contains("query") ? 1.0f : 0.0f,
                    lower.contains("buildcontextforquery") ? 1.0f : 0.0f
            };
        }
    }
}
