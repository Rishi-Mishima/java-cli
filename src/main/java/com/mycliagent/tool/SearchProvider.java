package com.mycliagent.tool;

import java.util.List;

interface SearchProvider {
    String name();

    boolean isReady();

    String unavailableHint();

    List<SearchResult> search(String query, int topK) throws Exception;
}

record SearchResult(int position, String title, String snippet, String url, String source) {
}

final class SearchProviderFactory {
    private SearchProviderFactory() {
    }

    static SearchProvider create() {
        return new DisabledSearchProvider();
    }

    private static final class DisabledSearchProvider implements SearchProvider {
        @Override
        public String name() {
            return "disabled";
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public String unavailableHint() {
            return "未配置搜索 provider；请配置 SEARCH_PROVIDER 或注册 StepSearch MCP 工具。";
        }

        @Override
        public List<SearchResult> search(String query, int topK) {
            return List.of();
        }
    }
}
