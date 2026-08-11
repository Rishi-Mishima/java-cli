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
                .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed()) /// 1. 按得分从高到低降序排列
                .limit(limit) // 2. 只截取前 limit 条最相关的记录
                .map(ScoredEntry::entry) // 3. 解包，只提取 MemoryEntry 对象
                .collect(Collectors.toList());
    }

    /**
     * 计算记忆条目与查询的相关度分数
     * 输入是一条记忆 entry 和用户查询 query，返回 double 分数。
     */
    private double computeRelevanceScore(MemoryEntry entry, String query) {
        // 把记忆内容和查询都转成小写，避免大小写影响匹配。
        String contentLower = entry.getContent().toLowerCase();
        String queryLower = query.toLowerCase();

        // 1. 精确匹配加分 - 一旦精确匹配，就不计算时间衰减了。- 所以哪怕这条记忆很旧，只要完整包含查询，也直接拿 1.0。
        // 将文本统一转小写（忽略大小写），如果记忆内容完整包含了当前查询短语（例如 query 为 `"Java并发"`，记忆为 `"对于Java并发编程"`），直接返回最高分 `1.0`，不再扣除时间分。
        if (contentLower.contains(queryLower)) {
            return 1.0;
        }

        // 2. 关键词匹配 (词频命中率) - 如果完整短语没有匹配，就开始做关键词匹配
        // **逻辑**：如果未能完整匹配，则把 `query` 拆分成词集合（Set），逐词检查记忆中是否包含该词。
        Set<String> queryWords = MemoryQueryTokenizer.tokenize(queryLower);
        // 记录命中了几个关键词。
        int matchedWords = 0;
        for (String word : queryWords) {
            if (!word.isEmpty() && contentLower.contains(word)) {
                matchedWords++;
            }
        }

        if (matchedWords == 0) return 0;

        // 基础分 = 命中的关键词数量/查询词总数
        double keywordScore = (double) matchedWords / queryWords.size();

        // 3. 时间衰减（越近分数越高，简单实现）- 记忆越旧，重要性越低。随着时间的推移，对基础分做线性折扣。
        // 当前时间 - 记忆创建时间。
        long ageMs = System.currentTimeMillis() - entry.getTimestamp().toEpochMilli();
        double ageHours = ageMs / (1000.0 * 60 * 60); //就是一小时的毫秒数
        double timeDecay = Math.max(0.5, 1.0 - ageHours / 24.0); // 24小时内从1.0衰减到0.5

        return keywordScore * timeDecay;
    }
}
