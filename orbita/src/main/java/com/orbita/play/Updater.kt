package com.orbita.play

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Versão publicada no site (orbitaplay.app/api/app/version). */
class AppUpdate(val code: Int, val name: String, val url: String, val notes: String)

/**
 * Atualização do app pelo site: consulta a versão publicada, baixa o APK do Cloudflare e abre o instalador do Android.
 * Estado observável pela tela inicial (aviso) e pelas Configurações (verificar atualizações).
 */
object Updater {
    var available by mutableStateOf<AppUpdate?>(null)
    var checking by mutableStateOf(false)
    var lastResult by mutableStateOf<String?>(null)
    /** -1 parado; 0..1 baixando. */
    var progress by mutableFloatStateOf(-1f)
    var error by mutableStateOf<String?>(null)
    /** O aviso da tela inicial já foi fechado nesta abertura do app. */
    var dismissed by mutableStateOf(false)

    fun currentCode(ctx: Context): Int = runCatching {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode
    }.getOrDefault(0)

    fun currentName(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0"

    /** Consulta o site. Devolve a atualização (se houver) e guarda em [available]. */
    suspend fun check(ctx: Context): AppUpdate? {
        if (!Activation.enabled() || checking) return available
        checking = true; error = null
        try {
            val o = withContext(Dispatchers.IO) { JSONObject(Net.text("${Activation.site}/api/app/version")) }
            val u = AppUpdate(o.optInt("code"), o.optString("name"), o.optString("url"), o.optString("notes"))
            val url = if (u.url.startsWith("http")) u.url else Activation.site + u.url
            val up = AppUpdate(u.code, u.name, url, u.notes)
            available = if (up.code > currentCode(ctx)) up else null
            lastResult = if (available != null) "Nova versão ${up.name} disponível" else "Você já está na versão mais nova"
        } catch (e: Exception) {
            lastResult = "Não foi possível verificar agora"
        } finally { checking = false }
        return available
    }

    /** Baixa o APK e abre o instalador. */
    suspend fun install(ctx: Context) {
        val up = available ?: return
        if (progress >= 0f) return
        error = null; progress = 0f
        try {
            val file = withContext(Dispatchers.IO) {
                val dir = File(ctx.cacheDir, "update").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val f = File(dir, "Orbita-Play-${up.name}.apk")
                Net.client.newCall(Net.req(up.url)).execute().use { r ->
                    if (!r.isSuccessful) throw IllegalStateException("Erro ${r.code} ao baixar")
                    val body = r.body ?: throw IllegalStateException("Download vazio")
                    val total = body.contentLength().coerceAtLeast(1L)
                    body.byteStream().use { inp ->
                        f.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024); var done = 0L
                            while (true) {
                                val n = inp.read(buf); if (n < 0) break
                                out.write(buf, 0, n); done += n
                                progress = (done.toFloat() / total).coerceIn(0f, 1f)
                            }
                        }
                    }
                }
                f
            }
            progress = 1f
            openInstaller(ctx, file)
        } catch (e: Exception) {
            error = e.message ?: "Falha ao baixar a atualização"
        } finally { delay(400); progress = -1f }
    }

    private fun openInstaller(ctx: Context, file: File) {
        // Android 8+: o app precisa da permissão "instalar apps desconhecidos"
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            runCatching {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            error = "Permita \"instalar apps desconhecidos\" para o Orbita Play e toque em Atualizar de novo."
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Confere a atualização ao abrir o app (uma vez por abertura). */
@Composable
fun UpdateCheckOnStart() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { delay(3_000); Updater.check(ctx) }
}

/** Aviso na tela inicial quando há versão nova: Atualizar agora | Depois. */
@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    val up = Updater.available ?: return
    if (Updater.dismissed) return
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val okReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { okReq.requestFocus() } }
    androidx.activity.compose.BackHandler { Updater.dismissed = true }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(360.dp).clip(RoundedCornerShape(16.dp)).background(C.panelSolid).border(1.dp, C.panelBorder, RoundedCornerShape(16.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(C.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Ic.Refresh, 22.dp, C.accentText) }
            Spacer(Modifier.height(12.dp))
            Txt("Nova versão disponível", F.s(15.sp, C.white, FontWeight.Bold, F.outfit))
            Spacer(Modifier.height(4.dp))
            Txt("Versão ${up.name} · você está na ${Updater.currentName(ctx)}", F.s(9.sp, C.dim))
            if (up.notes.isNotBlank()) { Spacer(Modifier.height(10.dp)); Txt(up.notes, F.s(9.sp, C.text), maxLines = 5, align = androidx.compose.ui.text.style.TextAlign.Center) }
            val p = Updater.progress
            if (p >= 0f) {
                Spacer(Modifier.height(16.dp))
                ProgressBar(p, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), C.accent)
                Spacer(Modifier.height(6.dp))
                Txt("Baixando… ${(p * 100).toInt()}%", F.s(9.sp, C.dim))
            }
            Updater.error?.let { Spacer(Modifier.height(10.dp)); Txt(it, F.s(8.5.sp, C.live, FontWeight.Medium), maxLines = 3, align = androidx.compose.ui.text.style.TextAlign.Center) }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Depois") { Updater.dismissed = true }
                ActionButton("Atualizar agora", primary = true, icon = Ic.Refresh, requester = okReq) {
                    scope.launch { Updater.install(ctx) }
                }
            }
        }
    }
}

