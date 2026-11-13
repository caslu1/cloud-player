package com.orbita.play

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Reconhece filmes e séries no TMDB: logo do título, fundo, pôster, sinopse em português, gêneros, duração, nota e trailer.
 * Os resultados ficam guardados no aparelho (arquivo tmdb.json), então cada título só é procurado uma vez.
 */
object Tmdb {
    var KEY = "310e51c0c1fd642c4547793ef75a82da"
    var baseUrl = "https://api.themoviedb.org/3"
    const val IMG = "https://image.tmdb.org/t/p/"

    val enabled get() = KEY.isNotBlank()

    class Meta(
        val id: Int, val tv: Boolean, val title: String, val overview: String?, val year: String?, val genres: String?,
        val runtimeMin: Int, val rating: Float, val backdrop: String?, val poster: String?, val logo: String?, val trailer: String?,
        val seasons: Int,
    ) {
        fun backdropUrl(size: String = "w780") = backdrop?.let { IMG + size + it }
        fun posterUrl(size: String = "w342") = poster?.let { IMG + size + it }
        fun logoUrl(size: String = "w500") = logo?.let { IMG + size + it }

        fun json() = JSONObject().put("id", id).put("tv", tv).put("t", title).put("o", overview ?: "").put("y", year ?: "")
            .put("g", genres ?: "").put("r", runtimeMin).put("v", rating.toDouble()).put("b", backdrop ?: "").put("p", poster ?: "")
            .put("l", logo ?: "").put("tr", trailer ?: "").put("s", seasons)

        companion object {
            fun of(o: JSONObject) = Meta(
                o.optInt("id"), o.optBoolean("tv"), o.optString("t"), o.optString("o").ifBlank { null }, o.optString("y").ifBlank { null },
                o.optString("g").ifBlank { null }, o.optInt("r"), o.optDouble("v", 0.0).toFloat(), o.optString("b").ifBlank { null },
                o.optString("p").ifBlank { null }, o.optString("l").ifBlank { null }, o.optString("tr").ifBlank { null }, o.optInt("s")
            )
        }
    }

    /** Episódio da temporada no TMDB (imagem e sinopse quando o provedor não tem). */
    class Ep(val name: String?, val overview: String?, val still: String?, val runtimeMin: Int)
    class Person(val name: String, val character: String?, val photo: String?)

    private val castCache = ConcurrentHashMap<String, List<Person>>()

    /** Elenco com fotos (séries usam os créditos de todas as temporadas). */
    fun cast(id: Int, tv: Boolean): List<Person> {
        val k = "$tv/$id"
        castCache[k]?.let { return it }
        val o = get(if (tv) "/tv/$id/aggregate_credits?language=pt-BR" else "/movie/$id/credits?language=pt-BR") ?: return emptyList()
        val a = o.optJSONArray("cast") ?: return emptyList()
        val out = (0 until minOf(a.length(), 20)).mapNotNull { a.optJSONObject(it) }.map { p ->
            val role = p.optJSONArray("roles")?.optJSONObject(0)?.optString("character") ?: p.optString("character")
            Person(p.optString("name"), role?.ifBlank { null }, p.optString("profile_path").ifBlank { null }?.let { IMG + "w185" + it })
        }.filter { it.name.isNotBlank() }.sortedBy { if (it.photo == null) 1 else 0 }.take(10)
        castCache[k] = out
        return out
    }

    // cache: chave → resultado ("" = não encontrado)
    private val cache = ConcurrentHashMap<String, String>()
    private var file: File? = null
    @Volatile private var dirty = false

    fun init(ctx: Context) {
        val f = File(ctx.filesDir, "tmdb.json"); file = f
        runCatching {
            if (f.exists()) {
                val o = JSONObject(f.readText())
                o.keys().forEach { k -> cache[k] = o.optString(k) }
            }
        }
    }

    @Synchronized
    fun flush() {
        if (!dirty) return
        val f = file ?: return
        runCatching {
            val o = JSONObject()
            cache.entries.toList().takeLast(3000).forEach { (k, v) -> o.put(k, v) }
            f.writeText(o.toString())
            dirty = false
        }
    }

    // ---- nome do provedor → título e ano para a busca

    private val yearRx = Regex("""[(\[]?\b(19[3-9]\d|20[0-4]\d)\b[)\]]?""")
    private val junk = Regex(
        """(?i)\b(dublado|dub|legendado|leg|dual(\s?audio)?|nacional|4k|uhd|fhd|hd|sd|1080p|720p|480p|2160p|hdr|h\.?265|hevc|x264|x265|web-?dl|bluray|cam|ts|remux|imax|completo|completa|temporada\s*\d+|t\d+|s\d{1,2})\b"""
    )
    private val prefix = Regex("""^\s*([A-Z]{2,3}\s*[|:\-]\s*)+""")

