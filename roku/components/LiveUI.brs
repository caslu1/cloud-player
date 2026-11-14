' Interface do canal ao vivo em tela cheia, igual ao Android:
' - informações (canal, AO VIVO, qualidade, programa de agora com progresso, a seguir e botões UHD/FHD/HD/SD)
' - menu lateral com CATEGORIAS e CANAIS
sub init()
    m.ov = m.top.findNode("ov")
    m.panel = m.top.findNode("panel")
    m.pCats = m.top.findNode("pCats")
    m.pChs = m.top.findNode("pChs")
    m.pCats.observeField("itemSelected", "onPickCat")
    m.pChs.observeField("itemSelected", "onPickCh")
    m.pChs.observeField("itemFocused", "onChFocus")
    m.top.findNode("ovTimer").observeField("fire", "autoHide")
    m.top.findNode("epgDelay").observeField("fire", "loadPanelEpg")
    t = m.top.findNode("clock")
    t.observeField("fire", "tickClock")
    t.control = "start"
    m.epg = {}
    m.qIdx = 0
    m.seq = 0
    pl = m.top.findNode("panelLabels")
    addLabel(pl, "CATEGORIAS", 64, 44, ISEM(), 18, "0x9D9ABBFF")
    m.pTitle = addLabel(pl, "", 472, 44, ISEM(), 18, "0x9D9ABBFF", 500)
    m.pCount = addLabel(pl, "", 980, 44, ISEM(), 18, "0x9D9ABBFF", 200, "right")
    buildOverlay()
end sub

' ------------------------------------------------------------- informações
sub buildOverlay()
    o = m.ov
    addPoster(o, "pkg:/images/player_top.png", 0, 0, 1920, 160)
    m.oWhere = addLabel(o, "", 700, 42, ISEM(), 19, "0xFFFFFFE6", 1000, "right")
    m.oClock = addLabel(o, "", 1716, 30, OUTF(), 38, "0xFFFFFFFF", 140, "right")
    addPoster(o, "pkg:/images/player_shade.png", 0, 560, 1920, 520)
    m.oLogoBg = addPoster(o, "pkg:/images/logo_box.png", 64, 640, 104, 104)
    m.oAbbr = addLabel(o, "", 64, 680, ISEM(), 18, "0xFFFFFFFF", 104, "center")
    m.oLogo = addPoster(o, "", 70, 646, 92, 92)
    m.oLogo.loadDisplayMode = "scaleToFit"
    m.oNum = addLabel(o, "", 196, 646, IMED(), 24, "0xFFFFFFE6")
    m.oBadge = addPoster(o, "pkg:/images/live_badge.png", 0, 645, 108, 34)
    m.oQChip = addPoster(o, "pkg:/images/pill_w.9.png", 0, 647, 60, 30)
    m.oQChip.blendColor = "0xFFFFFF33"
    m.oQ = addLabel(o, "", 0, 650, ISEM(), 15, "0xFFFFFFFF", 60, "center")
    m.oName = addLabel(o, "", 196, 684, OUTF(), 44, "0xFFFFFFFF", 1500)
    m.oTime = addLabel(o, "", 64, 776, ISEM(), 20, "0x3D6BFFF2")
    m.oProg = addLabel(o, "", 230, 772, ISEM(), 26, "0xFFFFFFFF", 1500)
    m.oTrack = addRect(o, 64, 830, 1640, 6, "0xFFFFFF55")
    m.oBar = addRect(o, 64, 830, 0, 6, "0x3D6BFFFF")
    m.oLeft = addLabel(o, "", 1724, 818, ISEM(), 19, "0xFFFFFFFF", 132, "right")
    m.oNext = addLabel(o, "", 64, 856, IMED(), 19, "0xE7E5F7FF", 1600)
    ' botão Canais (grade): abre categorias e canais dentro do player; ◀ também abre
    m.chBtn = o.createChild("Group")
    m.chBtn.translation = [64, 916]
    m.chBg = addPoster(m.chBtn, "pkg:/images/qpill.9.png", 0, 0, 190, 72)
    m.chIc = addPoster(m.chBtn, "pkg:/images/ic_grid.png", 26, 22, 28, 28)
    m.chLb = addLabel(m.chBtn, "Canais", 66, 22, ISEM(), 21, "0xFFFFFFFF")
    addRect(o, 282, 930, 2, 44, "0xFFFFFF33")
    addLabel(o, "Qualidade", 306, 936, IREG(), 17, "0x9D9ABBFF")
    m.qBtns = []
    names = ["UHD", "FHD", "HD", "SD"]
    for i = 0 to 3
        g = o.createChild("Group")
        g.translation = [410 + i * 118, 916]
        bg = addPoster(g, "", 0, 0, 104, 72)
        lb = addLabel(g, names[i], 0, 14, ISEM(), 20, "0xFFFFFFFF", 104, "center")
        dot = addRect(g, 48, 50, 8, 8, "0x3D6BFFFF")
        m.qBtns.Push({ q: names[i], bg: bg, label: lb, dot: dot })
    end for
    m.oWatch = addLabel(o, "", 892, 941, ISEM(), 19, "0xFFFFFFFF", 800)
