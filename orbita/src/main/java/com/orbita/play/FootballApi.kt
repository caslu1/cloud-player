package com.orbita.play

import android.util.JsonReader
import android.util.JsonToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.Reader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Agenda de jogos com placar ao vivo da API api.futebols.com.br.
 * TESTE: o token está fixo aqui a pedido do dono do app e deve ser removido depois (deixe TOKEN = "" para desligar;
 * sem token, a aba Futebol volta a procurar os jogos no guia/EPG dos canais).
 */
object FootballApi {
    var baseUrl = "https://api.futebols.com.br"
    var TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2Nlc3NLZXkiOiIzMDEwMTk4MDI1IiwiaWF0IjoxNzk0NDQ2NTUxLCJqdGkiOiI4MjY1ZmEwNC0zY2EzLTQ5Y2YtYjNiOC02MTNjNjk4NTNiY2YifQ.0IC4FAbXazczB6eiOoCYVNLE7y278ssxjGZ7hbYDzVw"

    val enabled get() = TOKEN.isNotBlank()

    class Game(
        val id: String, val home: String, val away: String, val golsA: Int?, val golsB: Int?,
        val penA: Int?, val penB: Int?, val logoA: String?, val logoB: String?,
        val status: String, val description: String, val minute: String?, val start: Long,
        val comp: String?, val compLogo: String?, val broadcasters: List<String>,
    )

    /** Jogos de um dia (data local, deslocamento em dias a partir de hoje). */
    fun day(offset: Int): List<Game> {
        val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, offset)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
        val body = """{"dataInicio":"$date","idJogo":null,"nomeTime":null}""".toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$baseUrl/api/buscarJogos").post(body).header("Authorization", "Bearer $TOKEN").build()
        Net.client.newCall(req).execute().use { r ->
            if (r.code == 401 || r.code == 403) throw IOException("Token da API de futebol inválido ou licença inativa.")
            if (!r.isSuccessful) throw IOException(Net.httpMessage(r.code))
            val games = parse(r.body?.charStream() ?: return emptyList())
            // a API pode devolver jogos de outros dias perto da virada (UTC): fica só com o dia pedido
            val start = Calendar.getInstance().apply { timeInMillis = c.timeInMillis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val inDay = games.filter { it.start in start until start + 86_400_000L }
            return inDay.distinctBy { it.id }.sortedBy { it.start }
        }
    }

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    fun parseTime(s: String): Long = runCatching { synchronized(iso) { iso.parse(s.take(19))!!.time } }.getOrDefault(0L)

    /** Lê a lista em fluxo, pulando os escudos em base64 (a resposta tem vários MB). */
    fun parse(reader: Reader): List<Game> {
        val out = ArrayList<Game>()
        val r = JsonReader(reader)
        r.isLenient = true
        if (r.peek() == JsonToken.BEGIN_OBJECT) {
            // formato {"data":[...]} ou erro {"message":...,"retorno":false}
            r.beginObject()
            var msg: String? = null
            while (r.hasNext()) {
                val n = r.nextName()
                if (r.peek() == JsonToken.BEGIN_ARRAY) readArray(r, out)
                else if (n == "message" && r.peek() == JsonToken.STRING) msg = r.nextString()
                else r.skipValue()
            }
            r.endObject()
            if (out.isEmpty() && msg != null) throw IOException(msg)
            return out
        }
        if (r.peek() == JsonToken.BEGIN_ARRAY) readArray(r, out)
        return out
    }

    private fun readArray(r: JsonReader, out: MutableList<Game>) {
        r.beginArray()
        while (r.hasNext()) {
            if (r.peek() == JsonToken.BEGIN_OBJECT) readGame(r)?.let { out += it } else r.skipValue()
        }
        r.endArray()
    }

    private fun str(r: JsonReader): String? = when (r.peek()) {
        JsonToken.STRING, JsonToken.NUMBER -> r.nextString()
        JsonToken.BOOLEAN -> r.nextBoolean().toString()
        else -> { r.skipValue(); null }
    }

