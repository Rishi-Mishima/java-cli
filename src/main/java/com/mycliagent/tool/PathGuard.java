package com.mycliagent.tool;

import java.nio.file.Path;

final class PathGuard {
    private Path rootPath;

    PathGuard(String projectPath) {
        this.rootPath = Path.of(projectPath == null || projectPath.isBlank() ? "." : projectPath)
                .toAbsolutePath()
                .normalize();
    }

    Path getRootPath() {
        return rootPath;
    }

    Path resolveSafe(String path) {
        String value = path == null || path.isBlank() ? "." : path;
        Path resolved = rootPath.resolve(value).toAbsolutePath().normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new PolicyException("路径越界: " + value);
        }
        return resolved;
    }
}