end sub

function curCh() as object
    d = m.top.data
    if d = invalid or d.list = invalid or d.idx = invalid or d.idx >= d.list.Count() then return invalid
    return d.list[d.idx]
end function

function catName(id as string) as string
    d = m.top.data
    if id = "__fav" then return "Favoritos"
    if id = "__recent" then return "Recentes"
    if id = "__all" or id = "" then return "Todos os canais"
    for each c in d.cats
        if c.id = id then return c.name
    end for
    return ""
end function

sub tickClock()
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    m.oClock.text = two(d.GetHours()) + ":" + two(d.GetMinutes())
end sub

' Desenha as informações do canal atual (chamado pelo MainScene ao abrir/trocar)
sub refresh(p = invalid as dynamic)
    c = curCh()
    if c = invalid then return
    d = m.top.data
    tickClock()
    where = "Canais"
    cn = catName(asText(c.cat))
    if cn <> "" then where = where + "  •  " + cn
    if d.list.Count() > 1 then where = where + "  ·  " + (d.idx + 1).ToStr() + " de " + fmtCount(d.list.Count())
    m.oWhere.text = where
    m.oNum.text = c.num
    m.oName.text = c.name
    m.oLogo.uri = c.logo
    m.oLogoBg.blendColor = "0x1A1D33FF"
    if c.logo = "" then m.oAbbr.text = UCase(Left(c.name, 5)) else m.oAbbr.text = ""
    x = 196 + Len(c.num) * 14 + 16
    if c.num = "" then x = 196
    m.oBadge.translation = [x, 645]
    q = asText(c.q)
    m.oQChip.visible = q <> ""
    m.oQ.visible = q <> ""
    m.oQChip.translation = [x + 120, 647]
    m.oQ.translation = [x + 120, 653]
    m.oQ.text = q
    ' qualidades disponíveis
    m.avail = {}
    if d.variants <> invalid and c.vkey <> invalid and d.variants.DoesExist(c.vkey) then m.avail = d.variants[c.vkey]
    m.qIdx = 2
    for i = 0 to 3
        if m.qBtns[i].q = q then m.qIdx = i
    end for
    if q = ""
        for i = 0 to 3
            if m.avail.DoesExist(m.qBtns[i].q) then
                m.qIdx = i
                exit for
            end if
        end for
    end if
    if q <> "" then m.oWatch.text = q + " · Assistindo agora" else m.oWatch.text = "Assistindo agora"
    paintQ()
    showProg(c)
    if not m.epg.DoesExist(c.id) or m.epg[c.id].full <> true then loadEpg([c.id], true)
end sub

sub paintQ()
    c = curCh()
    q = ""
    if c <> invalid then q = asText(c.q)
    cf = (m.top.mode = "overlay" and m.qIdx = -1)
    if cf then m.chBg.uri = "pkg:/images/qpill_sel.9.png" else m.chBg.uri = "pkg:/images/qpill.9.png"
    if cf then m.chLb.color = "0x14172BFF" else m.chLb.color = "0xFFFFFFFF"
    if cf then m.chIc.blendColor = "0x14172BFF" else m.chIc.blendColor = "0xFFFFFFFF"
    for i = 0 to 3
        b = m.qBtns[i]
        sel = (b.q = q)
        av = m.avail.DoesExist(b.q)
        f = (m.top.mode = "overlay" and i = m.qIdx)
        if sel
            b.bg.uri = "pkg:/images/qpill_sel.9.png"
            b.label.color = "0x14172BFF"
        else if f
            b.bg.uri = "pkg:/images/qpill_f.9.png"
            b.label.color = "0xFFFFFFFF"
        else
            b.bg.uri = "pkg:/images/qpill.9.png"
            if av then b.label.color = "0xFFFFFFFF" else b.label.color = "0xFFFFFF66"
        end if
        if f and sel then b.bg.uri = "pkg:/images/qpill_sel_f.9.png"
        b.dot.visible = sel or av
        if sel then b.dot.color = "0x3D6BFFFF" else b.dot.color = "0xFFFFFF99"
    end for
