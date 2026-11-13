' Playlists salvas nesta TV (igual ao Android): OK usa a lista, a lixeira apaga e o + abre o login para adicionar outra.
sub init()
    r = m.top.findNode("root")
    addLabel(r, "Playlists", 260, 80, OUTF(), 48, "0xFFFFFFFF")
    m.add = addPoster(r, "pkg:/images/pl_add_n.png", 1448, 82, 108, 68)
    m.empty = addLabel(r, "Nenhuma playlist salva. Toque em + para adicionar.", 260, 190, IREG(), 22, "0x9D9ABBFF")
    ' rodapé: versão, situação, site, chave e MAC
    addLabel(r, "Versão: " + CreateObject("roAppInfo").GetVersion(), 60, 1012, IREG(), 17, "0xE7E5F7FF")
    m.status = addLabel(r, "Status: Ativado", 60, 1036, IREG(), 17, "0xE7E5F7FF")
    s = siteUrl()
    if s <> "" then addLabel(r, "Página da web: " + s, 560, 1036, IREG(), 17, "0xE7E5F7FF", 800, "center")
    addLabel(r, "Chave do dispositivo: " + deviceKey(), 1160, 1012, IREG(), 17, "0xE7E5F7FF", 700, "right")
    addLabel(r, "Endereço MAC: " + deviceMac(), 1160, 1036, IREG(), 17, "0xE7E5F7FF", 700, "right")
    m.rowsBox = m.top.findNode("rows")
    m.idx = 0
    m.col = 0
    m.top_ = 0
    m.dialog = invalid
    load()
    if m.list.Count() = 0 then m.idx = -1
    callApi("acc", apiUrl(""))
    m.top.observeField("focusedChild", "onFocus")
end sub

sub onFocus()
    if m.top.hasFocus() and m.dialog = invalid then load()
end sub

sub onApi(event as object)
    r = event.getData()
    if r.ok and type(r.data) = "roAssociativeArray" and type(r.data.user_info) = "roAssociativeArray"
        st = asText(r.data.user_info.status)
        if LCase(st) = "active" or st = "" then st = "Ativado"
        m.status.text = "Status: " + st
    end if
end sub

function isCurrent(p as object) as boolean
    c = m.global.creds
    return asText(p.server) = asText(c.server) and asText(p.user) = asText(c.user)
end function

sub load()
    m.list = regRead("playlists")
    m.empty.visible = (m.list.Count() = 0)
    if m.idx >= m.list.Count() then m.idx = m.list.Count() - 1
    draw()
end sub

' até 7 linhas na tela; a lista rola só quando o foco passa da borda
sub draw()
    m.rowsBox.removeChildrenIndex(m.rowsBox.getChildCount(), 0)
    vis = 7
    if m.idx >= 0
        if m.idx < m.top_ then m.top_ = m.idx
        if m.idx > m.top_ + vis - 1 then m.top_ = m.idx - vis + 1
    end if
    m.rows = []
    for i = m.top_ to m.list.Count() - 1
        if i > m.top_ + vis - 1 then exit for
        p = m.list[i]
        y = 188 + (i - m.top_) * 116
        f = (i = m.idx and m.col = 0)
        if f then st = "_f" else st = "_n"
        if f then fg = "0x0B0A12FF" else fg = "0xFFFFFFFF"
        addPoster(m.rowsBox, "pkg:/images/pl_row" + st + ".png", 260, y, 1296, 96)
        addLabel(m.rowsBox, (i + 1).ToStr(), 304, y + 30, IMED(), 26, fg)
        addLabel(m.rowsBox, asText(p.name), 376, y + 30, IMED(), 26, fg, 900)
        if isCurrent(p)
            pill = addPoster(m.rowsBox, "pkg:/images/pill_w.9.png", 1390, y + 30, 120, 36)
            if f then pill.blendColor = "0x7C5CFFFF" else pill.blendColor = "0x22184AFF"
            addLabel(m.rowsBox, "Em uso", 1390, y + 37, ISEM(), 16, "0xFFFFFFFF", 120, "center")
        end if
        if i = m.idx and m.col = 1 then tt = "_f" else tt = "_n"
        addPoster(m.rowsBox, "pkg:/images/pl_trash" + tt + ".png", 1584, y + 10, 76, 76)
    end for
    if m.idx = -1 then m.add.uri = "pkg:/images/pl_add_f.png" else m.add.uri = "pkg:/images/pl_add_n.png"
end sub

sub useList()
    p = m.list[m.idx]
    if isCurrent(p)
        m.top.action = { type: "home" }
    else
        m.top.action = { type: "switch", server: asText(p.server), user: asText(p.user), pass: asText(p.pass) }
    end if
end sub

sub askDelete()
    p = m.list[m.idx]
    d = CreateObject("roSGNode", "ConfirmBox")
    d.setup = { title: "Apagar " + Chr(34) + asText(p.name) + Chr(34) + "?", message: "A playlist sai desta TV. Você pode adicioná-la de novo depois.", confirm: "Apagar", icon: "ic_trash", danger: true }
    d.observeField("result", "onAsk")
    m.top.appendChild(d)
    m.dialog = d
    d.setFocus(true)
end sub

sub onAsk()
    res = m.dialog.result
    m.top.removeChild(m.dialog)
    m.dialog = invalid
    m.top.setFocus(true)
    if res <> "ok" then return
    p = m.list[m.idx]
    wasCurrent = isCurrent(p)
    m.list.Delete(m.idx)
    regWrite("playlists", m.list)
    if wasCurrent
        if m.list.Count() = 0
            m.top.action = { type: "logout" }
        else
            n = m.list[0]
            m.top.action = { type: "switch", server: asText(n.server), user: asText(n.user), pass: asText(n.pass) }
        end if
        return
    end if
    m.col = 0
    load()
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.dialog <> invalid then return true
    n = m.list.Count()
    if key = "up"
        if m.idx > 0 then m.idx = m.idx - 1 else m.idx = -1
        if m.idx = -1 then m.col = 0
    else if key = "down"
        if m.idx < n - 1 then m.idx = m.idx + 1
    else if key = "right" and m.idx >= 0
        m.col = 1
    else if key = "left" and m.idx >= 0
        m.col = 0
    else if key = "OK"
        if m.idx = -1
            m.top.action = { type: "open", screen: "LoginScreen", params: { adding: true } }
        else if m.col = 1
            askDelete()
        else
            useList()
        end if
        return true
    else
        return false
    end if
    draw()
    return true
end function
