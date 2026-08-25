package com.mycliagent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

//先封装一个 GLMClient，支持普通对话和工具调用。
public class GLMClient implements LlmClient {
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private static final String MODEL = "glm-5.1";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public GLMClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getModelName() {
        return MODEL;
    }

    @Override
    public String getProviderName() {
        return "GLM";
    }



    @Override
    public ChatResponse chat(List<Message> messages, List<Tool> tools)
            throws IOException {
        // 第一阶段: 构建请求体
        // 创建了一个空的JSON对象
        ObjectNode requestBody = mapper.createObjectNode();
        //给 JSON 添加一个字段:   {"model" : "glm-5,1"}
        requestBody.put("model", MODEL);

        // 第二阶段: 把消息历史加入Json
        // 添加消息历史
        //这行在请求 JSON 中创建一个数组 {"model": "glm-5.1", "messages": []}
        ArrayNode messagesArray = requestBody.putArray("messages");
        // 遍历JAVA的所有消息
        for (Message msg : messages) {
            // 为每条消息创建 JSON 对象 {"messages": [{"role": "user", }]}
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.role());
            msgNode.put("content", msg.content());

            // 序列化assistant 工具的调用: 如果有工具调用，序列化 tool_calls
            //只有当这条消息确实包含工具调用时，才添加 tool_calls : toolCalss 不为Null, 并且toolCalls 里面至少有一个元素
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                //创建tool_calls数组
                ArrayNode toolCallsArray = msgNode.putArray("tool_calls");
                //然后遍历每一次工具调用
                for (ToolCall tc : msg.toolCalls()) {
                    //创建单个工具调用对象
                    ObjectNode tcNode = toolCallsArray.addObject();
                    tcNode.put("id", tc.id());
                    // function 固定写死, 因为这里API规定
                    tcNode.put("type", "function");
                    // 创建function 对象  {"function": {}}
                    ObjectNode functionNode = tcNode.putObject("function");
                    functionNode.put("name", tc.function().name());
                    functionNode.put("arguments", tc.function().arguments());
                }
            }

            // 如果是工具结果，添加 tool_call_id
            if (msg.toolCallId() != null) {
                msgNode.put("tool_call_id", msg.toolCallId());
            }
        }

        // 第三阶段: 添加工具定义
        // 如果当前确实提供了工具，才往请求 JSON 里加入 tools
        if (tools != null && !tools.isEmpty()) {
            // 创建工具数组
            ArrayNode toolsArray = requestBody.putArray("tools");
            // 遍历所有工具：
            for (Tool tool : tools) {
                //添加一个空工具对象：
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode functionNode = toolNode.putObject("function");
                functionNode.put("name", tool.name());
                functionNode.put("description", tool.description());
                // parameters用set(), 因为是jsonNode
                functionNode.set("parameters", tool.parameters());
            }
        }

        // 第四阶段: 创建HTTP请求体
        // 发送 HTTP 请求
        // 这里把Jackson的JSONrequestBody转换成了字符串, 后包装成 OkHttp 的：RequestBody
        // 同时告诉服务器, 这是一段JSON数据, 也就是 Content-Type: application/json
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        // 第五阶段: 创建HTTP请求
        // 使用 Builder 模式创建请求。
        Request request = new Request.Builder()
                // 设置地址
                .url(API_URL)
                //设置认证请求头
                .header("Authorization", "Bearer " + apiKey)
                //设置为 POST 请求
                .post(body)
                // builder 模式构建
                .build();

        // 第六阶段: 发送请求并解析响应
        // 解析响应
        // 发送请求
        // `httpClient.newCall(request)` 根据请求创建一次HTTP调用
        // .execute() 同步发送请求
        // 使用try(), 因为 Response 使用完需要关闭。
        try (Response response = httpClient.newCall(request).execute()) {

            //  读取响应
            // 服务器返回的响应最初是 HTTP body。 这行把它读取成字符串
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("GLM API 请求失败: HTTP " + response.code() + "\n" + responseBody);
            }
            //把 JSON 字符串解析成 JSON 树。
            // 可以root.get("choices") or root.path("choices").get(0).path("message")
            JsonNode root = mapper.readTree(responseBody);

            // 提取消息内容、工具调用、token 使用等信息
            // ...

            JsonNode messageNode = root
                    .path("choices")
                    .path(0)
                    .path("message");

            String content = messageNode
                    .path("content")
                    .asText("");

            List<ToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = messageNode.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode toolCallNode : toolCallsNode) {
                    JsonNode functionNode = toolCallNode.path("function");
                    JsonNode argumentsNode = functionNode.path("arguments");
                    String arguments = argumentsNode.isTextual()
                            ? argumentsNode.asText()
                            : argumentsNode.toString();

                    toolCalls.add(new ToolCall(
                            toolCallNode.path("id").asText(),
                            new ToolCall.Function(
                                    functionNode.path("name").asText(),
                                    arguments
                            )
                    ));
                }
            }

            return new ChatResponse(
                    "assistant",
                    content,
                    toolCalls,
                    root.path("usage").path("prompt_tokens").asInt(0),
                    root.path("usage").path("completion_tokens").asInt(0)
            );
        }

    }
}

