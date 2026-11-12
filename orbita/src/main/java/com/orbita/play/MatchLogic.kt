package com.orbita.play

import java.text.Normalizer

/** Encontra jogos de futebol nos títulos do guia (EPG). Sem dependências do Android, testável. */
object MatchLogic {
    data class Title(val home: String, val away: String, val comp: String?)

    private val sep = Regex("""\s+(?:x|X|×|vs\.?|VS\.?|Vs\.?)\s+""")
    private val skip = Regex("""(?i)reprise|melhores momentos|compacto|\bvt\b|highlights|resumo|pr[ée][- ]jogo|p[óo]s[- ]jogo|gols da|mesa redonda|debate|programa""")
    private val noise = Regex("""(?i)\((?:ao vivo|en vivo|live|hd|fhd|uhd|4k|ao-vivo|direto)\)|\b(?:ao vivo|en vivo|live|direto)\b""")
    private val segSplit = Regex("""\s+[-–—|]\s+|:\s+""")
    private val notTeam = Regex("""(?i)epis[óo]dio|temporada|cap[íi]tulo|filme|s[ée]rie\s+\d""")

    fun parseTitle(raw: String): Title? {
        if (raw.isBlank() || skip.containsMatchIn(raw)) return null
        val t = noise.replace(raw, " ").replace(Regex("""\s{2,}"""), " ").trim()
        val segments = t.split(segSplit).map { it.trim() }.filter { it.isNotEmpty() }
        val idx = segments.indexOfFirst { sep.containsMatchIn(it) }
        if (idx < 0) return null
        val parts = segments[idx].split(sep)
        if (parts.size != 2) return null
        val home = clean(parts[0]); val away = clean(parts[1])
        if (!okTeam(home) || !okTeam(away) || home.equals(away, true)) return null
        val comp = segments.filterIndexed { i, s -> i != idx && s.length in 3..40 && !sep.containsMatchIn(s) }.firstOrNull()
        return Title(home, away, comp)
    }

    private fun clean(s: String) = s.replace(Regex("""\(.*?\)|\[.*?\]"""), " ").replace(Regex("""\s{2,}"""), " ").trim().trim('-', '|', '.', ',', ' ')

    private fun okTeam(s: String): Boolean {
        if (s.length !in 2..32 || notTeam.containsMatchIn(s)) return false
        if (s.none { it.isLetter() }) return false
        if (s.split(' ').size > 5) return false
        if (s.first().isDigit()) return false
        return true
    }

    private val sportWords = Regex(
        """(?i)futebol|f[úu]tbol|soccer|football|esporte|sport|premiere|espn|sportv|tnt sports|band ?sports|combate|dazn|eleven|fox sports|bein|caz[ée]|goat|nosso futebol|paulist|brasileir|libertadores|champions|copa|ge tv|n sports|xsports|arena"""
    )

    fun isSportsName(s: String) = sportWords.containsMatchIn(s)

    /** Iniciais para o escudo: "Atlético Serrano" → "AS", "Ribeirense" → "RI". */
    fun initials(team: String): String {
        val words = norm(team).split(' ').filter { it.length > 2 || it.all(Char::isUpperCase) }.ifEmpty { listOf(team) }
        return if (words.size >= 2) "${words[0].first()}${words[1].first()}".uppercase()
        else words[0].take(2).uppercase()
    }

    fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "").replace(Regex("""\s+"""), " ").trim()

    fun key(home: String, away: String, start: Long) =
        "${norm(home).lowercase()}|${norm(away).lowercase()}|${start / (30 * 60_000)}"
}
