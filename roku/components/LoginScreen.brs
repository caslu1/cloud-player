' Login Xtream Codes no visual do Orbita: campos em caixas e botão Entrar.
sub init()
    m.site = siteUrl()
    r = m.top.findNode("root")
    ' Login padrão (igual ao Android): "Pelo celular" | ou | "Na TV"
    addPoster(r, "pkg:/images/logo_full.png", 96, 64, 260, 52)
    m.title = addLabel(r, "Bem-vindo ao Orbita Play", 96, 146, OUTF(), 54, "0xFFFFFFFF")
    m.adding = false
    c0 = m.global.creds
    m.prevCreds = { server: asText(c0.server), user: asText(c0.user), pass: asText(c0.pass) }
    m.top.observeField("params", "onParams")
    sub_ = "Conecte sua lista para começar."
    if m.site <> "" then sub_ = "Ative pelo celular em segundos ou conecte sua lista direto na TV."
    addLabel(r, sub_, 96, 222, IREG(), 22, "0x9D9ABBFF")
    cx = 508
    if m.site <> ""
        buildActCard(r)
        addPoster(r, "pkg:/images/ou_circle.png", 828, 610, 60, 60)
        addLabel(r, "ou", 828, 627, ISEM(), 18, "0x9D9ABBFF", 60, "center")
        cx = 920
    end if
    addPoster(r, "pkg:/images/panel_card_r.png", cx, 290, 904, 700)
    addLabel(r, "NA TV", cx + 40, 330, ISEM(), 16, "0x9B84FFFF")
    addLabel(r, "Conectar lista", cx + 40, 352, OUTF(), 38, "0xFFFFFFFF")
    addLabel(r, "Use os dados enviados pelo seu provedor.", cx + 40, 410, IREG(), 20, "0x9D9ABBFF")
    ' abas
    addPoster(r, "pkg:/images/tabs_bg.png", cx + 40, 456, 824, 64)
    m.tabs = []
    names = ["Conta XC", "Lista de reprodução"]
    for i = 0 to 1
        bg = addPoster(r, "", cx + 46 + i * 406, 462, 406, 52)
        lb = addLabel(r, names[i], cx + 46 + i * 406, 476, ISEM(), 20, "0xFFFFFFFF", 406, "center")
        m.tabs.Push({ bg: bg, label: lb })
    end for
    m.cx = cx
    m.formBox = r.createChild("Group")
    m.values = { name: "", server: "", user: "", pass: "", url: "" }
    m.xtream = true
    m.msg = addLabel(r, "", cx + 40, 906, IMED(), 19, "0xF43F5EFF", 824)
    m.msg.wrap = true
    m.msg.maxLines = 2
    m.foot = addLabel(r, "Sem lista? Fale com o seu provedor.", cx + 40, 940, IREG(), 17, "0x6E6B8CFF")
    m.timeout = m.top.findNode("timeout")
    m.timeout.observeField("fire", "onTimeout")
    m.idx = 1
    m.busy = false
    m.locked = false
    buildForm()
    if m.site <> ""
        poll = m.top.findNode("poll")
        poll.observeField("fire", "pollSite")
        poll.control = "start"
        startActTask(true)
    end if
end sub

' Campos conforme a aba: Xtream (servidor, usuário, senha) ou M3U (link).
sub buildForm()
    m.formBox.removeChildrenIndex(m.formBox.getChildCount(), 0)
    cx = m.cx
    m.fields = []
    if m.xtream
        m.order = ["tabs", "name", "server", "user", "pass", "btn"]
        addField("name", "NOME DA LISTA", cx + 40, 560, 320)
        addField("server", "PROVEDOR", cx + 380, 560, 484)
        addField("user", "USUÁRIO", cx + 40, 690, 402)
        addField("pass", "SENHA", cx + 462, 690, 402)
        by = 820
    else
        m.order = ["tabs", "name", "url", "btn"]
        addField("name", "NOME DA LISTA", cx + 40, 548, 824)
        addField("url", "LISTA DE REPRODUÇÃO (LINK)", cx + 40, 664, 824)
        addLabel(m.formBox, "Na Roku, use o link get.php do seu provedor (vira login Xtream).", cx + 40, 778, IREG(), 17, "0x6E6B8CFF", 824)
        by = 812
    end if
    m.btn = addButton(m.formBox, "Entrar", "ic_play", true, cx + 40, by)
    m.btn.bg.width = 824
    m.btn.bg.height = 76
    m.btn.group.scaleRotateCenter = [412, 38]
    m.btn.label.translation = [382, 25]
    m.btn.icon.translation = [344, 25]
    m.foot.translation = [cx + 40, by + 120]
    m.msg.translation = [cx + 40, by + 86]
    if m.idx >= m.order.Count() then m.idx = m.order.Count() - 1
    refresh()
