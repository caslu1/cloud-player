sub init()
    m.items = [
        { label: "Canais", icon: "ic_tv", screen: "ChannelsScreen", params: {} },
        { label: "Filmes", icon: "ic_film", screen: "VodScreen", params: { kind: "movie" } },
        { label: "Séries", icon: "ic_series", screen: "VodScreen", params: { kind: "series" } },
        { label: "Configurações", icon: "ic_sliders", screen: "SettingsScreen", params: {} },
    ]
    m.tiles = []
    row = m.top.findNode("tiles")
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        g = row.createChild("Group")
        g.translation = [i * 380, 0]
        bg = g.createChild("Poster")
        bg.uri = "pkg:/images/tile.png"
        bg.width = 340
        bg.height = 240
        ic = g.createChild("Poster")
        ic.uri = "pkg:/images/" + it.icon + ".png"
        ic.width = 96
        ic.height = 96
        ic.translation = [122, 40]
        lb = g.createChild("Label")
        lb.text = it.label
        lb.width = 340
        lb.horizAlign = "center"
        lb.translation = [0, 160]
        lb.font = "font:MediumBoldSystemFont"
        m.tiles.Push({ group: g, bg: bg, icon: ic, label: lb })
    end for
    m.idx = 0
    paint()
    m.top.findNode("info").text = "Servidor: " + m.global.creds.server + "    ·    Usuário: " + m.global.creds.user + "    ·    Orbita Play para Roku"
    t = m.top.findNode("timer")
    t.observeField("fire", "tick")
    t.control = "start"
    tick()
end sub

sub tick()
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    h = d.GetHours()
    mi = d.GetMinutes()
    hs = h.ToStr()
    if h < 10 then hs = "0" + hs
    ms = mi.ToStr()
    if mi < 10 then ms = "0" + ms
    m.top.findNode("clock").text = hs + ":" + ms
end sub

sub paint()
    for i = 0 to m.tiles.Count() - 1
        t = m.tiles[i]
        f = (i = m.idx)
        if f
            t.bg.blendColor = "0xF4F2FAFF"
            t.icon.blendColor = "0x2A1D6EFF"
            t.label.color = "0x120F1FFF"
            t.group.scale = [1.08, 1.08]
            t.group.scaleRotateCenter = [170, 120]
        else
            t.bg.blendColor = "0x15121FFF"
            t.icon.blendColor = "0xE9E6F5FF"
            t.label.color = "0xE9E6F5FF"
            t.group.scale = [1.0, 1.0]
        end if
    end for
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "left" and m.idx > 0
        m.idx = m.idx - 1
        paint()
        return true
    else if key = "right" and m.idx < m.tiles.Count() - 1
        m.idx = m.idx + 1
        paint()
        return true
    else if key = "OK"
        it = m.items[m.idx]
        m.top.action = { type: "open", screen: it.screen, params: it.params }
        return true
    end if
    return false
end function
