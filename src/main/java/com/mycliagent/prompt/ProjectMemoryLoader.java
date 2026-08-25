package com.mycliagent.prompt;

import java.io.IOException;
import java.nio.file.Path;

public class ProjectMemoryLoader {
    private final Path projectPath;

    private ProjectMemoryLoader(Path projectPath) {
        this.projectPath = projectPath == null ? Path.of(".") : projectPath;
    }

    public static ProjectMemoryLoader createDefault(Path projectPath) {
        return new ProjectMemoryLoader(projectPath);
    }

    public String loadForPrompt() throws IOException {
        Path memoryFile = projectPath.resolve("PAI.md").normalize();
        if (!java.nio.file.Files.exists(memoryFile) || !java.nio.file.Files.isRegularFile(memoryFile)) {
            return "";
        }
        return java.nio.file.Files.readString(memoryFile).trim();
    }
}
