package com.mycliagent.memory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ConversationMemory implements Memory{
    private final LinkedHashMap<String, MemoryEntry> entries;
    private final int maxTokens;
    private final AtomicInteger currentTokens;
    private final List<MemoryEntry> compressedSummaries;

    public ConversationMemory(LinkedHashMap<String, MemoryEntry> entries, int maxTokens, AtomicInteger currentTokens, List<MemoryEntry> compressedSummaries) {
        this.entries = entries;
        this.maxTokens = maxTokens;
        this.currentTokens = currentTokens;
        this.compressedSummaries = compressedSummaries;
    }

    @Override
    public synchronized void store(MemoryEntry entry) {
        entries.put(entry.getId(), entry);
        currentTokens.addAndGet(entry.getTokenCount());

        // 超出预算时自动淘汰最旧的条目
        while(currentTokens.get() > maxTokens && entries.size() > 1){
            evictOldest();
        }
    }

    @Override
    public synchronized boolean delete(String id) {
        MemoryEntry removed = entries.remove(id);
        if (removed != null) {
            currentTokens.addAndGet(-removed.getTokenCount());
            return true;
        }
        return false;
    }

    @Override
    public synchronized List<MemoryEntry> getAll() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public synchronized void clear() {
        entries.clear();
        currentTokens.set(0);
        compressedSummaries.clear();
    }

    @Override
    public int getTokenCount() {
        return currentTokens.get();
    }

    @Override
    public synchronized int size() {
        return entries.size();
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }

    private void evictOldest() {
        Iterator<Map.Entry<String, MemoryEntry>> it =
                entries.entrySet().iterator();

        if (it.hasNext()) {
            Map.Entry<String, MemoryEntry> oldest = it.next();

            it.remove();

            currentTokens.addAndGet(-oldest.getValue().getTokenCount());

            compressedSummaries.add(oldest.getValue());
        }
    }


    /**
     * 获取已压缩淘汰的记忆摘要
     */
    public synchronized List<MemoryEntry> getCompressedSummaries() {
        // 返回值不可以修改
        return Collections.unmodifiableList(new ArrayList<>(compressedSummaries));
    }

    /**
     * 将压缩摘要回注到记忆中（上下文压缩后调用）
     * 把summary(压缩后的)放回Memory中
     */
    public synchronized void injectSummary(MemoryEntry summary) {
        // 清空旧的压缩摘要
        compressedSummaries.clear();
        // 将摘要作为新条目插入
        entries.put(summary.getId(), summary);
        currentTokens.addAndGet(summary.getTokenCount());
    }

    /**
     * 获取记忆使用率
     */
    public synchronized double getUsageRatio() {
        return maxTokens > 0 ? (double) currentTokens.get() / maxTokens : 0;
    }

    @Override
    public synchronized Optional<MemoryEntry> retrieve(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public synchronized List<MemoryEntry> search(String query, int limit) {
        Set<String> queryTokens = MemoryQueryTokenizer.tokenize(query);
        return entries.values().stream()
                .filter(entry -> MemoryQueryTokenizer.matches(entry.getContent(), queryTokens))
                .limit(limit)
                .collect(Collectors.toList());
    }


    /**
     * 生成记忆状态摘要
     */
    public synchronized String getStatusSummary() {
        return String.format("短期记忆: %d条 / %d tokens (预算: %d, 使用率: %.0f%%, 已压缩: %d条)",
                entries.size(), currentTokens, maxTokens, getUsageRatio() * 100, compressedSummaries.size());
    }

}
