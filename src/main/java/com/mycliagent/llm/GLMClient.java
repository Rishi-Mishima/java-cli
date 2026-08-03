package com.mycliagent.llm;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

//先封装一个 GLMClient，支持普通对话和工具调用。
public class GLMClient {
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private static final String MODEL = "glm-5.1";
    private final String apiKey;
    private final OkHttpClient httpClient;

    public GLMClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }
}
