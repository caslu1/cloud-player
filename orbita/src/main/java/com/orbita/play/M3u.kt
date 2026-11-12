package com.orbita.play

/** Leitura de listas M3U / M3U8 (sem dependências do Android, testável). */
object M3u {
    class Entry(val name: String, val attrs: Map<String, String>, val url: String) {
        val group get() = attrs["group-title"].orEmpty().ifBlank { "Sem categoria" }
        val logo get() = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
        val tvgId get() = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
        val chno get() = attrs["tvg-chno"]?.toIntOrNull()
    }

    class Result(
        val liveCats: List<Cat>, val channels: List<Channel>,
        val movieCats: List<Cat>, val movies: List<Movie>,
        val seriesCats: List<Cat>, val series: List<Series>,
        val episodes: Map<String, List<Episode>>,
    )

    private val attrRx = Regex("""([\w-]+)="([^"]*)"""")
    private val seRx = Regex("""(?i)^(.*?)[\s._-]*S(\d{1,2})\s*E(\d{1,4})\b(.*)$""")
    private val videoExt = Regex("""(?i)\.(mp4|mkv|avi|mov|wmv|m4v|flv|webm)(\?.*)?$""")

    fun parse(lines: Sequence<String>): List<Entry> {
        val out = ArrayList<Entry>()
        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        var pendingGroup: String? = null
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = findNameComma(line)
                    val head = if (comma >= 0) line.substring(0, comma) else line
                    pendingName = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    pendingAttrs = attrRx.findAll(head).associate { it.groupValues[1].lowercase() to it.groupValues[2].trim() }
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> pendingGroup = line.substringAfter(':').trim()
                line.startsWith("#") -> Unit
                else -> {
                    if (pendingName != null) {
                        var attrs = pendingAttrs
                        if (attrs["group-title"].isNullOrBlank() && !pendingGroup.isNullOrBlank()) attrs = attrs + ("group-title" to pendingGroup!!)
                        val name = pendingName!!.ifBlank { attrs["tvg-name"].orEmpty() }.ifBlank { line.substringAfterLast('/') }
                        out += Entry(name, attrs, line)
                    }
                    pendingName = null; pendingAttrs = emptyMap(); pendingGroup = null
                }
            }
        }
        return out
    }

    /** A vírgula que separa os atributos do nome (ignora vírgulas dentro de aspas). */
    private fun findNameComma(line: String): Int {
        var quoted = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') quoted = !quoted else if (c == ',' && !quoted) return i
        }
        return -1
    }

    fun classify(entries: List<Entry>): Result {
        val liveCats = LinkedHashMap<String, Cat>(); val movieCats = LinkedHashMap<String, Cat>(); val seriesCats = LinkedHashMap<String, Cat>()
        val channels = ArrayList<Channel>(); val movies = ArrayList<Movie>()
        val seriesMap = LinkedHashMap<String, Series>(); val eps = HashMap<String, ArrayList<Episode>>()
        var num = 0
        for ((i, e) in entries.withIndex()) {
            val url = e.url
            val lower = url.lowercase()
            val se = seRx.find(e.name)
            val group = e.group
            when {
                se != null && (lower.contains("/series/") || (videoExt.containsMatchIn(lower) && !lower.contains("/movie/"))) -> {
                    val sName = se.groupValues[1].trim().trimEnd('-', '|', ':').trim().ifBlank { e.name }
                    val sid = "m3u:$group:$sName"
                    seriesCats.getOrPut(group) { Cat(group, group) }
                    seriesMap.getOrPut(sid) { Series(sid, sName, e.logo, group, 0f, (entries.size - i).toLong(), null, null, null, null) }
                    val season = se.groupValues[2].toIntOrNull() ?: 1
                    val epNum = se.groupValues[3].toIntOrNull() ?: 0
                    val title = se.groupValues[4].trim().trimStart('-', '|', ':', '.').trim().ifBlank { "Episódio $epNum" }
                    eps.getOrPut(sid) { ArrayList() } += Episode("m3u:$i", season, epNum, title, url, null, e.logo, 0)
                }
                lower.contains("/series/") -> {
                    // episódio sem SxxEyy: vira série de um só episódio
                    val sid = "m3u:$group:${e.name}"
                    seriesCats.getOrPut(group) { Cat(group, group) }
                    seriesMap.getOrPut(sid) { Series(sid, e.name, e.logo, group, 0f, (entries.size - i).toLong(), null, null, null, null) }
                    eps.getOrPut(sid) { ArrayList() } += Episode("m3u:$i", 1, (eps[sid]?.size ?: 0) + 1, e.name, url, null, e.logo, 0)
                }
                lower.contains("/movie/") || (videoExt.containsMatchIn(lower) && !lower.contains("/live/")) -> {
                    movieCats.getOrPut(group) { Cat(group, group) }
                    movies += Movie("m3u:$i", e.name, e.logo, group, 0f, (entries.size - i).toLong(), url)
                }
                else -> {
                    liveCats.getOrPut(group) { Cat(group, group) }
                    num++
                    channels += Channel("m3u:$i", e.chno ?: num, e.name, e.logo, group, e.tvgId, url)
                }
            }
        }
        val episodes = eps.mapValues { (_, l) -> l.sortedWith(compareBy({ it.season }, { it.num })) }
        return Result(liveCats.values.toList(), channels, movieCats.values.toList(), movies, seriesCats.values.toList(), seriesMap.values.toList(), episodes)
    }

    /** Endereço do tipo get.php?username=…&password=… : dá para usar a API Xtream (mais rápida e com guia). */
    fun xtreamFromUrl(url: String): Triple<String, String, String>? {
        val u = url.trim()
        val q = u.substringAfter('?', "")
        if (q.isEmpty() || !u.contains("get.php", ignoreCase = true)) return null
        val params = q.split('&').mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].lowercase() to decode(it[1]) } }.toMap()
        val user = params["username"] ?: return null
        val pass = params["password"] ?: return null
        val server = u.substringBefore('?').substringBeforeLast('/')
        return Triple(server, user, pass)
    }

    private fun decode(s: String): String = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
}
