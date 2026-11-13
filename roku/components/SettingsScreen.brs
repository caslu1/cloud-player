' Configurações igual ao Android: menu por grupos à esquerda, conteúdo em quadros e janelas de confirmação.
sub init()
    r = m.top.findNode("root")
    addPoster(r, "pkg:/images/logo_full.png", 64, 40, 260, 52)
    addRect(r, 388, 48, 2, 36, "0x1C1830FF")
    addLabel(r, "Configurações", 414, 38, OUTF(), 38, "0xFFFFFFFF")
    addPoster(r, "pkg:/images/set_menu.png", 64, 126, 460, 900)
    m.items = []
    groups = [
        { t: "CONTA", items: [{ k: "info", t: "Informações gerais", i: "ic_info", d: "Sua conta, assinatura e os dados deste aparelho." }] },
        { t: "REPRODUÇÃO", items: [
            { k: "format", t: "Formato de fluxo", i: "ic_tv2", d: "Como os canais ao vivo são abertos." },
            { k: "play", t: "Reprodução", i: "ic_play2", d: "Comportamento do player." }
        ] },
        { t: "PRIVACIDADE", items: [
            { k: "parental", t: "Controle parental", i: "ic_lock", d: "Bloqueie ou oculte o conteúdo adulto." },
            { k: "pin", t: "Alterar senha", i: "ic_key", d: "Senha do controle parental." },
            { k: "hide", t: "Ocultar categorias", i: "ic_eyeoff", d: "Escolha o que aparece nas listas." }
        ] },
        { t: "APARÊNCIA", items: [
            { k: "wall", t: "Papel de parede", i: "ic_grid", d: "Fundo das telas do app." },
            { k: "layout", t: "Layout da tela inicial", i: "ic_home", d: "Padrão, Lite, Vitrine, Redondo ou Quadrado." }
        ] },
        { t: "SISTEMA", items: [
            { k: "storage", t: "Armazenamento", i: "ic_replay", d: "Recarregar listas e limpar dados guardados." },
            { k: "logout", t: "Sair da conta", i: "ic_logout", d: "Remover a lista deste aparelho." }
        ] }
    ]
    y = 140
    for each g in groups
        addLabel(r, g.t, 100, y + 6, ISEM(), 14, "0x6E6B8CFF")
        y = y + 34
        for each it in g.items
            grp = r.createChild("Group")
            grp.translation = [80, y]
            bg = addPoster(grp, "", 0, 0, 428, 54)
            bar = addRect(grp, 18, 18, 5, 18, "0x7C5CFFFF")
            ic = addPoster(grp, "pkg:/images/" + it.i + ".png", 38, 13, 28, 28)
            color = "0xE7E5F7FF"
            if it.k = "logout" then color = "0xF43F5EFF"
            lb = addLabel(grp, it.t, 84, 14, ISEM(), 20, color)
            it.bg = bg
            it.bar = bar
            it.icon = ic
            it.label = lb
            it.color = color
            m.items.Push(it)
            y = y + 58
        end for
        y = y + 6
    end for
    m.content = m.top.findNode("content")
    m.top.findNode("msgTimer").observeField("fire", "hideMsg")
    m.idx = 0
    m.sel = 0
    m.zone = "menu"
    m.info = invalid
    m.dialog = invalid
    m.hideTab = 0
    m.hideCats = { live: invalid, vod: invalid, series: invalid }
    m.scroll = 0
    callApi("acc", apiUrl(""))
    render()
end sub

