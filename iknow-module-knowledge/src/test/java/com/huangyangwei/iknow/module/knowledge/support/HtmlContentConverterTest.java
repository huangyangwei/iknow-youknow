package com.huangyangwei.iknow.module.knowledge.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 富文本双通道转换单测：净化、HTML→纯文本、Markdown→HTML、摘要。
 */
class HtmlContentConverterTest {

    private final HtmlContentConverter converter = new HtmlContentConverter();

    @Test
    void sanitizeStripsScriptAndEventHandlers() {
        String dirty = "<p onclick=\"alert(1)\">hello</p><script>alert('x')</script><img src=x onerror=alert(1)>";
        String clean = converter.sanitize(dirty);
        assertFalse(clean.contains("<script"), clean);
        assertFalse(clean.contains("onclick"), clean);
        assertFalse(clean.contains("onerror"), clean);
        assertTrue(clean.contains("hello"), clean);
    }

    @Test
    void toPlainTextExtractsTextAndStripsTags() {
        String html = "<h1>标题</h1><p>第一段 <b>加粗</b></p><ul><li>列表项</li></ul>";
        String plain = converter.toPlainText(html);
        assertEquals("标题 第一段 加粗 列表项", plain);
    }

    @Test
    void markdownToHtmlConvertsCommonMarkAndTable() {
        String markdown = "# 标题\n\n| A | B |\n|---|---|\n| 1 | 2 |\n";
        String html = converter.markdownToHtml(markdown);
        assertTrue(html.contains("<h1>标题</h1>"), html);
        assertTrue(html.contains("<table>"), html);
        assertTrue(html.contains("<th>A</th>"), html);
    }

    @Test
    void summarizeTruncatesLongTextAndReturnsNullForBlank() {
        String shortText = "短文本";
        assertEquals(shortText, converter.summarize(shortText, 200));

        String longText = "这是一个很长的文本，".repeat(50);
        String summary = converter.summarize(longText, 20);
        assertTrue(summary.length() <= 21, summary);
        assertTrue(summary.endsWith("…"), summary);

        assertEquals(null, converter.summarize("   "));
    }
}
