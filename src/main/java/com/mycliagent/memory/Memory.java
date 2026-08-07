package com.mycliagent.memory;

import java.util.List;
import java.util.Optional;

public interface Memory {

    /**
     * 存储一条记忆
     */
    void store(MemoryEntry entry);


    /**
     * 获取所有记忆
     */
    List<MemoryEntry> getAll();


    /**
     * 删除指定记忆
     */
    boolean delete(String id);

    /**
     * 清空所有记忆
     */
    void clear();

    /**
     * 获取当前记忆的 token 总数
     */
    int getTokenCount();

    public int getMaxTokens();

    /**
     * 获取记忆条数
     */
    int size();

    /**
     * 根据ID检索记忆
     */
    Optional<MemoryEntry> retrieve(String id);

    /**
     * 搜索相关记忆
     */
    List<MemoryEntry> search(String query, int limit);
}
