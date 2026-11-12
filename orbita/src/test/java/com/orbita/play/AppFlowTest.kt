package com.orbita.play

import android.os.Looper
import android.util.Base64
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToString
import java.io.File
import java.time.Duration

/**
 * Abre o app num Android simulado, entra num servidor Xtream falso e passa por todas as abas.
 * Pega erros que fazem o app fechar (o teste falha com a exceção).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val server = MockWebServer()

    private val cats = listOf("Futebol", "Esportes", "Abertos", "Notícias", "Filmes 24h", "Séries 24h", "Infantil", "Documentários", "Música")
    private val channels = listOf("Futebol 1 HD", "Futebol 2 FHD", "Arena Esporte HD", "Gol TV FHD", "Campo 4K", "Placar HD", "Torcida SD", "Futebol+ HD", "Bola Rolando", "Esporte Total 1")
    // nomes reais: o teste no GitHub busca logo, fundo e sinopse no TMDB de verdade (para as fotos)
    private val movies = listOf("Duna: Parte Dois (2024)", "Oppenheimer", "Divertida Mente 2", "Deadpool & Wolverine", "Gladiador II", "Wicked", "Ainda Estou Aqui", "Moana 2", "Interestelar")
    private val series = listOf("The Last of Us", "Round 6", "Stranger Things", "The Bear", "Arcane", "Sintonia")
    private val matches = listOf(
        "Série A: Atlético Serrano x Ribeirense", "Série A: União Vale x Porto Alegre", "Série B: Real Cerrado x Operário Sul",
        "Copa Continental: Náutico Mar x Estrela do Norte", "Copa Continental: Vila Nova Paz x Cruzeiro do Leste"
    )

    /** Espera rede/imagens (TMDB) com o relógio andando. */
    private fun settle(ms: Long) { val end = System.currentTimeMillis() + ms; while (System.currentTimeMillis() < end) { pump(); Thread.sleep(40) } }

    private fun shot(name: String) {
        settle(2500)
        val dir = File(System.getProperty("user.dir"), "build/screens").apply { mkdirs() }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Resposta no formato da API api.futebols.com.br (escudo em base64 incluído, como na API real). */
    private fun apiGames(): String {
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        fun t(minFromNow: Int) = iso.format(java.util.Date(System.currentTimeMillis() + minFromNow * 60_000L))
        fun g(id: Int, a: String, b: String, ga: Int, gb: Int, st: String, desc: String, min: String?, start: String, camp: String, chs: List<String>) = """
            {"id":$id,"timeA":"$a","timeB":"$b","golsA":$ga,"golsB":$gb,"golsPenaltiA":null,"golsPenaltiB":null,
             "logoA":"data:image/png;base64,AAAA","logoB":"data:image/png;base64,AAAA","logoA_url":null,"logoB_url":null,
             "description":"$desc","status":"$st","start":"$start","tempo_atual":${min?.let { "\"$it\"" } ?: "null"},"visible":true,
             "campeonato":{"id":1,"campName":"$camp","logoCamp":"data:image/png;base64,AAAA"},
             "canais":[],"canais_ia":[],"canais_simples":[${chs.joinToString(",") { "{\"channel_id\":1,\"channel_name\":\"$it\"}" }}]}"""
        return listOf(
            g(1, "Atlético Serrano", "Ribeirense", 2, 1, "inprogress", "2nd half", "67", t(-70), "Série A", listOf("Futebol 2", "Premiere")),
            g(2, "União Vale", "Porto Alegre", 0, 0, "inprogress", "1st half", "23", t(-25), "Série A", listOf("Futebol 1")),
            g(3, "Real Cerrado", "Operário Sul", 0, 0, "notstarted", "Not started", null, t(90), "Série B", listOf("Gol TV")),
            g(4, "Náutico Mar", "Estrela do Norte", 0, 0, "notstarted", "Not started", null, t(120), "Copa Continental", listOf("Campo 4K")),
            g(5, "Vila Nova Paz", "Cruzeiro do Leste", 0, 0, "notstarted", "Not started", null, t(120), "Copa Continental", listOf("Canal Pago XYZ")),
            g(6, "Porto Alegre", "Vila Nova Paz", 3, 2, "finished", "Ended", null, t(-300), "Estadual", listOf("Placar")),
        ).joinToString(",", "[", "]")
    }

    private fun b64(s: String) = Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)

    @Before fun setUp() {
        val now = System.currentTimeMillis() / 1000
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (path.startsWith("/api/buscarJogos")) return MockResponse().setBody(apiGames()).setHeader("Content-Type", "application/json")
                if (!path.startsWith("/player_api.php")) return MockResponse().setResponseCode(404)
                val action = Regex("action=([a-z_]+)").find(path)?.groupValues?.get(1)
                val body = when (action) {
                    null -> """{"user_info":{"auth":1,"status":"Active","exp_date":"1893456000","max_connections":"2","active_cons":"0"},"server_info":{}}"""
                    "get_live_categories" -> cats.withIndex().joinToString(",", "[", "]") { (i, c) -> """{"category_id":"${i + 1}","category_name":"$c"}""" }
                    "get_live_streams" -> (channels.withIndex().joinToString(",", "[", "") { (i, c) ->
                        """{"num":${101 + i},"name":"$c","stream_id":${101 + i},"stream_icon":"","category_id":"1"}"""
                    } + """,{"num":101,"name":"Futebol 1 HD","stream_id":101,"category_id":"1"},
                        {"num":201,"name":"Jornal 24h","stream_id":201,"category_id":"4"}]""")
                    "get_vod_categories" -> """[{"category_id":"10","category_name":"Drama"}]"""
                    "get_vod_streams" -> movies.withIndex().joinToString(",", "[", "]") { (i, m) ->
                        """{"stream_id":${5 + i},"name":"$m","stream_icon":"","category_id":"10","rating":"${8.1 - i * 0.2}","added":"${1700000000 - i}","container_extension":"mp4"}"""
                    }
                    "get_vod_info" -> """{"info":{"plot":"Uma engenheira volta para casa. Depois de vinte anos longe, ela tenta impedir que a represa da família seja vendida e descobre que o passado não ficou enterrado.","genre":"Drama · Faroeste","releasedate":"2026-01-01","duration_secs":"7680","rating":"8.1","age":"16"}}"""
                    "get_series_categories" -> """[{"category_id":"20","category_name":"Médicos"}]"""
                    "get_series" -> series.withIndex().joinToString(",", "[", "]") { (i, n) ->
                        """{"series_id":${7 + i},"name":"$n","cover":"","category_id":"20","rating":"8.6","genre":"Drama · Médico","releaseDate":"2024","plot":"Entre turnos de doze horas, médicos residentes de um hospital público tentam salvar vidas sem perder a própria.","last_modified":"${1700000000 - i}"}"""
                    }
                    "get_series_info" -> """{"info":{"plot":"Residentes.","cast":"Ana Lima, Rafael Souza"},"episodes":{"1":[
                        {"id":"71","episode_num":1,"title":"O Plantão","container_extension":"mp4","info":{"duration_secs":"2880"}},
                        {"id":"72","episode_num":2,"title":"Sirene","container_extension":"mp4","info":{"duration_secs":"3060","plot":"Uma ambulância desaparece no meio do trajeto até o hospital."}},
                        {"id":"73","episode_num":3,"title":"Quarto 214","container_extension":"mp4","info":{"duration_secs":"2820","plot":"Uma paciente misteriosa se recusa a dizer o próprio nome."}}],
                        "2":[{"id":"81","episode_num":1,"title":"Sem Sinal","container_extension":"mp4","info":{"duration_secs":"3120"}}]}}"""
                    "get_short_epg" -> {
                        val id = (Regex("stream_id=(\\d+)").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 101) - 101
                        val m = matches[id.mod(matches.size)]
                        val h = if (id % 3 == 2) 7200 else 0
                        """{"epg_listings":[
                        {"title":"${b64("Esporte em Debate")}","description":"","start_timestamp":"${now - 7200 + h}","stop_timestamp":"${now - 1200 + h}"},
                        {"title":"${b64(m)}","description":"","start_timestamp":"${now - 1200 + h}","stop_timestamp":"${now + 5400 + h}"},
                        {"title":"${b64("Gols da Rodada")}","description":"","start_timestamp":"${now + 5400 + h}","stop_timestamp":"${now + 8100 + h}"},
                        {"title":"${b64("Resenha Esportiva")}","description":"","start_timestamp":"${now + 8100 + h}","stop_timestamp":"${now + 10800 + h}"},
                        {"title":"${b64("Reprise: Clássico da Semana")}","description":"","start_timestamp":"${now + 10800 + h}","stop_timestamp":"${now + 14400 + h}"}]}"""
                    }
                    else -> "[]"
                }
                return MockResponse().setBody(body).setHeader("Content-Type", "application/json")
            }
        }
        server.start()
        FootballApi.baseUrl = server.url("/").toString().trimEnd('/')
    }

    @After fun tearDown() { server.shutdown() }

    private fun pump(ms: Long = 100) {
        rule.mainClock.advanceTimeBy(ms)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        Thread.sleep(15)
    }

    private fun has(text: String) = rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitText(text: String, timeoutMs: Long = 20_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            pump()
            if (has(text)) return
        }
        runCatching { shot("falha-" + text.filter { it.isLetterOrDigit() }) }
        val tree = runCatching { rule.onRoot(useUnmergedTree = true).printToString(maxDepth = 60) }.getOrDefault("")
        throw AssertionError("Texto não apareceu: $text\n$tree".take(6000))
    }

    private fun click(text: String, exact: Boolean = false) {
        rule.onAllNodesWithText(text, substring = !exact)[0].performClick()
        repeat(5) { pump() }
    }

    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    private fun pressLeft() {
        rule.onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionLeft) }
        repeat(5) { pump() }
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        repeat(5) { pump() }
    }

    @Test fun loginAndBrowseEverything() {
        rule.mainClock.autoAdvance = false
        waitText("Conectar lista")
        shot("01-login")

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput(server.url("/").toString())
        fields[1].performTextInput("user")
        fields[2].performTextInput("pass")
        click("Entrar")

        // Canais
        waitText("Futebol 1 HD")
        waitText("Atlético Serrano x Ribeirense")
        waitText("Gol TV")
        shot("02-canais")
        click("Gol TV FHD") // toca na prévia
        repeat(10) { pump() }

        // Filmes
        click("Filmes", exact = true)
        waitText("Duna")
        waitText("Lançamentos")
        shot("03-filmes")
        rule.onAllNodesWithText("Oppenheimer")[0].performClick()
        waitText("FILME")
        shot("04-filme-detalhe")
        back()

        // Séries
        click("Séries", exact = true)
        waitText("The Last of Us")
        shot("05-series")
        rule.onAllNodesWithText("The Last of Us").let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        waitText("O Plantão")
        waitText("Sirene")
        shot("06-serie-episodios")
        back()

        // Futebol (jogos vindos do EPG)
        click("Futebol", exact = true)
        waitText("Atlético Serrano")
        waitText("Ribeirense")
        waitText("AO VIVO 67'")
        waitText("2 × 1")
        rule.onAllNodesWithText("Ribeirense", substring = true)[0].performClick() // seleciona / prévia
        repeat(10) { pump() }
        shot("07-futebol")
        rule.onAllNodesWithText("Ribeirense", substring = true)[0].performClick() // segundo OK: tela cheia
        repeat(10) { pump() }
        shot("10-player")
        settle(6000) // as informações somem sozinhas
        pressLeft() // abre a lista com categorias
        waitText("CATEGORIAS")
        shot("11-lista-player")
        back() // fecha a lista
        back() // sai da tela cheia

        // Buscar, Biblioteca, Configurações
        click("Buscar", exact = true)
        waitText("O que você quer assistir?")
        click("Biblioteca", exact = true)
        waitText("Canais favoritos")
        shot("08-biblioteca")
        click("⚙")
        waitText("Configurações")
        waitText("Ativa")
        shot("09-configuracoes")
        back()
        click("Canais", exact = true)
        waitText("Futebol 1 HD")
    }
}
