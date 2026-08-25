package com.mycliagent.prompt;

public class PromptAssembler {
    public static PromptAssembler createDefault() {
        return new PromptAssembler();
    }

    public String assemble(PromptMode mode, PromptContext context) {
        PromptContext safeContext = context == null ? PromptContext.empty() : context;
        StringBuilder prompt = new StringBuilder();

        prompt.append(switch (mode) {
            case TEAM_PLANNER -> """
                    你是 Multi-Agent 团队中的规划者。
                    你的任务是把用户目标拆解为可执行步骤，并且只输出 JSON。
                    JSON 格式：
                    {"steps":[{"id":"step_1","description":"具体任务","type":"COMMAND","dependencies":[]}]}
                    dependencies 必须引用其他步骤的 id。不要输出 Markdown 或额外解释。
                    """;
            case TEAM_WORKER -> """
                    你是 Multi-Agent 团队中的执行者。
                    你负责完成当前分配的单个步骤。需要读取文件、写文件、列目录或执行命令时使用工具。
                    完成后用中文简洁说明做了什么、关键结果是什么。
                    """;
            case TEAM_REVIEWER -> """
                    你是 Multi-Agent 团队中的审查者。
                    你负责判断执行结果是否满足原始任务，并且只输出 JSON。
                    JSON 格式：
                    {"approved":true,"issues":[],"suggestions":[],"summary":"简短审查结论"}
                    如果结果为空、偏离任务或明显失败，approved 必须为 false。
                    """;
            case PLAN, PLANNER -> """
                    你是任务规划助手。请输出结构化、可执行的计划。
                    """;
            case AGENT -> """
                    你是一个智能编程助手，可以帮助用户完成各种任务。请用中文回复。
                    """;
        });

        appendSection(prompt, "项目记忆", safeContext.projectMemoryContext());
        appendSection(prompt, "相关记忆", safeContext.memoryContext());
        appendSection(prompt, "外部上下文", safeContext.externalContext());
        appendSection(prompt, "可用 Skills", safeContext.skillIndex());
        if (!safeContext.toolsEnabled()) {
            prompt.append("\n当前角色不可调用工具，请直接输出最终内容。\n");
        }
        return prompt.toString().trim();
    }

    private static void appendSection(StringBuilder prompt, String title, String content) {
        if (content != null && !content.isBlank()) {
            prompt.append("\n\n## ").append(title).append("\n").append(content.trim()).append("\n");
        }
    }

}
