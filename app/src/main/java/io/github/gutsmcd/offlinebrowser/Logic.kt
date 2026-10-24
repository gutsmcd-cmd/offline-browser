package io.github.gutsmcd.offlinebrowser

import java.net.URI
import java.net.URLEncoder

/** Pure helpers (unit-tested). */
object Logic {
    /** Address bar text -> URL. Anything that does not look like a web address becomes a search. */
    fun toUrl(input: String): String {
        val s = input.trim()
        if (s.isEmpty()) return "https://duckduckgo.com/"
        if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(s)) return s
        if (!s.contains(' ') && Regex("^[\\w-]+(\\.[\\w-]+)+(:\\d+)?(/.*)?$").matches(s)) return "https://$s"
        return "https://duckduckgo.com/?q=" + URLEncoder.encode(s, "UTF-8")
    }

    /** First http(s) link inside shared text (apps often share "Title https://…"). */
    fun extractUrl(text: String?): String? =
        text?.let { Regex("https?://[^\\s<>\"]+").find(it)?.value?.trimEnd('.', ',', ')', '」', '』') }

    fun hostOf(url: String): String = try { URI(url).host?.removePrefix("www.") ?: "" } catch (e: Exception) { "" }

    /** Links worth saving one level deep: same site, http(s), not this page, no files, unique, capped. */
    fun linksToSave(page: String, links: List<String>, max: Int = 15): List<String> {
        val host = hostOf(page)
        fun norm(u: String) = u.substringBefore('#').replaceFirst(Regex("^https?://(www\\.)?"), "").trimEnd('/')
        val base = norm(page)
        val skip = Regex("\\.(pdf|zip|jpe?g|png|gif|webp|mp4|mp3|apk|exe|dmg)(\\?.*)?$", RegexOption.IGNORE_CASE)
        val out = LinkedHashSet<String>()
        for (raw in links) {
            val u = raw.substringBefore('#')
            if (!u.startsWith("http://") && !u.startsWith("https://")) continue
            if (norm(u) == base || hostOf(u) != host || skip.containsMatchIn(u)) continue
            out.add(u)
            if (out.size >= max) break
        }
        return out.toList()
    }

    fun matches(title: String, url: String, text: String, q: String): Boolean {
        val k = q.trim().lowercase()
        if (k.isEmpty()) return true
        return title.lowercase().contains(k) || url.lowercase().contains(k) || text.lowercase().contains(k)
    }
}
