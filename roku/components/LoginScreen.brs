' Login Xtream Codes no visual do Orbita: campos em caixas e botão Entrar.
sub init()
    m.site = siteUrl()
    r = m.top.findNode("root")
    if m.site <> "" then buildActCard(m.top.findNode("root"))
    r = r.createChild("Group")
    if m.site <> "" then r.translation = [380, 0]
    addPoster(r, "pkg:/images/logo_full.png", 810, 90, 300, 52)
    addPoster(r, "pkg:/images/panel_login.png", 560, 190, 800, 760)
    addLabel(r, "Conectar lista", 600, 226, OUTF(), 40, "0xFFFFFFFF", 720, "center")
    addLabel(r, "Informe os dados Xtream Codes do seu provedor", 600, 284, IREG(), 20, "0x9D9ABBFF", 720, "center")
    m.values = { server: "", user: "", pass: "" }
    m.keys = ["server", "user", "pass"]
    m.labels = ["SERVIDOR", "USUÁRIO", "SENHA"]
    m.fields = []
    for i = 0 to 2
        g = r.createChild("Group")
        g.translation = [620, 340 + i * 120]
        addLabel(g, m.labels[i], 4, 0, ISEM(), 16, "0x9D9ABBFF")
        bg = addPoster(g, "", 0, 28, 680, 76)
        vl = addLabel(g, "", 24, 51, IMED(), 24, "0xFFFFFFFF", 632)
        m.fields.Push({ bg: bg, value: vl })
    end for
    m.btn = addButton(r, "Entrar", "ic_play", true, 620, 712)
    m.btn.bg.width = 680
    m.btn.group.scaleRotateCenter = [340, 34]
    m.btn.label.translation = [310, 21]
    m.btn.icon.translation = [272, 21]
    m.msg = addLabel(r, "", 600, 810, IMED(), 20, "0xF43F5EFF", 720, "center")
    m.msg.wrap = true
    m.msg.maxLines = 3
    m.timeout = m.top.findNode("timeout")
    m.timeout.observeField("fire", "onTimeout")
    m.idx = 0
    m.busy = false
    m.locked = false
    refresh()
    if m.site <> ""
        poll = m.top.findNode("poll")
        poll.observeField("fire", "pollSite")
        poll.control = "start"
        startActTask(true)
    end if
end sub

' Cartão "Ative pelo celular": QR code, MAC, chave e situação (vem do site).
sub buildActCard(r as object)
    g = r.createChild("Group")
    g.translation = [140, 190]
    addPoster(g, "pkg:/images/panel_act.png", 0, 0, 600, 760)
    addLabel(g, "Ative pelo celular", 40, 36, OUTF(), 36, "0xFFFFFFFF", 520, "center")
    addLabel(g, "Aponte a câmera para o QR code ou acesse", 40, 90, IREG(), 19, "0x9D9ABBFF", 520, "center")
    host = m.site.Replace("https://", "").Replace("http://", "")
    addLabel(g, host, 40, 118, ISEM(), 19, "0x9B84FFFF", 520, "center")
    addRect(g, 150, 170, 300, 300, "0xFFFFFFFF")
    m.qrBox = g.createChild("Group")
    m.qrBox.translation = [162, 182]
    m.qrWait = addLabel(g, "Gerando QR code…", 150, 310, IMED(), 18, "0x6E6B8CFF", 300, "center")
    addLabel(g, "MAC", 60, 510, ISEM(), 16, "0x6E6B8CFF")
    m.macLbl = addLabel(g, "…", 60, 534, OUTF(), 32, "0xFFFFFFFF", 500)
    addLabel(g, "CHAVE", 60, 590, ISEM(), 16, "0x6E6B8CFF")
    m.keyLbl = addLabel(g, "…", 60, 614, OUTF(), 32, "0xFFFFFFFF", 500)
    m.actStatus = addLabel(g, "Conectando ao site…", 60, 680, ISEM(), 20, "0x9D9ABBFF", 500)
end sub

sub pollSite()
    startActTask(false)
end sub

sub drawQr(q as object)
    m.qrBox.removeChildrenIndex(m.qrBox.getChildCount(), 0)
    n = q.size
    cell = Int(276 / n)
    off = Int((276 - cell * n) / 2)
    for y = 0 to n - 1
        row = q.rows[y]
        x = 0
        while x < n
            if Mid(row, x + 1, 1) = "1"
                x0 = x
                while x < n and Mid(row, x + 1, 1) = "1"
                    x = x + 1
                end while
                addRect(m.qrBox, off + x0 * cell, off + y * cell, (x - x0) * cell, cell, "0x0B0A12FF")
            else
                x = x + 1
            end if
        end while
    end for
    m.qrWait.visible = false
end sub