end sub

sub addField(key as string, label as string, x as integer, y as integer, w as integer)
    addLabel(m.formBox, label, x + 2, y, ISEM(), 15, "0x6E6B8CFF")
    bg = addPoster(m.formBox, "", x, y + 28, w, 76)
    vl = addLabel(m.formBox, "", x + 24, y + 51, IMED(), 24, "0xFFFFFFFF", w - 48)
    m.fields.Push({ key: key, label: label, bg: bg, value: vl })
end sub

sub refresh()
    cur = m.order[m.idx]
    for each f in m.fields
        v = m.values[f.key]
        if f.key = "pass" and v <> "" then v = String(Len(v), "•")
        if v = ""
            if f.key = "name" then f.value.text = "Ex.: Minha lista" else f.value.text = "Toque OK para preencher"
            f.value.color = "0x6E6B8CFF"
        else
            f.value.text = v
            f.value.color = "0xFFFFFFFF"
        end if
        if f.key = cur then f.bg.uri = "pkg:/images/field_f.9.png" else f.bg.uri = "pkg:/images/field_n.9.png"
    end for
    for i = 0 to 1
        t = m.tabs[i]
        sel = (i = 0) = m.xtream
        f = (cur = "tabs")
        if sel and f
            t.bg.uri = "pkg:/images/tab_focus.9.png"
        else if sel
            t.bg.uri = "pkg:/images/tab_on.9.png"
        else
            t.bg.uri = ""
        end if
        if sel then t.label.color = "0xFFFFFFFF" else t.label.color = "0x9D9ABBFF"
    end for
    paintButton(m.btn, cur = "btn")
end sub

sub onParams()
    p = m.top.params
    if p <> invalid and p.adding = true
        m.adding = true
        m.title.text = "Adicionar playlist"
    end if
end sub

sub edit(key as string)
    m.editing = key
    d = CreateObject("roSGNode", "StandardKeyboardDialog")
    for each f in m.fields
        if f.key = key then d.title = f.label
    end for
    d.text = m.values[key]
    if key = "server" then d.message = ["Ex.: http://provedor.com:8080"]
    if key = "url" then d.message = ["Ex.: http://servidor.com/get.php?username=...&password=..."]
    d.buttons = ["OK", "Cancelar"]
    d.observeField("buttonSelected", "onDialog")
    m.dialog = d
    m.top.getScene().dialog = d
end sub

' Cartão "Pelo celular": QR code, MAC, chave e situação (vem do site).
sub buildActCard(r as object)
    g = r.createChild("Group")
    g.translation = [96, 290]
    addPoster(g, "pkg:/images/panel_card_l.png", 0, 0, 700, 700)
    addLabel(g, "PELO CELULAR", 40, 40, ISEM(), 16, "0x9B84FFFF")
    addLabel(g, "Ative com o QR code", 40, 62, OUTF(), 38, "0xFFFFFFFF")
    addLabel(g, "Aponte a câmera do celular ou acesse", 40, 120, IREG(), 20, "0x9D9ABBFF")
    host = m.site.Replace("https://", "").Replace("http://", "")
    addLabel(g, host, 40, 148, ISEM(), 20, "0x9B84FFFF", 620)
    addPoster(g, "pkg:/images/qr_bg.png", 40, 196, 300, 300)
    m.qrBox = g.createChild("Group")
    m.qrBox.translation = [56, 212]
    m.qrWait = addLabel(g, "Gerando QR code…", 40, 336, IMED(), 18, "0x6E6B8CFF", 300, "center")
    addPoster(g, "pkg:/images/info_box.png", 370, 216, 290, 86)
    addLabel(g, "MAC", 392, 230, ISEM(), 15, "0x6E6B8CFF")
    m.macLbl = addLabel(g, "…", 392, 252, OUTF(), 26, "0xFFFFFFFF", 260)
    addPoster(g, "pkg:/images/info_box.png", 370, 320, 290, 86)
    addLabel(g, "CHAVE", 392, 334, ISEM(), 15, "0x6E6B8CFF")
    m.keyLbl = addLabel(g, "…", 392, 356, OUTF(), 30, "0xFFFFFFFF", 260)
    m.pill = addPoster(g, "pkg:/images/pill_w.9.png", 370, 426, 250, 44)
    m.pill.blendColor = "0x22203AFF"
    m.actStatus = addLabel(g, "Conectando ao site…", 390, 436, ISEM(), 19, "0x9D9ABBFF", 230)
    addLabel(g, "Cadastre sua lista no site e a TV entra sozinha.", 40, 540, IREG(), 19, "0x9D9ABBFF", 620)
