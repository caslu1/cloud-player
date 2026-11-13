' Configurações igual ao Android: menu por grupos à esquerda, conteúdo em quadros e janelas de confirmação.
sub init()
    r = m.top.findNode("root")
    addPoster(r, "pkg:/images/logo_full.png", 64, 40, 300, 52)
    addRect(r, 388, 48, 2, 36, "0x1C1830FF")
    addLabel(r, "Configurações", 414, 38, OUTF(), 38, "0xFFFFFFFF")
    addPoster(r, "pkg:/images/set_menu.png", 64, 126, 460, 900)
    m.items = []
    groups = [
        { t: "CONTA", items: [{ k: "info", t: "Informações gerais", i: "ic_info", d: "Sua conta, assinatura e os dados deste aparelho." }] },
        { t: "SISTEMA", items: [
            { k: "storage", t: "Armazenamento", i: "ic_replay", d: "Limpar dados guardados nesta TV." },
            { k: "logout", t: "Sair da conta", i: "ic_logout", d: "Remover a lista deste aparelho." }
        ] }
    ]
    y = 146
    for each g in groups
        addLabel(r, g.t, 100, y + 8, ISEM(), 15, "0x6E6B8CFF")
        y = y + 40
        for each it in g.items
            grp = r.createChild("Group")
            grp.translation = [80, y]
            bg = addPoster(grp, "", 0, 0, 428, 58)
            bar = addRect(grp, 18, 20, 5, 18, "0x7C5CFFFF")
            ic = addPoster(grp, "pkg:/images/" + it.i + ".png", 38, 15, 28, 28)
            color = "0xE7E5F7FF"
            if it.k = "logout" then color = "0xF43F5EFF"
            lb = addLabel(grp, it.t, 84, 16, ISEM(), 20, color)
            it.bg = bg
            it.bar = bar
            it.icon = ic
            it.label = lb
            it.color = color
            m.items.Push(it)
            y = y + 64
        end for
        y = y + 10
    end for
    m.content = m.top.findNode("content")
    m.top.findNode("msgTimer").observeField("fire", "hideMsg")
    m.idx = 0
    m.sel = 0
    m.zone = "menu"
    m.info = invalid
    m.dialog = invalid
    callApi("acc", apiUrl(""))
    render()
end sub

sub onApi(event as object)
    r = event.getData()
    if r.ok and type(r.data) = "roAssociativeArray" then m.info = r.data.user_info
    if m.items[m.sel].k = "info" then render()
end sub

' Quadro com título pequeno em cima
function card(title as string, x as integer, y as integer, w as integer, h as integer) as object
    g = m.content.createChild("Group")
    g.translation = [x, y]
    addPoster(g, "pkg:/images/card.9.png", 0, 0, w, h)
    if title <> "" then addLabel(g, title, 28, 24, ISEM(), 15, "0x6E6B8CFF")
    return g
end function

sub line(g as object, k as string, v as string, y as integer)
    addLabel(g, k, 28, y, IREG(), 19, "0x9D9ABBFF", 160)
    l = addLabel(g, v, 190, y, IMED(), 19, "0xFFFFFFFF", 440)
    l.maxLines = 1
end sub

sub big(g as object, k as string, v as string, x as integer, y as integer)
    addLabel(g, k, x, y, ISEM(), 14, "0x6E6B8CFF")
    addLabel(g, v, x, y + 20, OUTF(), 30, "0xFFFFFFFF", 320)
end sub