sub onAct(event as object)
    r = event.getData()
    if r.mac <> invalid then m.macLbl.text = r.mac
    if r.key <> invalid then m.keyLbl.text = r.key
    if r.qr <> invalid then drawQr(r.qr)
    if not r.ok or type(r.data) <> "roAssociativeArray"
        m.actStatus.text = "Sem conexão com o site"
        m.actStatus.color = "0x9D9ABBFF"
        return
    end if
    d = r.data
    st = asText(d.status)
    m.locked = (st = "expired" or st = "blocked")
    if st = "trial"
        m.actStatus.text = "Teste grátis · " + asText(d.days_left) + " dias"
        m.actStatus.color = "0xFFC56BFF"
    else if st = "active"
        if d.days_left = invalid then m.actStatus.text = "Ativo · vitalício" else m.actStatus.text = "Ativo · " + asText(d.days_left) + " dias"
        m.actStatus.color = "0x7EE2A8FF"
    else if st = "blocked"
        m.actStatus.text = "Aparelho bloqueado"
        m.actStatus.color = "0xFF8095FF"
    else
        m.actStatus.text = "Vencido · ative pelo celular"
        m.actStatus.color = "0xFF8095FF"
    end if
    if m.locked then return
    ' lista cadastrada no site: entra sozinho
    if type(d.playlists) = "roArray" and d.playlists.Count() > 0
        c = playlistCreds(d.playlists[0])
        if c <> invalid and not m.busy
            m.values = { server: c.server, user: c.user, pass: c.pass }
            refresh()
            login()
        end if
    end if
end sub

sub refresh()
    for i = 0 to 2
        v = m.values[m.keys[i]]
        if i = 2 and v <> "" then v = String(Len(v), "•")
        f = m.fields[i]
        if v = ""
            f.value.text = "Toque OK para preencher"
            f.value.color = "0x6E6B8CFF"
        else
            f.value.text = v
            f.value.color = "0xFFFFFFFF"
        end if
        if i = m.idx then f.bg.uri = "pkg:/images/field_f.9.png" else f.bg.uri = "pkg:/images/field_n.9.png"
    end for
    paintButton(m.btn, m.idx = 3)
end sub

sub edit(i as integer)
    m.editing = m.keys[i]
    d = CreateObject("roSGNode", "StandardKeyboardDialog")
    d.title = m.labels[i]
    d.text = m.values[m.editing]
    if i = 0 then d.message = ["Ex.: http://meuservidor.com:8080"]
    d.buttons = ["OK", "Cancelar"]
    d.observeField("buttonSelected", "onDialog")
    m.dialog = d
    m.top.getScene().dialog = d
end sub

sub onDialog()
    if m.dialog.buttonSelected = 0 then m.values[m.editing] = m.dialog.text.Trim()
    m.dialog.close = true
    refresh()
    m.top.setFocus(true)
end sub

sub login()
    if m.locked
        m.msg.color = "0xF43F5EFF"
        m.msg.text = "Este aparelho precisa ser ativado pelo site."
        return
    end if
    s = m.values.server
    if s = "" or m.values.user = "" or m.values.pass = ""
        m.msg.text = "Preencha servidor, usuário e senha."
        return
    end if
    if Left(LCase(s), 4) <> "http" then s = "http://" + s
    while Right(s, 1) = "/"
        s = Left(s, Len(s) - 1)
    end while
    m.values.server = s
    m.global.creds = { server: s, user: m.values.user, pass: m.values.pass }
    m.msg.color = "0x9D9ABBFF"
    m.msg.text = "Conectando a " + s + " …"
    m.busy = true
    m.timeout.control = "start"
    callApi("login", apiUrl(""))
end sub

sub onTimeout()
    if not m.busy then return
    m.busy = false
    m.msg.color = "0xF43F5EFF"
    m.msg.text = "O servidor não respondeu. Confira o endereço (com http:// ou https:// e a porta, se houver)."
end sub

sub onApi(event as object)
    m.timeout.control = "stop"
    m.busy = false
    r = event.getData()
    ok = false
    if r.ok and type(r.data) = "roAssociativeArray" and type(r.data.user_info) = "roAssociativeArray"
        ok = asText(r.data.user_info.auth) = "1"
    end if
    if ok
        reg = CreateObject("roRegistrySection", "orbita")
        reg.Write("server", m.values.server)
        reg.Write("user", m.values.user)
        reg.Write("pass", m.values.pass)
        reg.Flush()
        if m.site <> "" then m.top.findNode("poll").control = "stop"
        m.top.action = { type: "home" }
        return
    end if
    m.msg.color = "0xF43F5EFF"
    if r.code = 0
        m.msg.text = "Sem resposta do servidor. Confira o endereço e a internet da TV."
    else if r.code = 200
        m.msg.text = "Usuário ou senha incorretos, ou a lista está vencida."
    else
        m.msg.text = "Não foi possível entrar (código " + asText(r.code) + ")."
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "up" and m.idx > 0
        m.idx = m.idx - 1
    else if key = "down" and m.idx < 3
        m.idx = m.idx + 1
    else if key = "OK"
        if m.idx < 3 then edit(m.idx) else login()
        return true
    else
        return false
    end if
    refresh()
    return true
end function
