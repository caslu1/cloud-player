' Tela inicial Lite (igual ao Android): moldura comum e 4 blocos quadrados com ícones de linha.
sub init()
    r = m.top.findNode("root")
    frameInit(r)
    m.items = homeItems()
    m.tiles = []
    x0 = (1920 - (4 * 300 + 3 * 36)) / 2
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        g = r.createChild("Group")
        g.translation = [x0 + i * 336, 320]
        g.scaleRotateCenter = [150, 150]
        bg = addPoster(g, "pkg:/images/lite_n.9.png", 0, 0, 300, 300)
        ic = addPoster(g, "pkg:/images/" + it.ln + ".png", 110, 80, 80, 80)
        lb = addLabel(g, it.short, 0, 196, ISEM(), 24, "0xE9E6F5FF", 300, "center")
        m.tiles.Push({ group: g, bg: bg, icon: ic, label: lb })
    end for
    m.idx = 0
    m.zone = "tiles"
    paint()
end sub

sub onApi(event as object)
    frameOnApi(event.getData())
end sub

sub paint()
    for i = 0 to m.tiles.Count() - 1
        t = m.tiles[i]
        f = (m.zone = "tiles" and i = m.idx)
        if f
            t.bg.uri = "pkg:/images/lite_f.9.png"
            t.icon.blendColor = "0x2A1D6EFF"
            t.label.color = "0x120F1FFF"
            t.group.scale = [1.08, 1.08]
        else
            t.bg.uri = "pkg:/images/lite_n.9.png"
            t.icon.blendColor = "0xE9E6F5FF"
            t.label.color = "0xE9E6F5FF"
            t.group.scale = [1.0, 1.0]
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
