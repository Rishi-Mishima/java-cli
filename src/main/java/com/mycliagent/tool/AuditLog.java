package com.mycliagent.tool;

import com.mycliagent.browser.BrowserAuditMetadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditLog {
    private final List<AuditEntry> entries = new ArrayList<>();

    public synchronized void record(AuditEntry entry) {
        if (entry != null) {
            entries.add(entry);
        }
    }

    public synchronized List<AuditEntry> entries() {
        return List.copyOf(entries);
    }

    public record AuditEntry(String decision, String toolName, String argumentsJson, String reason,
                             long elapsedMillis, Instant timestamp, BrowserAuditMetadata browserMetadata) {
        public static AuditEntry allow(String toolName, String argumentsJson, long elapsedMillis,
                                       BrowserAuditMetadata metadata) {
            return new AuditEntry("allow", toolName, argumentsJson, "", elapsedMillis, Instant.now(), metadata);
        }

        public static AuditEntry denyByPolicy(String toolName, String argumentsJson, String reason,
                                              long elapsedMillis, BrowserAuditMetadata metadata) {
            return new AuditEntry("deny_policy", toolName, argumentsJson, reason, elapsedMillis, Instant.now(), metadata);
        }

        public static AuditEntry denyByHitl(String toolName, String argumentsJson, String reason,
                                            long elapsedMillis) {
            return new AuditEntry("deny_hitl", toolName, argumentsJson, reason, elapsedMillis, Instant.now(), null);
        }

        public static AuditEntry error(String toolName, String argumentsJson, String reason,
                                       long elapsedMillis, BrowserAuditMetadata metadata) {
            return new AuditEntry("error", toolName, argumentsJson, reason, elapsedMillis, Instant.now(), metadata);
        }
    }
}
