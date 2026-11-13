' Tela inicial "Padrão" do app Android: logo, ações no topo, saudação, 4 blocos e Continuar assistindo.
sub init()
    m.root = m.top.findNode("root")
    r = m.root
    addPoster(r, "pkg:/images/logo_full.png", 56, 26, 245, 52)
    ' ações do topo (recarregar e configurações)
    m.topBtns = []
    names = ["refresh", "settings"]
    for i = 0 to 1
        p = addPoster(r, "pkg:/images/rbtn_" + names[i] + "_n.png", 1636 + i * 68, 22, 52, 52)
        m.topBtns.Push({ poster: p, name: names[i] })
    end for
    m.clock = addLabel(r, "", 1664, 18, OUTF(), 30, "0xFFFFFFFF", 200, "right")
    m.date = addLabel(r, "", 1664, 58, IREG(), 15, "0x9D9ABBFF", 200, "right")
    ' saudação
    m.greet = addLabel(r, "", 56, 118, OUTF(), 44, "0xFFFFFFFF")
    addLabel(r, "O que vamos assistir hoje?", 56, 180, IREG(), 20, "0x9D9ABBFF")
    ' blocos
    m.items = [
        { key: "tv", label: "CANAIS", screen: "ChannelsScreen", params: {} },
        { key: "film", label: "FILMES", screen: "VodScreen", params: { kind: "movie" } },
        { key: "series", label: "SÉRIES", screen: "VodScreen", params: { kind: "series" } },
        { key: "settings", label: "AJUSTES", screen: "SettingsScreen", params: {} },
    ]
    m.subs = []
    m.tiles = []
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        g = r.createChild("Group")
        g.translation = [136 + i * 420, 268]
        g.scaleRotateCenter = [194, 138]
        bg = addPoster(g, "pkg:/images/tile_" + it.key + "_n.png", 0, 0, 388, 276)
        lb = addLabel(g, it.label, 28, 186, OUTF(), 30, "0xFFFFFFFF")
        sb = addLabel(g, "", 28, 230, IREG(), 16, "0x9D9ABBFF")
        m.tiles.Push({ group: g, bg: bg, subLbl: sb, key: it.key })
    end for
    m.tiles[3].subLbl.text = "Conta e preferências"
    m.tiles[0].subLbl.text = "Ao vivo"
    m.tiles[1].subLbl.text = "Filmes da sua lista"
    m.tiles[2].subLbl.text = "Séries da sua lista"
    ' Continuar assistindo
    addPoster(r, "pkg:/images/ic_resume.png", 56, 600, 26, 26)
    addLabel(r, "Continuar assistindo", 94, 594, OUTF(), 24, "0xFFFFFFFF")
    m.contCount = addLabel(r, "", 360, 600, ISEM(), 16, "0x6E6B8CFF")
    ' rodapé: MAC e chave do aparelho (usados no site de ativação)
    addLabel(r, "MAC: " + deviceMac() + "     Chave: " + deviceKey(), 1100, 1030, IREG(), 17, "0x6E6B8CFF", 760, "right")
    m.contBox = r.createChild("Group")
    m.contBox.translation = [56, 644]
    m.zone = "tiles"
    m.idx = 0
    m.topIdx = 1
    m.contIdx = 0
    m.cards = []
    t = m.top.findNode("timer")
    t.observeField("fire", "tick")
    t.control = "start"
    tick()
    buildContinue()
    paint()
    m.top.observeField("focusedChild", "onFocus")
end sub

sub onFocus()
    if m.top.hasFocus()
        buildContinue()
        paint()
    end if
end sub

sub tick()
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    m.clock.text = two(d.GetHours()) + ":" + two(d.GetMinutes())
    days = ["dom.", "seg.", "ter.", "qua.", "qui.", "sex.", "sáb."]
    months = ["jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."]
    m.date.text = days[d.GetDayOfWeek()] + ", " + d.GetDayOfMonth().ToStr() + " " + months[d.GetMonth() - 1]
    h = d.GetHours()
    if h < 5 or h >= 18
        m.greet.text = "Boa noite"
    else if h < 12
        m.greet.text = "Bom dia"
    else
        m.greet.text = "Boa tarde"
    end if
end sub

