'Linha de canal igual ao ChannelRow do Android: número, logo, nome, programa de agora e a estrela ao lado.
sub init()
    m.focus = addPoster(m.top, "pkg:/images/ch_focus.png", 0, 0, 520, 88)
    m.num = addLabel(m.top, "", 16, 32, ISEM(), 20, "0x9D9ABBFF", 64)
    m.logoBg = addPoster(m.top, "pkg:/images/logo_box.png", 82, 14, 84, 60)
    m.abbr = addLabel(m.top, "", 82, 33, ISEM(), 15, "0xFFFFFFFF", 84, "center")
    m.logo = addPoster(m.top, "", 88, 20, 72, 48)
    m.logo.loadDisplayMode = "scaleToFit"
    m.name = addLabel(m.top, "", 182, 18, ISEM(), 23, "0xFFFFFFFF", 324)
    m.prog = addLabel(m.top, "", 182, 52, IREG(), 18, "0x9D9ABBFF", 324)
    m.star = addPoster(m.top, "pkg:/images/star_btn_n.png", 532, 10, 64, 68)
end sub

function hashColor(s as string) as string
    pal = ["0x5B3FD0FF", "0x2F7FD6FF", "0x1F9D63FF", "0xC2408FFF", "0xD9822BFF", "0x7A3FD0FF", "0x2BA3A3FF", "0xB8452EFF"]
    h = 0
    for i = 1 to Len(s)
        h = (h * 31 + Asc(Mid(s, i, 1))) mod 9973
    end for
    return pal[h mod pal.Count()]
end function

sub onContent()
    c = m.top.itemContent
    if c = invalid then return
    m.num.text = c.num
    m.name.text = c.title
    m.logo.uri = c.logo
    ' sem logo: sigla num quadrado colorido (igual ao Android)
    if c.logo = ""
        m.abbr.text = UCase(Left(c.title, 5))
        m.logoBg.blendColor = hashColor(c.title)
    else
        m.abbr.text = ""
        m.logoBg.blendColor = "0x1B1828FF"
    end if
    c.observeField("fav", "paint")
    c.observeField("prog", "paint")
    c.observeField("starFocus", "paint")
    c.observeField("playing", "paint")
    paint()
end sub

sub paint()
    c = m.top.itemContent
    if c = invalid then return
    f = m.top.listHasFocus and m.top.focusPercent > 0.5
    m.focus.visible = f and not c.starFocus
    if c.playing and c.prog = "" then m.prog.text = "Assistindo agora" else m.prog.text = c.prog
    if c.playing then m.prog.color = "0x22D3EEFF" else m.prog.color = "0x9D9ABBFF"
    if f or c.playing then m.num.color = "0x22D3EEFF" else m.num.color = "0x9D9ABBFF"
    if c.prog = "" and not c.playing then m.name.translation = [182, 32] else m.name.translation = [182, 18]
    st = "n"
    if c.fav then st = "on"
    if f and c.starFocus then st = st + "_f"
    m.star.uri = "pkg:/images/star_btn_" + st + ".png"
end sub
