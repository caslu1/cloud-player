' Série igual ao Android: informações e botão à esquerda; temporadas e episódios à direita.
sub init()
    r = m.top.findNode("root")
    m.kind = addLabel(r, "SÉRIE", 48, 128, ISEM(), 17, "0x22D3EEFF")
    m.title = addLabel(r, "", 48, 154, OUTF(), 56, "0xFFFFFFFF", 740)
    m.title.wrap = true
    m.title.maxLines = 2
    m.chips = r.createChild("Group")
    m.plot = addLabel(r, "", 48, 296, IREG(), 20, "0xE7E5F7FF", 720)
    m.plot.wrap = true
    m.plot.maxLines = 4
    m.btnBox = r.createChild("Group")
    m.status = addLabel(r, "Carregando episódios…", 828, 500, IMED(), 22, "0x9D9ABBFF", 1028, "center")
    m.seasonBox = m.top.findNode("seasons")
    m.epCount = addLabel(m.seasonBox, "", 1500, 76, IREG(), 18, "0x9D9ABBFF", 346, "right")
    m.chipBox = m.seasonBox.createChild("Group")
    m.eps = m.top.findNode("eps")
    m.eps.observeField("itemSelected", "onEpisode")
    m.top.observeField("focusedChild", "onFocus")
    m.zone = "btn"
    m.seasonKeys = []
    m.sIdx = 0
    m.cur = []
    m.buttons = []
end sub

sub onParams()
    m.title.text = m.top.params.name
    callApi("info", apiUrl("get_series_info", "&series_id=" + esc(m.top.params.id)))
end sub

sub onFocus()
    if m.top.hasFocus()
        if m.data <> invalid
            showSeason(m.sIdx)
            buildButtons()
        end if
        applyFocus()
    end if
end sub

sub onApi(event as object)
    r = event.getData()
    if not r.ok or type(r.data) <> "roAssociativeArray"
        m.status.text = "Não foi possível carregar os episódios."
        return
    end if
    info = r.data.info
    m.cover = ""
    if type(info) = "roAssociativeArray"
        m.plot.text = asText(info.plot)
        m.cover = asText(info.cover)
        bd = info.backdrop_path
        if type(bd) = "roArray" and bd.Count() > 0 then m.top.findNode("backdrop").uri = asText(bd[0]) else m.top.findNode("backdrop").uri = m.cover
        setChips([Left(asText(info.releaseDate), 4)], asNum(info.rating), asText(info.genre))
    end if
    m.data = r.data.episodes
    if type(m.data) <> "roAssociativeArray"
        m.status.text = "Nenhum episódio disponível"
        return
    end if
    keys = []
    for each k in m.data
        keys.Push(k)
    end for
    for i = 0 to keys.Count() - 1
        for j = i + 1 to keys.Count() - 1
            if Val(keys[j]) < Val(keys[i])
                t = keys[i]
                keys[i] = keys[j]
                keys[j] = t
            end if
        end for
    end for
    m.seasonKeys = keys
    if keys.Count() = 1 then m.kind.text = "SÉRIE · 1 TEMPORADA" else m.kind.text = "SÉRIE · " + keys.Count().ToStr() + " TEMPORADAS"
    m.status.text = ""
    ' abre na temporada do último episódio assistido
    last = lastProgress()
    m.sIdx = 0
    if last <> invalid
        for i = 0 to keys.Count() - 1
            for each e in m.data[keys[i]]
                if "ep:" + asText(e.id) = last.key then m.sIdx = i
            end for
        end for
    end if
    showSeason(m.sIdx)
    buildButtons()
    applyFocus()
end sub

function lastProgress() as object
    for each p in regRead("progress")
        if p.seriesId = m.top.params.id then return p
    end for
    return invalid
end function

sub setChips(texts as object, rating as float, genre as string)
    m.chips.removeChildrenIndex(m.chips.getChildCount(), 0)
    y = 238
    if Len(m.title.text) > 22 then y = 306
    m.plot.translation = [48, y + 58]
    m.btnY = y + 58 + 140
    x = 48
    for each t in texts
        if t <> "" and t <> "0"
            w = Len(t) * 12 + 30
            addPoster(m.chips, "pkg:/images/chip.9.png", x, y, w, 40)
            addLabel(m.chips, t, x, y + 8, ISEM(), 19, "0xFFFFFFFF", w, "center")
            x = x + w + 12
        end if
    end for
    if rating > 0
        txt = "★ " + asText(Int(rating * 10) / 10)
        w = Len(txt) * 12 + 30
        addPoster(m.chips, "pkg:/images/chip_star.9.png", x, y, w, 40)
        addLabel(m.chips, txt, x, y + 8, ISEM(), 19, "0xFBBF24FF", w, "center")
        x = x + w + 12
    end if
    if genre <> "" then addLabel(m.chips, genre, x + 4, y + 8, IREG(), 19, "0x9D9ABBFF", 400)