sub onApi(event as object)
    r = event.getData()
    if r.id = "acc"
        if r.ok and type(r.data) = "roAssociativeArray" then m.info = r.data.user_info
        if m.items[m.sel].k = "info" then render()
    else if Left(r.id, 4) = "cat:"
        k = Mid(r.id, 5)
        if r.ok and type(r.data) = "roArray" then m.hideCats[k] = r.data else m.hideCats[k] = []
        if m.items[m.sel].k = "hide" then render()
    end if
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
    m.opts = []
    m.optBox = invalid
    c = m.global.creds
    if it.k = "info"
        ' usuário, endereço MAC e chave em destaque (igual ao Android)
        tiles = [["USUÁRIO", asText(c.user), 404], ["ENDEREÇO MAC", deviceMac(), 484], ["CHAVE DO APARELHO", deviceKey(), 364]]
        x = 564
        for each t in tiles
            g = card(t[0], x, 220, t[2], 150)
            v = addLabel(g, t[1], 28, 70, OUTF(), 32, "0xFFFFFFFF", t[2] - 56)
            v.maxLines = 1
            x = x + t[2] + 20
        end for
        g = card("ASSINATURA", 564, 390, 636, 250)
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
        big(g, "VALIDADE", venc, 28, 130)
        big(g, "CONEXÕES", cons, 330, 130)
        g = card("APLICATIVO", 1220, 390, 636, 250)
        line(g, "Versão", "Orbita Play " + CreateObject("roAppInfo").GetVersion(), 66)
        pn = ""
        for each p in regRead("playlists")
            if asText(p.server) = asText(c.server) and asText(p.user) = asText(c.user) then pn = asText(p.name)
        end for
        if pn = "" then pn = "—"
        line(g, "Playlist", pn, 106)
        line(g, "Servidor", c.server.Replace("http://", "").Replace("https://", ""), 146)
        su = siteUrl()
        if su <> "" then line(g, "Site", su.Replace("https://", "").Replace("http://", ""), 186)
        addLabel(m.content, "O app não fornece conteúdo: use apenas listas que você tem direito de usar.", 564, 664, IREG(), 17, "0x6E6B8CFF")
    else
        renderOptions(it.k)
    end if
    if m.bIdx = invalid or m.bIdx >= m.opts.Count() then m.bIdx = 0
    drawOpts()
    paint()
end sub

' ---------------------------------------------------------------- opções

