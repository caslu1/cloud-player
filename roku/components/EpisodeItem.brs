' Linha de episódio (igual ao EpisodeRow do Android): imagem, número, nome e sinopse.
sub init()
    m.focus = addPoster(m.top, "pkg:/images/ep_focus.png", 0, 0, 1000, 150)
    addPoster(m.top, "pkg:/images/ep_bg.png", 14, 8, 240, 134)
    m.img = addRounded(m.top, "mask_ep", 14, 8, 240, 134)
    m.numBg = addPoster(m.top, "pkg:/images/num_badge.png", 24, 18, 36, 28)
    m.numTx = addLabel(m.top, "", 24, 21, ISEM(), 15, "0xFFFFFFFF", 36, "center")
    m.prog = addRect(m.top, 14, 137, 0, 5, "0x7C5CFFFF")
    m.meta = addLabel(m.top, "", 276, 24, ISEM(), 16, "0x9D9ABBFF", 700)
    m.title = addLabel(m.top, "", 276, 48, OUTF(), 24, "0xFFFFFFFF", 700)
    m.plot = addLabel(m.top, "", 276, 84, IREG(), 18, "0x9D9ABBFF", 700)
    m.plot.wrap = true
    m.plot.maxLines = 2
end sub

sub onContent()
    c = m.top.itemContent
    if c = invalid then return
    m.img.uri = c.HDPOSTERURL
    m.numTx.text = c.num
    m.meta.text = c.meta
    m.title.text = c.title
    m.plot.text = c.plot
    w = Int(240 * c.frac)
    m.prog.width = w
    m.prog.visible = w > 0
    paint()
end sub

sub paint()
    m.focus.visible = m.top.listHasFocus and m.top.focusPercent > 0.5
end sub
