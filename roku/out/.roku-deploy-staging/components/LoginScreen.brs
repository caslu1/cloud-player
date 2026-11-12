sub init()
    m.form = m.top.findNode("form")
    m.msg = m.top.findNode("msg")
    m.values = { server: "", user: "", pass: "" }
    m.keys = ["server", "user", "pass"]
    m.labels = ["Servidor", "Usuário", "Senha"]
    m.form.observeField("itemSelected", "onSelect")
    m.top.observeField("focusedChild", "onFocus")
    refresh()
end sub

sub onFocus()
    if m.top.hasFocus() then m.form.setFocus(true)
end sub

sub refresh()
    root = CreateObject("roSGNode", "ContentNode")
    for i = 0 to 2
        v = m.values[m.keys[i]]
        if i = 2 and v <> "" then v = String(Len(v), "•")
        if v = "" then v = "(toque para preencher)"
        n = root.createChild("ContentNode")
        n.title = m.labels[i] + ":   " + v
    end for
    n = root.createChild("ContentNode")
    n.title = "Entrar"
    idx = m.form.itemFocused
    m.form.content = root
    if idx > 0 then m.form.jumpToItem = idx
end sub

sub onSelect()
    i = m.form.itemSelected
    if i < 3
        m.editing = m.keys[i]
        d = CreateObject("roSGNode", "StandardKeyboardDialog")
        d.title = m.labels[i]
        d.text = m.values[m.editing]
        if i = 0 then d.message = ["Ex.: http://meuservidor.com:8080"]
        d.buttons = ["OK", "Cancelar"]
        d.observeField("buttonSelected", "onDialog")
        m.dialog = d
        m.top.getScene().dialog = d
    else
        login()
    end if
end sub

sub onDialog()
    if m.dialog.buttonSelected = 0 then m.values[m.editing] = m.dialog.text.Trim()
    m.dialog.close = true
    refresh()
    m.form.setFocus(true)
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
    m.msg.color = "0xA59FC0FF"
    m.msg.text = "Conectando…"
    callApi("login", apiUrl(""))
end sub

sub onApi(event as object)
    r = event.getData()
    ok = false
    if r.ok and type(r.data) = "roAssociativeArray" and r.data.user_info <> invalid
        ok = asText(r.data.user_info.auth) = "1"
    end if
    if ok
        reg = CreateObject("roRegistrySection", "orbita")
        reg.Write("server", m.values.server)
        reg.Write("user", m.values.user)
        reg.Write("pass", m.values.pass)
        reg.Flush()
        m.top.action = { type: "home" }
    else
        m.msg.color = "0xFF6B81FF"
        m.msg.text = "Não foi possível entrar. Confira os dados (código " + asText(r.code) + ")."
    end if
end sub
