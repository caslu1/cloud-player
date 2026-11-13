package com.orbita.play

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

/** Progresso salvo de filme/episódio ("Continuar assistindo"). */
class Progress(
    val key: String, val kind: Kind, val title: String, val subtitle: String, val image: String?,
    val pos: Long, val dur: Long, val time: Long, val seriesId: String?, val season: Int, val episode: Int, val url: String = "",
) {
    val fraction get() = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val finished get() = dur > 0 && pos > dur * 0.92
    val remainingMin get() = ((dur - pos).coerceAtLeast(0) / 60000).toInt()

    fun json() = JSONObject().put("k", key).put("t", kind.name).put("ti", title).put("st", subtitle).put("i", image ?: "")
        .put("p", pos).put("d", dur).put("tm", time).put("s", seriesId ?: "").put("se", season).put("ep", episode).put("u", url)

    companion object {
        fun of(o: JSONObject) = Progress(
            o.optString("k"), runCatching { Kind.valueOf(o.optString("t")) }.getOrDefault(Kind.MOVIE), o.optString("ti"), o.optString("st"),
            o.optString("i").ifBlank { null }, o.optLong("p"), o.optLong("d"), o.optLong("tm"), o.optString("s").ifBlank { null },
            o.optInt("se"), o.optInt("ep"), o.optString("u")
        )
    }
}

/** Dados salvos no aparelho. As listas são estados do Compose, então a tela atualiza sozinha. */
class Store(ctx: Context) {
    private val p: SharedPreferences = ctx.getSharedPreferences("orbita", Context.MODE_PRIVATE)

    val favChannels = mutableStateListOf<String>()
    val recentChannels = mutableStateListOf<String>()
    val myList = mutableStateListOf<String>() // "m:<id>" filmes, "s:<id>" séries
    val progress = mutableStateMapOf<String, Progress>()
    val liveFormat = mutableStateOf(p.getString("liveFormat", "ts") ?: "ts")
    val autoNext = mutableStateOf(p.getBoolean("autoNext", true))
    val previewAutoplay = mutableStateOf(p.getBoolean("previewAutoplay", false))
    /** Conteúdo adulto: "lock" (pede senha), "hide" (oculto) ou "show". */
    val adultMode = mutableStateOf(p.getString("adultMode", "lock") ?: "lock")
    /** Categorias ocultas: "live:<id>", "vod:<id>", "series:<id>". */
    val hiddenCats = mutableStateListOf<String>()
    val sortAZ = mutableStateOf(p.getBoolean("sortAZ", false))
    /** Layout da tela inicial: "default" (Padrão) ou "lite". */
    val homeLayout = mutableStateOf(p.getString("homeLayout", "default") ?: "default")

    /** Listas salvas no aparelho (tela Playlists). A que está em uso é a conta atual (acc.*). */
    val playlists = mutableStateListOf<Playlist>()

    init { reload(); Wall.index = p.getInt("wallpaper", 1) }

    fun reload() {
        favChannels.clear(); favChannels += list("fav")
        recentChannels.clear(); recentChannels += list("recent")
        myList.clear(); myList += list("myList")
        hiddenCats.clear(); hiddenCats += list("hiddenCats")
        playlists.clear()
        runCatching {
            val a = JSONArray(p.getString("playlists", "[]"))
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { Playlist.of(it) }?.let { playlists += it }
        }
        progress.clear()
        runCatching {
            val a = JSONArray(p.getString("progress", "[]"))
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { Progress.of(it) }?.let { progress[it.key] = it }
        }
    }

