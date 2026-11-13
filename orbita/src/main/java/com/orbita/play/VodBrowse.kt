package com.orbita.play

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Categoria da lateral de Filmes/Séries. */
class BrowseCat(val id: String, val name: String, val count: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null)

/**
 * Filmes e Séries: categorias na lateral esquerda; à direita o destaque (do título com foco) e a grade de capas.
 * Passar o foco por uma categoria troca a grade (com uma pequena espera, para a rolagem ficar leve).
 */
@Composable
fun VodBrowse(
    vm: Vm,
    cats: List<BrowseCat>,
    selected: String?,
    onSelect: (String) -> Unit,
    catState: LazyListState,
    gridState: LazyGridState,
    entriesOf: (String) -> List<VodEntry>,
    hero: @Composable (VodEntry?) -> HeroData,
    empty: String,
    search: (String) -> List<VodEntry>,
    searchHint: String,
    locked: (String) -> Boolean = { false },
    unlock: (then: () -> Unit) -> Unit = { it() },
) {
    // sem escolha: abre em Continuar assistindo se houver algo em andamento, senão na primeira categoria com títulos
    val cat = selected?.takeIf { id -> cats.any { it.id == id } && !locked(id) } ?: (cats.firstOrNull { it.count > 0 } ?: cats.firstOrNull())?.id ?: ""
    var catQuery by remember { mutableStateOf("") }
    var shownCat by remember { mutableStateOf(cat) }
    LaunchedEffect(cat) { if (shownCat != cat) { gridState.scrollToItem(0); shownCat = cat } }

    // busca: filtra as categorias e também procura os títulos (fora da thread da tela)
    var results by remember { mutableStateOf<List<VodEntry>?>(null) }
    LaunchedEffect(catQuery) {
        if (catQuery.trim().length < 2) { results = null; return@LaunchedEffect }
        delay(300)
        results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { search(catQuery.trim()) }
        gridState.scrollToItem(0)
    }
    val searching = results != null
    val catEntries = remember(cat, cats) { entriesOf(cat) }
    val entries = results ?: catEntries
    var focused by remember { mutableStateOf<VodEntry?>(null) }
    var heroEntry by remember(cat, searching) { mutableStateOf(entries.firstOrNull()) }
    LaunchedEffect(focused) { val f = focused ?: return@LaunchedEffect; delay(280); heroEntry = f }

    val visibleCats = remember(cats, catQuery) {
        if (catQuery.isBlank()) cats else cats.filter { Names.norm(it.name).contains(Names.norm(catQuery), true) }
    }
    val gridReq = remember { FocusRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    /** Vai para a primeira capa (▶ nas categorias, OK numa categoria). */
    fun toFirst() { scope.launch { delay(40); gridState.scrollToItem(0); delay(30); runCatching { gridReq.requestFocus() } } }
    val catReq = remember { FocusRequester() }
    InitialFocus(if (entries.isEmpty()) catReq else gridReq)

    val h = hero(heroEntry)
    Box(Modifier.fillMaxSize()) {
        Backdrop(h.backdrop, h.title, dim = true)
        Row(Modifier.fillMaxSize().padding(top = 18.dp, start = 24.dp, end = 0.dp)) {
            // categorias
            Panel(Modifier.width(206.dp).fillMaxHeight().padding(bottom = 14.dp)) {
                Column {
                    PanelHeader("Categorias", "${cats.size}")
                    SearchField(catQuery, { catQuery = it }, searchHint, Modifier.padding(horizontal = 10.dp))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(state = catState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(visibleCats, key = { it.id }) { c ->
                            CatRow(c.name, c.count, c.id == cat && !searching, "vcat:${c.id}", {}, if (c.id == cat) catReq else null, c.icon, ::toFirst) {
                                val go = { catQuery = ""; results = null; onSelect(c.id); toFirst() }
                                if (locked(c.id)) unlock(go) else go()
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(18.dp))
            // destaque + grade
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.height(168.dp).padding(end = 28.dp)) {
                    Txt(h.label, F.s(8.sp, C.cyan, FontWeight.SemiBold, spacing = 2.sp))
                    Spacer(Modifier.height(5.dp))
                    TitleArt(h.title, h.logo, big = false)
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        h.chips.forEach { MetaChip(it) }
                        if (h.rating > 0) RatingChip(h.rating)
                        h.genre?.let { Txt(it, F.s(8.5.sp, C.dim), Modifier.padding(start = 2.dp)) }
                    }
                    Spacer(Modifier.height(7.dp))
                    Txt(h.plot ?: "", F.s(9.5.sp, C.text.copy(alpha = 0.92f)), Modifier.width(520.dp), maxLines = 2)
                }
                Row(Modifier.padding(end = 28.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (searching) "Resultados para \"${catQuery.trim()}\"" else cats.firstOrNull { it.id == cat }?.name ?: "", F.s(12.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f))
                    Txt(if (entries.size == 1) "1 título" else "${entries.size} títulos", F.s(9.sp, C.dim))
                }
                if (entries.isEmpty()) {
                    if (searching) EmptyState("Nada encontrado", "Tente outro nome.", Modifier.fillMaxWidth().padding(top = 40.dp))
                    else EmptyState(if (cat == "__cont") "Nada em andamento" else if (cat == "__mine") "Sua lista está vazia" else empty,
                        if (cat == "__cont") "O que você começar a assistir aparece aqui." else if (cat == "__mine") "Segure OK sobre uma capa para adicionar." else "",
                        Modifier.fillMaxWidth().padding(top = 40.dp))
                } else TvScroll {
                    LazyVerticalGrid(
                        GridCells.Fixed(4), Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x33050409)))),
                        state = gridState,
                        contentPadding = PaddingValues(start = 4.dp, end = 28.dp, top = 8.dp, bottom = 60.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(entries, key = { _, e -> e.key }) { i, e ->
                            LaunchedEffect(e.key) { delay(200); e.movie?.let { vm.requestMeta(it) }; e.series?.let { vm.requestMeta(it) } }
                            val img = (e.movie?.let { vm.metaOf(it) } ?: e.series?.let { vm.metaOf(it) })?.posterUrl() ?: e.image
                            BigPoster(e, img, 152.dp, if (searching) "vsearch:${e.key}" else "vgrid:$cat:${e.key}", if (i == 0) gridReq else null, onFocus = { focused = e })
                        }
                    }
                }
            }
        }
    }
}
