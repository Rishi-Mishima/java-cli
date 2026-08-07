package com.mycliagent.memory;

import java.util.LinkedHashMap;
import java.util.List;
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

    private void evictOldest() {
        // early return安全检查
        if (entries.isEmpty()) return;

        // 拿出所有的Key --> 从头访问 .next ---> m1
        String oldestKey = entries.keySet().iterator().next();

        // 删除 - remove() 会把被删除的 value 返回 - 刚才删掉的 MemoryEntry 有多少 token？
        // Map<K, V> - > Map<String, MemoryEnry>
        MemoryEntry removedEntry = entries.remove(oldestKey);

        if (removedEntry != null) {
            currentTokens.addAndGet(-removedEntry.getTokenCount());
        }
    }
}