    fun clean(raw: String): Pair<String, String?> {
        var s = raw
        val year = yearRx.findAll(s).lastOrNull()?.groupValues?.get(1)
        s = prefix.replace(s, "")
        s = s.replace(Regex("""\[.*?\]|\{.*?\}"""), " ")
        s = yearRx.replace(s, " ")
        s = s.replace(Regex("""\(.*?\)"""), " ")
        s = junk.replace(s, " ")
        s = s.replace(Regex("""[_.]+"""), " ").replace(Regex("""\s*[-|:]\s*$"""), "").replace(Regex("""\s{2,}"""), " ").trim()
        return (s.ifBlank { raw.trim() }) to year
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(path: String): JSONObject? = runCatching {
        val sep = if (path.contains('?')) "&" else "?"
        JSONObject(Net.text("$baseUrl$path${sep}api_key=$KEY"))
    }.getOrNull()

    /** Busca (ou devolve do cache) os dados de um filme/série. [hintId] = id TMDB informado pelo provedor, se houver. */
    fun find(name: String, tv: Boolean, hintId: String? = null): Meta? {
        if (!enabled) return null
        val (title, year) = clean(name)
        val key = (if (tv) "t2:" else "m2:") + Names.norm(title).lowercase() + "|" + (year ?: "")
        cache[key]?.let { return if (it.isEmpty()) null else runCatching { Meta.of(JSONObject(it)) }.getOrNull() }

        var id = hintId?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        if (id == null) id = search(title, year, tv) ?: if (year != null) search(title, null, tv) else null
        val meta = id?.let { details(it, tv) }
        cache[key] = meta?.json()?.toString() ?: ""
        dirty = true
        return meta
    }

    private fun search(title: String, year: String?, tv: Boolean): Int? {
        val path = if (tv) "/search/tv?language=pt-BR&query=${enc(title)}" + (year?.let { "&first_air_date_year=$it" } ?: "")
        else "/search/movie?language=pt-BR&query=${enc(title)}" + (year?.let { "&year=$it" } ?: "")
        val results = get(path)?.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        // pontua: mesmo título + mesmo ano > mesmo ano > mesmo título > ordem do TMDB (relevância)
        val norm = Names.norm(title).lowercase()
        var best: JSONObject? = null
        var bestScore = -1
        for (i in 0 until minOf(results.length(), 8)) {
            val r = results.optJSONObject(i) ?: continue
            val names = listOf("title", "name", "original_title", "original_name").map { Names.norm(r.optString(it)).lowercase() }
            val y = (r.optString("release_date").ifBlank { r.optString("first_air_date") }).take(4).toIntOrNull()
            val sameTitle = names.any { it == norm }
            val sameYear = year != null && y != null && kotlin.math.abs(y - year.toInt()) <= 1
            val score = (if (sameTitle) 4 else 0) + (if (sameYear) 3 else 0) + (if (names.any { it.contains(norm) || norm.contains(it) }) 1 else 0) - i / 3
            if (score > bestScore) { best = r; bestScore = score }
        }
        // com ano informado e nada parecido, melhor não mostrar dados de outro filme
        if (year != null && bestScore < 3) return null
        return best?.optInt("id")?.takeIf { it > 0 }
    }

    private fun details(id: Int, tv: Boolean): Meta? {
        val o = get("/${if (tv) "tv" else "movie"}/$id?language=pt-BR&append_to_response=images,videos&include_image_language=pt,en,null") ?: return null
        val genres = o.optJSONArray("genres")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("name") }.take(3).joinToString(" · ") }
        val runtime = if (tv) o.optJSONArray("episode_run_time")?.let { if (it.length() > 0) it.optInt(0) else 0 } ?: 0 else o.optInt("runtime")
        val date = if (tv) o.optString("first_air_date") else o.optString("release_date")
        var overview = o.optString("overview").ifBlank { null }
        if (overview == null) overview = get("/${if (tv) "tv" else "movie"}/$id?language=en-US")?.optString("overview")?.ifBlank { null }
        return Meta(
            id, tv, o.optString(if (tv) "name" else "title"), overview, date.take(4).ifBlank { null }, genres?.ifBlank { null }, runtime,
            o.optDouble("vote_average", 0.0).toFloat(), o.optString("backdrop_path").ifBlank { null }, o.optString("poster_path").ifBlank { null },
            bestLogo(o.optJSONObject("images")?.optJSONArray("logos")), trailer(o.optJSONObject("videos")?.optJSONArray("results")),
            o.optInt("number_of_seasons")
        )
    }

    private fun bestLogo(a: JSONArray?): String? {
        if (a == null) return null
        val logos = (0 until a.length()).mapNotNull { a.optJSONObject(it) }.filter { !it.optString("file_path").endsWith(".svg") }
        fun pick(lang: String?) = logos.filter { (it.optString("iso_639_1").ifBlank { null }) == lang }.maxByOrNull { it.optDouble("vote_average") }
        return (pick("pt") ?: pick("en") ?: pick(null) ?: logos.firstOrNull())?.optString("file_path")?.ifBlank { null }
    }

    private fun trailer(a: JSONArray?): String? {
        if (a == null) return null
        val vids = (0 until a.length()).mapNotNull { a.optJSONObject(it) }.filter { it.optString("site") == "YouTube" }
        return (vids.firstOrNull { it.optString("type") == "Trailer" } ?: vids.firstOrNull())?.optString("key")
    }

    private val seasonCache = ConcurrentHashMap<String, Map<Int, Ep>>()

    fun season(tvId: Int, n: Int): Map<Int, Ep> {
        val k = "$tvId/$n"
        seasonCache[k]?.let { return it }
        val eps = get("/tv/$tvId/season/$n?language=pt-BR")?.optJSONArray("episodes")
        val out = HashMap<Int, Ep>()
        if (eps != null) for (i in 0 until eps.length()) {
            val e = eps.optJSONObject(i) ?: continue
            out[e.optInt("episode_number")] = Ep(
                e.optString("name").ifBlank { null }, e.optString("overview").ifBlank { null },
                e.optString("still_path").ifBlank { null }?.let { IMG + "w300" + it }, e.optInt("runtime")
            )
        }
        seasonCache[k] = out
        return out
    }
}
