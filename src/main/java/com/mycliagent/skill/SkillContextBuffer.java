package com.mycliagent.skill;

public class SkillContextBuffer {
    private final StringBuilder buffer = new StringBuilder();

    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }

    public synchronized void push(String name, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        if (!buffer.isEmpty()) {
            buffer.append("\n\n");
        }
        buffer.append("## 已加载 Skill：")
                .append(name == null || name.isBlank() ? "unknown" : name)
                .append("\n\n")
                .append(body);
    }

    public synchronized String drain() {
        String value = buffer.toString();
        clear();
        return value;
    }

    public synchronized void clear() {
        buffer.setLength(0);
    }
}