end sub

sub buildButtons()
    m.btnBox.removeChildrenIndex(m.btnBox.getChildCount(), 0)
    m.buttons = []
    if m.btnY = invalid then m.btnY = 436
    m.resume = invalid
    last = lastProgress()
    if last <> invalid
        m.resume = last
        b = addButton(m.btnBox, "Continuar " + asText(last.subtitle), "ic_play", true, 48, m.btnY)
    else
        first = firstEpisode()
        label = "Assistir"
        if first <> invalid then label = "Assistir T" + first.season + " · E" + asText(first.ep.episode_num)
        b = addButton(m.btnBox, label, "ic_play", true, 48, m.btnY)
    end if
    b.action = "play"
    m.buttons.Push(b)
    k = "sr:" + m.top.params.id
    inList = regFind("mylist", k) <> invalid
    if inList then t = "Na minha lista" else t = "Minha lista"
    if inList then ic = "ic_check" else ic = "ic_add"
    b2 = addButton(m.btnBox, t, ic, false, 48 + b.width + 16, m.btnY)
    b2.action = "list"
    m.buttons.Push(b2)
    if m.bIdx = invalid or m.bIdx >= m.buttons.Count() then m.bIdx = 0
end sub

function firstEpisode() as object
    if m.seasonKeys.Count() = 0 then return invalid
    k = m.seasonKeys[0]
    a = m.data[k]
    if type(a) <> "roArray" or a.Count() = 0 then return invalid
    return { season: k, ep: a[0] }
end function

sub showSeason(i as integer)
    if i < 0 or i >= m.seasonKeys.Count() then return
    m.sIdx = i
    m.cur = m.data[m.seasonKeys[i]]
    if type(m.cur) <> "roArray" then m.cur = []
    ' chips de temporada
    m.chipBox.removeChildrenIndex(m.chipBox.getChildCount(), 0)
    m.chipNodes = []
    x = 836
    for j = 0 to m.seasonKeys.Count() - 1
        t = "Temporada " + m.seasonKeys[j]
        w = Len(t) * 12 + 36
        if x + w > 1500 then exit for
        bg = addPoster(m.chipBox, "", x, 66, w, 44)
        lb = addLabel(m.chipBox, t, x, 76, ISEM(), 18, "0xFFFFFFFF", w, "center")
        m.chipNodes.Push({ bg: bg, label: lb })
        x = x + w + 10
    end for
    if m.cur.Count() = 1 then m.epCount.text = "1 episódio" else m.epCount.text = m.cur.Count().ToStr() + " episódios"
    prog = {}
    for each p in regRead("progress")
        prog[p.key] = p
    end for
    root = CreateObject("roSGNode", "ContentNode")
    for each e in m.cur
        n = root.createChild("ItemData")
        n.title = asText(e.title)
        n.num = asText(e.episode_num)
        ei = e.info
        meta = "Episódio " + asText(e.episode_num)
        if type(ei) = "roAssociativeArray"
            n.HDPOSTERURL = asText(ei.movie_image)
            n.plot = asText(ei.plot)
            d = asInt((ei.duration_secs))
            if d > 0 then meta = meta + " · " + Int(d / 60).ToStr() + " min"
        end if
        if n.HDPOSTERURL = "" then n.HDPOSTERURL = m.cover
        k = "ep:" + asText(e.id)
        if prog.DoesExist(k) and prog[k].dur > 0
            n.frac = prog[k].pos / prog[k].dur
            meta = meta + " · Faltam " + Int((prog[k].dur - prog[k].pos) / 60).ToStr() + " min"
        end if
        n.meta = meta
    end for
    m.eps.content = root
    paintChips()
end sub