    private fun readGame(r: JsonReader): Game? {
        val f = HashMap<String, String?>()
        var comp: String? = null; var compLogo: String? = null
        val casts = ArrayList<String>()
        var visible = true
        r.beginObject()
        while (r.hasNext()) {
            when (val n = r.nextName()) {
                "logoA", "logoB" -> r.skipValue()
                "visible" -> visible = str(r) != "false"
                "campeonato" -> if (r.peek() == JsonToken.BEGIN_OBJECT) {
                    r.beginObject()
                    while (r.hasNext()) when (r.nextName()) {
                        "campName" -> comp = str(r)
                        "logoCamp_url" -> compLogo = str(r)
                        else -> r.skipValue()
                    }
                    r.endObject()
                } else r.skipValue()
                "canais_simples", "canais", "canais_ia" -> if (r.peek() == JsonToken.BEGIN_ARRAY) {
                    r.beginArray()
                    while (r.hasNext()) {
                        if (r.peek() == JsonToken.BEGIN_OBJECT) {
                            r.beginObject()
                            while (r.hasNext()) {
                                val k = r.nextName()
                                if (k == "channel_name" || k == "nome" || k == "name") str(r)?.let { if (it.isNotBlank() && casts.none { c -> c.equals(it, true) }) casts += it.trim() }
                                else r.skipValue()
                            }
                            r.endObject()
                        } else if (r.peek() == JsonToken.STRING) {
                            val s = r.nextString(); if (s.isNotBlank() && casts.none { it.equals(s, true) }) casts += s.trim()
                        } else r.skipValue()
                    }
                    r.endArray()
                } else r.skipValue()
                else -> f[n] = str(r)
            }
        }
        r.endObject()
        if (!visible) return null
        val home = f["timeA"]?.takeIf { it.isNotBlank() } ?: return null
        val away = f["timeB"]?.takeIf { it.isNotBlank() } ?: return null
        return Game(
            f["id"] ?: "$home-$away", home, away, f["golsA"]?.toIntOrNull(), f["golsB"]?.toIntOrNull(),
            f["golsPenaltiA"]?.toIntOrNull(), f["golsPenaltiB"]?.toIntOrNull(),
            f["logoA_url"]?.takeIf { it.startsWith("http") }, f["logoB_url"]?.takeIf { it.startsWith("http") },
            f["status"].orEmpty().lowercase(), f["description"].orEmpty(), f["tempo_atual"]?.takeIf { it.isNotBlank() && it != "null" },
            parseTime(f["start"].orEmpty()), comp, compLogo?.takeIf { it.startsWith("http") }, casts
        )
    }

    // ---- status

    fun isLive(g: Game) = g.status in setOf("inprogress", "live", "in_progress", "playing", "started", "halftime", "pause", "paused") ||
        g.description.contains("half", true) || g.description.contains("tempo", true) || g.description.equals("Halftime", true)

    fun isOver(g: Game) = g.status in setOf("finished", "ended", "afterpenalties", "aet") || g.description in setOf("Ended", "AET", "AP")

    fun statusLabel(g: Game): String = when {
        g.status == "postponed" -> "Adiado"
        g.status == "canceled" || g.status == "cancelled" -> "Cancelado"
        isOver(g) -> when (g.description) { "AET" -> "Fim (prorr.)"; "AP" -> "Fim (pên.)"; else -> "Encerrado" }
        g.description.equals("Halftime", true) || g.status == "halftime" -> "Intervalo"
        isLive(g) -> "AO VIVO" + (g.minute?.let { " ${it.trimEnd('\'')}'" } ?: "")
        else -> hhmm(g.start)
    }

    // ---- canais: "Sportv 2" (API) ↔ "SPORTV 2 FHD" (lista do usuário)

    private val quality = Regex("""(?i)\b(fhd|uhd|hd|sd|4k|fullhd|2160p|1080p|720p|480p|h\.?265|hevc|br|brasil|brazil|\[.*?\]|\(.*?\)|alt|backup|vip|24h)\b""")

    fun channelKey(name: String): String {
        val n = MatchLogic.norm(name).lowercase()
            .replace(Regex("""^\s*([a-z]{2}\s*[|:\-]\s*)"""), "") // prefixo "BR: " / "BR | "
            .replace(quality, " ").replace("+", "plus").replace(Regex("[^a-z0-9]"), "")
        return n
    }

    /** Canais da lista que transmitem o jogo (pelos nomes de transmissão da API). */
    fun matchChannels(broadcasters: List<String>, index: Map<String, List<Channel>>): List<Channel> {
        val out = LinkedHashMap<String, Channel>()
        for (b in broadcasters) {
            val k = channelKey(b)
            if (k.length < 3) continue
            val hits = index[k] ?: index[k + "1"] ?: emptyList()
            hits.take(2).forEach { out[it.id] = it }
        }
        return out.values.toList()
    }
}
