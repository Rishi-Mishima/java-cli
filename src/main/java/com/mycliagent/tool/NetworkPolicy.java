package com.mycliagent.tool;

import java.net.URI;

final class NetworkPolicy {
    String checkUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return "仅允许 http/https URL";
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return "URL 缺少 host";
            }
            return null;
        } catch (Exception e) {
            return "URL 格式非法: " + e.getMessage();
        }
    }

    String acquire() {
        return null;
    }
}
