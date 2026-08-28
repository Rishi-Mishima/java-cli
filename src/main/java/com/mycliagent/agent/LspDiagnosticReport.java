package com.mycliagent.agent;

public record LspDiagnosticReport(String promptText, String displayText) {
    public static final LspDiagnosticReport EMPTY = new LspDiagnosticReport("", "");

    public boolean isEmpty() {
        return (promptText == null || promptText.isBlank())
                && (displayText == null || displayText.isBlank());
    }
}