sub render()
    m.content.removeChildrenIndex(m.content.getChildCount(), 0)
    it = m.items[m.sel]
    ' cabeçalho da seção
    addPoster(m.content, "pkg:/images/head_icon.png", 564, 126, 68, 68)
    hi = addPoster(m.content, "pkg:/images/" + it.i + ".png", 584, 146, 28, 28)
    hi.blendColor = "0x9B84FFFF"
    addLabel(m.content, it.t, 652, 124, OUTF(), 30, "0xFFFFFFFF")
    addLabel(m.content, it.d, 652, 166, IREG(), 18, "0x9D9ABBFF")
    m.msgBox = m.content.createChild("Group")
    m.btns = []
    c = m.global.creds
    if it.k = "info"
        g = card("CONTA", 564, 220, 630, 236)
        line(g, "Tipo", "Xtream Codes", 66)
        line(g, "Servidor", c.server.Replace("http://", "").Replace("https://", ""), 106)
        line(g, "Usuário", c.user, 146)
        g = card("ASSINATURA", 1214, 220, 630, 236)
        u = m.info
        st = "—"
        venc = "—"
        cons = "—"
        ok = true
        if type(u) = "roAssociativeArray"
            st = asText(u.status)
            ok = LCase(st) = "active"
            if ok then st = "Ativa"
            exp = asText(u.exp_date)
            if exp <> "" and exp <> "0"
                d = CreateObject("roDateTime")
                d.FromSeconds(asInt(exp))
                venc = two(d.GetDayOfMonth()) + "/" + two(d.GetMonth()) + "/" + d.GetYear().ToStr()
            end if
            cons = asText(u.active_cons) + " de " + asText(u.max_connections)
        end if
        pill = addPoster(g, "pkg:/images/pill_w.9.png", 28, 62, Len(st) * 12 + 50, 36)
        if ok then pill.blendColor = "0x163A2AFF" else pill.blendColor = "0x3A1620FF"
        if ok then pc = "0x7EE2A8FF" else pc = "0xFF8095FF"
        addLabel(g, "●  " + st, 44, 69, ISEM(), 17, pc)
        big(g, "VALIDADE", venc, 28, 126)
        big(g, "CONEXÕES", cons, 330, 126)
        g = card("ESTE APARELHO", 564, 476, 630, 236)
        big(g, "MAC", deviceMac(), 28, 62)
        big(g, "CHAVE", deviceKey(), 380, 62)
        s = siteUrl()
        if s <> "" then addLabel(g, "Use o MAC e a chave em " + s.Replace("https://", "").Replace("http://", ""), 28, 150, IREG(), 17, "0x9D9ABBFF", 580)
        addLabel(g, "Orbita Play para Roku 1.0", 28, 186, IREG(), 17, "0x6E6B8CFF")
        g = card("GUARDADO NESTA TV", 1214, 476, 630, 236)
        stats = [[regRead("favs").Count(), "favoritos"], [regRead("mylist").Count(), "minha lista"], [regRead("progress").Count(), "em andamento"]]
        for i = 0 to 2
            addLabel(g, stats[i][0].ToStr(), 28 + i * 200, 66, OUTF(), 40, "0xFFFFFFFF")
            addLabel(g, stats[i][1], 28 + i * 200, 118, IREG(), 18, "0x9D9ABBFF")
        end for
        addLabel(m.content, "O app não fornece conteúdo: use apenas listas que você tem direito de usar.", 564, 736, IREG(), 17, "0x6E6B8CFF")
    else if it.k = "storage"
        g = card("DADOS GUARDADOS NESTA TV", 564, 220, 1280, 330)
        addOpt(g, "Limpar histórico", "Recentes e continuar assistindo", 64, "history")
        addOpt(g, "Limpar Minha lista", "Filmes e séries guardados", 148, "mylist")
        addOpt(g, "Limpar favoritos", "Canais marcados com a estrela", 232, "favs")
    else
        g = card("SAIR DA CONTA", 564, 220, 1280, 270)
        addLabel(g, "Remove a lista desta TV e volta para a tela de login.", 28, 62, IREG(), 20, "0xE7E5F7FF", 1200)
        addOpt(g, "Sair da conta", "", 108, "logout")
        addLabel(g, "Favoritos, histórico e preferências desta TV também serão apagados. O MAC e a chave continuam os mesmos.", 28, 208, IREG(), 17, "0x6E6B8CFF", 1220)
    end if
    if m.bIdx = invalid or m.bIdx >= m.btns.Count() then m.bIdx = 0
    paint()
end sub

' Opção em linha (igual ao Option do Android)
sub addOpt(g as object, label as string, value as string, y as integer, act as string)
    o = g.createChild("Group")
    o.translation = [28, y]
    bg = addPoster(o, "", 0, 0, 1224, 72)
    color = "0xFFFFFFFF"
    if act = "logout" then color = "0xF43F5EFF"
    addLabel(o, label, 28, 22, ISEM(), 21, color)
    if value <> "" then addLabel(o, value, 600, 24, IMED(), 18, "0x9D9ABBFF", 596, "right")
    m.btns.Push({ bg: bg, act: act })
end sub

