package com.mycliagent.memory;

import java.util.List;

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

}
