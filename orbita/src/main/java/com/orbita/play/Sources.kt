package com.orbita.play

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object Net {
    /** Muitos servidores IPTV só aceitam players conhecidos. */
    const val UA = "IPTVSmartersPlayer"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { ch -> ch.proceed(ch.request().newBuilder().header("User-Agent", UA).build()) }
            .build()
    }

    fun req(url: String) = Request.Builder().url(url).build()

    fun text(url: String): String =
        client.newCall(req(url)).execute().use { r ->
            if (!r.isSuccessful) throw IOException(httpMessage(r.code))
            r.body?.string().orEmpty()
        }

    fun httpMessage(code: Int) = when (code) {
        401, 403 -> "Acesso negado pelo servidor (código $code). Confira usuário, senha e assinatura."
        404 -> "Endereço não encontrado (404). Confira o servidor."
        in 500..599 -> "O servidor do provedor está com problemas ($code). Tente novamente."
        else -> "Erro do servidor ($code)."
    }

    fun normalizeServer(s: String): String {
        var u = s.trim().trimEnd('/')
        if (u.isEmpty()) return u
        if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "http://$u"
        u = u.removeSuffix("/player_api.php").removeSuffix("/get.php").removeSuffix("/c")
        return u.trimEnd('/')
    }
}

interface Source {
    fun status(): AccountStatus?
    fun loadLive(): Pair<List<Cat>, List<Channel>>
    fun loadMovies(): Pair<List<Cat>, List<Movie>>
    fun loadSeries(): Pair<List<Cat>, List<Series>>
    fun movieInfo(m: Movie): MovieInfo?
    fun seriesInfo(s: Series): SeriesInfo
    fun epg(c: Channel, limit: Int): List<Prog>
    /** Guia completo (vários dias) do canal. */
    fun epgFull(c: Channel): List<Prog> = emptyList()
}

class XtreamSource(private val a: Account, private val liveExt: () -> String) : Source {
    private val server = Net.normalizeServer(a.server)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun api(action: String? = null, extra: String = "") =
        "$server/player_api.php?username=${enc(a.user)}&password=${enc(a.pass)}" + (action?.let { "&action=$it" } ?: "") + extra

    fun verify(): AccountStatus {
        val body = Net.text(api())
        val o = runCatching { JSONObject(body) }.getOrNull()
            ?: throw IOException("O servidor não respondeu como um painel Xtream. Confira o endereço (ex.: http://servidor:8080).")
        val ui = o.optJSONObject("user_info") ?: throw IOException("Usuário ou senha incorretos.")
        val auth = ui.opt("auth")?.toString()
        if (auth == "0" || auth == "false") throw IOException("Usuário ou senha incorretos.")
        val st = ui.optString("status")
        if (st.isNotBlank() && !st.equals("Active", true)) throw IOException("Assinatura não está ativa (status: $st).")
        return statusOf(ui)
    }

    private fun statusOf(ui: JSONObject) = AccountStatus(
        ui.optString("status").ifBlank { null },
        ui.optString("exp_date").toLongOrNull()?.let { it * 1000 },
        ui.optString("max_connections").ifBlank { null },
        ui.optString("active_cons").ifBlank { null },
    )

    override fun status(): AccountStatus? = runCatching {
        JSONObject(Net.text(api())).optJSONObject("user_info")?.let { statusOf(it) }
    }.getOrNull()

    private fun cats(action: String): List<Cat> {
        val arr = runCatching { JSONArray(Net.text(api(action))) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.opt("category_id")?.toString()?.trim().orEmpty()
            val n = o.optString("category_name").trim()
            if (id.isEmpty() || n.isEmpty()) null else Cat(id, n)
        }
    }

    /** Lê listas grandes item por item (dezenas de milhares de filmes sem estourar a memória). */
    private fun each(url: String, f: (Map<String, String>) -> Unit) {
        Net.client.newCall(Net.req(url)).execute().use { r ->
            if (!r.isSuccessful) throw IOException(Net.httpMessage(r.code))
            val body = r.body ?: return
            val jr = JsonReader(body.charStream())
            jr.isLenient = true
            if (jr.peek() != JsonToken.BEGIN_ARRAY) return
            jr.beginArray()
            while (jr.hasNext()) {
                if (jr.peek() == JsonToken.BEGIN_OBJECT) f(flat(jr)) else jr.skipValue()
            }
            jr.endArray()
        }
    }

    private fun flat(r: JsonReader): Map<String, String> {
        val m = HashMap<String, String>(24)
        r.beginObject()
        while (r.hasNext()) {
            val n = r.nextName()
            when (r.peek()) {
                JsonToken.STRING, JsonToken.NUMBER -> m[n] = r.nextString()
                JsonToken.BOOLEAN -> m[n] = r.nextBoolean().toString()
                JsonToken.BEGIN_ARRAY -> {
                    r.beginArray()
                    var first: String? = null
                    while (r.hasNext()) {
                        if (first == null && (r.peek() == JsonToken.STRING || r.peek() == JsonToken.NUMBER)) first = r.nextString() else r.skipValue()
                    }
                    r.endArray()
                    if (first != null) m[n] = first
                }
                else -> r.skipValue()
            }
        }
        r.endObject()
        if (m["category_id"].isNullOrBlank()) m["category_ids"]?.let { m["category_id"] = it }
        return m
    }

