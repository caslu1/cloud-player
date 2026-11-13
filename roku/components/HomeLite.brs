' Tela inicial Lite (igual ao Android): logo no centro, blocos quadrados com ícones de linha, Recarregar e rodapé.
sub init()
    r = m.top.findNode("root")
    addPoster(r, "pkg:/images/logo_big.png", 780, 112, 361, 72)
    m.clock = addLabel(r, "", 1620, 36, OUTF(), 30, "0xFFFFFFFF", 240, "right")
    m.date = addLabel(r, "", 1620, 76, IREG(), 15, "0x9D9ABBFF", 240, "right")
    m.items = [
        { label: "Canais", icon: "ln_tv", screen: "ChannelsScreen", params: {} },
        { label: "Filmes", icon: "ln_movie", screen: "VodScreen", params: { kind: "movie" } },
        { label: "Séries", icon: "ln_series", screen: "VodScreen", params: { kind: "series" } },
        { label: "Configurações", icon: "ln_gear", screen: "SettingsScreen", params: {} },
    ]
    m.tiles = []
    x0 = (1920 - (4 * 248 + 3 * 32)) / 2
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        g = r.createChild("Group")
        g.translation = [x0 + i * 280, 300]
        g.scaleRotateCenter = [124, 124]
        bg = addPoster(g, "pkg:/images/lite_n.9.png", 0, 0, 248, 248)
        ic = addPoster(g, "pkg:/images/" + it.icon + ".png", 84, 58, 80, 80)
        lb = addLabel(g, it.label, 0, 166, ISEM(), 21, "0xE9E6F5FF", 248, "center")
        m.tiles.Push({ group: g, bg: bg, icon: ic, label: lb })
    end for
    m.reload = addButton(r, "Recarregar", "ic_replay", false, 790, 620)
    m.reload.bg.width = 340
    m.reload.group.scaleRotateCenter = [170, 34]
    m.reload.label.translation = [150, 21]
    m.reload.icon.translation = [110, 21]
    ' rodapé
    addLabel(r, "Versão: Orbita Play para Roku 1.0", 60, 1000, IREG(), 16, "0x9D9ABBFF")
    m.status = addLabel(r, "Status: …", 60, 1024, IREG(), 16, "0x9D9ABBFF")
    addLabel(r, "MAC: " + deviceMac(), 1260, 1000, IREG(), 16, "0x9D9ABBFF", 600, "right")
    addLabel(r, "Chave: " + deviceKey() + "    Usuário: " + m.global.creds.user, 1260, 1024, IREG(), 16, "0x9D9ABBFF", 600, "right")
    m.idx = 0
    m.zone = "tiles"
    t = m.top.findNode("timer")
    t.observeField("fire", "tick")
    t.control = "start"
    tick()
    callApi("acc", apiUrl(""))
    paint()
end sub

sub onApi(event as object)
    r = event.getData()
    if not r.ok or type(r.data) <> "roAssociativeArray" or type(r.data.user_info) <> "roAssociativeArray" then return
    u = r.data.user_info
    st = asText(u.status)
    if LCase(st) = "active" then st = "Ativa"
    exp = asText(u.exp_date)
    if exp <> "" and exp <> "0"
        d = CreateObject("roDateTime")
        d.FromSeconds(asInt(exp))
        st = st + " · vence " + two(d.GetDayOfMonth()) + "/" + two(d.GetMonth()) + "/" + d.GetYear().ToStr()
    end if
    m.status.text = "Status: " + st
end sub

sub tick()
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    m.clock.text = two(d.GetHours()) + ":" + two(d.GetMinutes())
    days = ["dom.", "seg.", "ter.", "qua.", "qui.", "sex.", "sáb."]
    months = ["jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."]
    m.date.text = days[d.GetDayOfWeek()] + ", " + d.GetDayOfMonth().ToStr() + " " + months[d.GetMonth() - 1]
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
    paintButton(m.reload, m.zone = "reload")
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.zone = "tiles"
        if key = "left" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "right" and m.idx < m.tiles.Count() - 1
            m.idx = m.idx + 1
        else if key = "down"
            m.zone = "reload"
        else if key = "OK"
            it = m.items[m.idx]
            m.top.action = { type: "open", screen: it.screen, params: it.params }
            return true
        else
            return false
        end if
    else
        if key = "up"
            m.zone = "tiles"
        else if key = "OK"
            m.top.action = { type: "home" }
            return true
        else
            return false
        end if
    end if
    paint()
    return true
end function
