package com.mycliagent.tool;

public record RestoreResult(boolean restored, String message) {
    public String formatForCli() {
        return message == null || message.isBlank() ? (restored ? "快照已恢复" : "没有可恢复的快照") : message;
    }
}
