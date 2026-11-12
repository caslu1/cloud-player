' Filmes e Séries igual ao Android: categorias à esquerda, destaque com a capa ao fundo e grade de 4 capas.
sub init()
    m.cats = m.top.findNode("cats")
    m.grid = m.top.findNode("grid")
    m.backdrop = m.top.findNode("backdrop")
    lb = m.top.findNode("labels")
    addLabel(lb, "CATEGORIAS", 72, 60, ISEM(), 17, "0x9D9ABBFF")
    m.catCount = addLabel(lb, "", 300, 60, ISEM(), 17, "0x9D9ABBFF", 136, "right")
    m.kindLbl = addLabel(lb, "", 496, 40, ISEM(), 17, "0x22D3EEFF")
    m.title = addLabel(lb, "", 496, 62, OUTF(), 42, "0xFFFFFFFF", 1360)
    m.title.maxLines = 1
    m.chips = lb.createChild("Group")
    m.plot = addLabel(lb, "", 496, 176, IREG(), 20, "0xE7E5F7EB", 1040)
    m.plot.wrap = true
    m.plot.maxLines = 2
    m.secTitle = addLabel(lb, "", 496, 378, ISEM(), 24, "0xFFFFFFFF", 900)
    m.secCount = addLabel(lb, "", 1500, 384, IREG(), 18, "0x9D9ABBFF", 364, "right")
    m.status = addLabel(lb, "Carregando…", 504, 640, IMED(), 22, "0x9D9ABBFF", 1360, "center")
    m.cats.observeField("itemSelected", "onCat")
    m.grid.observeField("itemFocused", "onItemFocus")
    m.grid.observeField("itemSelected", "onItem")
    m.top.findNode("heroTimer").observeField("fire", "loadHero")
    m.top.observeField("focusedChild", "onFocus")
    m.catData = []
    m.items = []
    m.focusList = m.cats
    m.seq = 0
    m.heroSeq = 0
    m.catKey = ""
end sub

sub onParams()
    m.series = (m.top.params.kind = "series")
    if m.series
        m.kindLbl.text = "SÉRIE"
        callApi("cats", apiUrl("get_series_categories"))
    else
        m.kindLbl.text = "FILME"
        callApi("cats", apiUrl("get_vod_categories"))
    end if
end sub

sub onFocus()
    if m.top.hasFocus()
        ' volta do detalhe: atualiza a contagem da Minha lista
        if m.catNodes <> invalid and m.catNodes.DoesExist("__mine")
            m.catNodes["__mine"].count = mineItems().Count().ToStr()
            if m.catKey = "__mine" then showItems(mineItems())
        end if
        m.focusList.setFocus(true)
    end if
end sub

sub onApi(event as object)
    r = event.getData()
    if r.id = "cats"
        if r.ok and type(r.data) = "roArray"
            m.catData = r.data
            buildCats()
        else
            m.status.text = "Não foi possível carregar."
        end if
    else if r.id = "items" + m.seq.ToStr()
        if r.ok and type(r.data) = "roArray" then showItems(r.data) else m.status.text = "Não foi possível carregar."
    else if r.id = "hero" + m.heroSeq.ToStr()
        if r.ok and type(r.data) = "roAssociativeArray" and type(r.data.info) = "roAssociativeArray" then fillMovieHero(r.data.info)
    end if
end sub

sub buildCats()
    root = CreateObject("roSGNode", "ContentNode")
    m.catNodes = {}
    m.contKind = "movie"
    if m.series then m.contKind = "episode"
    cont = contItems()
    if cont.Count() > 0
        n = root.createChild("ItemData")
        n.key = "__cont"
        n.title = "Continuar assistindo"
        n.icon = "ic_resume"
        n.count = cont.Count().ToStr()
        m.catNodes["__cont"] = n
    end if
    mine = mineItems()
    n = root.createChild("ItemData")
    n.key = "__mine"
    n.title = "Minha lista"
    n.icon = "ic_bookmark"
    n.count = mine.Count().ToStr()
    m.catNodes["__mine"] = n
    for each c in m.catData
        n = root.createChild("ItemData")
        n.key = asText(c.category_id)
        n.title = asText(c.category_name)
        m.catNodes[n.key] = n
    end for
    m.cats.content = root
    m.catCount.text = m.catData.Count().ToStr()
    if root.getChildCount() > 0
        first = root.getChild(0).key
        i = 0
        while i < root.getChildCount() - 1 and (root.getChild(i).key = "__cont" or root.getChild(i).key = "__mine")
            i = i + 1
        end while
        first = root.getChild(i).key
        choose(first, false)
    else
        m.status.text = "Nenhuma categoria"
    end if
end sub

' Títulos guardados com "Minha lista"
function mineItems() as object
    out = []
    want = "movie"
    if m.series then want = "series"
    for each p in regRead("mylist")
        if p.kind = want
            if m.series
                out.Push({ series_id: p.id, name: p.name, cover: p.icon, rating: p.rating })
            else
                out.Push({ stream_id: p.id, name: p.name, stream_icon: p.icon, container_extension: p.ext, rating: p.rating })
            end if
        end if
    end for
    return out
end function

function contItems() as object
    out = []
    seen = {}
    for each p in regRead("progress")
        if p.kind = m.contKind
            if m.series
                if p.seriesId <> invalid and not seen.DoesExist(p.seriesId)
                    seen[p.seriesId] = true
                    out.Push({ series_id: p.seriesId, name: p.seriesName, cover: p.image })
                end if
            else
                out.Push({ stream_id: Mid(p.key, 4), name: p.title, stream_icon: p.image, container_extension: "" , url: p.url })
            end if
        end if
    end for
    return out
