package com.orbita.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {
    private val sample = """
        #EXTM3U url-tvg="http://x/epg.xml"
        #EXTINF:-1 tvg-id="fut1" tvg-logo="http://l/f1.png" group-title="Futebol",Futebol 1 HD
        http://srv:8080/live/u/p/101.ts
        #EXTINF:-1 group-title="Filmes | Ação",Missão, Parte 2
        http://srv:8080/movie/u/p/555.mp4
        #EXTINF:-1 group-title="Séries | Drama",Plantão Noturno S02 E03 Quarto 214
        http://srv:8080/series/u/p/777.mkv
        #EXTINF:-1 group-title="Séries | Drama",Plantão Noturno S02E04
        http://srv:8080/series/u/p/778.mkv
        #EXTINF:-1,Canal sem grupo
        http://outro/stream
    """.trimIndent()

    @Test fun parsesAndClassifiesM3u() {
        val r = M3u.classify(M3u.parse(sample.lineSequence()))
        assertEquals(2, r.channels.size)
        assertEquals("Futebol 1 HD", r.channels[0].name)
        assertEquals("fut1", r.channels[0].epgId)
        assertEquals("http://l/f1.png", r.channels[0].logo)
        assertEquals(1, r.movies.size)
        assertEquals("Missão, Parte 2", r.movies[0].name)
        assertEquals(1, r.series.size)
        assertEquals("Plantão Noturno", r.series[0].name)
        val eps = r.episodes[r.series[0].id]!!
        assertEquals(2, eps.size)
        assertEquals(2, eps[0].season); assertEquals(3, eps[0].num); assertEquals("Quarto 214", eps[0].title)
        assertEquals("Sem categoria", r.liveCats[1].name)
    }

    @Test fun xtreamFromGetPhp() {
        val x = M3u.xtreamFromUrl("http://srv.tv:8080/get.php?username=joao&password=a%40b&type=m3u_plus")
        assertNotNull(x)
        assertEquals("http://srv.tv:8080", x!!.first); assertEquals("joao", x.second); assertEquals("a@b", x.third)
        assertNull(M3u.xtreamFromUrl("http://srv/lista.m3u"))
    }

    @Test fun initialsAndServer() {
        assertEquals("AS", Names.initials("Atlético Serrano"))
        assertEquals("RI", Names.initials("Ribeirense"))
        assertEquals("http://srv:8080", Net.normalizeServer(" srv:8080/ "))
        assertEquals("https://a.b", Net.normalizeServer("https://a.b/player_api.php"))
    }

    @Test fun groupsChannelVersionsByName() {
        assertEquals(Names.channelKey("Sportv 2"), Names.channelKey("SPORTV 2 FHD"))
        assertEquals(Names.channelKey("Premiere 1"), Names.channelKey("BR: Premiere 1 HD"))
        assertEquals(Names.channelKey("Disney+"), Names.channelKey("Disney+ HD"))
    }

    @Test fun cleansProviderTitlesForTmdb() {
        assertEquals("Duna: Parte Dois" to "2024", Tmdb.clean("PT | Duna: Parte Dois (2024) [4K] Dublado"))
        assertEquals("Oppenheimer" to null, Tmdb.clean("Oppenheimer"))
        assertEquals("Up: Altas Aventuras" to "2009", Tmdb.clean("Up: Altas Aventuras 2009 FHD"))
        assertEquals("The Last of Us" to null, Tmdb.clean("BR: The Last of Us [L]"))
    }

    @Test fun epgOverlapsLeaveOneProgrammeNow() {
        val m = 60_000L
        val raw = listOf(
            Prog("Mais Você", "", 0, 70 * m),        // 11:35–12:45 no provedor
            Prog("SP1", "", 10 * m, 70 * m),        // 11:45 começa antes do anterior terminar
            Prog("SP1", "", 70 * m, 85 * m),
            Prog("Mais Você", "", 0, 70 * m),        // repetido
        )
        val n = normalizeEpg(raw)
        assertEquals(listOf("Mais Você", "SP1", "SP1"), n.map { it.title })
        assertEquals(10 * m, n[0].end)
        assertEquals(1, n.count { it.isNow(15 * m) })
    }

    @Test fun oneLinePerChannelAcrossQualities() {
        fun ch(id: String, n: String) = Channel(id, id.toInt(), n, null, "1", null, "u")
        val list = listOf(
            ch("1", "Animal Planet FHD"), ch("2", "Animal Planet FHD²"), ch("3", "Animal Planet HD"), ch("4", "Animal Planet SD²"),
            ch("5", "H2 HD"), ch("6", "H2 FHD"), ch("7", "Globo SP SD"), ch("8", "Globo RJ FHD"),
        )
        assertEquals(listOf("Animal Planet FHD", "H2 FHD", "Globo SP SD", "Globo RJ FHD"), Quality.unique(list).map { it.name })
    }
}
