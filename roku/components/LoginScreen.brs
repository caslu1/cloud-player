' Login Xtream Codes no visual do Orbita: campos em caixas e botão Entrar.
sub init()
    r = m.top.findNode("root")
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
    refresh()
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
