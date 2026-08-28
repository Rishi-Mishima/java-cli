package com.mycliagent.tool;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;

final class WebFetcher {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(20))
            .build();

    RawResponse fetch(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MyCliAgent/1.0")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            String body = response.body() == null ? "" : response.body().string();
            String finalUrl = response.request().url().toString();
            return new RawResponse(finalUrl, body);
        }
    }

    record RawResponse(String url, String body) {
    }
}