    private fun rating(m: Map<String, String>) =
        (m["rating_5based"]?.toFloatOrNull()?.times(2) ?: m["rating"]?.replace(',', '.')?.toFloatOrNull() ?: 0f).coerceIn(0f, 10f)

    override fun loadLive(): Pair<List<Cat>, List<Channel>> {
        val cats = cats("get_live_categories")
        val out = ArrayList<Channel>()
        val ext = liveExt()
        each(api("get_live_streams")) { m ->
            val id = m["stream_id"] ?: return@each
            out += Channel(
                id, m["num"]?.toIntOrNull() ?: (out.size + 1), m["name"]?.trim().orEmpty(),
                m["stream_icon"]?.takeIf { it.startsWith("http") }, m["category_id"].orEmpty(),
                m["epg_channel_id"]?.takeIf { it.isNotBlank() }, "$server/live/${a.user}/${a.pass}/$id.$ext"
            )
        }
        return cats to out
    }

    override fun loadMovies(): Pair<List<Cat>, List<Movie>> {
        val cats = cats("get_vod_categories")
        val out = ArrayList<Movie>()
        each(api("get_vod_streams")) { m ->
            val id = m["stream_id"] ?: return@each
            val ext = m["container_extension"]?.takeIf { it.isNotBlank() } ?: "mp4"
            out += Movie(
                id, m["name"]?.trim().orEmpty(), m["stream_icon"]?.takeIf { it.startsWith("http") }, m["category_id"].orEmpty(),
                rating(m), m["added"]?.toLongOrNull() ?: 0L, "$server/movie/${a.user}/${a.pass}/$id.$ext",
                (m["tmdb"] ?: m["tmdb_id"])?.takeIf { it.isNotBlank() && it != "0" }
            )
        }
        return cats to out
    }

    override fun loadSeries(): Pair<List<Cat>, List<Series>> {
        val cats = cats("get_series_categories")
        val out = ArrayList<Series>()
        each(api("get_series")) { m ->
            val id = m["series_id"] ?: return@each
            out += Series(
                id, m["name"]?.trim().orEmpty(), m["cover"]?.takeIf { it.startsWith("http") }, m["category_id"].orEmpty(), rating(m),
                m["last_modified"]?.toLongOrNull() ?: 0L, m["plot"]?.takeIf { it.isNotBlank() }, m["genre"]?.takeIf { it.isNotBlank() },
                (m["releaseDate"] ?: m["release_date"] ?: m["year"])?.take(4)?.takeIf { it.isNotBlank() },
                m["backdrop_path"]?.takeIf { it.startsWith("http") },
                (m["tmdb"] ?: m["tmdb_id"])?.takeIf { it.isNotBlank() && it != "0" }
            )
        }
        return cats to out
    }

    override fun movieInfo(m: Movie): MovieInfo? = runCatching {
        val o = JSONObject(Net.text(api("get_vod_info", "&vod_id=${m.id}")))
        val i = o.optJSONObject("info") ?: JSONObject()
        val dur = i.optString("duration_secs").toIntOrNull()
            ?: i.optString("duration").split(":").mapNotNull { it.trim().toIntOrNull() }.let { p -> if (p.size == 3) p[0] * 3600 + p[1] * 60 + p[2] else 0 }
        MovieInfo(
            plot = i.optString("plot").ifBlank { i.optString("description") }.ifBlank { null },
            genre = i.optString("genre").ifBlank { null },
            year = i.optString("releasedate").ifBlank { i.optString("release_date") }.take(4).ifBlank { null },
            durationSec = dur,
            rating = i.optString("rating").replace(',', '.').toFloatOrNull() ?: m.rating,
            backdrop = firstUrl(i.opt("backdrop_path")) ?: i.optString("movie_image").takeIf { it.startsWith("http") },
            trailer = i.optString("youtube_trailer").ifBlank { null },
            cast = i.optString("cast").ifBlank { i.optString("actors") }.ifBlank { null },
            age = i.optString("age").ifBlank { i.optString("mpaa_rating") }.ifBlank { null },
        )
    }.getOrNull()

    private fun firstUrl(v: Any?): String? = when (v) {
        is JSONArray -> (0 until v.length()).map { v.optString(it) }.firstOrNull { it.startsWith("http") }
        is String -> v.takeIf { it.startsWith("http") }
        else -> null
    }

