package com.orbita.play

data class Cat(val id: String, val name: String)

class Channel(
    val id: String,
    val num: Int,
    val name: String,
    val logo: String?,
    val catId: String,
    val epgId: String?,
    val url: String,
)

class Movie(
    val id: String,
    val name: String,
    val poster: String?,
    val catId: String,
    val rating: Float,
    val added: Long,
    val url: String,
    val tmdb: String? = null,
)

class Series(
    val id: String,
    val name: String,
    val cover: String?,
    val catId: String,
    val rating: Float,
    val added: Long,
    val plot: String?,
    val genre: String?,
    val year: String?,
    val backdrop: String?,
    val tmdb: String? = null,
)

class Episode(
    val id: String,
    val season: Int,
    val num: Int,
    val title: String,
    val url: String,
    val plot: String?,
    val img: String?,
    val durationSec: Int,
)

class MovieInfo(
    val plot: String?,
    val genre: String?,
    val year: String?,
    val durationSec: Int,
    val rating: Float,
    val backdrop: String?,
    val trailer: String?,
    val cast: String?,
    val age: String?,
)

class SeriesInfo(
    val plot: String?,
    val genre: String?,
    val year: String?,
    val rating: Float,
    val backdrop: String?,
    val cast: List<String>,
    val seasons: Map<Int, List<Episode>>,
)

/**
 * Arruma o guia do provedor: ordena, tira repetidos e corta programas sobrepostos
 * (ex.: "Mais Você 11:35–12:45" e "SP1 11:45" → Mais Você termina 11:45). Assim só um programa fica "agora".
 */
fun normalizeEpg(list: List<Prog>): List<Prog> {
    val sorted = list.filter { it.end > it.start && it.title.isNotBlank() }.sortedBy { it.start }.distinctBy { it.start }
    return sorted.mapIndexed { i, p ->
        val next = sorted.getOrNull(i + 1)
        if (next != null && next.start < p.end) p.copy(end = next.start) else p
    }.filter { it.end > it.start }
}

/** Um programa do guia (EPG). Horários em milissegundos UTC. */
data class Prog(val title: String, val desc: String, val start: Long, val end: Long) {
    fun isNow(now: Long = System.currentTimeMillis()) = now in start until end
    fun progress(now: Long = System.currentTimeMillis()): Float =
        if (end <= start) 0f else ((now - start).toFloat() / (end - start)).coerceIn(0f, 1f)
}

class Account(
    val type: String, // "xtream" ou "m3u"
    val server: String,
    val user: String,
    val pass: String,
    val m3uUrl: String,
)

class AccountStatus(val status: String?, val expires: Long?, val maxConnections: String?, val activeConnections: String?)

enum class Kind { LIVE, MOVIE, EPISODE }

/** O que o player toca. [list] permite trocar de canal (▲▼) ou ir para o próximo episódio. */
class PlayItem(
    val kind: Kind,
    val key: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val image: String?,
    val channel: Channel? = null,
    val series: Series? = null,
    val episode: Episode? = null,
)