end sub

sub showProg(c as object)
    e = invalid
    if m.epg.DoesExist(c.id) then e = m.epg[c.id]
    now = nowSecs()
    if e <> invalid and e.now <> invalid
        n = e.now
        m.oTime.text = hhmmOf(n.start) + " – " + hhmmOf(n.stop)
        m.oProg.text = n.title
        m.oProg.translation = [64 + Len(m.oTime.text) * 11 + 24, 772]
        frac = (now - n.start) / (n.stop - n.start)
        if frac < 0 then frac = 0
        if frac > 1 then frac = 1
        m.oBar.width = Int(1640 * frac)
        left = Int((n.stop - now) / 60)
        if left < 0 then left = 0
        m.oLeft.text = "faltam " + left.ToStr() + " min"
        m.oTrack.visible = true
        m.oBar.visible = true
        if e.nxt <> invalid then m.oNext.text = "A seguir     " + hhmmOf(e.nxt.start) + "     " + e.nxt.title else m.oNext.text = ""
    else
        m.oTime.text = ""
        cn = catName(asText(c.cat))
        if cn = "" then cn = "Ao vivo"
        m.oProg.text = cn
        m.oProg.translation = [64, 772]
        m.oTrack.visible = false
        m.oBar.visible = false
        m.oLeft.text = ""
        m.oNext.text = ""
    end if
end sub

' Guia (agora e a seguir) — busca em segundo plano
sub loadEpg(ids as object, full as boolean)
    t = CreateObject("roSGNode", "EpgTask")
    t.ids = ids
    t.full = full
    t.observeField("response", "onEpg")
    t.control = "RUN"
    if m.tasks = invalid then m.tasks = []
    m.tasks.Push(t)
    if m.tasks.Count() > 6 then m.tasks.Shift()
end sub

sub onEpg(event as object)
    r = event.getData()
    for each id in r
        m.epg[id] = r[id]
    end for
    c = curCh()
    if c <> invalid and r.DoesExist(c.id) then showProg(c)
    if m.panel.visible and m.pChs.content <> invalid
        root = m.pChs.content
        for i = 0 to root.getChildCount() - 1
            n = root.getChild(i)
            if r.DoesExist(n.key) then setRowEpg(n, r[n.key])
        end for
    end if
end sub

sub setRowEpg(n as object, e as object)
    if e = invalid or e.now = invalid then return
    n.prog = hhmmOf(e.now.start) + " · " + e.now.title
    now = nowSecs()
    f = (now - e.now.start) / (e.now.stop - e.now.start)
    if f < 0 then f = 0
    if f > 1 then f = 1
    n.frac = f
end sub

sub onMode()
    md = m.top.mode
    m.ov.visible = (md = "overlay")
    m.panel.visible = (md = "panel")
    t = m.top.findNode("ovTimer")
    t.control = "stop"
    if md = "overlay"
        refresh()
        t.control = "start"
        m.top.setFocus(true)
    else if md = "panel"
        openPanel()
    end if
end sub

sub autoHide()
    if m.top.mode = "overlay" then m.top.action = { type: "hide" }
end sub

' ------------------------------------------------------------- menu lateral
function catList() as object
    d = m.top.data
    out = [{ id: "__fav", name: "Favoritos", icon: "ic_star" }, { id: "__recent", name: "Recentes", icon: "ic_history" }, { id: "__all", name: "Todos os canais", icon: "ic_livetv" }]
    for each c in d.cats
        out.Push({ id: c.id, name: c.name, icon: "" })
    end for
    return out
end function

function channelsOf(id as string) as object
    d = m.top.data
    if id = "__fav" then return regRead("favs")
    if id = "__recent" then return regRead("recents")
    out = []
    for each it in d.all
        if id = "__all" or it.cat = id then out.Push(it)
    end for
    return out
