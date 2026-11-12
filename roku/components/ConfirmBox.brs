' Janela de confirmação (igual à do Android): ícone, título, mensagem, Cancelar | Confirmar.
' setup = { title, message, confirm, icon, danger }  →  result = "ok" ou "cancel"
sub init()
    m.idx = 0
end sub

sub onSetup()
    s = m.top.setup
    m.top.removeChildrenIndex(m.top.getChildCount(), 0)
    addPoster(m.top, "pkg:/images/dim.png", 0, 0, 1920, 1080)
    g = m.top.createChild("Group")
    g.translation = [620, 330]
    addPoster(g, "pkg:/images/dialog.png", 0, 0, 680, 420)
    red = (s.danger = true)
    if red then addPoster(g, "pkg:/images/dialog_icon_red.png", 296, 44, 88, 88) else addPoster(g, "pkg:/images/dialog_icon.png", 296, 44, 88, 88)
    ic = addPoster(g, "pkg:/images/" + s.icon + ".png", 322, 70, 36, 36)
    if red then ic.blendColor = "0xF43F5EFF" else ic.blendColor = "0x9B84FFFF"
    addLabel(g, s.title, 40, 156, OUTF(), 34, "0xFFFFFFFF", 600, "center")
    msg = addLabel(g, s.message, 60, 208, IREG(), 20, "0x9D9ABBFF", 560, "center")
    msg.wrap = true
    msg.maxLines = 3
    m.btns = []
    b1 = addButton(g, "Cancelar", "", false, 0, 316)
    b2 = addButton(g, s.confirm, s.icon, true, 0, 316)
    total = b1.width + 20 + b2.width
    b1.group.translation = [(680 - total) / 2, 316]
    b2.group.translation = [(680 - total) / 2 + b1.width + 20, 316]
    m.btns = [b1, b2]
    m.idx = 0
    paint()
end sub

sub paint()
    for i = 0 to m.btns.Count() - 1
        paintButton(m.btns[i], i = m.idx)
    end for
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return true
    if key = "left"
        m.idx = 0
    else if key = "right"
        m.idx = 1
    else if key = "OK"
        if m.idx = 1 then m.top.result = "ok" else m.top.result = "cancel"
        return true
    else if key = "back"
        m.top.result = "cancel"
        return true
    end if
    paint()
    return true
end function
