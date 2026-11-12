' Capa (igual ao BigPoster do Android): cantos arredondados, nota no canto e borda branca no foco.
sub init()
    m.g = m.top.createChild("Group")
    m.g.scaleRotateCenter = [152, 228]
    addPoster(m.g, "pkg:/images/poster_bg.png", 0, 0, 304, 456)
    m.initials = addLabel(m.g, "", 0, 200, OUTF(), 34, "0xFFFFFF99", 304, "center")
    m.img = addRounded(m.g, "mask_poster", 0, 0, 304, 456)
    m.badge = addPoster(m.g, "pkg:/images/rating_badge.png", 214, 12, 78, 32)
    m.rating = addLabel(m.g, "", 214, 16, ISEM(), 16, "0xFFFFFFFF", 70, "right")
    m.ring = addPoster(m.g, "pkg:/images/ring_poster.png", 0, 0, 304, 456)
    m.title = addLabel(m.top, "", 0, 470, IREG(), 18, "0x9D9ABBFF", 304)
end sub

sub onContent()
    c = m.top.itemContent
    if c = invalid then return
    m.img.uri = c.HDPOSTERURL
    m.title.text = c.title
    m.initials.text = UCase(Left(c.title, 2))
    r = c.stars
    m.badge.visible = r <> "" and r <> "0"
    m.rating.visible = m.badge.visible
    m.rating.text = "★ " + r
    paint()
end sub

sub paint()
    f = m.top.gridHasFocus and m.top.focusPercent > 0.5
    m.ring.visible = f
    if f
        m.g.scale = [1.07, 1.07]
        m.title.color = "0xFFFFFFFF"
        m.title.font = mkFont(ISEM(), 18)
    else
        m.g.scale = [1.0, 1.0]
        m.title.color = "0x9D9ABBFF"
        m.title.font = mkFont(IREG(), 18)
    end if
end sub
