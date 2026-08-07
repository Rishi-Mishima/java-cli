package com.mycliagent.memory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void store(MemoryEntry entry) {
        entries.put(entry.getId(), entry);
        currentTokens.addAndGet(entry.getTokenCount());

        // 超出预算时自动淘汰最旧的条目
        while(currentTokens.get() > maxTokens && entries.size() > 1){
            evictOldest();
        }
    }

    @Override
    public boolean delete(String id) {
        MemoryEntry removed = entries.remove(id);
        if (removed != null) {
            currentTokens.addAndGet(-removed.getTokenCount());
            return true;
        }
        return false;
    }

    @Override
    public List<MemoryEntry> getAll() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public void clear() {
        entries.clear();
        currentTokens.set(0);
        compressedSummaries.clear();
    }

    @Override
    public int getTokenCount() {
        return currentTokens.get();
    }

    @Override
    public int size() {
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
    public List<MemoryEntry> getCompressedSummaries() {
        // 返回值不可以修改
        return Collections.unmodifiableList(compressedSummaries);
    }

    /**
     * 将压缩摘要回注到记忆中（上下文压缩后调用）
     * 把summary(压缩后的)放回Memory中
     */
    public void injectSummary(MemoryEntry summary) {
        // 清空旧的压缩摘要
        compressedSummaries.clear();
        // 将摘要作为新条目插入
        entries.put(summary.getId(), summary);
        currentTokens.addAndGet(summary.getTokenCount());
    }

    /**
     * 获取记忆使用率
     */
    public double getUsageRatio() {
        return maxTokens > 0 ? (double) currentTokens.get() / maxTokens : 0;
    }
}
