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
import java.time.Duration

/**
 * Abre o app num Android simulado, entra num servidor Xtream falso e passa por todas as abas.
 * Pega erros que fazem o app fechar (o teste falha com a exceção).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land-mdpi")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val server = MockWebServer()

    private fun b64(s: String) = Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)

    @Before fun setUp() {
        val now = System.currentTimeMillis() / 1000
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (!path.startsWith("/player_api.php")) return MockResponse().setResponseCode(404)
                val action = Regex("action=([a-z_]+)").find(path)?.groupValues?.get(1)
                val body = when (action) {
                    null -> """{"user_info":{"auth":1,"status":"Active","exp_date":"1893456000","max_connections":"2","active_cons":"0"},"server_info":{}}"""
                    "get_live_categories" -> """[{"category_id":"1","category_name":"Futebol"},{"category_id":"2","category_name":"Notícias"}]"""
                    "get_live_streams" -> """[
                        {"num":101,"name":"Futebol 1 HD","stream_id":101,"stream_icon":"","category_id":"1","epg_channel_id":"f1"},
                        {"num":101,"name":"Futebol 1 HD","stream_id":101,"stream_icon":"","category_id":"1"},
                        {"num":102,"name":"Jornal 24h","stream_id":102,"category_id":"2"}]"""
                    "get_vod_categories" -> """[{"category_id":"10","category_name":"Drama"}]"""
                    "get_vod_streams" -> """[{"stream_id":5,"name":"Sertão Vermelho","stream_icon":"","category_id":"10","rating":"8.1","added":"1700000000","container_extension":"mp4"}]"""
                    "get_vod_info" -> """{"info":{"plot":"Uma engenheira volta para casa.","genre":"Drama","releasedate":"2026-01-01","duration_secs":"7680","rating":"8.1"}}"""
                    "get_series_categories" -> """[{"category_id":"20","category_name":"Médicos"}]"""
                    "get_series" -> """[{"series_id":7,"name":"Plantão Noturno","cover":"","category_id":"20","rating":"8.6","plot":"Residentes.","last_modified":"1700000000"}]"""
                    "get_series_info" -> """{"info":{"plot":"Residentes.","cast":"Ana Lima, Rafael Souza"},"episodes":{"1":[
                        {"id":"71","episode_num":1,"title":"O Plantão","container_extension":"mp4","info":{"duration_secs":"2880"}},
                        {"id":"72","episode_num":2,"title":"Sirene","container_extension":"mp4","info":{}}]}}"""
                    "get_short_epg" -> """{"epg_listings":[
                        {"title":"${b64("Campeonato Nacional: Flamengo x Palmeiras")}","description":"","start_timestamp":"${now - 600}","stop_timestamp":"${now + 6000}"},
                        {"title":"${b64("Gols da Rodada")}","description":"","start_timestamp":"${now + 6000}","stop_timestamp":"${now + 9000}"}]}"""
                    else -> "[]"
                }
                return MockResponse().setBody(body).setHeader("Content-Type", "application/json")
            }
        }
        server.start()
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
        throw AssertionError("Texto não apareceu: $text")
    }

    private fun click(text: String) {
        rule.onAllNodesWithText(text, substring = true)[0].performClick()
        repeat(5) { pump() }
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        repeat(5) { pump() }
    }

    @Test fun loginAndBrowseEverything() {
        rule.mainClock.autoAdvance = false
        waitText("Conectar lista")

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput(server.url("/").toString())
        fields[1].performTextInput("user")
        fields[2].performTextInput("pass")
        click("Entrar")

        // Canais
        waitText("Futebol 1 HD")
        waitText("Jornal 24h")
        click("Jornal 24h") // toca na prévia
        repeat(10) { pump() }

        // Filmes
        click("Filmes")
        waitText("Sertão Vermelho")
        waitText("Lançamentos")
        rule.onAllNodesWithText("Sertão Vermelho")[rule.onAllNodesWithText("Sertão Vermelho").fetchSemanticsNodes().size - 1].performClick()
        waitText("FILME")
        waitText("Uma engenheira")
        back()

        // Séries
        click("Séries")
        waitText("Plantão Noturno")
        rule.onAllNodesWithText("Ver episódios")[0].performClick()
        waitText("O Plantão")
        waitText("Sirene")
        back()

        // Futebol (jogos vindos do EPG)
        click("Futebol")
        waitText("Flamengo")
        waitText("Palmeiras")

        // Buscar, Biblioteca, Configurações
        click("Buscar")
        waitText("O que você quer assistir?")
        click("Biblioteca")
        waitText("Canais favoritos")
        click("⚙")
        waitText("Configurações")
        waitText("Ativa")
        back()
        click("Canais")
        waitText("Futebol 1 HD")
    }
}
