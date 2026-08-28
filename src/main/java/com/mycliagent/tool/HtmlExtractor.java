package com.mycliagent.tool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

final class HtmlExtractor {
    Extracted extract(String html, String url) {
        if (html == null || html.isBlank()) {
            return new Extracted("", "");
        }
        Document document = Jsoup.parse(html, url == null ? "" : url);
        document.select("script,style,noscript,svg").remove();
        String title = document.title() == null ? "" : document.title();
        String text = document.body() == null ? document.text() : document.body().text();
        return new Extracted(title, text == null ? "" : text);
    }

    record Extracted(String title, String markdown) {
    }
}
