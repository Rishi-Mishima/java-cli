package com.mycliagent.memory;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryRetriever {
    private final ConversationMemory shortTermMemory;
    private final LongTermMemory longTermMemory;

    public MemoryRetriever(ConversationMemory shortTermMemory, LongTermMemory longTermMemory) {
        this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory;
    }

    private record ScoredEntry(MemoryEntry entry, double score, boolean fromShortTerm) {}

    public List<MemoryEntry> retrieve(String query, int limit) {
        List<ScoredEntry> scored = new ArrayList<>();

        // 从短期记忆中检索
        for (MemoryEntry entry : shortTermMemory.getAll()) {
            double score = computeRelevanceScore(entry, query);
            if (score > 0) scored.add(new ScoredEntry(entry, score, true));
        }

        // 从长期记忆中检索（权重 ×1.2，因为更精炼）
        for (MemoryEntry entry : longTermMemory.getAll()) {
            double score = computeRelevanceScore(entry, query) * 1.2;
            if (score > 0) scored.add(new ScoredEntry(entry, score, false));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed())
                .limit(limit)
                .map(ScoredEntry::entry)
                .collect(Collectors.toList());
    }

    /**
     * 计算记忆条目与查询的相关度分数
     */
    private double computeRelevanceScore(MemoryEntry entry, String query) {
        String contentLower = entry.getContent().toLowerCase();
        String queryLower = query.toLowerCase();

        // 1. 精确匹配加分
        if (contentLower.contains(queryLower)) {
            return 1.0;
        }

        // 2. 关键词匹配
        Set<String> queryWords = MemoryQueryTokenizer.tokenize(queryLower);
        int matchedWords = 0;
        for (String word : queryWords) {
            if (!word.isEmpty() && contentLower.contains(word)) {
                matchedWords++;
            }
        }

        if (matchedWords == 0) return 0;

        double keywordScore = (double) matchedWords / queryWords.size();

        // 3. 时间衰减（越近分数越高，简单实现）
        long ageMs = System.currentTimeMillis() - entry.getTimestamp().toEpochMilli();
        double ageHours = ageMs / (1000.0 * 60 * 60);
        double timeDecay = Math.max(0.5, 1.0 - ageHours / 24.0); // 24小时内从1.0衰减到0.5

        return keywordScore * timeDecay;
    }
}