' Monta a lista de opções da seção (cada uma: radio, switch, ação, abas ou papel de parede)
sub renderOptions(k as string)
    o = []
    title = ""
    hint = ""
    if k = "format"
        title = "CANAIS AO VIVO"
        f = prefGet("fmt", "m3u8")
        o.Push({ kind: "radio", label: "HLS (.m3u8)", value: "Recomendado na Roku", on: f = "m3u8", act: "fmt:m3u8" })
        o.Push({ kind: "radio", label: "MPEG-TS (.ts)", value: "Use se algum canal não abrir", on: f = "ts", act: "fmt:ts" })
        hint = "Se o canal não abrir no formato escolhido, o app tenta o outro sozinho."
    else if k = "play"
        title = "PLAYER"
        o.Push({ kind: "switch", label: "Próximo episódio automático", on: prefGet("autoNext", "1") = "1", act: "sw:autoNext" })
        o.Push({ kind: "switch", label: "Prévia automática nos canais", on: prefGet("preview", "0") = "1", act: "sw:preview" })
    else if k = "parental"
        title = "CANAIS, FILMES E SÉRIES ADULTOS"
        md = prefGet("adult", "lock")
        o.Push({ kind: "radio", label: "Bloquear com senha", on: md = "lock", act: "adult:lock" })
        o.Push({ kind: "radio", label: "Ocultar", on: md = "hide", act: "adult:hide" })
        o.Push({ kind: "radio", label: "Mostrar", on: md = "show", act: "adult:show" })
        if md = "lock" and m.global.adultUnlocked = true then o.Push({ kind: "action", label: "Bloquear de novo agora", act: "relock" })
        hint = "As categorias adultas são reconhecidas pelo nome (Adultos, XXX, +18…). Senha padrão: 0000."
    else if k = "pin"
        title = "SENHA DO CONTROLE PARENTAL"
        o.Push({ kind: "action", label: "Alterar senha", value: "4 números", act: "pin" })
        hint = "A senha protege o conteúdo adulto e as mudanças no controle parental."
    else if k = "hide"
        kinds = ["live", "vod", "series"]
        kind = kinds[m.hideTab]
        o.Push({ kind: "tabs", act: "tabs" })
        cats = m.hideCats[kind]
        if cats = invalid
            actions = ["get_live_categories", "get_vod_categories", "get_series_categories"]
            m.hideCats[kind] = []
            callApi("cat:" + kind, apiUrl(actions[m.hideTab]))
        else
            hidden = hiddenSet()
            for each c in cats
                id = asText(c.category_id)
                o.Push({ kind: "switch", label: asText(c.category_name), on: not hidden.DoesExist(kind + ":" + id), act: "hide:" + kind + ":" + id })
            end for
        end if
    else if k = "wall"
        title = "ESCOLHA O FUNDO DAS TELAS DO APP"
        names = ["Sem fundo", "Órbita", "Aurora", "Nebulosa", "Meia-noite", "Ametista", "Eclipse", "Oceano", "Brasa", "Floresta", "Grafite"]
        cur = prefGet("wall", "3")
        for i = 0 to names.Count() - 1
            o.Push({ kind: "wall", label: names[i], on: cur = i.ToStr(), act: "wall:" + i.ToStr() })
        end for
    else if k = "layout"
        title = "TELA INICIAL"
        lay = prefGet("layout", "cards")
        o.Push({ kind: "radio", label: "Padrão", value: "Blocos grandes e Continuar assistindo", on: lay = "default", act: "layout:default" })
        o.Push({ kind: "radio", label: "Lite", value: "Blocos quadrados com ícones de linha", on: lay = "lite", act: "layout:lite" })
        o.Push({ kind: "radio", label: "Vitrine", value: "Cartões de vidro grandes", on: lay = "cards", act: "layout:cards" })
        o.Push({ kind: "radio", label: "Redondo", value: "Ícones grandes em círculos de vidro", on: lay = "round", act: "layout:round" })
        o.Push({ kind: "radio", label: "Quadrado", value: "Ícones grandes em quadrados arredondados", on: lay = "square", act: "layout:square" })
    else if k = "storage"
        title = "LISTAS E DADOS GUARDADOS NESTA TV"
        o.Push({ kind: "action", label: "Recarregar listas", value: "Canais, filmes e séries", act: "reload" })
        o.Push({ kind: "action", label: "Limpar histórico", value: "Recentes e continuar assistindo", act: "history" })
        o.Push({ kind: "action", label: "Limpar Minha lista", value: "Filmes e séries guardados", act: "mylist" })
        o.Push({ kind: "action", label: "Limpar favoritos", value: "Canais marcados com a estrela", act: "favs" })
    else if k = "logout"
        title = "SAIR DA CONTA"
        o.Push({ kind: "action", label: "Sair da conta", act: "logout", danger: true })
        hint = "Favoritos, histórico e preferências desta TV também serão apagados. O MAC e a chave continuam os mesmos."
    end if
    m.opts = o
    m.optTitle = title
    m.optHint = hint
    m.optKind = k
end sub

