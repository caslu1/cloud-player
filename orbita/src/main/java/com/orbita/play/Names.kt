package com.orbita.play

import java.text.Normalizer

/** Utilidades de texto para nomes (busca sem acento, iniciais, chave de canal). Sem dependências do Android, testável. */
object Names {
    /** Iniciais: "Grey's Anatomy" → "GA", "Dexter" → "DE". */
    fun initials(name: String): String {
        val words = norm(name).split(' ').filter { it.length > 2 || it.all(Char::isUpperCase) }.ifEmpty { listOf(name) }
        return if (words.size >= 2) "${words[0].first()}${words[1].first()}".uppercase()
        else words[0].take(2).uppercase()
    }

    fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "").replace(Regex("""\s+"""), " ").trim()

    private val quality = Regex("""(?i)\b(fhd|uhd|hd|sd|4k|fullhd|2160p|1080p|720p|480p|h\.?265|hevc|br|brasil|brazil|\[.*?\]|\(.*?\)|alt|backup|vip|24h)\b""")

    /** Nome do canal sem prefixo de país e sem qualidade: agrupa "Canal X FHD" e "BR: Canal X HD". */
    fun channelKey(name: String): String =
        norm(name).lowercase()
            .replace(Regex("""^\s*([a-z]{2}\s*[|:\-]\s*)"""), "")
            .replace(quality, " ").replace("+", "plus").replace(Regex("[^a-z0-9]"), "")
}
