package com.mycliagent.memory;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.mycliagent.util.JiebaSegmenterFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


final class MemoryQueryTokenizer {
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    static Set<String> tokenize(String query) {

        //  LinkedHashSet 会保持插入顺序
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        List<String> words = SEGMENTER.sentenceProcess(
                query.toLowerCase(Locale.ROOT).trim());
        for (String word : words) {
            String trimmed = word.trim();
            // 过滤单字符和纯标点 - 过滤垃圾 Token。
            if (trimmed.length() >= 2 && !isPunctuation(trimmed)) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }
}
