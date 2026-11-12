' Tela de canais igual à do Android: categorias | canais com logo e estrela | prévia ao vivo + guia de hoje.
sub init()
    m.cats = m.top.findNode("cats")
    m.list = m.top.findNode("channels")
    m.pv = m.top.findNode("pv")
    m.pvLogo = m.top.findNode("pvLogo")
    lb = m.top.findNode("labels")
    addLabel(lb, "CATEGORIAS", 72, 60, ISEM(), 17, "0x9D9ABBFF")
    m.catCount = addLabel(lb, "", 300, 60, ISEM(), 17, "0x9D9ABBFF", 116, "right")
    m.chTitle = addLabel(lb, "", 482, 60, ISEM(), 17, "0x9D9ABBFF", 420)
    addLabel(lb, "✱ favoritar", 900, 60, IMED(), 15, "0x6E6B8CFF", 170, "right")
    m.status = addLabel(lb, "Carregando canais…", 466, 500, IMED(), 22, "0x9D9ABBFF", 616, "center")
    ' prévia
    info = m.top.findNode("pvInfo")
    m.shade = addPoster(info, "pkg:/images/preview_shade.png", 1110, 262, 760, 200)
    m.live = addPoster(info, "pkg:/images/live_badge.png", 1132, 60, 108, 34)
    m.live.visible = false
    m.pvTitle = addLabel(info, "", 1136, 316, OUTF(), 30, "0xFFFFFFFF", 708)
    m.pvNow = addLabel(info, "", 1136, 360, IREG(), 20, "0xE7E5F7FF", 708)
    m.pvTrack = addRect(info, 1136, 396, 708, 5, "0x33FFFFFF")
    m.pvBar = addRect(info, 1136, 396, 0, 5, "0x22D3EEFF")
    m.pvNext = addLabel(info, "", 1136, 410, IREG(), 18, "0x9D9ABBFF", 708)
    ' guia
    g = m.top.findNode("epg")
    addLabel(g, "PROGRAMAÇÃO", 1134, 506, ISEM(), 17, "0x9D9ABBFF")
    addLabel(g, "HOJE", 1700, 506, ISEM(), 17, "0x9D9ABBFF", 146, "right")
    m.epgBox = g.createChild("Group")
    m.cats.observeField("itemSelected", "onCat")
    m.list.observeField("itemFocused", "onChFocus")
    m.list.observeField("itemSelected", "onChannel")
    m.top.findNode("epgTimer").observeField("fire", "loadEpg")
    m.top.observeField("focusedChild", "onFocus")
    m.focusList = m.cats
    m.items = []
    m.shown = []
    m.catId = "__all"
    m.seq = 0
    m.playingId = ""
    t = CreateObject("roSGNode", "LiveTask")
    t.observeField("response", "onLive")
    t.control = "RUN"
    m.task = t
end sub

sub cleanup(p = invalid as dynamic)
    m.pv.control = "stop"
end sub

sub onFocus()
    if m.top.hasFocus() then m.focusList.setFocus(true)
end sub

sub onLive(event as object)
    r = event.getData()
    if not r.ok
        m.status.text = "Não foi possível carregar os canais."
        return
    end if
    m.items = r.items
    m.catData = r.cats
    buildCats()
    choose("__all", false)
end sub

function favKeys() as object
    out = {}
    for each f in regRead("favs")
        out[f.key] = true
    end for
    return out
end function

sub buildCats()
    root = CreateObject("roSGNode", "ContentNode")
    m.catNodes = {}
    fixed = [["__fav", "Favoritos", "ic_star", regRead("favs").Count()], ["__recent", "Recentes", "ic_history", regRead("recents").Count()], ["__all", "Todos os canais", "ic_livetv", m.items.Count()]]
    for each f in fixed
        n = root.createChild("ItemData")
        n.key = f[0]
        n.title = f[1]
        n.icon = f[2]
        n.count = fmtCount(f[3])
        m.catNodes[f[0]] = n
    end for
    for each c in m.catData
        n = root.createChild("ItemData")
        n.key = c.id
        n.title = c.name
        n.count = fmtCount(c.count)
        m.catNodes[c.id] = n
    end for
    m.cats.content = root
    m.catCount.text = m.catData.Count().ToStr()
    m.cats.jumpToItem = 2
end sub

' Mostra os canais da categoria (só muda ao apertar OK, como no Android).
sub choose(id as string, focusList as boolean)
    if m.catNodes.DoesExist(m.catId) then m.catNodes[m.catId].selected = false
    m.catId = id
    if m.catNodes.DoesExist(id) then m.catNodes[id].selected = true
    favs = favKeys()
    if id = "__fav"
        src = regRead("favs")
    else if id = "__recent"
        src = regRead("recents")
    else
        src = []
        for each it in m.items
            if id = "__all" or it.cat = id then src.Push(it)
        end for
    end if
    m.shown = []
    root = CreateObject("roSGNode", "ContentNode")
    for each it in src
        k = it.id
        if k = invalid then k = it.key
        n = root.createChild("ItemData")
        n.key = k
        n.title = it.name
        n.num = asText(it.num)
        n.logo = asText(it.logo)
        n.fav = favs.DoesExist(k)
        m.shown.Push({ id: k, name: it.name, num: asText(it.num), logo: asText(it.logo) })
    end for
    m.list.content = root
    m.chTitle.text = UCase(m.catNodes[id].title) + "   " + fmtCount(m.shown.Count())
    if m.shown.Count() = 0
        if id = "__fav" then m.status.text = "Aperte ✱ num canal para favoritar." else m.status.text = "Nenhum canal"
    else
        m.status.text = ""
    end if
    if focusList and m.shown.Count() > 0
        m.list.jumpToItem = 0
        m.focusList = m.list
        m.list.setFocus(true)
    end if
    if m.shown.Count() > 0 then showChannel(0)