' Desenha as opções num quadro (com rolagem quando a lista é longa)
sub drawOpts()
    if m.opts.Count() = 0 or m.optKind = invalid or m.optKind = "info" then return
    if m.optBox <> invalid then m.content.removeChild(m.optBox)
    g = m.content.createChild("Group")
    m.optBox = g
    k = m.optKind
    if k = "wall"
        h = 120 + Int((m.opts.Count() + 3) / 4) * 172
        addPoster(g, "pkg:/images/card.9.png", 564, 220, 1280, h)
        addLabel(g, m.optTitle, 592, 244, ISEM(), 15, "0x6E6B8CFF")
        m.optNodes = []
        for i = 0 to m.opts.Count() - 1
            op = m.opts[i]
            x = 592 + (i mod 4) * 306
            y = 290 + Int(i / 4) * 172
            th = addPoster(g, "pkg:/images/wall_" + i.ToStr() + ".png", x, y, 286, 128)
            ring = addPoster(g, "", x - 4, y - 4, 294, 136)
            if op.on
                chk = addPoster(g, "pkg:/images/radio_dot.png", x + 252, y + 12, 22, 22)
            end if
            lb = addLabel(g, op.label, x, y + 138, ISEM(), 18, "0x9D9ABBFF", 286)
            m.optNodes.Push({ bg: ring, label: lb, op: op, idx: i })
        end for
        return
    end if
    rows = 9
    if k = "hide" then rows = 8
    top = 270
    if m.optTitle = "" then top = 220
    cardH = 60 + Min(rows, m.opts.Count()) * 84
    if m.optHint <> "" then cardH = cardH + 60
    if m.optTitle = "" then cardH = cardH - 30
    addPoster(g, "pkg:/images/card.9.png", 564, 220, 1280, cardH)
    if m.optTitle <> "" then addLabel(g, m.optTitle, 592, 244, ISEM(), 15, "0x6E6B8CFF")
    if m.bIdx < m.scroll then m.scroll = m.bIdx
    if m.bIdx >= m.scroll + rows then m.scroll = m.bIdx - rows + 1
    m.optNodes = []
    y = top
    last = m.scroll + rows - 1
    if last > m.opts.Count() - 1 then last = m.opts.Count() - 1
    for i = m.scroll to last
        op = m.opts[i]
        node = { op: op, idx: i }
        if op.kind = "tabs"
            names = ["TV", "FILMES", "SÉRIES"]
            node.tabs = []
            for t = 0 to 2
                tb = addPoster(g, "", 592 + t * 150, y + 8, 136, 54)
                tl = addLabel(g, names[t], 592 + t * 150, y + 22, ISEM(), 18, "0xFFFFFFFF", 136, "center")
                node.tabs.Push({ bg: tb, label: tl })
            end for
        else
            bg = addPoster(g, "", 592, y, 1224, 72)
            x = 620
            if op.kind = "radio"
                if op.on then addPoster(g, "pkg:/images/radio_on.png", 620, y + 20, 32, 32) else addPoster(g, "pkg:/images/radio_off.png", 620, y + 20, 32, 32)
                if op.on then addPoster(g, "pkg:/images/radio_dot.png", 628, y + 28, 16, 16)
                x = 670
            end if
            color = "0xFFFFFFFF"
            if op.danger = true then color = "0xF43F5EFF"
            lb = addLabel(g, op.label, x, y + 22, ISEM(), 21, color, 760)
            lb.maxLines = 1
            rx = 1786
            if op.kind = "switch"
                if op.on then addPoster(g, "pkg:/images/sw_on.png", 1720, y + 17, 68, 38) else addPoster(g, "pkg:/images/sw_off.png", 1720, y + 17, 68, 38)
                if op.on then addPoster(g, "pkg:/images/sw_knob.png", 1756, y + 22, 28, 28) else addPoster(g, "pkg:/images/sw_knob.png", 1724, y + 22, 28, 28)
                rx = 1700
            end if
            if op.value <> invalid and op.value <> "" then addLabel(g, op.value, rx - 600, y + 24, IMED(), 18, "0x9D9ABBFF", 600, "right")
            node.bg = bg
        end if
        m.optNodes.Push(node)
        y = y + 84
    end for
    if m.opts.Count() > rows
        addLabel(g, (m.bIdx + 1).ToStr() + " de " + m.opts.Count().ToStr(), 1500, 244, IMED(), 15, "0x6E6B8CFF", 316, "right")
    end if
    if m.optHint <> "" then addLabel(g, m.optHint, 592, y + 8, IREG(), 17, "0x6E6B8CFF", 1220)
end sub

function Min(a as integer, b as integer) as integer
    if a < b then return a
    return b
