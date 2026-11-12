' Linha de canal (igual ao ChannelRow do Android): número, logo, nome, programa e estrela.
sub init()
    m.focus = addPoster(m.top, "pkg:/images/ch_focus.png", 0, 0, 520, 88)
    m.num = addLabel(m.top, "", 14, 30, IMED(), 22, "0x9D9ABBFF", 64)
    m.logoBg = addPoster(m.top, "pkg:/images/logo_box.png", 86, 14, 84, 60)
    m.logo = addPoster(m.top, "", 92, 20, 72, 48)
    m.logo.loadDisplayMode = "scaleToFit"
    m.abbr = addLabel(m.top, "", 86, 33, ISEM(), 14, "0xFFFFFFFF", 84, "center")
    m.name = addLabel(m.top, "", 188, 16, OUTF(), 24, "0xFFFFFFFF", 320)
    m.prog = addLabel(m.top, "", 188, 50, IREG(), 18, "0x9D9ABBFF", 320)
    m.playing = addPoster(m.top, "pkg:/images/live_dot.png", 0, 36, 14, 14)
    m.star = addPoster(m.top, "pkg:/images/star_btn_n.png", 532, 10, 64, 68)
end sub

sub onContent()
    c = m.top.itemContent
    if c = invalid then return
    m.num.text = c.num
    m.name.text = c.title
    m.prog.text = c.prog
    m.logo.uri = c.logo
    m.abbr.text = ""
    if c.logo = "" then m.abbr.text = UCase(Left(c.title, 5))
    m.playing.visible = false
    c.observeField("fav", "paint")
    paint()
end sub

sub paint()
    c = m.top.itemContent
    f = m.top.listHasFocus and m.top.focusPercent > 0.5
    m.focus.visible = f
    fav = false
    if c <> invalid then fav = c.fav
    if fav then m.star.uri = "pkg:/images/star_btn_on.png" else m.star.uri = "pkg:/images/star_btn_n.png"
end sub
