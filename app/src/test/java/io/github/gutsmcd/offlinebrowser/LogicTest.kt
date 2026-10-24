package io.github.gutsmcd.offlinebrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test fun urls() {
        assertEquals("https://nhk.or.jp", Logic.toUrl("nhk.or.jp"))
        assertEquals("http://a.b/c", Logic.toUrl("http://a.b/c"))
        assertTrue(Logic.toUrl("大阪 天気").startsWith("https://duckduckgo.com/?q="))
    }
    @Test fun share() {
        assertEquals("https://x.jp/a", Logic.extractUrl("記事「タイトル」 https://x.jp/a"))
    }
    @Test fun links() {
        val l = Logic.linksToSave("https://www.x.jp/a", listOf("https://x.jp/b", "https://x.jp/a#top", "https://y.jp/c", "https://x.jp/f.pdf", "https://x.jp/b", "mailto:q"))
        assertEquals(listOf("https://x.jp/b"), l)
    }
}
