package com.mycliagent.tool;

import com.mycliagent.agent.LspDiagnosticReport;

import java.nio.file.Path;

public class LspManager {
    private String projectPath;

    public LspManager(String projectPath) {
        setProjectPath(projectPath);
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath == null || projectPath.isBlank() ? System.getProperty("user.dir") : projectPath;
    }

    public void runPostEditLspHook(String displayPath, Path safePath) {
    }

    public LspDiagnosticReport flushPendingDiagnostics() {
        return LspDiagnosticReport.EMPTY;
    }
}
