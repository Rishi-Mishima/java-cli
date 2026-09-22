package com.mycliagent.eval;

import java.util.List;

public record RetrievalEvalCase(
        String id,
        String query,
        List<String> expectedFiles
) {
}