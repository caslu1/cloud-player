package com.orbita.play

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Cores e tipografia do design Orbita Play (medidas em dp = pixels do layout 1920×1080 ÷ 2). */
object C {
    val bg = Color(0xFF0B0A1C)
    val bgTop = Color(0xFF1B1446)
    val panel = Color(0xE6131229)
    val panelSolid = Color(0xFF14132B)
    val panelBorder = Color(0xFF24223F)
    val field = Color(0xFF0E0D22)
    val item = Color(0xFF1C1A36)
    val accent = Color(0xFF7C5CFF)
    val accentText = Color(0xFF9B84FF)
    val accentSoft = Color(0xFF2B2462)
    val focusBg = Color(0xFF2D2768)
    val cyan = Color(0xFF22D3EE)
    val cyanSoft = Color(0xFF123447)
    val live = Color(0xFFF43F5E)
    val star = Color(0xFFFBBF24)
    val white = Color(0xFFFFFFFF)
    val text = Color(0xFFE7E5F7)
    val dim = Color(0xFF9D9ABB)
    val faint = Color(0xFF6E6B8C)
    val chip = Color(0xFF262440)
    val fieldGreen = Color(0xFF1C7A49)
    val fieldDark = Color(0xFF0C2E1E)

    val screen = Brush.linearGradient(listOf(Color(0xFF1E1650), Color(0xFF0E0C26), Color(0xFF0A0A18)))

    private val palette = listOf(
        0xFF7C3AED, 0xFF2563EB, 0xFF0891B2, 0xFF059669, 0xFFCA8A04, 0xFFEA580C,
        0xFFDC2626, 0xFFDB2777, 0xFF9333EA, 0xFF0D9488, 0xFF4F46E5, 0xFF65A30D
    )

    /** Cor estável derivada de um texto (logos de canais sem imagem, escudos de times, capas geradas). */
    fun hash(s: String): Color = Color(palette[(s.hashCode() and 0x7fffffff) % palette.size])
}

object F {
    val outfit = FontFamily(Font(R.font.outfit_regular, FontWeight.Normal), Font(R.font.outfit_bold, FontWeight.Bold))
    val inter = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_bold, FontWeight.Bold)
    )

    fun s(size: TextUnit, color: Color = C.text, w: FontWeight = FontWeight.Normal, family: FontFamily = inter, spacing: TextUnit = TextUnit.Unspecified) =
        TextStyle(fontSize = size, color = color, fontWeight = w, fontFamily = family, letterSpacing = spacing)

    val label = s(8.5.sp, C.dim, FontWeight.SemiBold, spacing = 1.6.sp)
}