end function

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
    if m.optNodes = invalid then return
    for each n in m.optNodes
        f = (m.zone = "content" and n.idx = m.bIdx)
        if n.op.kind = "wall"
            if f
                n.bg.uri = "pkg:/images/wall_ring.9.png"
            else if n.op.on
                n.bg.uri = "pkg:/images/wall_sel.9.png"
            else
                n.bg.uri = ""
            end if
            if f or n.op.on then n.label.color = "0xFFFFFFFF" else n.label.color = "0x9D9ABBFF"
        else if n.op.kind = "tabs"
            for t = 0 to 2
                if t = m.hideTab and f
                    n.tabs[t].bg.uri = "pkg:/images/chip_focus.9.png"
                else if t = m.hideTab
                    n.tabs[t].bg.uri = "pkg:/images/chip_on.9.png"
                else
                    n.tabs[t].bg.uri = "pkg:/images/chip_off.9.png"
                end if
            end for
        else
            if f then n.bg.uri = "pkg:/images/opt_f.9.png" else n.bg.uri = "pkg:/images/opt_n.9.png"
        end if
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
    if act = "reload"
        m.top.action = { type: "reload" }
        return
    end if
    if Left(act, 4) = "fmt:"
        prefSet("fmt", Mid(act, 5))
        rerender()
        showMsg("Formato alterado")
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
    op = m.opts[m.bIdx]
    a = op.act
    if a = "reload"
        ask("Recarregar as listas?", "Canais, filmes e séries são baixados de novo do seu provedor.", "Recarregar", "ic_replay", false, a)
    else if a = "history"
        ask("Limpar o histórico?", "Os canais recentes e o ""Continuar assistindo"" serão apagados.", "Limpar", "ic_replay", true, a)
    else if a = "mylist"
        ask("Limpar a Minha lista?", "Os filmes e séries guardados serão removidos.", "Limpar", "ic_replay", true, a)
    else if a = "favs"
        ask("Limpar os favoritos?", "Os canais marcados com a estrela serão desmarcados.", "Limpar", "ic_replay", true, a)
    else if a = "logout"
        ask("Sair da conta?", "Você vai precisar entrar de novo com os dados do seu provedor.", "Sair", "ic_logout", true, a)
    else if Left(a, 4) = "fmt:"
        if op.on then return
        ask("Trocar o formato?", "Os canais vão abrir primeiro no formato escolhido.", "Trocar", "ic_replay", false, a)
    else if Left(a, 3) = "sw:"
        k = Mid(a, 4)
        if op.on then prefSet(k, "0") else prefSet(k, "1")
        rerender()
        showMsg("Salvo")
    else if Left(a, 6) = "adult:"
        md = Mid(a, 7)
        cur = prefGet("adult", "lock")
        if md = cur then return
        if cur = "lock" or cur = "hide"
            m.pendingAdult = md
            askPin("Confirme a senha", "Necessária para liberar o conteúdo adulto.", "adult")
        else
            prefSet("adult", md)
            m.global.adultUnlocked = false
            rerender()
            showMsg("Alterado")
        end if
    else if a = "relock"
        m.global.adultUnlocked = false
        rerender()
        showMsg("Conteúdo adulto bloqueado")
    else if a = "pin"
        askPin("Senha atual", "Digite a senha atual (padrão 0000).", "pin1")
    else if Left(a, 5) = "hide:"
        k = Mid(a, 6)
        if op.on then regPush("hiddenCats", { key: k }, 2000) else regRemove("hiddenCats", k)
        rerender()
    else if Left(a, 5) = "wall:"
        prefSet("wall", Mid(a, 6))
        m.top.getScene().findNode("bg").uri = "pkg:/images/wall_" + Mid(a, 6) + ".png"
        rerender()
        showMsg("Papel de parede alterado")
    else if Left(a, 7) = "layout:"
        prefSet("layout", Mid(a, 8))
        rerender()
        showMsg("Layout alterado")
    end if
