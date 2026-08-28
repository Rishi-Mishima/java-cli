package com.mycliagent.tool;

import java.util.Locale;
import java.util.regex.Pattern;

final class CommandGuard {
    private static final Pattern DANGEROUS_RM = Pattern.compile("(^|\\s)rm\\s+(-[^\\s]*[rf][^\\s]*|-[^\\s]*[fr][^\\s]*)(\\s|$)");

    private CommandGuard() {
    }

    static String check(String command) {
        if (command == null || command.isBlank()) {
            return "命令不能为空";
        }
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        if (DANGEROUS_RM.matcher(normalized).find()) {
            return "拒绝执行高风险 rm -rf 命令";
        }
        if (normalized.contains(":(){") || normalized.contains("mkfs") || normalized.contains("shutdown")) {
            return "拒绝执行明显高风险系统命令";
        }
        return null;
    }
}
