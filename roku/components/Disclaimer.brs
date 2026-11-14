' Aviso importante (igual ao Android): aparece só no primeiro acesso, OK em "Entendo" fecha.
sub init()
    addPoster(m.top, "pkg:/images/dim.png", 0, 0, 1920, 1080)
    g = m.top.createChild("Group")
    g.translation = [580, 170]
    addPoster(g, "pkg:/images/dialog_tall.png", 0, 0, 760, 740)
    addLabel(g, "Aviso importante", 0, 50, OUTF(), 34, "0xFFFFFFFF", 760, "center")
    addPoster(g, "pkg:/images/dialog_icon.png", 324, 116, 112, 112)
    ic = addPoster(g, "pkg:/images/ic_info.png", 356, 148, 48, 48)
    ic.blendColor = "0xC9A227FF"
    addRect(g, 50, 260, 660, 300, "0x26242EFF")
    texts = ["O aplicativo é apenas uma plataforma de tecnologia. Não somos responsáveis por qualquer conteúdo adicionado pelos usuários.", "Não fornecemos nem vendemos canais de TV, assinaturas, conteúdo de streaming ou serviços de IPTV. A disponibilidade e o uso de todo o conteúdo são responsabilidade do usuário."]
    y = 286
    for each t in texts
        dot = addPoster(g, "pkg:/images/ic_info.png", 78, y + 2, 22, 22)
        dot.blendColor = "0xC9A227FF"
        l = addLabel(g, t, 114, y, IREG(), 19, "0xE7E5F7FF", 570)
        l.wrap = true
        l.maxLines = 4
        y = y + 136
    end for
    m.btn = addButton(g, "Entendo", "", true, 0, 610)
    m.btn.group.translation = [(760 - m.btn.width) / 2, 610]
    paintButton(m.btn, true)
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if press and key = "OK" then m.top.done = true
    return true
end function
