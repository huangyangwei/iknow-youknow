package com.huangyangwei.iknow.module.knowledge.support;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 富文本双通道转换：
 * - sanitize()：HTML 净化（白名单标签，剥离 script/onclick 等）
 * - toPlainText()：HTML → 纯文本（检索通道，发布时写入 plain_text）
 * - markdownToHtml()：Markdown（含 GFM 表格）→ HTML，用于批量导入
 * - summarize()：纯文本 → 摘要
 */
@Component
public class HtmlContentConverter {

    private static final int DEFAULT_SUMMARY_LENGTH = 200;

    private final Parser markdownParser;
    private final HtmlRenderer markdownRenderer;

    public HtmlContentConverter() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.markdownParser = Parser.builder().extensions(extensions).build();
        this.markdownRenderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    /** 白名单净化：保留常用排版标签，移除脚本/事件属性。 */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, Safelist.relaxed());
    }

    /** HTML → 纯文本（去标签、去空白），供检索通道 plain_text 使用。 */
    public String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text().trim();
    }

    /** Markdown（含 GFM 表格）→ HTML。 */
    public String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return markdownRenderer.render(markdownParser.parse(markdown));
    }

    /** 从纯文本截取摘要；空文本返回 null。 */
    public String summarize(String plainText, int maxLength) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        String normalized = plainText.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "…";
    }

    public String summarize(String plainText) {
        return summarize(plainText, DEFAULT_SUMMARY_LENGTH);
    }
}