end function

sub choose(key as string, focusGrid as boolean)
    if m.catNodes.DoesExist(m.catKey) then m.catNodes[m.catKey].selected = false
    m.catKey = key
    m.catNodes[key].selected = true
    m.secTitle.text = m.catNodes[key].title
    m.moveRight = focusGrid
    m.items = []
    m.grid.content = CreateObject("roSGNode", "ContentNode")
    m.secCount.text = ""
    if key = "__cont"
        showItems(contItems())
        return
    end if
    if key = "__mine"
        showItems(mineItems())
        return
    end if
    m.status.text = "Carregando…"
    m.seq = m.seq + 1
    action = "get_vod_streams"
    if m.series then action = "get_series"
    callApi("items" + m.seq.ToStr(), apiUrl(action, "&category_id=" + esc(key)))
end sub

sub showItems(data as object)
    m.items = data
    root = CreateObject("roSGNode", "ContentNode")
    for each it in data
        n = root.createChild("ItemData")
        n.title = asText(it.name)
        if m.series then n.HDPOSTERURL = asText(it.cover) else n.HDPOSTERURL = asText(it.stream_icon)
        r = asNum(it.rating)
        if r > 0 then n.stars = asText(Int(r * 10) / 10) else n.stars = ""
    end for
    m.grid.content = root
    if data.Count() = 1 then m.secCount.text = "1 título" else m.secCount.text = fmtCount(data.Count()) + " títulos"
    m.catNodes[m.catKey].count = fmtCount(data.Count())
    if data.Count() = 0
        m.status.text = "Nada nesta categoria"
        return
    end if
    m.status.text = ""
    hero(0)
    if m.moveRight = true
        m.focusList = m.grid
        m.grid.setFocus(true)
    end if
end sub

sub onCat()
    n = m.cats.content.getChild(m.cats.itemSelected)
    choose(n.key, true)
end sub

sub onItemFocus()
    hero(m.grid.itemFocused)
end sub

' Destaque: nome, ano, nota, gênero, sinopse e capa ao fundo.
sub hero(i as integer)
    if i < 0 or i >= m.items.Count() then return
    it = m.items[i]
    m.heroIdx = i
    m.title.text = asText(it.name)
    if m.series
        bd = it.backdrop_path
        img = asText(it.cover)
        if type(bd) = "roArray" and bd.Count() > 0 then img = asText(bd[0])
        m.backdrop.uri = img
        m.plot.text = asText(it.plot)
        setChips([Left(asText(it.releaseDate), 4)], asNum(it.rating), asText(it.genre))
    else
        m.backdrop.uri = asText(it.stream_icon)
        m.plot.text = ""
        setChips([], asNum(it.rating), "")
        t = m.top.findNode("heroTimer")
        t.control = "stop"
        t.control = "start"
    end if
end sub

sub loadHero()
    if m.series or m.heroIdx = invalid or m.heroIdx >= m.items.Count() then return
    it = m.items[m.heroIdx]
    m.heroSeq = m.heroSeq + 1
    callApi("hero" + m.heroSeq.ToStr(), apiUrl("get_vod_info", "&vod_id=" + asText(it.stream_id)))
end sub

sub fillMovieHero(info as object)
    m.plot.text = asText(info.plot)
    bd = info.backdrop_path
    if type(bd) = "roArray" and bd.Count() > 0 and asText(bd[0]) <> "" then m.backdrop.uri = asText(bd[0])
    chips = [Left(asText(info.releasedate), 4), fmtDuration(Int(asNum(info.duration_secs)))]
    setChips(chips, asNum(info.rating), asText(info.genre))
end sub

sub setChips(texts as object, rating as float, genre as string)
    m.chips.removeChildrenIndex(m.chips.getChildCount(), 0)
    x = 496
    for each t in texts
        if t <> "" and t <> "0"
            w = Len(t) * 11 + 26
            addChip(m.chips, "pkg:/images/chip.9.png", x, 128, w, 34)
            addLabel(m.chips, t, x, 134, ISEM(), 17, "0xFFFFFFFF", w, "center")
            x = x + w + 12
        end if
    end for
    if rating > 0
        txt = "★ " + asText(Int(rating * 10) / 10)
        w = Len(txt) * 11 + 26
        addChip(m.chips, "pkg:/images/chip_star.9.png", x, 128, w, 34)
        addLabel(m.chips, txt, x, 134, ISEM(), 17, "0xFBBF24FF", w, "center")
        x = x + w + 12
    end if
    if genre <> "" then addLabel(m.chips, genre, x + 4, 134, IREG(), 17, "0x9D9ABBFF", 700)
end sub

sub addChip(parent as object, uri as string, x as integer, y as integer, w as integer, h as integer)
    addPoster(parent, uri, x, y, w, h)
end sub

sub onItem()
    it = m.items[m.grid.itemSelected]
    if m.series
        m.top.action = { type: "open", screen: "SeriesScreen", params: { id: asText(it.series_id), name: asText(it.name), cover: asText(it.cover), rating: asText(it.rating) } }
    else
        m.top.action = { type: "open", screen: "MovieDetail", params: { id: asText(it.stream_id), name: asText(it.name), icon: asText(it.stream_icon), ext: asText(it.container_extension), rating: asText(it.rating) } }
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "right" and m.cats.hasFocus() and m.items.Count() > 0
        m.focusList = m.grid
        m.grid.jumpToItem = 0
        m.grid.setFocus(true)
        return true
    else if key = "left" and m.grid.hasFocus()
        m.focusList = m.cats
        m.cats.setFocus(true)
        return true
    end if
    return false
end function
