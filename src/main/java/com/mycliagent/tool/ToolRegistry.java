package com.mycliagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycliagent.agent.LspDiagnosticReport;
import com.mycliagent.agent.ToolExecutionResult;
import com.mycliagent.agent.ToolInvocation;
import com.mycliagent.context.ContextProfile;
import com.mycliagent.llm.LlmClient;
import com.mycliagent.rag.CodeRetriever;
import com.mycliagent.rag.SearchResultFormatter;
import com.mycliagent.rag.VectorStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ToolRegistry {
    private final Map<String, Tool> tools;
    private final ObjectMapper mapper;
    private ContextProfile contextProfile = ContextProfile.from(null);
    private String projectPath = System.getProperty("user.dir");
    private Consumer<String> scopedMemorySaver = fact -> {};
    private String providerName = "";
    private String modelName = "";

    public void setCurrentModel(String providerName, String modelName) {
        this.providerName = providerName == null ? "" : providerName;
        this.modelName = modelName == null ? "" : modelName;
        this.contextProfile = ContextProfile.from(new ModelInfoClient(this.providerName, this.modelName));
    }

    public ContextProfile getContextProfile() {
        return contextProfile;
    }

    public void setContextProfile(ContextProfile contextProfile) {
        if (contextProfile != null) {
            this.contextProfile = contextProfile;
        }
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        if (projectPath != null && !projectPath.isBlank()) {
            this.projectPath = projectPath;
        }
    }

    public void setScopedMemorySaver(Consumer<String> scopedMemorySaver) {
        this.scopedMemorySaver = scopedMemorySaver == null ? fact -> {} : scopedMemorySaver;
    }

    public LspDiagnosticReport flushPendingLspDiagnostics() {
        return null;
    }

    public record Tool
            (
             String name,
             String description,
             JsonNode parameters,
             ToolExecutor executor
            ){}

    private record Param(String name, String type, String description, boolean required) {}

    public ToolRegistry() {
        this.tools = new HashMap<>();
        this.mapper = new ObjectMapper();
        registerFileTools();
        registerShellTools();
        registerRagTools();
    }

    // 把所有文件相关的工具放进 tools 里面。
    private void registerFileTools() {
        // read_file 工具
        // key 是 "read_file" , value是一个Tool 对象
        // new Tool 传上面四个record的参数
        tools.put("read_file", new Tool(
                "read_file",
                // 面向AI
                "读取文件内容，用于查看代码、配置文件等",
                // 面向LLM, read_file需要的参数
                createParameters(new Param("path", "string", "文件路径", true)),
                // 就是execute方法, 因为ToolExecutor 就一个方法, JAVA可以自动推倒
                args -> {
                    String path = args.get("path");
                    try {
                        //path 只是"文件的位置"，Files.readString() 会根据这个位置去磁盘把文件内容读出来。
                        String content = Files.readString(resolvePath(path));
                        return "文件内容:\n" + content;
                    } catch (Exception e) {
                        return "读取文件失败: " + e.getMessage();
                    }
                }
        ));

        tools.put("list_dir", new Tool(
                "list_dir",
                "列出目录内容",
                createParameters(new Param("path", "string", "目录路径", true)),
                args -> {
                    String path = args.get("path");
                    try {
                        return Files.list(resolvePath(path))
                                .map(p -> p.getFileName().toString())
                                .collect(Collectors.joining("\n"));
                    } catch (Exception e) {
                        return "列出目录失败: " + e.getMessage();
                    }
                }
        ));

        // write_file工具
        tools.put("write_file", new Tool(
                "write_file",
                "写入文件内容",
                createParameters(
                        // 这个工具需要path和content两个参数
                        new Param("path", "string", "文件路径", true),
                        new Param("content", "string", "文件内容", true)
                ), args -> {
            //  取参数
            String path = args.get("path");
            String content = args.get("content");
            //Path.of(path) 这个文件的位置
            // Files.writeString(...) 把字符串写到文件里
            try {
                Files.writeString(resolvePath(path), content);
                return "文件已写入: " + path;
            } catch (Exception e) {
                return "写入文件失败: " + e.getMessage();
            }
        }
        ));

        tools.put("create_project", new Tool(
                "create_project",
                "创建新项目目录",
                createParameters(new Param("path", "string", "项目目录路径", true)),
                args -> {
                    String path = args.get("path");
                    try {
                        Files.createDirectories(resolvePath(path));
                        return "项目目录已创建: " + path;
                    } catch (Exception e) {
                        return "创建项目失败: " + e.getMessage();
                    }
                }
        ));

        tools.put("save_memory", new Tool(
                "save_memory",
                "保存一条对后续任务有帮助的长期记忆",
                createParameters(new Param("fact", "string", "要保存的事实或偏好", true)),
                args -> {
                    String fact = args.get("fact");
                    if (fact == null || fact.isBlank()) {
                        return "保存记忆失败: fact 为空";
                    }
                    scopedMemorySaver.accept(fact);
                    return "记忆已保存";
                }
        ));
    }

    private void registerShellTools(){
        tools.put("execute_command", new Tool(
                "execute_command",
                "执行Shell命令，用于编译、运行、Git操作等",
                createParameters(new Param("command", "string", "要执行的命令", true)),
                args -> {
                    String command = args.get("command");
                    try {
                        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();

                        // 读取命令输出
                        StringBuilder output = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                        }

                        int exitCode = process.waitFor();
                        return String.format("命令执行完成 (exit code: %d)\n%s",
                                exitCode, output);
                    } catch (Exception e) {
                        return "执行命令失败: " + e.getMessage();
                    }
                }
        ));
    }

    /**
     * 注册 RAG 检索工具
     */
    private void registerRagTools() {
        tools.put("search_code", new Tool(
                "search_code",
                "RAG 语义辅助检索代码库，根据自然语言描述查找相关代码块；精确符号/字符串定位请优先用 grep_code/glob_files/read_file；默认 top_k=5，可显式指定（上限 30）",
                createParameters(
                        new Param("query", "string", "自然语言查询描述，例如'用户登录的实现'", true),
                        new Param("top_k", "integer", "返回结果数量（默认 5，上限 30）", false)
                ),
                args -> {
                    String query = args.get("query");
                    int topK = 5;
                    try {
                        if (args.containsKey("top_k")) {
                            topK = Integer.parseInt(args.get("top_k"));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    topK = Math.max(1, Math.min(topK, 30));

                    try (CodeRetriever retriever = new CodeRetriever(projectPath)) {
                        var stats = retriever.getStats();
                        if (stats.chunkCount() == 0) {
                            return "代码库尚未索引，请先使用 /index 命令索引当前项目。";
                        }

                        List<VectorStore.SearchResult> results = retriever.hybridSearch(query, topK);
                        if (results.isEmpty()) {
                            return "未找到与查询相关的代码。";
                        }

                        return SearchResultFormatter.formatForTool(query, results);
                    } catch (Exception e) {
                        return "代码检索失败: " + e.getMessage();
                    }
                }
        ));
    }

// 创建参数定义
private JsonNode createParameters(Param... params) {
    ObjectNode parameters = mapper.createObjectNode();
    parameters.put("type", "object");
    ObjectNode properties = parameters.putObject("properties");
    ArrayNode required = parameters.putArray("required");

    for (Param param : params) {
        ObjectNode prop = properties.putObject(param.name());
        prop.put("type", param.type());
        prop.put("description", param.description());
        if (param.required()) {
            required.add(param.name());
        }
    }

    return parameters;
}

    public List<LlmClient.Tool> getToolDefinitions() {
        return tools.values().stream()
                .map(tool -> new LlmClient.Tool(
                        tool.name(),
                        tool.description(),
                        tool.parameters()
                ))
                .toList();
    }

    public String executeTool(String name, String argumentsJson) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return "未知工具: " + name;
        }

        try {
            Map<String, String> args = mapper.readValue(
                    argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)
            );
            return tool.executor().execute(args);
        } catch (Exception e) {
            return "工具参数解析失败: " + e.getMessage();
        }
    }

    public List<ToolExecutionResult> executeTools(List<ToolInvocation> invocations) {
        if (invocations == null || invocations.isEmpty()) {
            return List.of();
        }
        return invocations.parallelStream()
                .map(invocation -> new ToolExecutionResult(
                        invocation.id(),
                        invocation.name(),
                        executeTool(invocation.name(), invocation.argumentsJson())))
                .toList();
    }

    private Path resolvePath(String path) {
        Path candidate = Path.of(path == null || path.isBlank() ? "." : path);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return Path.of(projectPath).resolve(candidate).normalize();
    }

    // 负责执行,以后所有工具都会实现这个接口。
    public interface ToolExecutor {

        String execute(Map<String, String> args);


    }

    private record ModelInfoClient(String providerName, String modelName) implements LlmClient {
        @Override
        public ChatResponse chat(List<Message> messages, List<LlmClient.Tool> tools) {
            throw new UnsupportedOperationException("ModelInfoClient cannot chat");
        }

        @Override
        public ChatResponse chat(List<Message> messages, List<LlmClient.Tool> tools, StreamListener listener) {
            throw new UnsupportedOperationException("ModelInfoClient cannot chat");
        }

        @Override
        public String getModelName() {
            return modelName == null ? "" : modelName;
        }

        @Override
        public String getProviderName() {
            return providerName == null ? "" : providerName;
        }
    }
}
