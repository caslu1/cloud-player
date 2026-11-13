' Telas iniciais Vitrine (cartões de vidro), Redondo e Quadrado (ícones grandes) — iguais ao Android.
sub init()
    r = m.top.findNode("root")
    frameInit(r)
    m.style = prefGet("layout", "cards")
    m.items = homeItems()
    m.tiles = []
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        g = r.createChild("Group")
        t = { group: g, key: it.key }
        if m.style = "cards"
            g.translation = [122 + i * 428, 304]
            g.scaleRotateCenter = [196, 176]
            t.bg = addPoster(g, "pkg:/images/vit_card_n.png", 0, 0, 392, 352)
            addLabel(g, it.title, 32, 28, OUTF(), 30, "0xFFFFFFFF", 340)
            t.badge = addPoster(g, "pkg:/images/badge_n.png", 188, 102, 172, 172)
            addPoster(g, "pkg:/images/" + it.sic + ".png", 232, 146, 84, 84)
            t.count = addLabel(g, "", 32, 252, ISEM(), 21, "0xFFFFFFFF", 330)
            t.sub = addLabel(g, it.sub, 32, 286, IREG(), 15, "0x9D9ABBFF", 330)
        else
            g.translation = [222 + i * 392, 300]
            g.scaleRotateCenter = [150, 150]
            if m.style = "round" then t.prefix = "big_c" else t.prefix = "big_s"
            t.bg = addPoster(g, "pkg:/images/" + t.prefix + "_n.png", -20, -20, 340, 340)
            addPoster(g, "pkg:/images/" + it.sic + ".png", 80, 80, 140, 140)
            addLabel(g, it.title, -40, 328, ISEM(), 26, "0xFFFFFFFF", 380, "center")
            t.count = addLabel(g, "", -40, 366, IREG(), 17, "0x9D9ABBFF", 380, "center")
        end if
        m.tiles.Push(t)
    end for
    setPlaylistInfo()
    m.idx = 0
    m.zone = "tiles"
    frameCounts()
    paint()
    m.top.observeField("focusedChild", "onFocus")
end sub

sub setPlaylistInfo()
    t = m.tiles[3]
    t.count.text = playlistsLabel()
    if t.sub <> invalid
        n = currentPlaylistName()
        if n <> "" then t.sub.text = "Em uso: " + n else t.sub.text = "Suas listas"
    end if
end sub

sub onFocus()
    if m.top.hasFocus() then setPlaylistInfo()
end sub

sub onApi(event as object)
    r = event.getData()
    if frameOnApi(r) then return
    if r.id = "lc" then m.tiles[0].count.text = catsLabel(r)
    if r.id = "vc" then m.tiles[1].count.text = catsLabel(r)
    if r.id = "sc" then m.tiles[2].count.text = catsLabel(r)
end sub

sub paint()
    for i = 0 to m.tiles.Count() - 1
        t = m.tiles[i]
        f = (m.zone = "tiles" and i = m.idx)
        if m.style = "cards"
            if f
                t.bg.uri = "pkg:/images/vit_card_f.png"
                t.badge.uri = "pkg:/images/badge_f.png"
                t.group.scale = [1.06, 1.06]
                t.sub.color = "0xE7E5F7FF"
            else
                t.bg.uri = "pkg:/images/vit_card_n.png"
                t.badge.uri = "pkg:/images/badge_n.png"
                t.group.scale = [1.0, 1.0]
                t.sub.color = "0x9D9ABBFF"
            end if
        else
            if f then st = "_f" else st = "_n"
            t.bg.uri = "pkg:/images/" + t.prefix + st + ".png"
            if f then t.group.scale = [1.08, 1.08] else t.group.scale = [1.0, 1.0]
            if f then t.count.color = "0xE7E5F7FF" else t.count.color = "0x9D9ABBFF"
        end if
    end for
    framePaint(m.zone = "top")
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.zone = "top"
        r = frameKey(key)
        if r = "no" then return false
        if r = "down" then m.zone = "tiles"
    else
        if key = "left" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "right" and m.idx < m.tiles.Count() - 1
            m.idx = m.idx + 1
        else if key = "up"
            m.zone = "top"
        else if key = "OK"
            it = m.items[m.idx]
            m.top.action = { type: "open", screen: it.screen, params: it.params }
            return true
        else
            return false
        end if
    end if
    paint()
    return true
end function