sub buildContinue()
    m.contBox.removeChildrenIndex(m.contBox.getChildCount(), 0)
    m.cards = []
    m.cont = regRead("progress")
    n = m.cont.Count()
    if n > 5 then n = 5
    if n = 0
        m.contCount.text = ""
        addLabel(m.contBox, "Comece um filme ou episódio e ele aparece aqui para você continuar de onde parou.", 0, 4, IREG(), 18, "0x9D9ABBFF")
        if m.zone = "cont" then m.zone = "tiles"
        return
    end if
    if m.cont.Count() = 1 then m.contCount.text = "1 TÍTULO" else m.contCount.text = m.cont.Count().ToStr() + " TÍTULOS"
    for i = 0 to n - 1
        p = m.cont[i]
        g = m.contBox.createChild("Group")
        g.translation = [i * 360, 0]
        g.scaleRotateCenter = [168, 94]
        addPoster(g, "pkg:/images/thumb_bg.png", 0, 0, 336, 188)
        img = addRounded(g, "mask_thumb", 0, 0, 336, 188)
        img.uri = asText(p.image)
        addPoster(g, "pkg:/images/thumb_shade.png", 0, 0, 336, 188)
        addPoster(g, "pkg:/images/play_circle.png", 140, 66, 56, 56)
        frac = 0.0
        if p.dur <> invalid and p.dur > 0 then frac = p.pos / p.dur
        addRect(g, 12, 174, 312, 6, "0xFFFFFF55")
        addRect(g, 12, 174, Int(312 * frac), 6, "0x7C5CFFFF")
        ring = addPoster(g, "pkg:/images/ring_thumb.png", 0, 0, 336, 188)
        ring.visible = false
        addLabel(g, asText(p.title), 0, 198, ISEM(), 19, "0xFFFFFFFF", 336)
        left = 0
        if p.dur <> invalid then left = Int((p.dur - p.pos) / 60)
        sub_ = "Faltam " + left.ToStr() + " min"
        if p.kind = "episode" then sub_ = asText(p.subtitle) + " · faltam " + left.ToStr() + " min"
        addLabel(g, sub_, 0, 226, IREG(), 16, "0x9D9ABBFF", 336)
        m.cards.Push({ group: g, ring: ring })
    end for
    if m.contIdx >= m.cards.Count() then m.contIdx = m.cards.Count() - 1
end sub

sub paint()
    for i = 0 to m.tiles.Count() - 1
        t = m.tiles[i]
        f = (m.zone = "tiles" and i = m.idx)
        if f then st = "_f" else st = "_n"
        t.bg.uri = "pkg:/images/tile_" + t.key + st + ".png"
        if f
            t.group.scale = [1.07, 1.07]
            t.subLbl.color = "0xFFFFFFE6"
        else
            t.group.scale = [1.0, 1.0]
            t.subLbl.color = "0x9D9ABBFF"
        end if
    end for
    for i = 0 to m.topBtns.Count() - 1
        b = m.topBtns[i]
        if m.zone = "top" and i = m.topIdx then st = "_f" else st = "_n"
        b.poster.uri = "pkg:/images/rbtn_" + b.name + st + ".png"
    end for
    for i = 0 to m.cards.Count() - 1
        f = (m.zone = "cont" and i = m.contIdx)
        m.cards[i].ring.visible = f
        if f then m.cards[i].group.scale = [1.05, 1.05] else m.cards[i].group.scale = [1.0, 1.0]
    end for
end sub

sub openTile()
    it = m.items[m.idx]
    m.top.action = { type: "open", screen: it.screen, params: it.params }
end sub

sub openContinue()
    p = m.cont[m.contIdx]
    m.top.action = { type: "play", key: p.key, kind: p.kind, title: p.title, subtitle: p.subtitle, image: p.image, url: p.url, format: p.format, live: false, start: p.pos, seriesId: p.seriesId, seriesName: p.seriesName }
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.zone = "tiles"
        if key = "left" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "right" and m.idx < m.tiles.Count() - 1
            m.idx = m.idx + 1
        else if key = "up"
            m.zone = "top"
        else if key = "down" and m.cards.Count() > 0
            m.zone = "cont"
        else if key = "OK"
            openTile()
            return true
        else
            return false
        end if
    else if m.zone = "top"
        if key = "left" and m.topIdx > 0
            m.topIdx = m.topIdx - 1
        else if key = "right" and m.topIdx < m.topBtns.Count() - 1
            m.topIdx = m.topIdx + 1
        else if key = "down"
            m.zone = "tiles"
        else if key = "OK"
            if m.topBtns[m.topIdx].name = "settings"
                m.top.action = { type: "open", screen: "SettingsScreen", params: {} }
            else
                m.top.action = { type: "home" }
            end if
            return true
        else
            return false
        end if
    else if m.zone = "cont"
        if key = "left" and m.contIdx > 0
            m.contIdx = m.contIdx - 1
        else if key = "right" and m.contIdx < m.cards.Count() - 1
            m.contIdx = m.contIdx + 1
        else if key = "up"
            m.zone = "tiles"
        else if key = "OK"
            openContinue()
            return true
        else
            return false
        end if
    end if
    paint()
    return true
end function
