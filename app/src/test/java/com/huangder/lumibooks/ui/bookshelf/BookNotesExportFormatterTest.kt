package com.huangder.lumibooks.ui.bookshelf

import com.huangder.lumibooks.domain.model.Bookmark
import com.huangder.lumibooks.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookNotesExportFormatterTest {
    @Test fun exportsTagsForAllFourMarkKinds() {
        val notes = listOf("highlight", "underline", "note").map {
            note("正文", if (it == "note") "笔记" else "", 0).copy(type = it, tags = listOf("人物", "复习"))
        }
        val bookmark = Bookmark(bookId = "book", chapterIndex = 0, position = 0f, title = "书签", createdAt = 0, tags = listOf("人物"))
        val text = BookNotesExportFormatter.format("书", notes, listOf(BookmarkExportItem(bookmark, "一", "正文")), emptyMap(), labels)
        assertEquals(4, Regex("【标签】").findAll(text).count())
        assertTrue(text.contains("【标签】人物、复习"))
    }

    @Test
    fun formatsHighlightsNotesAndBookmarksInSeparateSections() {
        val highlight = note(selectedText = "高亮原文", noteText = "", chapterIndex = 0)
        val note = note(selectedText = "笔记原文", noteText = "我的笔记", chapterIndex = 1)
        val bookmark = Bookmark(
            id = 7,
            bookId = "book",
            chapterIndex = 2,
            position = 3f,
            title = "第3章 第4页",
            remark = "回看这里",
            createdAt = 3L
        )

        val text = BookNotesExportFormatter.format(
            bookTitle = "测试书",
            notes = listOf(highlight, note),
            bookmarks = listOf(
                BookmarkExportItem(
                    bookmark = bookmark,
                    chapterTitle = "第三章",
                    pageText = "一二三四五六七八九十甲乙"
                )
            ),
            chapterTitles = mapOf(0 to "开篇", 1 to "转折"),
            labels = labels
        )

        assertTrue(text.contains("【高亮：】"))
        assertTrue(text.contains("【高亮内容】高亮原文"))
        assertTrue(text.contains("【所在章节】开篇"))
        assertTrue(text.contains("【笔记：】"))
        assertTrue(text.contains("【笔记原文内容】笔记原文"))
        assertTrue(text.contains("【用户所写笔记内容】我的笔记"))
        assertTrue(text.contains("【书签：】"))
        assertTrue(text.contains("【书签页内容】一二三四五…八九十甲乙"))
        assertTrue(text.contains("【备注】回看这里"))
        assertTrue(text.contains("【所在章节】第三章"))
        assertTrue(text.contains("【书籍名】测试书"))
    }

    @Test
    fun pageExcerptCountsUnicodeCodePointsAndUsesFirstParagraph() {
        val excerpt = BookNotesExportFormatter.pageExcerpt(
            pageText = "😀甲乙丙丁戊\n\n中间内容收尾甲乙丙丁戊",
            fallback = "fallback",
            pageUnavailable = labels.pageUnavailable
        )

        assertEquals("😀甲乙丙丁…甲乙丙丁戊", excerpt)
    }

    @Test
    fun pageExcerptFallsBackWhenPageTextIsUnavailable() {
        assertEquals(
            "第2章 第3页",
            BookNotesExportFormatter.pageExcerpt(null, "第2章 第3页", labels.pageUnavailable)
        )
    }

    @Test
    fun suggestedFileNameRemovesInvalidPathCharacters() {
        assertEquals(
            "书_名-书签与笔记.txt",
            BookNotesExportFormatter.suggestedFileName("书/名", labels.fileSuffix)
        )
    }


    @Test
    fun markdownExportPreservesChaptersNotesAndTagsAndEscapesMarkup() {
        val highlight = note("A *highlight*\nsecond line", "", 0).copy(tags = listOf("思考"))
        val memo = note("原句", "我的 [笔记]", 1)
        val bookmark = Bookmark(bookId = "book", chapterIndex = 2, position = 0f, title = "第二页", createdAt = 0)
        val output = BookNotesExportFormatter.formatMarkdown(
            "书 *题*",
            listOf(highlight, memo),
            listOf(BookmarkExportItem(bookmark, "尾声", null)),
            mapOf(0 to "开篇", 1 to "中段"),
            labels
        )
        assertTrue(output.startsWith("# 书 \\*题\\*"))
        assertTrue(output.contains("> A \\*highlight\\*\n> second line"))
        assertTrue(output.contains("**用户所写笔记内容**"))
        assertTrue(output.contains("我的 \\[笔记\\]"))
        assertTrue(output.contains("思考"))
        assertTrue(output.contains("尾声"))
        assertTrue(output.contains("第二页"))
        assertEquals("书_名-书签与笔记.md", BookNotesExportFormatter.suggestedFileName("书/名", labels.fileSuffix, "md"))
    }

    @Test
    fun markdownEscapesUntrustedAnnotationsWithoutDroppingUnicode() {
        val highlight = note("Emoji 😀 <tag>\nsecond line", "", 0)
            .copy(tags = listOf("Tag|One"))
        val output = BookNotesExportFormatter.formatMarkdown(
            "Title#One",
            listOf(highlight),
            emptyList(),
            emptyMap(),
            labels
        )
        assertTrue(output.contains("# Title\\#One"))
        assertTrue(output.contains("> Emoji 😀 \\<tag\\>"))
        assertTrue(output.contains("> second line"))
        assertTrue(output.contains("Tag\\|One"))
    }

    @Test
    fun markdownExportKeepsLiteralNumberedAndBulletLines() {
        val note = note("原文", "- 这不是列表\\n1. 也不是列表\\n+ 加号条目", 0)
        val output = BookNotesExportFormatter.formatMarkdown(
            "书", listOf(note), emptyList(), emptyMap(), labels
        )
        assertTrue(output.contains("\\- 这不是列表\\n1\\. 也不是列表\\n\\+ 加号条目"))
    }

    @Test
    fun markdownBlockquoteDoesNotCreateAccidentalNestedList() {
        val highlight = note("首句\\n- 这是引用中的普通原文\\n10) 普通行", "", 0)
        val output = BookNotesExportFormatter.formatMarkdown(
            "书", listOf(highlight), emptyList(), emptyMap(), labels
        )
        assertTrue(output.contains("> 首句\\n> \\- 这是引用中的普通原文\\n> 10\\) 普通行"))
    }

    private fun note(
        selectedText: String,
        noteText: String,
        chapterIndex: Int
    ) = Note(
        id = chapterIndex.toLong(),
        bookId = "book",
        chapterIndex = chapterIndex,
        startPosition = 0,
        endPosition = selectedText.length,
        selectedText = selectedText,
        note = noteText,
        color = "#FFEB3B",
        createdAt = chapterIndex.toLong()
    )

    private val labels = BookNotesExportLabels(
        highlightSection = "高亮",
        highlightContent = "高亮内容",
        chapter = "所在章节",
        book = "书籍名",
        noteSection = "笔记",
        noteSource = "笔记原文内容",
        userNote = "用户所写笔记内容",
        bookmarkSection = "书签",
        bookmarkPage = "书签页内容",
        bookmarkRemark = "备注",
        pageUnavailable = "（无法提取页面文字）",
        fileSuffix = "书签与笔记",
        chapterNumber = { "第${it}章" }
    )
}
