package com.mycliagent.hitl;

import com.mycliagent.browser.BrowserCheckResult;
import com.mycliagent.tool.ToolOutput;
import com.mycliagent.tool.ToolRegistry;

import java.util.concurrent.TimeUnit;
import com.mycliagent.tool.*;

public class HitlToolRegistry extends ToolRegistry {

    private final HitlHandler hitlHandler;

    public HitlToolRegistry(HitlHandler hitlHandler) {
        super();
        this.hitlHandler = hitlHandler;
    }

    @Override
    public String executeTool(String name, String argumentsJson) {
        return executeToolOutput(name, argumentsJson).text();
    }

    @Override
    public ToolOutput executeToolOutput(String name, String argumentsJson) {
        // HITL 未启用或该工具不需要审批，直接执行
        if (!hitlHandler.isEnabled() || !ApprovalPolicy.requiresApproval(name)) {
            return super.doExecuteTool(name, argumentsJson);
        }
        BrowserCheckResult browserCheck = checkBrowserTool(name, argumentsJson, true);
        if (browserCheck.blocked()) {
            return super.doExecuteTool(name, argumentsJson);
        }
        if (browserCheck.requiresPerCallApproval()) {
            return executeAfterExplicitApproval(name, argumentsJson, browserCheck.sensitiveNotice());
        }
        String mcpServer = ApprovalPolicy.mcpServerName(name);
        if (hitlHandler.isApprovedAllByTool(name) || hitlHandler.isApprovedAllByServer(mcpServer)) {
            return super.doExecuteTool(name, argumentsJson);
        }

        return executeAfterExplicitApproval(name, argumentsJson, null);
    }

    private ToolOutput executeAfterExplicitApproval(String name, String argumentsJson, String sensitiveNotice) {
        long start = System.nanoTime();
        ApprovalRequest request = ApprovalRequest.of(name, argumentsJson, null, null, sensitiveNotice);
        ApprovalResult result = hitlHandler.requestApproval(request);

        if (result.isRejected()) {
            String reason = result.reason() != null && !result.reason().isBlank()
                    ? result.reason()
                    : "用户拒绝了此操作";
            getAuditLog().record(AuditLog.AuditEntry.denyByHitl(
                    name, argumentsJson, reason, elapsedMillis(start)));
            return ToolOutput.text("[HITL] 操作已被拒绝：" + reason);
        }

        if (result.isSkipped()) {
            getAuditLog().record(AuditLog.AuditEntry.denyByHitl(
                    name, argumentsJson, "用户跳过", elapsedMillis(start)));
            return ToolOutput.text("[HITL] 操作已被跳过");
        }

        // 批准（含修改参数）- 使用 effectiveArguments 获取最终参数；父类执行路径会负责 allow audit
        String effectiveArgs = result.effectiveArguments(argumentsJson);
        return super.doExecuteTool(name, effectiveArgs);
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    public HitlHandler getHitlHandler() {
        return hitlHandler;
    }
}