    override fun seriesInfo(s: Series): SeriesInfo {
        val o = JSONObject(Net.text(api("get_series_info", "&series_id=${s.id}")))
        val info = o.optJSONObject("info") ?: JSONObject()
        val seasons = sortedMapOf<Int, MutableList<Episode>>()
        fun addEp(e: JSONObject, seasonKey: Int) {
            val id = e.opt("id")?.toString() ?: return
            val ext = e.optString("container_extension").ifBlank { "mp4" }
            val ei = e.optJSONObject("info")
            val season = e.optString("season").toIntOrNull() ?: seasonKey
            val num = e.optString("episode_num").toIntOrNull() ?: ((seasons[season]?.size ?: 0) + 1)
            seasons.getOrPut(season) { ArrayList() } += Episode(
                id, season, num, e.optString("title").ifBlank { "Episódio $num" }, "$server/series/${a.user}/${a.pass}/$id.$ext",
                ei?.optString("plot")?.ifBlank { null }, ei?.optString("movie_image")?.takeIf { it.startsWith("http") },
                ei?.optString("duration_secs")?.toIntOrNull() ?: 0
            )
        }
        when (val eps = o.opt("episodes")) {
            is JSONObject -> eps.keys().forEach { k ->
                val arr = eps.optJSONArray(k) ?: return@forEach
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { addEp(it, k.toIntOrNull() ?: 1) }
            }
            is JSONArray -> for (i in 0 until eps.length()) {
                val arr = eps.optJSONArray(i) ?: continue
                for (j in 0 until arr.length()) arr.optJSONObject(j)?.let { addEp(it, i + 1) }
            }
        }
        // títulos tipo "Nome - S01E03 - Título": deixa só o título
        val clean = seasons.mapValues { (_, l) ->
            l.sortedBy { it.num }.map { e ->
                val t = Regex("""(?i)^.*?S\d{1,2}\s*E\d{1,4}\s*[-:|.]*\s*""").replace(e.title, "").ifBlank { "Episódio ${e.num}" }
                Episode(e.id, e.season, e.num, t, e.url, e.plot, e.img, e.durationSec)
            }
        }
        val cast = info.optString("cast").split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(8)
        return SeriesInfo(
            plot = info.optString("plot").ifBlank { s.plot.orEmpty() }.ifBlank { null },
            genre = info.optString("genre").ifBlank { s.genre.orEmpty() }.ifBlank { null },
            year = info.optString("releaseDate").ifBlank { info.optString("release_date") }.take(4).ifBlank { s.year },
            rating = info.optString("rating").replace(',', '.').toFloatOrNull() ?: s.rating,
            backdrop = firstUrl(info.opt("backdrop_path")) ?: s.backdrop,
            cast = cast, seasons = clean,
        )
    }

    override fun epgFull(c: Channel): List<Prog> = parseEpg(JSONObject(Net.text(api("get_simple_data_table", "&stream_id=${c.id}"))))

    override fun epg(c: Channel, limit: Int): List<Prog> = parseEpg(JSONObject(Net.text(api("get_short_epg", "&stream_id=${c.id}&limit=$limit"))))

    private fun parseEpg(o: JSONObject): List<Prog> {
        val arr = o.optJSONArray("epg_listings") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val e = arr.optJSONObject(i) ?: return@mapNotNull null
            val start = e.optString("start_timestamp").toLongOrNull()?.times(1000) ?: return@mapNotNull null
            val end = e.optString("stop_timestamp").toLongOrNull()?.times(1000) ?: return@mapNotNull null
            Prog(b64(e.optString("title")), b64(e.optString("description")), start, end)
        }.sortedBy { it.start }
    }

    private fun b64(s: String): String {
        if (s.isBlank()) return ""
        return runCatching { String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8) }.getOrDefault(s).trim()
    }
}

class M3uSource(private val url: String) : Source {
    private var result: M3u.Result? = null

    private fun get(): M3u.Result {
        result?.let { return it }
        val r = Net.client.newCall(Net.req(url.trim())).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException(Net.httpMessage(resp.code))
            val body = resp.body ?: throw IOException("Lista vazia.")
            M3u.classify(body.charStream().buffered().lineSequence().let { M3u.parse(it) })
        }
        if (r.channels.isEmpty() && r.movies.isEmpty() && r.series.isEmpty()) throw IOException("A lista M3U não tem nenhum item.")
        result = r
        return r
    }

    override fun status(): AccountStatus? = null
    override fun loadLive() = get().let { it.liveCats to it.channels }
    override fun loadMovies() = get().let { it.movieCats to it.movies }
    override fun loadSeries() = get().let { it.seriesCats to it.series }
    override fun movieInfo(m: Movie): MovieInfo? = null
    override fun seriesInfo(s: Series): SeriesInfo {
        val eps = get().episodes[s.id].orEmpty()
        return SeriesInfo(null, null, null, 0f, null, emptyList(), eps.groupBy { it.season }.toSortedMap())
    }
    override fun epg(c: Channel, limit: Int): List<Prog> = emptyList()
}
