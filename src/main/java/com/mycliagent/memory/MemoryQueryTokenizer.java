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

    /**
     * 检查文本中是否包含任意一个 query token（子串匹配）。
     * 判断一段文本是否包含用户查询的任意一个关键词。
     * 它通常会被 search() 调用
     */
    static boolean matches(String text, Set<String> queryTokens) {
        if (text == null || text.isBlank() || queryTokens.isEmpty()) {
            return false;
        }

        //统一大小写
        String normalizedText = text.toLowerCase(Locale.ROOT);
        //遍历所有关键词
        for (String token : queryTokens) {
            if (normalizedText.contains(token)) {
                return true;
            }
        }
        return false;
    }

    // 判断一个字符串是不是全部由标点符号组成。
    private static boolean isPunctuation(String s) {
        // .codePoints()  一个一个 Unicode 码点（Code Point）- 遍历字符串里的每一个字符（准确来说是每一个 Unicode 字符）。
        // allMatch(cp -> )  Stream API, 意思是所有字符都满足条件吗？每一个字符都必须满足条件。
        // cp: Unicode Code Point。
        return s.codePoints().allMatch(cp ->
                // !Character.isLetterOrDigit(cp) 是JAVA自带方法, 判断是不是字母或者数字 --》 不是字母，也不是数字。
                // Character.UnicodeScript.of(cp) 判断这个字符属于哪个文字系统（Script）。
                //!= Character.UnicodeScript.HAN 不是汉字
                // 所以 真正的条件是：既不是字母数字，也不是中文汉字。所以这两个条件都满足才是标点
                !Character.isLetterOrDigit(cp) && Character.UnicodeScript.of(cp) != Character.UnicodeScript.HAN);
    }
}