sub paint()
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        f = (m.zone = "menu" and i = m.idx)
        sel = (i = m.sel)
        if f
            it.bg.uri = "pkg:/images/menu_focus.9.png"
        else if sel
            it.bg.uri = "pkg:/images/menu_sel.9.png"
        else
            it.bg.uri = ""
        end if
        it.bar.visible = sel and not f
        if f or sel
            it.label.color = "0xFFFFFFFF"
            it.icon.blendColor = "0xFFFFFFFF"
        else
            it.label.color = it.color
            if it.k = "logout" then it.icon.blendColor = "0xF43F5EFF" else it.icon.blendColor = "0x9D9ABBFF"
        end if
    end for
    for i = 0 to m.btns.Count() - 1
        if m.zone = "content" and i = m.bIdx then m.btns[i].bg.uri = "pkg:/images/field_f.9.png" else m.btns[i].bg.uri = "pkg:/images/field_n.9.png"
    end for
end sub

sub ask(title as string, message as string, confirm as string, icon as string, danger as boolean, act as string)
    d = CreateObject("roSGNode", "ConfirmBox")
    d.setup = { title: title, message: message, confirm: confirm, icon: icon, danger: danger }
    d.observeField("result", "onAsk")
    m.top.appendChild(d)
    m.dialog = d
    m.pending = act
    d.setFocus(true)
end sub

sub onAsk()
    res = m.dialog.result
    m.top.removeChild(m.dialog)
    m.dialog = invalid
    m.top.setFocus(true)
    if res <> "ok" then return
    act = m.pending
    if act = "logout"
        m.top.action = { type: "logout" }
        return
    end if
    if act = "history"
        regWrite("recents", [])
        regWrite("progress", [])
        showMsg("Histórico apagado")
    else if act = "mylist"
        regWrite("mylist", [])
        showMsg("Minha lista apagada")
    else if act = "favs"
        regWrite("favs", [])
        showMsg("Favoritos apagados")
    end if
end sub

sub showMsg(t as string)
    m.msgBox.removeChildrenIndex(m.msgBox.getChildCount(), 0)
    w = Len(t) * 11 + 70
    p = addPoster(m.msgBox, "pkg:/images/chip_ok.9.png", 1844 - w, 140, w, 40)
    p.height = 40
    ic = addPoster(m.msgBox, "pkg:/images/ic_check.png", 1844 - w + 16, 150, 20, 20)
    ic.blendColor = "0x7EE2A8FF"
    addLabel(m.msgBox, t, 1844 - w + 44, 149, ISEM(), 17, "0x7EE2A8FF")
    tm = m.top.findNode("msgTimer")
    tm.control = "stop"
    tm.control = "start"
end sub

sub hideMsg()
    if m.msgBox <> invalid then m.msgBox.removeChildrenIndex(m.msgBox.getChildCount(), 0)
end sub

sub pressOpt()
    a = m.btns[m.bIdx].act
    if a = "history"
        ask("Limpar o histórico?", "Os canais recentes e o ""Continuar assistindo"" serão apagados.", "Limpar", "ic_replay", true, a)
    else if a = "mylist"
        ask("Limpar a Minha lista?", "Os filmes e séries guardados serão removidos.", "Limpar", "ic_replay", true, a)
    else if a = "favs"
        ask("Limpar os favoritos?", "Os canais marcados com a estrela serão desmarcados.", "Limpar", "ic_replay", true, a)
    else if a = "logout"
        ask("Sair da conta?", "Você vai precisar entrar de novo com os dados do seu provedor.", "Sair", "ic_logout", true, a)
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.dialog <> invalid then return true
    if m.zone = "menu"
        if key = "up" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "down" and m.idx < m.items.Count() - 1
            m.idx = m.idx + 1
        else if key = "OK" or key = "right"
            m.sel = m.idx
            m.bIdx = 0
            render()
            if m.btns.Count() > 0 then m.zone = "content"
        else
            return false
        end if
    else
        if key = "left" or key = "back"
            m.zone = "menu"
        else if key = "up" and m.bIdx > 0
            m.bIdx = m.bIdx - 1
        else if key = "down" and m.bIdx < m.btns.Count() - 1
            m.bIdx = m.bIdx + 1
        else if key = "OK"
            pressOpt()
            return true
        else
            return true
        end if
    end if
    paint()
    return true
end function
