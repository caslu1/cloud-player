' Botões de ação (iguais ao ActionButton do Android), desenhados com imagens 9-patch.
function addButton(parent as object, text as string, icon as string, primary as boolean, x as integer, y as integer) as object
    g = parent.createChild("Group")
    g.translation = [x, y]
    w = Len(text) * 13 + 74
    if icon = "" then w = w - 34
    bg = addPoster(g, "", 0, 0, w, 68)
    tx = 26
    ic = invalid
    if icon <> ""
        ic = addPoster(g, "pkg:/images/" + icon + ".png", 24, 21, 26, 26)
        tx = 58
    end if
    lb = addLabel(g, text, tx, 21, ISEM(), 22, "0xFFFFFFFF")
    g.scaleRotateCenter = [w / 2, 34]
    return { group: g, bg: bg, icon: ic, label: lb, primary: primary, width: w }
end function

sub paintButton(b as object, focused as boolean)
    if b.primary
        if focused then b.bg.uri = "pkg:/images/btn_primary_f.9.png" else b.bg.uri = "pkg:/images/btn_primary_n.9.png"
    else
        if focused then b.bg.uri = "pkg:/images/btn_f.9.png" else b.bg.uri = "pkg:/images/btn_n.9.png"
    end if
    if focused then b.group.scale = [1.05, 1.05] else b.group.scale = [1.0, 1.0]
end sub
