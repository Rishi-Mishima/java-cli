package com.mycliagent.util;

public class AnsiStyle {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String DIM = "\u001B[2m";

    public static String heading(String text) {
        return BOLD + CYAN + text + RESET;
    }

    public static String section(String text) {
        return BOLD + text + RESET;
    }

    public static String subtle(String text) {
        return DIM + text + RESET;
    }
}