end function

sub openPanel()
    d = m.top.data
    m.cats = catList()
    m.pCat = d.catId
    if m.pCat = invalid or m.pCat = "" then m.pCat = "__all"
    root = CreateObject("roSGNode", "ContentNode")
    ci = 0
    for i = 0 to m.cats.Count() - 1
        c = m.cats[i]
        n = root.createChild("ItemData")
        n.key = c.id
        n.title = c.name
        n.icon = c.icon
        n.selected = (c.id = m.pCat)
        if c.id = m.pCat then ci = i
    end for
    m.pCats.content = root
    m.pCats.jumpToItem = ci
    showPanelList(d.list, curCh())
    m.pChs.setFocus(true)
end sub

sub showPanelList(list as object, cur as object)
    m.pList = list
    m.pTitle.text = UCase(catName(m.pCat))
    m.pCount.text = fmtCount(list.Count()) + " CANAIS"
    favs = {}
    for each f in regRead("favs")
        favs[f.key] = true
    end for
    root = CreateObject("roSGNode", "ContentNode")
    ci = 0
    for i = 0 to list.Count() - 1
        c = list[i]
        id = c.id
        if id = invalid then id = c.key
        n = root.createChild("ItemData")
        n.key = id
        n.title = c.name
        n.num = asText(c.num)
        n.logo = asText(c.logo)
        n.icon = "panel"
        n.fav = favs.DoesExist(id)
        n.playing = (cur <> invalid and cur.id = id)
        if n.playing then ci = i
        if m.epg.DoesExist(id) then setRowEpg(n, m.epg[id])
    end for
    m.pChs.content = root
    m.pChs.jumpToItem = ci
    loadPanelEpg()
end sub

sub onChFocus()
    t = m.top.findNode("epgDelay")
    t.control = "stop"
    t.control = "start"
end sub

sub loadPanelEpg()
    if m.pList = invalid then return
    i0 = m.pChs.itemFocused - 4
    if i0 < 0 then i0 = 0
    ids = []
    for i = i0 to i0 + 12
        if i < m.pList.Count()
            id = m.pList[i].id
            if id = invalid then id = m.pList[i].key
            if not m.epg.DoesExist(id) then ids.Push(id)
        end if
    end for
    if ids.Count() > 0 then loadEpg(ids, false)
end sub

sub onPickCat()
    c = m.cats[m.pCats.itemSelected]
    root = m.pCats.content
    for i = 0 to root.getChildCount() - 1
        root.getChild(i).selected = (root.getChild(i).key = c.id)
    end for
    m.pCat = c.id
    showPanelList(channelsOf(c.id), curCh())
    if m.pList.Count() > 0
        m.pChs.jumpToItem = 0
        m.pChs.setFocus(true)
    end if
end sub

sub onPickCh()
    i = m.pChs.itemSelected
    m.top.action = { type: "pick", list: m.pList, idx: i, catId: m.pCat }
end sub

' ------------------------------------------------------------- teclas
function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    md = m.top.mode
    if md = "overlay"
        t = m.top.findNode("ovTimer")
        t.control = "stop"
        t.control = "start"
        if key = "left" and m.qIdx > -1
            m.qIdx = m.qIdx - 1
            paintQ()
            return true
        else if key = "right" and m.qIdx < 3
            m.qIdx = m.qIdx + 1
            paintQ()
            return true
        else if key = "OK" and m.qIdx = -1
            m.top.mode = "panel"
            return true
        else if key = "OK"
            b = m.qBtns[m.qIdx]
            c = curCh()
            if m.avail.DoesExist(b.q) and c <> invalid and m.avail[b.q] <> c.id
                m.top.action = { type: "quality", id: m.avail[b.q], q: b.q }
            end if
            return true
        else if key = "back"
            m.top.action = { type: "hide" }
            return true
        else if key = "up" or key = "down"
            m.top.action = { type: "zap", dir: key }
            return true
        end if
        return true
    else if md = "panel"
        if key = "left" and m.pChs.hasFocus()
            m.pCats.setFocus(true)
            return true
        else if key = "right" and m.pCats.hasFocus()
            m.pChs.setFocus(true)
            return true
        else if key = "right" or key = "back"
            m.top.action = { type: "hide" }
            return true
        else if key = "left"
            return true
        end if
    end if
    return false
end function
