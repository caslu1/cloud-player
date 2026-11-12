' Configurações em duas colunas (igual ao Android): menu à esquerda, conteúdo à direita.
sub init()
    r = m.top.findNode("root")
    addPoster(r, "pkg:/images/logo_full.png", 72, 44, 260, 46)
    addLabel(r, "Configurações", 360, 40, OUTF(), 40, "0xFFFFFFFF")
    m.sections = [{ t: "Informações gerais", i: "ic_info" }, { t: "Sair da conta", i: "ic_logout" }]
    m.rows = []
    for i = 0 to m.sections.Count() - 1
        g = r.createChild("Group")
        g.translation = [72, 128 + i * 86]
        bg = addPoster(g, "", 0, 0, 460, 76)
        ic = addPoster(g, "pkg:/images/" + m.sections[i].i + ".png", 28, 23, 30, 30)
        color = "0xFFFFFFFF"
        if i = 1 then color = "0xF43F5EFF"
        lb = addLabel(g, m.sections[i].t, 76, 24, ISEM(), 21, color)
        m.rows.Push({ bg: bg, icon: ic })
    end for
    m.content = m.top.findNode("content")
    m.idx = 0
    m.zone = "menu"
    m.sel = 0
    m.confirm = false
    m.info = invalid
    callApi("acc", apiUrl(""))
    render()
end sub

sub onApi(event as object)
    r = event.getData()
    if r.ok and type(r.data) = "roAssociativeArray" then m.info = r.data.user_info
    render()
end sub

sub line(k as string, v as string, y as integer)
    addLabel(m.content, k, 604, y, IREG(), 20, "0x9D9ABBFF", 220)
    l = addLabel(m.content, v, 830, y, IMED(), 20, "0xFFFFFFFF", 980)
    l.maxLines = 1
end sub

sub render()
    m.content.removeChildrenIndex(m.content.getChildCount(), 0)
    addLabel(m.content, m.sections[m.sel].t, 604, 160, OUTF(), 28, "0xFFFFFFFF")
    c = m.global.creds
    m.btn = invalid
    if m.sel = 0
        line("Tipo", "Xtream Codes", 230)
        line("Servidor", c.server, 270)
        line("Usuário", c.user, 310)
        y = 350
        u = m.info
        if type(u) = "roAssociativeArray"
            st = asText(u.status)
            if LCase(st) = "active" then st = "Ativa"
            line("Status", st, y)
            y = y + 40
            exp = asText(u.exp_date)
            if exp <> "" and exp <> "0"
                d = CreateObject("roDateTime")
                d.FromSeconds(Int(Val(exp)))
                line("Validade", two(d.GetDayOfMonth()) + "/" + two(d.GetMonth()) + "/" + d.GetYear().ToStr(), y)
                y = y + 40
            end if
            line("Conexões", asText(u.active_cons) + " de " + asText(u.max_connections) + " em uso", y)
            y = y + 40
        end if
        line("MAC", deviceMac(), y)
        y = y + 40
        line("Chave", deviceKey(), y)
        y = y + 40
        line("Versão", "Orbita Play para Roku 1.0", y)
        addLabel(m.content, "O app não fornece conteúdo: use apenas listas que você tem direito de usar.", 604, y + 70, IREG(), 17, "0x6E6B8CFF", 1200)
    else
        msg = "Remove a lista desta TV e volta para a tela de login."
        if m.confirm then msg = "Aperte OK de novo para confirmar."
        addLabel(m.content, msg, 604, 230, IREG(), 20, "0x9D9ABBFF", 1200)
        m.btn = addButton(m.content, "Sair da conta", "ic_logout", false, 604, 290)
        paintButton(m.btn, m.zone = "content")
    end if
    paint()
end sub

sub paint()
    for i = 0 to m.rows.Count() - 1
        f = (m.zone = "menu" and i = m.idx)
        if f
            m.rows[i].bg.uri = "pkg:/images/set_focus.9.png"
        else if i = m.sel
            m.rows[i].bg.uri = "pkg:/images/set_sel.9.png"
        else
            m.rows[i].bg.uri = "pkg:/images/set_row.9.png"
        end if
    end for
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.zone = "menu"
        if key = "up" and m.idx > 0
            m.idx = m.idx - 1
        else if key = "down" and m.idx < m.rows.Count() - 1
            m.idx = m.idx + 1
        else if key = "OK" or key = "right"
            m.sel = m.idx
            m.confirm = false
            if m.sel = 1 then m.zone = "content"
            render()
            return true
        else
            return false
        end if
        paint()
        return true
    else
        if key = "left"
            m.zone = "menu"
            m.confirm = false
            render()
            return true
        else if key = "OK"
            if m.confirm
                m.top.action = { type: "logout" }
            else
                m.confirm = true
                render()
            end if
            return true
        end if
    end if
    return false
end function
