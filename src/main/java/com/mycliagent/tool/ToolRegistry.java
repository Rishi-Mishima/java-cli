package com.mycliagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycliagent.context.ContextProfile;
import com.mycliagent.llm.LlmClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ToolRegistry {
    private final Map<String, Tool> tools;
    private final ObjectMapper mapper;

    public void setCurrentModel(String providerName, String modelName) {

    }

    public ContextProfile getContextProfile() {
        return null;
    }

    public String getProjectPath() {
        return "";
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
                        String content = Files.readString(Path.of(path));
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
                        return Files.list(Path.of(path))
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
                Files.writeString(Path.of(path), content);
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
                        Files.createDirectories(Path.of(path));
                        return "项目目录已创建: " + path;
                    } catch (Exception e) {
                        return "创建项目失败: " + e.getMessage();
                    }
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

    // 负责执行,以后所有工具都会实现这个接口。
    public interface ToolExecutor {

        String execute(Map<String, String> args);


    }
}
