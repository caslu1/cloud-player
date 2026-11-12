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

    @Test fun findsMatches() {
        val t = MatchLogic.parseTitle("Campeonato Brasileiro: Flamengo x Palmeiras (Ao Vivo)")!!
        assertEquals("Flamengo", t.home); assertEquals("Palmeiras", t.away); assertEquals("Campeonato Brasileiro", t.comp)
        val t2 = MatchLogic.parseTitle("Real Madrid vs Barcelona - LaLiga")!!
        assertEquals("Real Madrid", t2.home); assertEquals("Barcelona", t2.away); assertEquals("LaLiga", t2.comp)
        assertNull(MatchLogic.parseTitle("Reprise: Flamengo x Palmeiras"))
        assertNull(MatchLogic.parseTitle("Jornal da Noite"))
        assertNull(MatchLogic.parseTitle("Mesa Redonda"))
    }

    @Test fun initialsAndServer() {
        assertEquals("AS", MatchLogic.initials("Atlético Serrano"))
        assertEquals("RI", MatchLogic.initials("Ribeirense"))
        assertEquals("http://srv:8080", Net.normalizeServer(" srv:8080/ "))
        assertEquals("https://a.b", Net.normalizeServer("https://a.b/player_api.php"))
    }
}
