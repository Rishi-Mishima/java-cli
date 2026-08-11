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

    // 1. 无参构造方法（给懒人用的默认配置）
    public LongTermMemory() {
        this(resolveStorageDir()); // 自动调用下面的带参构造方法，并传入默认文件夹路径
    }

    // 2. 带参构造方法（核心逻辑）
    public LongTermMemory(File storageDir) {
        // 线程安全的 Map。未来多线程同时读写记忆时，不会把数据搞乱。
        this.entries = new ConcurrentHashMap<>();
        // 线程安全的整数计数器，初始值为 0，用来记录当前记忆总共占用了多少 Token。
        this.tokenCounter = new AtomicInteger(0);
        // Jackson 库提供的工具，专门用来把 Java 对象和 JSON 文本互转。
        this.mapper = new ObjectMapper();
        // 开启 JSON 美化排版。开启后存到磁盘上的 JSON 文件会有换行和缩进（像漂亮的代码一样），而不是挤成一团的单行文本。
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 确保存储目录存在
        File dir = storageDir;
        if (!dir.exists()) {
            dir.mkdirs(); // // 如果文件夹不存在，就自动创建（包括多级父目录）
        }
        // // 拼接出最终的文件路径（如：.mycliagent/memory/long_term_memory.json）
        this.storageFile = new File(dir, STORAGE_FILE);

        // 启动时加载已有记忆
        // 在准备工作做完的最后一步，立刻调用 loadFromDisk() 去硬盘里读取之前存过的 JSON 文件，把记忆加载到 entries 中。这样程序重启后，历史记忆不会丢失。
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

    /**
     * 持久化到磁盘
     */
    private void saveToDisk() {
        try {
            // 1. 取出 entries 里所有的 MemoryEntry 对象，用 Stream 流逐个处理
            List<Map<String, Object>> dataList = entries.values().stream()
                    // 2. 把每个 MemoryEntry 对象转成 Map<String, Object> 结构
                    .map(this::entryToMap)
                    // 3. 收集成一个 List 列表
                    .collect(Collectors.toList());
            // 4. 用 Jackson 库把这个 List 直接写进存储文件（自动转为 JSON 数组）
            mapper.writeValue(storageFile, dataList);
        } catch (IOException e) {
            // 5. 如果写入失败（如磁盘满了或没有读写权限），记录警告日志，防止程序奔溃
            log.warn("长期记忆持久化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从磁盘加载
     */
    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        if (!storageFile.exists()) return;

        try {
            List<Map<String, Object>> dataList = mapper.readValue(storageFile, List.class);
            for (Map<String, Object> data : dataList) {
                MemoryEntry entry = mapToEntry(data);
                if (entry != null) {
                    entries.put(entry.getId(), entry);
                    tokenCounter.addAndGet(entry.getTokenCount());
                }
            }
            log.info("加载了 {} 条长期记忆", entries.size());
        } catch (IOException e) {
            log.warn("加载长期记忆失败: {}", e.getMessage(), e);
        }
    }

    //决定 LongTermMemory 最终应该把文件存到哪个目录。
    private static File resolveStorageDir() {
        // 优先级 1：从 Java 系统属性 (-D 参数) 获取
        String configuredDir = System.getProperty(STORAGE_DIR_PROPERTY);
        if (configuredDir == null || configuredDir.isBlank()) {
            // 优先级 2：如果系统属性没有，再从环境变量获取
            configuredDir = System.getenv(STORAGE_DIR_ENV);
        }

        // 如果通过上述两种方式拿到了有效路径，直接使用该路径
        if (configuredDir != null && !configuredDir.isBlank()) {
            return new File(configuredDir);
        }

        // 优先级 3：保底默认路径（用户家目录下的 .paicli/memory）
        // 在 Linux/Mac 下相当于：~/.paicli/memory
        // 在 Windows 下相当于：C:\Users\你的用户名\.paicli\memory
        return new File(new File(System.getProperty("user.home"), ".mycliagent"), "memory");
    }

    private Map<String, Object> entryToMap(MemoryEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.getId());
        map.put("content", entry.getContent());
        map.put("type", entry.getType().name());
        map.put("timestamp", entry.getTimestamp().toString());
        map.put("metadata", entry.getMetadata());
        map.put("tokenCount", entry.getTokenCount());
        return map;
    }


    @SuppressWarnings("unchecked")
    private MemoryEntry mapToEntry(Map<String, Object> map) {
        try {
            String id = (String) map.get("id");
            String content = (String) map.get("content");
            MemoryEntry.MemoryType type = MemoryEntry.MemoryType.valueOf((String) map.get("type"));
            Instant timestamp = null;
            Object timestampObj = map.get("timestamp");
            if (timestampObj instanceof String timestampValue && !timestampValue.isBlank()) {
                timestamp = Instant.parse(timestampValue);
            }
            Map<String, String> metadata = new HashMap<>();
            Object metaObj = map.get("metadata");
            if (metaObj instanceof Map) {
                ((Map<String, Object>) metaObj).forEach((k, v) -> metadata.put(k, String.valueOf(v)));
            }
            int tokenCount = map.get("tokenCount") instanceof Number n ? n.intValue() : MemoryEntry.estimateTokens(content);
            return new MemoryEntry(id, content, type, timestamp, metadata, tokenCount);
        } catch (Exception e) {
            return null;
        }
    }

}
