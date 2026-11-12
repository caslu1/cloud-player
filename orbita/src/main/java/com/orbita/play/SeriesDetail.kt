package com.orbita.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale

fun episodeItem(s: Series, e: Episode) = PlayItem(
    Kind.EPISODE, "ep:${e.id}", s.name, "T${e.season} · E${e.num} — ${e.title}", e.url, e.img ?: s.cover, series = s, episode = e
)

@Composable
fun SeriesDetail(vm: Vm, pb: Playback, ui: Ui, s: Series) {
    var info by remember { mutableStateOf(vm.seriesInfos[s.id]) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(s.id) { vm.requestMeta(s) }
    val tm = vm.metaOf(s)
    LaunchedEffect(s.id, attempt) {
        if (info == null) runCatching { vm.seriesInfo(s) }.onSuccess { info = it; error = null }.onFailure { error = it.message ?: "Erro ao carregar episódios." }
    }
    val last = vm.store.lastEpisodeOf(s.id)
    val seasons = info?.seasons?.keys?.toList().orEmpty()
    var season by remember(info) { mutableIntStateOf(last?.season?.takeIf { it in seasons } ?: seasons.firstOrNull() ?: 1) }
    val allEps = remember(info) { info?.seasons?.values?.flatten().orEmpty() }
    val eps = info?.seasons?.get(season).orEmpty()
    // imagens e sinopses dos episódios pelo TMDB (quando o provedor não manda)
    var tmEps by remember { mutableStateOf<Map<Int, Tmdb.Ep>>(emptyMap()) }
    LaunchedEffect(tm?.id, season) {
        val id = tm?.id ?: return@LaunchedEffect
        tmEps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Tmdb.season(id, season) }.getOrDefault(emptyMap()) }
    }

    fun play(e: Episode) {
        val p = vm.store.progress["ep:${e.id}"]
        val start = if (p != null && !p.finished && p.pos > 30_000) p.pos else 0L
        pb.openFull(episodeItem(s, e), allEps.map { episodeItem(s, it) }, start)
    }

    // Continuar: episódio em andamento ou o próximo depois do último assistido
    val resumeEp: Episode? = remember(info, last) {
        if (last == null) null else {
            val i = allEps.indexOfFirst { "ep:${it.id}" == last.key }
            when {
                i < 0 -> null
                last.finished -> allEps.getOrNull(i + 1) ?: allEps[i]
                else -> allEps[i]
            }
        }
    }

    val playReq = remember { FocusRequester() }
    InitialFocus(playReq, info != null)

    Box(Modifier.fillMaxSize()) {
        Backdrop(tm?.backdropUrl() ?: info?.backdrop ?: s.backdrop, s.name)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xCC050409), Color(0x66050409)))))
        Row(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 34.dp)) {
            // Informações
            Column(Modifier.width(360.dp).padding(top = 42.dp)) {
                Txt("SÉRIE · ${seasons.size.coerceAtLeast(1)} TEMPORADA${if (seasons.size > 1) "S" else ""}", F.s(8.sp, C.cyan, FontWeight.SemiBold, spacing = 2.sp))
                Spacer(Modifier.height(4.dp))
                TitleArt(tm?.title ?: Tmdb.clean(s.name).first, tm?.logoUrl())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (tm?.year ?: info?.year ?: s.year)?.let { MetaChip(it) }
                    val r = tm?.rating?.takeIf { it > 0 } ?: info?.rating?.takeIf { it > 0 } ?: s.rating
                    if (r > 0) MetaChip(String.format(Locale("pt", "BR"), "★ %.1f", r), C.star, Color(0x33FBBF24))
                    (tm?.genres ?: info?.genre ?: s.genre)?.let { Txt(it, F.s(8.5.sp, C.dim)) }
                }
                Spacer(Modifier.height(10.dp))
                Txt(tm?.overview ?: info?.plot ?: s.plot ?: "", F.s(9.5.sp, C.text), maxLines = 4)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val label = when {
                        resumeEp != null -> "Continuar T${resumeEp.season} · E${resumeEp.num}"
                        else -> "Assistir T${allEps.firstOrNull()?.season ?: 1} · E${allEps.firstOrNull()?.num ?: 1}"
                    }
                    ActionButton(label, primary = true, icon = "▶", requester = playReq) {
                        (resumeEp ?: allEps.firstOrNull())?.let { play(it) }
                    }
                    ActionButton(if (vm.store.inList("s:${s.id}")) "✓ Na minha lista" else "+ Minha lista") { vm.store.toggleList("s:${s.id}") }
                }
                val cast = info?.cast.orEmpty()
                if (cast.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Txt("ELENCO", F.label)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        cast.take(5).forEach { n ->
                            Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(C.hash(n), C.hash(n + "x")))),
                                    contentAlignment = Alignment.Center
                                ) { Txt(MatchLogic.initials(n), F.s(10.sp, C.white, FontWeight.Bold)) }
                                Spacer(Modifier.height(5.dp))
                                Txt(n, F.s(7.sp, C.text), maxLines = 2, align = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(22.dp))
            // Temporadas e episódios
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    info == null && error == null -> Loading("Carregando episódios…", Modifier.fillMaxSize())
                    info == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        EmptyState("Não foi possível carregar os episódios", error.orEmpty())
                        ActionButton("Tentar de novo", primary = true) { error = null; attempt++ }
                    }
                    allEps.isEmpty() -> EmptyState("Nenhum episódio disponível", "", Modifier.fillMaxSize())
                    else -> Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(seasons, key = { it }) { n ->
                                    ChipPill("Temporada $n", n == season, onFocus = { season = n }) { season = n }
                                }
                            }
                            Txt("${eps.size} episódios", F.s(8.5.sp, C.dim))
                        }
                        val listState = rememberLazyListState()
                        LaunchedEffect(season) { listState.scrollToItem(0) }
                        LazyColumn(state = listState, contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(eps, key = { _, e -> e.id }) { _, e ->
                                EpisodeRow(vm, s, e, tmEps[e.num]) { play(e) }
                            }
                        }
                    }
                }
            }
        }
        HintBar(
            listOf("OK" to "Assistir episódio", "◀▶" to "Temporadas", "▲▼" to "Episódios", "VOLTAR" to "Séries"),
            Modifier.align(Alignment.BottomStart).padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun EpisodeRow(vm: Vm, s: Series, e: Episode, tme: Tmdb.Ep?, onClick: () -> Unit) {
    val p = vm.store.progress["ep:${e.id}"]
    Focus(Modifier.fillMaxWidth(), onClick = onClick, memKey = "ep:${e.id}") { f ->
        val shape = RoundedCornerShape(9.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(if (f) C.focusBg else Color.Transparent)
                .then(if (f) Modifier.border(1.5.dp, C.accent, shape) else Modifier).padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(120.dp).height(67.dp).clip(RoundedCornerShape(6.dp))) {
                val c = C.hash(s.name + e.num)
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.copy(alpha = 0.8f), Color(0xFF16142C)))))
                (e.img ?: tme?.still)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(Modifier.padding(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xCC000000)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Txt("${e.num}", F.s(7.5.sp, C.white, FontWeight.Bold))
                }
                if (p != null) ProgressBar(if (p.finished) 1f else p.fraction, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.5.dp), C.accent, Color(0x55FFFFFF))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    val meta = listOfNotNull("Episódio ${e.num}", e.durationSec.takeIf { it > 0 }?.let { "${it / 60} min" }).joinToString(" · ")
                    Txt(meta, F.s(7.5.sp, C.dim, FontWeight.SemiBold))
                    if (p?.finished == true) Txt(" · Assistido", F.s(7.5.sp, C.cyan, FontWeight.SemiBold))
                    else if (p != null && p.pos > 30_000) Txt(" · Faltam ${p.remainingMin} min", F.s(7.5.sp, C.cyan, FontWeight.SemiBold))
                }
                val name = tme?.name?.takeIf { e.title.startsWith("Episódio") || e.title.matches(Regex("""(?i)^(ep(isode|isódio)?\s*)?\d+$""")) } ?: e.title
                Txt(name, F.s(11.sp, C.white, FontWeight.SemiBold))
                (e.plot ?: tme?.overview)?.let { Txt(it, F.s(8.5.sp, C.dim), maxLines = 2) }
            }
        }
    }
}
