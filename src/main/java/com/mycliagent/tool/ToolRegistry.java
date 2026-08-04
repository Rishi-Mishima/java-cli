package com.mycliagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ToolRegistry {
    public record Tool
            (
             String name,
             String description,
             JsonNode parameters,
             ToolExecutor executor
            ){}

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

        // shell工具
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
            Files.writeString(Path.of(path), content);
            return "文件已写入: " + path;
        }
        ));
    }

// 创建参数定义
private JsonNode createParameters(Param... params) {
    ObjectNode parameters = mapper.createObjectNode();
    parameters.put("type", "object");
    ObjectNode properties = parameters.putObject("properties");
    ArrayNode required = parameters.putArray("required");

    for (CreatorCandidate.Param param : params) {
        ObjectNode prop = properties.putObject(param.name());
        prop.put("type", param.type());
        prop.put("description", param.description());
        if (param.required()) {
            required.add(param.name());
        }
    }

    return parameters;
}

    // 负责执行,以后所有工具都会实现这个接口。
    public interface ToolExecutor {

        String execute(Map<String, String> args);


    }
}