end sub

sub pollSite()
    startActTask(false)
end sub

sub drawQr(q as object)
    m.qrBox.removeChildrenIndex(m.qrBox.getChildCount(), 0)
    n = q.size
    cell = Int(268 / n)
    off = Int((268 - cell * n) / 2)
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
        m.pill.blendColor = "0x3A2A10FF"
    else if st = "active"
        if d.days_left = invalid then m.actStatus.text = "Ativo · vitalício" else m.actStatus.text = "Ativo · " + asText(d.days_left) + " dias"
        m.actStatus.color = "0x7EE2A8FF"
        m.pill.blendColor = "0x163A2AFF"
    else if st = "blocked"
        m.actStatus.text = "Aparelho bloqueado"
        m.actStatus.color = "0xFF8095FF"
        m.pill.blendColor = "0x3A1620FF"
    else
        m.actStatus.text = "Vencido · ative pelo celular"
        m.actStatus.color = "0xFF8095FF"
        m.pill.blendColor = "0x3A1620FF"
    end if
    if m.locked then return
    ' lista cadastrada no site: entra sozinho
    ' a primeira lista do site que não foi apagada no app (apagada não volta sozinha)
    c = invalid
    if type(d.playlists) = "roArray"
        for each pl in d.playlists
            pc = playlistCreds(pl)
            if pc <> invalid and not isRemovedList(pc.server, pc.user)
                c = pc
                c.name = asText(pl.name)
                exit for
            end if
        end for
    end if
    if c <> invalid
        if c <> invalid and not m.busy and not m.adding
            m.values = { name: asText(c.name), server: c.server, user: c.user, pass: c.pass, url: "" }
            m.xtream = true
            buildForm()
            login()
        end if
    end if
end sub



sub onDialog()
    if m.dialog.buttonSelected = 0 then m.values[m.editing] = m.dialog.text.Trim()
    m.dialog.close = true
    refresh()
    m.top.setFocus(true)
end sub

sub login()
    if not m.xtream
        c = playlistCreds({ type: "m3u", url: m.values.url })
        if c = invalid
            m.msg.color = "0xF43F5EFF"
            m.msg.text = "Use o link get.php do seu provedor (com username e password)."
            return
        end if
        m.values.server = c.server
        m.values.user = c.user
        m.values.pass = c.pass
    end if
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
        savePlaylist(m.values.name, m.values.server, m.values.user, m.values.pass)
        if m.site <> "" then m.top.findNode("poll").control = "stop"
        m.top.action = { type: "home" }
        return
    end if
    ' adicionando playlist: se não entrou, continua com a lista que estava em uso
    if m.adding then m.global.creds = m.prevCreds
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
    cur = m.order[m.idx]
    ' mapa de setas: nome e servidor na mesma linha; usuário e senha na mesma linha
    if m.xtream
        nav = { tabs: { down: "name" }, name: { right: "server", down: "user", up: "tabs" }, server: { left: "name", down: "user", up: "tabs" }, user: { right: "pass", up: "name", down: "btn" }, pass: { left: "user", up: "server", down: "btn" }, btn: { up: "user" } }
    else
        nav = { tabs: { down: "name" }, name: { down: "url", up: "tabs" }, url: { up: "name", down: "btn" }, btn: { up: "url" } }
    end if
    dest = invalid
    if nav[cur] <> invalid then dest = nav[cur][key]
    if dest <> invalid
        for i = 0 to m.order.Count() - 1
            if m.order[i] = dest then m.idx = i
        end for
    else if (key = "left" or key = "right") and cur = "tabs"
        m.xtream = (key = "left")
        m.idx = 0
        buildForm()
        return true
    else if key = "OK"
        if cur = "tabs"
            m.xtream = not m.xtream
            buildForm()
        else if cur = "btn"
            login()
        else
            edit(cur)
        end if
        return true
    else
        return false
    end if
    refresh()
    return true
end function
