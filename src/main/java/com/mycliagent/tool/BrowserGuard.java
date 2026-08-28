package com.mycliagent.tool;

import com.mycliagent.browser.BrowserCheckResult;

public class BrowserGuard {
    public static boolean isChromeTool(String name) {
        return name != null && (name.startsWith("mcp__chrome__") || name.startsWith("mcp__browser__"));
    }

    public BrowserCheckResult check(String name, String argumentsJson, boolean apply) {
        return BrowserCheckResult.allow(null);
    }

    public void applyAfterExecution(String name, String argumentsJson, String outputText) {
    }
}
