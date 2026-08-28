package com.mycliagent.tool;

import java.nio.file.Path;

public class SnapshotService implements AutoCloseable {
    private final Path projectPath;

    private SnapshotService(Path projectPath) {
        this.projectPath = projectPath;
    }

    public static SnapshotService forProject(Path projectPath) {
        return new SnapshotService(projectPath == null ? Path.of(".").toAbsolutePath().normalize() : projectPath);
    }

    public RestoreResult restorePreTurn(int offset) {
        return new RestoreResult(false, "当前快照服务未配置可恢复的 pre-turn 快照。");
    }

    @Override
    public void close() {
    }
}
