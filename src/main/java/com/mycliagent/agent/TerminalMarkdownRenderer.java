package com.mycliagent.agent;

import java.io.PrintStream;

final class TerminalMarkdownRenderer {
    private final PrintStream out;

    TerminalMarkdownRenderer(PrintStream out) {
        this.out = out == null ? System.out : out;
    }

    void append(String text) {
        if (text != null && !text.isEmpty()) {
            out.print(text);
        }
    }

    void finish() {
        out.flush();
    }
}
