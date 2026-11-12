package com.orbita.play

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Ativação pelo site: o aparelho tem um MAC e uma chave; o cliente informa os dois no site
 * (ou lê o QR code), cadastra a lista e o app recebe tudo sozinho.
 */
object Activation {
    /** Endereço do site (vazio = ativação desligada). Vem do build: ORBITA_SITE_URL. */
    var site: String by androidx.compose.runtime.mutableStateOf(BuildConfig.SITE_URL.trimEnd('/'))
    fun enabled() = site.isNotBlank()

    /** "MAC" do aparelho: gerado a partir do ID do Android (o MAC real não é acessível nas versões novas). */
    @SuppressLint("HardwareIds")
    fun mac(ctx: Context): String {
        val p = ctx.getSharedPreferences("orbita", Context.MODE_PRIVATE)
        p.getString("dmac", null)?.let { return it }
        val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: java.util.UUID.randomUUID().toString()
        val h = MessageDigest.getInstance("SHA-256").digest(id.toByteArray())
        val b = h.copyOf(6)
        b[0] = ((b[0].toInt() and 0xFC) or 0x02).toByte() // endereço "local", nunca de um fabricante real
        val mac = b.joinToString(":") { "%02X".format(it) }
        p.edit().putString("dmac", mac).apply()
        return mac
    }

    fun key(ctx: Context): String {
        val p = ctx.getSharedPreferences("orbita", Context.MODE_PRIVATE)
        p.getString("dkey", null)?.let { return it }
        val k = (100000 + java.security.SecureRandom().nextInt(900000)).toString()
        p.edit().putString("dkey", k).apply()
        return k
    }

    /** Cadastra/atualiza o aparelho no site e devolve a situação + listas. */
    fun register(ctx: Context): ActState? {
        if (!enabled()) return null
        val body = JSONObject().put("mac", mac(ctx)).put("key", key(ctx)).put("platform", "android")
            .put("model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()).toString()
        val req = Request.Builder().url("$site/api/app/register").post(body.toRequestBody("application/json".toMediaType())).build()
        Net.client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val o = JSONObject(r.body?.string() ?: return null)
            val a = o.optJSONArray("playlists")
            val lists = (0 until (a?.length() ?: 0)).mapNotNull { a!!.optJSONObject(it) }.map {
                ActPlaylist(it.optString("name"), it.optString("type"), it.optString("server"), it.optString("username"), it.optString("password"), it.optString("url"))
            }
            return ActState(o.optString("status"), if (o.isNull("days_left")) null else o.optInt("days_left"), o.optString("activate_url"), o.optString("support"), lists)
        }
    }
}

class ActPlaylist(val name: String, val type: String, val server: String, val user: String, val pass: String, val url: String) {
    fun account(): Account = if (type == "xtream") Account("xtream", Net.normalizeServer(server), user, pass, "")
    else M3u.xtreamFromUrl(url)?.let { Account("xtream", Net.normalizeServer(it.first), it.second, it.third, url) } ?: Account("m3u", "", "", "", url)
    val key get() = "$type|$server|$user|$pass|$url"
}

class ActState(val status: String, val daysLeft: Int?, val activateUrl: String, val support: String, val playlists: List<ActPlaylist>) {
    val locked get() = status == "expired" || status == "blocked"
}

