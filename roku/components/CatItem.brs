' Linha de categoria (igual ao CatRow do Android): barra roxa = escolhida, fundo roxo = foco.
sub init()
    m.focus = addPoster(m.top, "pkg:/images/cat_focus.png", 0, 0, 374, 72)
    m.bar = addRect(m.top, 0, 0, 5, 72, "0x7C5CFFFF")
    m.icon = addPoster(m.top, "", 24, 22, 28, 28)
    m.label = addLabel(m.top, "", 24, 21, IMED(), 22, "0xE7E5F7FF", 250)
    m.label.maxLines = 1
    m.chip = addPoster(m.top, "pkg:/images/pill.9.png", 0, 21, 40, 30)
    m.count = addLabel(m.top, "", 0, 25, ISEM(), 16, "0x6E6B8CFF")
end sub

sub onContent()
    c = m.top.itemContent
    if c = invalid then return
    m.label.text = c.title
    icon = ""
    if c.hasField("icon") then icon = c.icon
    if icon <> ""
        m.icon.uri = "pkg:/images/" + icon + ".png"
        m.icon.visible = true
        m.label.translation = [64, 21]
        m.label.width = 210
    else
        m.icon.visible = false
        m.label.translation = [24, 21]
        m.label.width = 250
    end if
    cnt = ""
    if c.hasField("count") then cnt = c.count
    m.chip.visible = cnt <> ""
    m.count.visible = cnt <> ""
    if cnt <> ""
        n = Len(cnt)
        if n > 5 then n = 5
        w = [0, 40, 48, 58, 70, 82][n]
        m.chip.uri = "pkg:/images/pill.9.png"
        m.chip.width = w
        m.chip.translation = [374 - 18 - w, 21]
        m.count.text = cnt
        m.count.width = w
        m.count.horizAlign = "center"
        m.count.translation = [374 - 18 - w, 25]
    end if
    if c.hasField("selected") then c.observeField("selected", "paint")
    paint()
end sub

sub paint()
    c = m.top.itemContent
    sel = false
    if c <> invalid and c.hasField("selected") then sel = c.selected
    f = m.top.listHasFocus and m.top.focusPercent > 0.5
    m.focus.visible = f
    m.bar.visible = sel
    if f or sel
        m.label.color = "0xFFFFFFFF"
        m.label.font = mkFont(ISEM(), 22)
    else
        m.label.color = "0xE7E5F7FF"
        m.label.font = mkFont(IMED(), 22)
    end if
    if f or sel then m.icon.blendColor = "0xFFFFFFFF" else m.icon.blendColor = "0x9D9ABBFF"
end sub
