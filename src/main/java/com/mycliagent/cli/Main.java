package com.mycliagent.cli;

import com.mycliagent.agent.Agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        printBanner();

        // 加载 API Key
        String apiKey = loadApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ 错误: 未找到 GLM_API_KEY");
            // 立即结束程序
            System.exit(1);
        }

        // 创建 Agent - 执行之前写的构造函数 Agent ( 此时AGENT已经有了GLM客户端, tool registry, history, system prompt)
        Agent agent = new Agent(apiKey);

        // 交互式循环
        // 读取终端输入
        Scanner scanner = new Scanner(System.in);
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

    private static String loadApiKey() {
        // 先尝试从当前目录读取 .env
        File envFile = new File(".env");
        if (envFile.exists()) {
            return readApiKeyFromFile(envFile);
        }

        // 再尝试从环境变量读取
        return System.getenv("GLM_API_KEY");
    }

    private static String readApiKeyFromFile(File envFile) {
        try {
            for (String line : Files.readAllLines(envFile.toPath())) {
                String trimmed = line.trim();
                if (trimmed.startsWith("GLM_API_KEY=")) {
                    String value = trimmed.substring("GLM_API_KEY=".length()).trim();
                    return value.replaceAll("^['\"]|['\"]$", "");
                }
            }
        } catch (IOException e) {
            System.err.println("读取 .env 失败: " + e.getMessage());
        }
        return null;
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
