package com.mycliagent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class LongTermMemory implements Memory {
    private static final Logger log = LoggerFactory.getLogger(LongTermMemory.class);
    private static final String STORAGE_DIR_PROPERTY = "paicli.memory.dir";
    private static final String STORAGE_DIR_ENV = "PAICLI_MEMORY_DIR";
    private static final String STORAGE_FILE = "long_term_memory.json";
    private final Map<String, MemoryEntry> entries;
    private final AtomicInteger tokenCounter;
    private final ObjectMapper mapper;
    private final File storageFile;

    // 存储
    private static final String STORAGE_DIR = ".MyCliAgent/memory";
    private static final String STORAGE_FILE = "long_term_memory.json";

    public LongTermMemory() {

        loadFromDisk();

    }

    @Override
    public void store(MemoryEntry entry) {
        // 去重检查：内容完全相同则跳过
        Optional<Map.Entry<String, MemoryEntry>> existing = entries.entrySet().stream()
                .filter(e -> e.getValue().getContent().equals(entry.getContent()))
                .findFirst();
        if (existing.isPresent()) return;

        entries.put(entry.getId(), entry);
        tokenCounter.addAndGet(entry.getTokenCount());
        saveToDisk();  // 每次存完都持久化
    }

    @Override
    public List<MemoryEntry> getAll() {
        return List.of();
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public int getTokenCount() {
        return 0;
    }

    @Override
    public int getMaxTokens() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Optional<MemoryEntry> retrieve(String id) {
        return Optional.empty();
    }

    @Override
    public List<MemoryEntry> search(String query, int limit) {
        // 把查询分词
        Set<String> queryTokens = MemoryQueryTokenizer.tokenize(query);

        return entries.values().stream()
                .filter(entry -> {
                    if (MemoryQueryTokenizer.matches(entry.getContent(), queryTokens)) {
                        return true;
                    }
                    // 如果正文没有匹配, 只要有一个 metadata 匹配，就返回 true。
                    return entry.getMetadata().values().stream()
                            .anyMatch(value -> MemoryQueryTokenizer.matches(value, queryTokens));
                })
                // 只保留Limit数量的结果
                .limit(limit)
                .collect(Collectors.toList());
    }
}
