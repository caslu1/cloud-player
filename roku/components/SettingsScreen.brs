sub init()
    m.menu = m.top.findNode("menu")
    root = CreateObject("roSGNode", "ContentNode")
    n = root.createChild("ContentNode")
    n.title = "Sair da conta"
    m.menu.content = root
    m.menu.observeField("itemSelected", "onSelect")
    m.top.observeField("focusedChild", "onFocus")
    c = m.global.creds
    m.top.findNode("info").text = "Servidor:  " + c.server + chr(10) + "Usuário:  " + c.user + chr(10) + "Carregando status da conta…"
    callApi("acc", apiUrl(""))
end sub

sub onFocus()
    if m.top.hasFocus() then m.menu.setFocus(true)
end sub

sub onApi(event as object)
    r = event.getData()
    c = m.global.creds
    t = "Servidor:  " + c.server + chr(10) + "Usuário:  " + c.user
    if r.ok and type(r.data) = "roAssociativeArray" and type(r.data.user_info) = "roAssociativeArray"
        u = r.data.user_info
        st = asText(u.status)
        if LCase(st) = "active" then st = "Ativa"
        t = t + chr(10) + "Status:  " + st
        exp = asText(u.exp_date)
        if exp <> "" and exp <> "0"
            d = CreateObject("roDateTime")
            d.FromSeconds(Int(Val(exp)))
            t = t + chr(10) + "Vencimento:  " + d.AsDateString("short-date")
        end if
        t = t + chr(10) + "Conexões:  " + asText(u.active_cons) + " de " + asText(u.max_connections)
    end if
    t = t + chr(10) + "Orbita Play para Roku 1.0"
    m.top.findNode("info").text = t
end sub

sub onSelect()
    m.top.action = { type: "logout" }
end sub
