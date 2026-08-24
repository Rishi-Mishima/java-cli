package com.mycliagent.cli;

import com.mycliagent.agent.Agent;
import com.mycliagent.llm.GLMClient;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.llm.MockLlmClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        printBanner();

        AppConfig config = loadConfig();

        // 创建 Agent - 执行之前写的构造函数 Agent ( 此时AGENT已经有了GLM客户端, tool registry, history, system prompt)
        LlmClient llmClient = createLlmClient(config);
        Agent agent = new Agent(llmClient);

        // 交互式循环
        // 读取终端输入
        Scanner scanner = new Scanner(System.in);
        System.out.printf("✅ Provider: %s (%s)%n", llmClient.getProviderName(), llmClient.getModelName());
        System.out.println("💡 提示: 输入 'clear' 清空历史, 'exit' 退出\n");

        while (true) {
            System.out.print("👤 你: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("exit")) break;
            if (input.equalsIgnoreCase("clear")) {
                agent.clearHistory();
                System.out.println("🗑️ 历史已清空\n");
                continue;
            }

            // 运行 Agent - run方法
            String response = agent.run(input);
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