    private fun list(k: String): List<String> = runCatching {
        val a = JSONArray(p.getString(k, "[]")); (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())

    private fun save(k: String, l: List<String>) = p.edit().putString(k, JSONArray(l).toString()).apply()

    // Conta
    fun account(): Account? {
        val t = p.getString("acc.type", null) ?: return null
        return Account(t, p.getString("acc.server", "")!!, p.getString("acc.user", "")!!, p.getString("acc.pass", "")!!, p.getString("acc.m3u", "")!!)
    }

    fun saveAccount(a: Account) = p.edit().putString("acc.type", a.type).putString("acc.server", a.server).putString("acc.user", a.user)
        .putString("acc.pass", a.pass).putString("acc.m3u", a.m3uUrl).apply()

    private fun savePlaylists() = p.edit().putString("playlists", JSONArray(playlists.map { it.json() }).toString()).apply()

    /** Guarda (ou renomeia) a lista; sem nome usa o que já tinha ou "Lista N". */
    fun upsertPlaylist(name: String?, a: Account) {
        val i = playlists.indexOfFirst { it.same(a) }
        val n = name?.trim()?.takeIf { it.isNotEmpty() } ?: playlists.getOrNull(i)?.name ?: "Lista ${playlists.size + 1}"
        if (i >= 0) playlists[i] = Playlist(n, a) else playlists += Playlist(n, a)
        savePlaylists()
    }

    fun removePlaylist(pl: Playlist) { playlists.removeAll { it.same(pl.acc) }; savePlaylists() }

    fun logout() {
        // o MAC e a chave do aparelho continuam (são usados no site de ativação)
        val mac = p.getString("dmac", null); val key = p.getString("dkey", null)
        val seen = p.getBoolean("disclaimer", false)
        p.edit().clear().apply()
        p.edit().putString("dmac", mac).putString("dkey", key).putBoolean("disclaimer", seen).apply()
        reload()
        liveFormat.value = "ts"
    }

    // Canais
    fun isFav(id: String) = favChannels.contains(id)
    fun toggleFav(id: String) {
        if (!favChannels.remove(id)) favChannels.add(0, id)
        save("fav", favChannels.toList())
    }

    fun addRecent(id: String) {
        recentChannels.remove(id); recentChannels.add(0, id)
        while (recentChannels.size > 30) recentChannels.removeAt(recentChannels.size - 1)
        save("recent", recentChannels.toList())
        p.edit().putString("lastChannel", id).apply()
    }

    fun clearHistory() {
        recentChannels.clear(); save("recent", emptyList())
        progress.clear(); p.edit().putString("progress", "[]").apply()
    }

    // Minha lista
    fun inList(key: String) = myList.contains(key)
    fun toggleList(key: String) {
        if (!myList.remove(key)) myList.add(0, key)
        save("myList", myList.toList())
    }

    // Times seguidos

    // Progresso
    fun saveProgress(pr: Progress) {
        progress[pr.key] = pr
        val keep = progress.values.sortedByDescending { it.time }.take(200)
        p.edit().putString("progress", JSONArray(keep.map { it.json() }).toString()).apply()
    }

    fun continueWatching(kind: Kind? = null): List<Progress> {
        val all = progress.values.filter { !it.finished && it.pos > 30_000 && (kind == null || it.kind == kind) }.sortedByDescending { it.time }
        // uma entrada por série (o episódio mais recente)
        val seen = HashSet<String>()
        return all.filter { it.seriesId == null || seen.add(it.seriesId) }
    }

    fun lastEpisodeOf(seriesId: String): Progress? =
        progress.values.filter { it.seriesId == seriesId }.maxByOrNull { it.time }

    fun lastSiteList(): String? = p.getString("siteList", null)
    fun setLastSiteList(k: String) = p.edit().putString("siteList", k).apply()

    fun pin(): String = p.getString("pin", "0000") ?: "0000"
    fun setPin(v: String) = p.edit().putString("pin", v).apply()
    fun setAdultMode(m: String) { adultMode.value = m; p.edit().putString("adultMode", m).apply() }
    fun isHidden(key: String) = hiddenCats.contains(key)
    fun toggleHidden(key: String) {
        if (!hiddenCats.remove(key)) hiddenCats.add(key)
        save("hiddenCats", hiddenCats.toList())
    }
    fun setWallpaper(i: Int) { Wall.index = i; p.edit().putInt("wallpaper", i).apply() }
    /** Aviso importante: aparece só na primeira vez que o app é aberto. */
    val disclaimerSeen = mutableStateOf(p.getBoolean("disclaimer", false))
    fun acceptDisclaimer() { disclaimerSeen.value = true; p.edit().putBoolean("disclaimer", true).apply() }
    fun setHomeLayout(v: String) { homeLayout.value = v; p.edit().putString("homeLayout", v).apply() }
    fun setSortAZ(b: Boolean) { sortAZ.value = b; p.edit().putBoolean("sortAZ", b).apply() }

    fun setLiveFormat(f: String) { liveFormat.value = f; p.edit().putString("liveFormat", f).apply() }
    fun setAutoNext(b: Boolean) { autoNext.value = b; p.edit().putBoolean("autoNext", b).apply() }
    fun setPreviewAutoplay(b: Boolean) { previewAutoplay.value = b; p.edit().putBoolean("previewAutoplay", b).apply() }
}

class Playlist(val name: String, val acc: Account) {
    fun same(a: Account) = acc.type == a.type && acc.server == a.server && acc.user == a.user && acc.m3uUrl == a.m3uUrl
    fun json(): JSONObject = JSONObject().put("n", name).put("t", acc.type).put("s", acc.server).put("u", acc.user).put("p", acc.pass).put("m", acc.m3uUrl)
    companion object {
        fun of(o: JSONObject) = Playlist(o.optString("n"), Account(o.optString("t"), o.optString("s"), o.optString("u"), o.optString("p"), o.optString("m")))
    }
}