sub paintChips()
    for j = 0 to m.chipNodes.Count() - 1
        c = m.chipNodes[j]
        f = (m.zone = "seasons" and j = m.sIdx)
        if j = m.sIdx then c.bg.uri = "pkg:/images/chip_on.9.png" else c.bg.uri = "pkg:/images/chip_off.9.png"
        if f then c.bg.uri = "pkg:/images/chip_focus.9.png"
        if j = m.sIdx or f then c.label.color = "0xFFFFFFFF" else c.label.color = "0x9D9ABBFF"
    end for
end sub

sub applyFocus()
    for i = 0 to m.buttons.Count() - 1
        paintButton(m.buttons[i], m.zone = "btn" and i = m.bIdx)
    end for
    if m.chipNodes <> invalid then paintChips()
    if m.zone = "eps" then m.eps.setFocus(true) else m.top.setFocus(true)
end sub

function epAction(e as object, season as string, start as integer) as object
    ext = asText(e.container_extension)
    if ext = "" then ext = "mp4"
    img = m.cover
    if type(e.info) = "roAssociativeArray" and asText(e.info.movie_image) <> "" then img = asText(e.info.movie_image)
    subtitle = "T" + season + " · E" + asText(e.episode_num)
    return { type: "play", key: "ep:" + asText(e.id), kind: "episode", title: m.top.params.name, subtitle: subtitle, image: img, url: streamUrl("series", asText(e.id), ext), format: formatFor(ext), live: false, start: start, seriesId: m.top.params.id, seriesName: m.top.params.name }
end function

sub playEpisode(e as object, start as integer)
    a = epAction(e, m.seasonKeys[m.sIdx], start)
    ' próximos episódios (para o "Próximo episódio automático")
    q = []
    found = false
    for si = 0 to m.seasonKeys.Count() - 1
        for each x in m.data[m.seasonKeys[si]]
            if found and q.Count() < 30 then q.Push(epAction(x, m.seasonKeys[si], 0))
            if asText(x.id) = asText(e.id) then found = true
        end for
    end for
    a.queue = q
    m.top.action = a
end sub

sub onEpisode()
    e = m.cur[m.eps.itemSelected]
    p = regFind("progress", "ep:" + asText(e.id))
    start = 0
    if p <> invalid then start = p.pos
    playEpisode(e, start)
end sub

sub pressButton()
    if m.resume <> invalid
        for i = 0 to m.seasonKeys.Count() - 1
            for each e in m.data[m.seasonKeys[i]]
                if "ep:" + asText(e.id) = m.resume.key
                    m.sIdx = i
                    playEpisode(e, m.resume.pos)
                    return
                end if
            end for
        end for
    end if
    f = firstEpisode()
    if f <> invalid
        m.sIdx = 0
        playEpisode(f.ep, 0)
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.data = invalid then return false
    if m.zone = "btn"
        if key = "right" and m.bIdx < m.buttons.Count() - 1
            m.bIdx = m.bIdx + 1
        else if key = "left" and m.bIdx > 0
            m.bIdx = m.bIdx - 1
        else if key = "right" and m.cur.Count() > 0
            m.zone = "eps"
            m.eps.jumpToItem = 0
        else if key = "OK"
            if m.buttons[m.bIdx].action = "list"
                k = "sr:" + m.top.params.id
                p = m.top.params
                cover = m.cover
                if cover = "" and p.cover <> invalid then cover = p.cover
                if regFind("mylist", k) <> invalid then regRemove("mylist", k) else regPush("mylist", { key: k, kind: "series", id: p.id, name: p.name, icon: cover, rating: p.rating }, 500)
                buildButtons()
                applyFocus()
            else
                pressButton()
            end if
            return true
        else
            return false
        end if
    else if m.zone = "seasons"
        if key = "left" and m.sIdx > 0
            showSeason(m.sIdx - 1)
        else if key = "right" and m.sIdx < m.chipNodes.Count() - 1
            showSeason(m.sIdx + 1)
        else if key = "left"
            m.zone = "btn"
        else if key = "down" and m.cur.Count() > 0
            m.zone = "eps"
            m.eps.jumpToItem = 0
        else if key = "OK"
            return true
        else
            return false
        end if
    else if m.zone = "eps"
        if key = "up" and m.eps.itemFocused = 0
            m.zone = "seasons"
        else if key = "left"
            m.zone = "btn"
        else
            return false
        end if
    end if
    applyFocus()
    return true
end function