/** QR code desenhado na tela (fundo branco). */
@Composable
fun QrCode(text: String, modifier: Modifier) {
    val m = remember(text) {
        runCatching { QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1)) }.getOrNull()
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).padding(5.dp)) {
        if (m != null) Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / m.width
            for (y in 0 until m.height) for (x in 0 until m.width) if (m.get(x, y))
                drawRect(Color(0xFF0B0A12), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

private fun statusLine(a: ActState?): Pair<String, Color> = when (a?.status) {
    "trial" -> "Teste grátis · ${a.daysLeft ?: 0} dia${if (a.daysLeft == 1) "" else "s"}" to Color(0xFFFFC56B)
    "active" -> (if (a.daysLeft == null) "Ativo · vitalício" else "Ativo · ${a.daysLeft} dias") to Color(0xFF7EE2A8)
    "expired" -> "Vencido" to Color(0xFFFF8095)
    "blocked" -> "Bloqueado" to Color(0xFFFF8095)
    else -> "Conectando ao site…" to C.dim
}

/** Cartão da tela de login: QR code + MAC + chave para ativar pelo celular. */
@Composable
fun ActivationCard(vm: Vm) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mac = remember { Activation.mac(ctx) }
    val key = remember { Activation.key(ctx) }
    val a = vm.act
    val url = a?.activateUrl?.takeIf { it.isNotBlank() } ?: "${Activation.site}/ativar?mac=$mac&key=$key"
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xCC0E0B1A)).border(1.dp, Color(0xFF221A3A), RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QrCode(url, Modifier.size(104.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Txt("Ative pelo celular", F.s(12.sp, C.white, FontWeight.Bold, F.outfit))
            Txt("Aponte a câmera para o QR code ou acesse", F.s(7.5.sp, C.dim))
            Txt(Activation.site.removePrefix("https://").removePrefix("http://"), F.s(8.sp, C.accentText, FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column { Txt("MAC", F.s(6.5.sp, C.faint, FontWeight.SemiBold, spacing = 1.sp)); Txt(mac, F.s(11.sp, C.white, FontWeight.Bold)) }
                Spacer(Modifier.width(16.dp))
                Column { Txt("CHAVE", F.s(6.5.sp, C.faint, FontWeight.SemiBold, spacing = 1.sp)); Txt(key, F.s(11.sp, C.white, FontWeight.Bold, spacing = 2.sp)) }
            }
            Spacer(Modifier.height(6.dp))
            val (t, c) = statusLine(a)
            Txt(t, F.s(8.sp, c, FontWeight.SemiBold))
        }
    }
}

/** Cartão "Pelo celular" do login padrão: QR code grande, MAC, chave e situação. */
@Composable
fun ActivationPanel(vm: Vm, modifier: Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mac = remember { Activation.mac(ctx) }
    val key = remember { Activation.key(ctx) }
    ActivationPolling(vm)
    val a = vm.act
    val url = a?.activateUrl?.takeIf { it.isNotBlank() } ?: "${Activation.site}/ativar?mac=$mac&key=$key"
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xE60C0A16)).border(1.dp, C.panelBorder, RoundedCornerShape(14.dp)).padding(20.dp)
    ) {
        Txt("PELO CELULAR", F.s(8.sp, C.accentText, FontWeight.SemiBold, spacing = 1.2.sp))
        Spacer(Modifier.height(5.dp))
        Txt("Ative com o QR code", F.s(19.sp, C.white, FontWeight.Bold, F.outfit))
        Row {
            Txt("Aponte a câmera do celular ou acesse ", F.s(10.sp, C.dim))
            Txt(Activation.site.removePrefix("https://").removePrefix("http://"), F.s(10.sp, C.accentText, FontWeight.SemiBold))
        }
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QrCode(url, Modifier.size(150.dp))
            Spacer(Modifier.width(15.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoBox("MAC", mac, 13f, 0.5f)
                InfoBox("CHAVE", key, 15f, 3f)
                val (t, c) = statusLine(a)
                Row(
                    Modifier.clip(RoundedCornerShape(15.dp)).background(c.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(c))
                    Spacer(Modifier.width(5.dp))
                    Txt(t, F.s(9.5.sp, c, FontWeight.SemiBold))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Txt("Cadastre sua lista no site e a TV entra sozinha.", F.s(9.sp, C.dim))
    }
}

@Composable
private fun InfoBox(label: String, value: String, size: Float, spacing: Float) {
    Column(Modifier.width(142.dp).clip(RoundedCornerShape(8.dp)).background(C.field).border(1.dp, C.panelBorder, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Txt(label, F.s(7.sp, C.faint, FontWeight.SemiBold, spacing = 0.8.sp))
        Spacer(Modifier.height(2.dp))
        Txt(value, F.s(size.sp, C.white, FontWeight.Bold, F.outfit, spacing.sp))
    }
}

/** Consulta o site de tempos em tempos (lista nova, pagamento confirmado…). */
@Composable
fun ActivationPolling(vm: Vm, everyMs: Long = 5_000) {
    if (!Activation.enabled()) return
    LaunchedEffect(Unit) { while (true) { vm.checkActivation(); delay(everyMs) } }
}

/** Tela de bloqueio quando o teste/plano venceu ou o aparelho foi bloqueado. */
@Composable
fun ActivationScreen(vm: Vm) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mac = remember { Activation.mac(ctx) }
    val key = remember { Activation.key(ctx) }
    val a = vm.act
    ActivationPolling(vm)
    val req = remember { FocusRequester() }
    InitialFocus(req)
    Box(Modifier.fillMaxSize().background(C.bg), contentAlignment = Alignment.Center) {
        ScreenBackground()
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xF00E0B1A)).border(1.dp, Color(0xFF221A3A), RoundedCornerShape(14.dp)).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(size = 16f)
            Spacer(Modifier.height(14.dp))
            Txt(if (a?.status == "blocked") "Aparelho bloqueado" else "Seu período de uso terminou", F.s(17.sp, C.white, FontWeight.Bold, F.outfit))
            Spacer(Modifier.height(4.dp))
            Txt("Ative pelo celular: aponte a câmera para o QR code e escolha um plano.", F.s(9.sp, C.dim))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                QrCode(a?.activateUrl ?: "${Activation.site}/ativar?mac=$mac&key=$key", Modifier.size(150.dp))
                Column {
                    Txt("SITE", F.s(7.sp, C.faint, FontWeight.SemiBold, spacing = 1.sp))
                    Txt(Activation.site.removePrefix("https://").removePrefix("http://"), F.s(10.sp, C.accentText, FontWeight.SemiBold))
                    Spacer(Modifier.height(8.dp))
                    Txt("MAC", F.s(7.sp, C.faint, FontWeight.SemiBold, spacing = 1.sp)); Txt(mac, F.s(13.sp, C.white, FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Txt("CHAVE", F.s(7.sp, C.faint, FontWeight.SemiBold, spacing = 1.sp)); Txt(key, F.s(13.sp, C.white, FontWeight.Bold, spacing = 2.sp))
                    if (!a?.support.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); Txt("Suporte: ${a!!.support}", F.s(8.5.sp, C.dim)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            ActionButton("Já ativei — verificar", primary = true, icon = Ic.Refresh, requester = req) { vm.checkActivation() }
        }
    }
}
