package com.orbita.play

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Papel de parede escolhido (lido por todas as telas). */
object Wall {
    var index by mutableIntStateOf(1)
}

class WallStyle(val name: String, val draw: DrawScope.() -> Unit)

private fun DrawScope.base(c: Long) = drawRect(Color(c))
private fun DrawScope.glow(c: Long, x: Float, y: Float, r: Float) {
    val col = Color(c)
    drawRect(Brush.radialGradient(listOf(col, col.copy(alpha = 0f)), Offset(size.width * x, size.height * y), size.width * r))
}

/** Estrelas fixas (mesma posição sempre), bem discretas. */
private val stars = java.util.Random(7).let { r -> List(110) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.08f + r.nextFloat() * 0.3f, 0.5f + r.nextFloat()) } }
private fun DrawScope.starfield() {
    val k = size.width / 960f
    stars.forEach { s -> drawCircle(Color.White.copy(alpha = s[2]), s[3] * k, Offset(size.width * s[0], size.height * s[1])) }
}

/** "Sem fundo" + 10 fundos escuros, desenhados no aparelho (não pesam e não baixam nada). */
val wallpapers = listOf(
    WallStyle("Sem fundo") { base(0xFF050409) },
    WallStyle("Órbita") {
        drawRect(Brush.radialGradient(listOf(Color(0xFF1A0E44), Color(0xFF09061A), Color(0xFF040308)), Offset(size.width * 0.12f, 0f), size.width * 0.8f))
    },
    WallStyle("Aurora") { base(0xFF050409); glow(0x665B2CFF, 0.15f, 0f, 0.65f); glow(0x3322A6FF, 0.92f, 0.15f, 0.55f) },
    WallStyle("Nebulosa") { base(0xFF040307); glow(0x48B83CFF, 0.78f, 0.3f, 0.5f); glow(0x334E2BFF, 0.18f, 0.85f, 0.55f); starfield() },
    WallStyle("Meia-noite") { drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1233), Color(0xFF05071A), Color(0xFF030409)))) },
    WallStyle("Ametista") {
        drawRect(Brush.linearGradient(listOf(Color(0xFF1C0F3A), Color(0xFF07050E), Color(0xFF130A28)), Offset.Zero, Offset(size.width, size.height)))
    },
    WallStyle("Eclipse") {
        base(0xFF030207)
        drawRect(
            Brush.radialGradient(
                0f to Color.Transparent, 0.78f to Color.Transparent, 0.86f to Color(0x707C5CFF), 0.93f to Color(0x223A7BFF), 1f to Color.Transparent,
                center = Offset(size.width * 0.5f, size.height * 1.45f), radius = size.width * 0.75f
            )
        )
    },
    WallStyle("Oceano") { base(0xFF03070B); glow(0x4D0F6B8A, 0.82f, 0f, 0.7f); glow(0x22105070, 0.1f, 1f, 0.5f) },
    WallStyle("Brasa") { base(0xFF080405); glow(0x558A2A12, 0.15f, 1f, 0.7f); glow(0x22803010, 0.9f, 0.1f, 0.4f) },
    WallStyle("Floresta") { base(0xFF030805); glow(0x480F6B43, 0.85f, 0.95f, 0.7f); glow(0x1A1E8A55, 0.1f, 0f, 0.45f) },
    WallStyle("Grafite") {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF17171E), Color(0xFF08080B))))
        glow(0x0FFFFFFF, 0.1f, 0f, 0.6f)
    },
)

@Composable
fun WallpaperView(index: Int, modifier: Modifier = Modifier.fillMaxSize()) {
    val w = wallpapers.getOrElse(index) { wallpapers[1] }
    Canvas(modifier) { w.draw(this) }
}

/** Grade de escolha nas Configurações. */
@Composable
fun WallpaperPicker(vm: Vm) {
    Txt("Escolha o fundo das telas do app.", F.s(9.5.sp, C.dim))
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        wallpapers.indices.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { i ->
                    val sel = Wall.index == i
                    Focus(Modifier.size(118.dp, 82.dp), onClick = { vm.store.setWallpaper(i) }, memKey = "wall:$i") { f ->
                        Column {
                            val shape = RoundedCornerShape(8.dp)
                            Box(
                                Modifier.fillMaxWidth().height(62.dp).clip(shape)
                                    .border(if (f) 2.dp else if (sel) 1.5.dp else 1.dp, if (f) C.white else if (sel) C.accent else C.panelBorder, shape)
                            ) {
                                WallpaperView(i)
                                if (sel) Box(
                                    Modifier.align(Alignment.TopEnd).padding(5.dp).size(16.dp).clip(CircleShape).background(C.accent),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Ic.Check, 11.dp) }
                            }
                            Txt(wallpapers[i].name, F.s(8.5.sp, if (f || sel) C.white else C.dim, FontWeight.SemiBold), Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
