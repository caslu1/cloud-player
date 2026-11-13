' Senha de 4 números com teclado na tela (o controle da Roku não tem números), igual ao PinDialog do Android.
sub init()
    m.pin = ""
    m.idx = 0
end sub

sub onSetup()
    s = m.top.setup
    m.top.removeChildrenIndex(m.top.getChildCount(), 0)
    addPoster(m.top, "pkg:/images/dim.png", 0, 0, 1920, 1080)
    g = m.top.createChild("Group")
    g.translation = [660, 150]
    addPoster(g, "pkg:/images/dialog_tall.png", 0, 0, 600, 780)
    addPoster(g, "pkg:/images/dialog_icon.png", 256, 36, 88, 88)
    ic = addPoster(g, "pkg:/images/ic_lock.png", 282, 62, 36, 36)
    ic.blendColor = "0x9B84FFFF"
    addLabel(g, s.title, 30, 140, OUTF(), 32, "0xFFFFFFFF", 540, "center")
    sub_ = addLabel(g, asText(s.subtitle), 40, 186, IREG(), 18, "0x9D9ABBFF", 520, "center")
    sub_.wrap = true
    sub_.maxLines = 2
    m.dots = []
    for i = 0 to 3
        d = addPoster(g, "", 186 + i * 60, 250, 44, 44)
        m.dots.Push(d)
    end for
    m.err = addLabel(g, "", 30, 306, ISEM(), 18, "0xF43F5EFF", 540, "center")
    m.keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK"]
    m.kb = []
    for i = 0 to 11
        k = g.createChild("Group")
        k.translation = [105 + (i mod 3) * 134, 346 + Int(i / 3) * 100]
        bg = addPoster(k, "", 0, 0, 120, 86)
        addLabel(k, m.keys[i], 0, 26, OUTF(), 30, "0xFFFFFFFF", 120, "center")
        m.kb.Push(bg)
    end for
    m.pin = ""
    m.idx = 0
    paint()
end sub

sub onError()
    m.err.text = m.top.error
    m.pin = ""
    paint()
end sub

sub paint()
    for i = 0 to 3
        if i < Len(m.pin) then m.dots[i].uri = "pkg:/images/pin_on.png" else m.dots[i].uri = "pkg:/images/pin_off.png"
    end for
    for i = 0 to 11
        if i = m.idx then m.kb[i].uri = "pkg:/images/key_f.9.png" else m.kb[i].uri = "pkg:/images/key_n.9.png"
    end for
end sub

sub pressKey()
    k = m.keys[m.idx]
    if k = "⌫"
        if Len(m.pin) > 0 then m.pin = Left(m.pin, Len(m.pin) - 1)
    else if k = "OK"
        if Len(m.pin) = 4 then m.top.result = m.pin
        return
    else if Len(m.pin) < 4
        m.pin = m.pin + k
        m.err.text = ""
    end if
    paint()
    if Len(m.pin) = 4 then m.top.result = m.pin
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return true
    if key = "back"
        m.top.result = "cancel"
        return true
    end if
    if key = "left" and m.idx mod 3 > 0 then m.idx = m.idx - 1
    if key = "right" and m.idx mod 3 < 2 then m.idx = m.idx + 1
    if key = "up" and m.idx > 2 then m.idx = m.idx - 3
    if key = "down" and m.idx < 9 then m.idx = m.idx + 3
    if key = "OK"
        pressKey()
        return true
    end if
    paint()
    return true
end function
