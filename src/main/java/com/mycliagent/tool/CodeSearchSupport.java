package com.mycliagent.tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

record CodeSearchRequest(String pattern, Path root, Path projectRoot, String glob, boolean regex,
                         boolean caseSensitive, int contextLines, int maxResults, int headLimit) {
}

record ContextLine(int lineNumber, String text) {
}

record GrepMatch(String file, int lineNumber, List<ContextLine> context) {
}

record CodeSearchResult(String engine, List<GrepMatch> matches, boolean partial, String partialReason) {
}

final class RipgrepCodeSearchEngine {
    private final Set<String> excludedDirs;

    RipgrepCodeSearchEngine(Set<String> excludedDirs) {
        this.excludedDirs = excludedDirs == null ? Set.of() : excludedDirs;
    }

    CodeSearchResult search(CodeSearchRequest request) {
        try {
            return searchWithRipgrep(request);
        } catch (Exception ignored) {
            return searchInJava(request);
        }
    }

    private CodeSearchResult searchWithRipgrep(CodeSearchRequest request) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("rg");
        command.add("--line-number");
        command.add("--color");
        command.add("never");
        command.add("--max-count");
        command.add(String.valueOf(request.headLimit()));
        if (!request.regex()) {
            command.add("--fixed-strings");
        }
        if (!request.caseSensitive()) {
            command.add("--ignore-case");
        }
        for (String dir : excludedDirs) {
            command.add("--glob");
            command.add("!" + dir + "/**");
        }
        if (request.glob() != null && !request.glob().isBlank()) {
            command.add("--glob");
            command.add(request.glob());
        }
        command.add(request.pattern());
        command.add(request.root().toString());

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<GrepMatch> matches = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && matches.size() < request.maxResults()) {
                String[] parts = line.split(":", 3);
                if (parts.length < 3) {
                    continue;
                }
                Path file = Path.of(parts[0]).toAbsolutePath().normalize();
                int lineNumber = parseInt(parts[1], 1);
                String text = parts[2];
                matches.add(new GrepMatch(toRelative(request.projectRoot(), file), lineNumber,
                        List.of(new ContextLine(lineNumber, text))));
            }
        }
        int code = process.waitFor();
        boolean partial = matches.size() >= request.maxResults();
        String reason = code > 1 ? "rg exit code " + code : "";
        return new CodeSearchResult("rg", matches, partial, reason);
    }

    private CodeSearchResult searchInJava(CodeSearchRequest request) {
        List<GrepMatch> matches = new ArrayList<>();
        Pattern compiled = request.regex()
                ? Pattern.compile(request.pattern(), request.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE)
                : null;
        try (var stream = Files.walk(request.root())) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isExcluded(request.projectRoot(), path))
                    .toList();
            for (Path file : files) {
                if (matches.size() >= request.maxResults()) {
                    break;
                }
                if (request.glob() != null && !request.glob().isBlank()
                        && !request.projectRoot().getFileSystem().getPathMatcher("glob:" + request.glob())
                        .matches(request.projectRoot().relativize(file))) {
                    continue;
                }
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                int perFile = 0;
                for (int i = 0; i < lines.size() && matches.size() < request.maxResults(); i++) {
                    if (perFile >= request.headLimit()) {
                        break;
                    }
                    String line = lines.get(i);
                    boolean hit = request.regex()
                            ? compiled.matcher(line).find()
                            : contains(line, request.pattern(), request.caseSensitive());
                    if (hit) {
                        int lineNumber = i + 1;
                        matches.add(new GrepMatch(toRelative(request.projectRoot(), file), lineNumber,
                                context(lines, lineNumber, request.contextLines())));
                        perFile++;
                    }
                }
            }
        } catch (Exception e) {
            return new CodeSearchResult("java", matches, matches.size() >= request.maxResults(), e.getMessage());
        }
        return new CodeSearchResult("java", matches, matches.size() >= request.maxResults(), "");
    }

    private boolean isExcluded(Path projectRoot, Path file) {
        Path relative = projectRoot.relativize(file.toAbsolutePath().normalize());
        for (Path part : relative) {
            if (excludedDirs.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static List<ContextLine> context(List<String> lines, int lineNumber, int contextLines) {
        int from = Math.max(1, lineNumber - contextLines);
        int to = Math.min(lines.size(), lineNumber + contextLines);
        List<ContextLine> result = new ArrayList<>();
        for (int n = from; n <= to; n++) {
            result.add(new ContextLine(n, lines.get(n - 1)));
        }
        return result;
    }

    private static boolean contains(String text, String pattern, boolean caseSensitive) {
        if (caseSensitive) {
            return text.contains(pattern);
        }
        return text.toLowerCase().contains(pattern.toLowerCase());
    }

    private static String toRelative(Path root, Path file) {
        try {
            return root.relativize(file).toString();
        } catch (Exception e) {
            return file.toString();
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
