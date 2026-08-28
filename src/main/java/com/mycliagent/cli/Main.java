package com.mycliagent.cli;

import com.mycliagent.agent.Agent;
import com.mycliagent.agent.AgentOrchestrator;
import com.mycliagent.hitl.HitlHandler;
import com.mycliagent.hitl.HitlToolRegistry;
import com.mycliagent.hitl.TerminalHitlHandler;
import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.llm.MockLlmClient;
import com.mycliagent.rag.CodeIndex;
import com.mycliagent.rag.CodeRetriever;
import com.mycliagent.rag.SearchResultFormatter;
import com.mycliagent.tool.ToolRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        printBanner();

        AppConfig config = loadConfig();

        // 创建 Agent - 执行之前写的构造函数 Agent ( 此时AGENT已经有了GLM客户端, tool registry, history, system prompt)
        LlmClient llmClient = createLlmClient(config);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintStream output = System.out;
        HitlHandler hitlHandler = new TerminalHitlHandler(config.hitlEnabled(), inputReader, output);
        ToolRegistry toolRegistry = new HitlToolRegistry(hitlHandler);
        Agent agent = new Agent(llmClient, toolRegistry);
        agent.setHitlEnabledSupplier(hitlHandler::isEnabled);
        AgentOrchestrator orchestrator = new AgentOrchestrator(llmClient, toolRegistry);

        // 交互式循环
        // 读取终端输入
        System.out.printf("✅ Provider: %s (%s)%n", llmClient.getProviderName(), llmClient.getModelName());
        System.out.println("💡 提示: 输入 'clear' 清空历史, 'exit' 退出, '/multi 任务' 启动多 Agent, '/index' 索引当前项目, '/search 查询' 检索代码\n");

        while (true) {
            System.out.print("👤 你: ");
            String line;
            try {
                line = inputReader.readLine();
            } catch (IOException e) {
                System.out.println("读取输入失败: " + e.getMessage());
                break;
            }
            if (line == null) {
                break;
            }
            String input = line.trim();

            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("exit")) break;
            if (input.equalsIgnoreCase("clear")) {
                agent.clearHistory();
                hitlHandler.clearApprovedAll();
                System.out.println("🗑️ 历史已清空\n");
                continue;
            }
            if (input.equalsIgnoreCase("/index")) {
                CodeIndex.IndexResult result = new CodeIndex(System.out::println)
                        .index(System.getProperty("user.dir"));
                System.out.println(result.message() + "\n");
                continue;
            }
            if (input.startsWith("/search ")) {
                String query = input.substring("/search ".length()).trim();
                if (query.isEmpty()) {
                    System.out.println("请输入搜索内容，例如: /search 记忆检索怎么实现\n");
                    continue;
                }
                try (CodeRetriever retriever = new CodeRetriever(System.getProperty("user.dir"))) {
                    var stats = retriever.getStats();
                    if (stats.chunkCount() == 0) {
                        System.out.println("代码库尚未索引，请先执行 /index\n");
                    } else {
                        System.out.println(SearchResultFormatter.formatForCli(
                                query, retriever.hybridSearch(query, 5)) + "\n");
                    }
                } catch (Exception e) {
                    System.out.println("代码检索失败: " + e.getMessage() + "\n");
                }
                continue;
            }

            String response;
            if (input.startsWith("/multi ")) {
                response = orchestrator.run(input.substring("/multi ".length()).trim());
            } else {
                // 运行 Agent - run方法
                response = agent.run(input);
            }
            System.out.println("🤖 Agent: " + response + "\n");
        }
    }

    private static LlmClient createLlmClient(AppConfig config) {
        if ("glm".equalsIgnoreCase(config.provider())) {
            String apiKey = config.get("GLM_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("❌ 错误: LLM_PROVIDER=glm 时必须配置 GLM_API_KEY");
                System.exit(1);
            }
            return new GLMClient(apiKey);
        }

        return new MockLlmClient();
    }

    private static AppConfig loadConfig() {
        Map<String, String> values = new HashMap<>(System.getenv());
        File envFile = new File(".env");
        if (envFile.exists()) {
            values.putAll(readEnvFile(envFile));
        }

        String provider = values.getOrDefault("LLM_PROVIDER", "mock").trim();
        if (provider.isEmpty()) {
            provider = "mock";
        }
        return new AppConfig(provider, values);
    }

    private static Map<String, String> readEnvFile(File envFile) {
        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envFile.toPath())) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                values.put(key, value.replaceAll("^['\"]|['\"]$", ""));
            }
        } catch (IOException e) {
            System.err.println("读取 .env 失败: " + e.getMessage());
        }
        return values;
    }

    private record AppConfig(String provider, Map<String, String> values) {
        String get(String key) {
            return values.getOrDefault(key, "").trim();
        }

        boolean hitlEnabled() {
            String value = values.getOrDefault("HITL_ENABLED", "true").trim();
            return !"false".equalsIgnoreCase(value)
                    && !"0".equals(value)
                    && !"no".equalsIgnoreCase(value);
        }
    }

    private static void printBanner() {
        System.out.println("""
        ╔═══════════════════════════════════════════════════════════════════╗
        ║   ██████╗ ██╗███████╗██╗  ██╗ ██████╗██╗     ██╗                  ║
        ║   ██╔══██╗██║██╔════╝██║  ██║██╔════╝██║     ██║                  ║
        ║   ██████╔╝██║███████╗███████║██║     ██║     ██║                  ║
        ║   ██╔══██╗██║╚════██║██╔══██║██║     ██║     ██║                  ║
        ║   ██║  ██║██║███████║██║  ██║╚██████╗███████╗██║                  ║
        ║   ╚═╝  ╚═╝╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝╚══════╝╚═╝                  ║
        ║                      简单的 Java Agent CLI v1.0.0            ║
        ╚═══════════════════════════════════════════════════════════════════╝
        """);
    }
}