end sub

sub rerender()
    keep = m.bIdx
    render()
    m.bIdx = keep
    if m.bIdx >= m.opts.Count() then m.bIdx = m.opts.Count() - 1
    if m.bIdx < 0 then m.bIdx = 0
    m.zone = "content"
    drawOpts()
    paint()
end sub

' ---------------------------------------------------------------- senha
sub askPin(title as string, subtitle as string, stage as string)
    if m.pinBox <> invalid then m.top.removeChild(m.pinBox)
    d = CreateObject("roSGNode", "PinBox")
    d.setup = { title: title, subtitle: subtitle }
    d.observeField("result", "onPin")
    m.top.appendChild(d)
    m.pinBox = d
    m.pinStage = stage
    d.setFocus(true)
end sub

sub closePin()
    if m.pinBox <> invalid then m.top.removeChild(m.pinBox)
    m.pinBox = invalid
    m.top.setFocus(true)
end sub

sub onPin()
    v = m.pinBox.result
    if v = "cancel"
        closePin()
        return
    end if
    st = m.pinStage
    if st = "adult"
        if v = pinGet()
            closePin()
            prefSet("adult", m.pendingAdult)
            m.global.adultUnlocked = false
            rerender()
            showMsg("Alterado")
        else
            m.pinBox.error = "Senha incorreta"
        end if
    else if st = "pin1"
        if v = pinGet() then askPin("Nova senha", "Digite a nova senha de 4 números.", "pin2") else m.pinBox.error = "Senha incorreta"
    else if st = "pin2"
        m.newPin = v
        askPin("Confirme a nova senha", "", "pin3")
    else if st = "pin3"
        if v = m.newPin
            prefSet("pin", v)
            closePin()
            showMsg("Senha alterada")
        else
            m.pinBox.error = "As senhas não conferem"
        end if
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.dialog <> invalid or m.pinBox <> invalid then return true
    if m.zone = "menu"
        if key = "up" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "down" and m.idx < m.items.Count() - 1
            m.idx = m.idx + 1
        else if key = "OK" or key = "right"
            m.sel = m.idx
            m.bIdx = 0
            m.scroll = 0
            render()
            if m.opts.Count() > 0 then m.zone = "content"
        else
            return false
        end if
        paint()
        return true
    end if
    op = m.opts[m.bIdx]
    if key = "back"
        m.zone = "menu"
    else if op.kind = "wall"
        if key = "left" and m.bIdx mod 4 = 0
            m.zone = "menu"
        else if key = "left"
            m.bIdx = m.bIdx - 1
        else if key = "right" and m.bIdx < m.opts.Count() - 1
            m.bIdx = m.bIdx + 1
        else if key = "up" and m.bIdx > 3
            m.bIdx = m.bIdx - 4
        else if key = "down" and m.bIdx + 4 < m.opts.Count()
            m.bIdx = m.bIdx + 4
        else if key = "OK"
            pressOpt()
            return true
        end if
    else if op.kind = "tabs"
        if key = "left" and m.hideTab > 0
            m.hideTab = m.hideTab - 1
            m.scroll = 0
            rerender()
            return true
        else if key = "left"
            m.zone = "menu"
        else if key = "right" and m.hideTab < 2
            m.hideTab = m.hideTab + 1
            m.scroll = 0
            rerender()
            return true
        else if key = "down" and m.opts.Count() > 1
            m.bIdx = 1
            drawOpts()
        end if
    else
        if key = "left"
            m.zone = "menu"
        else if key = "up" and m.bIdx > 0
            m.bIdx = m.bIdx - 1
            drawOpts()
        else if key = "down" and m.bIdx < m.opts.Count() - 1
            m.bIdx = m.bIdx + 1
            drawOpts()
        else if key = "OK"
            pressOpt()
            return true
        end if
    end if
    paint()
    return true
end function
