' Tela inicial "Padrão" (igual ao Android): moldura comum, 4 blocos grandes e Continuar assistindo.
sub init()
    m.root = m.top.findNode("root")
    r = m.root
    frameInit(r)
    m.items = homeItems()
    m.tiles = []
    labels = ["CANAIS", "FILMES", "SÉRIES", "PLAYLISTS"]
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        g = r.createChild("Group")
        g.translation = [136 + i * 420, 264]
        g.scaleRotateCenter = [194, 138]
        bg = addPoster(g, "pkg:/images/tile_" + it.key + "_n.png", 0, 0, 388, 276)
        addLabel(g, labels[i], 28, 186, OUTF(), 30, "0xFFFFFFFF")
        sb = addLabel(g, it.sub, 28, 230, IREG(), 16, "0x9D9ABBFF", 340)
        m.tiles.Push({ group: g, bg: bg, subLbl: sb, key: it.key })
    end for
    m.tiles[3].subLbl.text = playlistsLabel()
    ' Continuar assistindo
    addPoster(r, "pkg:/images/ic_resume.png", 56, 594, 26, 26)
    addLabel(r, "Continuar assistindo", 94, 588, OUTF(), 24, "0xFFFFFFFF")
    m.contCount = addLabel(r, "", 360, 594, ISEM(), 16, "0x6E6B8CFF")
    m.contBox = r.createChild("Group")
    m.contBox.translation = [56, 638]
    m.zone = "tiles"
    m.idx = 0
    m.contIdx = 0
    m.cards = []
    frameCounts()
    buildContinue()
    paint()
    m.top.observeField("focusedChild", "onFocus")
end sub

sub onApi(event as object)
    r = event.getData()
    if frameOnApi(r) then return
    if r.id = "lc" then m.tiles[0].subLbl.text = catsLabel(r)
    if r.id = "vc" then m.tiles[1].subLbl.text = catsLabel(r)
    if r.id = "sc" then m.tiles[2].subLbl.text = catsLabel(r)
end sub

sub onFocus()
    if m.top.hasFocus()
        m.tiles[3].subLbl.text = playlistsLabel()
        buildContinue()
        paint()
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
    framePaint(m.zone = "top")
    for i = 0 to m.cards.Count() - 1
        f = (m.zone = "cont" and i = m.contIdx)
        m.cards[i].ring.visible = f
        if f then m.cards[i].group.scale = [1.05, 1.05] else m.cards[i].group.scale = [1.0, 1.0]
    end for
end sub

sub openContinue()
    p = m.cont[m.contIdx]
    m.top.action = { type: "play", key: p.key, kind: p.kind, title: p.title, subtitle: p.subtitle, image: p.image, url: p.url, format: p.format, live: false, start: p.pos, seriesId: p.seriesId, seriesName: p.seriesName }
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.zone = "top"
        r = frameKey(key)
        if r = "no" then return false
        if r = "down" then m.zone = "tiles"
    else if m.zone = "tiles"
        if key = "left" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "right" and m.idx < m.tiles.Count() - 1
            m.idx = m.idx + 1
        else if key = "up"
            m.zone = "top"
        else if key = "down" and m.cards.Count() > 0
            m.zone = "cont"
        else if key = "OK"
            it = m.items[m.idx]
            m.top.action = { type: "open", screen: it.screen, params: it.params }
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