end sub

sub onCat()
    n = m.cats.content.getChild(m.cats.itemSelected)
    choose(n.key, true)
end sub

sub onChFocus()
    showChannel(m.list.itemFocused)
end sub

sub showChannel(i as integer)
    if i < 0 or i >= m.shown.Count() then return
    c = m.shown[i]
    m.cur = c
    m.pvTitle.text = c.num + " · " + c.name
    if c.num = "" then m.pvTitle.text = c.name
    if m.playingId <> c.id
        m.pvLogo.uri = c.logo
        m.pvLogo.visible = true
    end if
    m.pvNow.text = ""
    m.pvNext.text = ""
    m.pvBar.width = 0
    m.epgBox.removeChildrenIndex(m.epgBox.getChildCount(), 0)
    t = m.top.findNode("epgTimer")
    t.control = "stop"
    t.control = "start"
end sub

sub loadEpg()
    if m.cur = invalid then return
    m.seq = m.seq + 1
    callApi("epg" + m.seq.ToStr(), apiUrl("get_short_epg", "&stream_id=" + m.cur.id + "&limit=10"))
end sub

sub onApi(event as object)
    r = event.getData()
    if r.id <> "epg" + m.seq.ToStr() then return
    if not r.ok or type(r.data) <> "roAssociativeArray" or type(r.data.epg_listings) <> "roArray" then return
    now = nowSecs()
    progs = []
    for each e in r.data.epg_listings
        s = Int(asNum(e.start_timestamp))
        en = Int(asNum(e.stop_timestamp))
        if en > now then progs.Push({ title: b64(asText(e.title)), start: s, stop: en })
    end for
    y = 548
    shown = 0
    for each p in progs
        if shown >= 6 then exit for
        isNow = p.start <= now and now < p.stop
        if isNow
            addPoster(m.epgBox, "pkg:/images/epg_now.png", 1124, y, 732, 76)
            addPoster(m.epgBox, "pkg:/images/dot_green.png", 1146, y + 30, 16, 16)
            addLabel(m.epgBox, "AGORA", 1764, y + 25, ISEM(), 14, "0x22D3EEFF", 76, "center")
            m.pvNow.text = p.title + " · " + hhmmOf(p.start) + " – " + hhmmOf(p.stop)
            m.pvBar.width = Int(708 * (now - p.start) / (p.stop - p.start))
        else
            addPoster(m.epgBox, "pkg:/images/epg_row.png", 1124, y + 4, 732, 68)
            addPoster(m.epgBox, "pkg:/images/dot_white.png", 1146, y + 30, 16, 16)
            if m.pvNext.text = "" then m.pvNext.text = hhmmOf(p.start) + "  " + p.title
        end if
        addLabel(m.epgBox, hhmmOf(p.start) + " – " + hhmmOf(p.stop), 1180, y + 24, IMED(), 20, "0x9D9ABBFF", 160)
        tl = addLabel(m.epgBox, p.title, 1350, y + 22, IMED(), 22, "0xFFFFFFFF", 400)
        tl.maxLines = 1
        y = y + 82
        shown = shown + 1
    end for
    if shown = 0 then addLabel(m.epgBox, "Sem programação disponível", 1124, 760, IMED(), 20, "0x6E6B8CFF", 732, "center")
end sub

sub addRecent(c as object)
    regPush("recents", { key: c.id, id: c.id, name: c.name, num: c.num, logo: c.logo }, 30)
end sub

' 1º OK: toca na prévia. 2º OK no mesmo canal: tela cheia.
sub onChannel()
    c = m.shown[m.list.itemSelected]
    url = streamUrl("live", c.id, "m3u8")
    if m.playingId = c.id
        m.pv.control = "stop"
        m.pv.visible = false
        m.live.visible = false
        m.playingId = ""
        m.top.action = { type: "play", url: url, title: c.name, format: "hls", live: true }
        return
    end if
    addRecent(c)
    v = CreateObject("roSGNode", "ContentNode")
    v.url = url
    v.streamFormat = "hls"
    v.live = true
    m.pv.content = v
    m.pv.visible = true
    m.pv.control = "play"
    m.pvLogo.visible = false
    m.live.visible = true
    m.playingId = c.id
    m.list.setFocus(true)
end sub

sub toggleFav()
    i = m.list.itemFocused
    if i < 0 or i >= m.shown.Count() then return
    c = m.shown[i]
    n = m.list.content.getChild(i)
    if regFind("favs", c.id) <> invalid
        regRemove("favs", c.id)
        n.fav = false
    else
        regPush("favs", { key: c.id, id: c.id, name: c.name, num: c.num, logo: c.logo }, 500)
        n.fav = true
    end if
    m.catNodes["__fav"].count = fmtCount(regRead("favs").Count())
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "right" and m.cats.hasFocus() and m.shown.Count() > 0
        m.focusList = m.list
        m.list.jumpToItem = 0
        m.list.setFocus(true)
        return true
    else if key = "left" and m.list.hasFocus()
        m.focusList = m.cats
        m.cats.setFocus(true)
        return true
    else if key = "options" and m.list.hasFocus()
        toggleFav()
        return true
    end if
    return false
end function
