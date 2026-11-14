' Teclado próprio do app (igual ao Android): aparece embaixo da tela e escreve direto no campo.
' setup = { label, text, password, inline }  →  text muda a cada tecla; result = "done" ou "cancel"
sub init()
    ' (BrightScript não soma listas com +: monta cada linha com cat)
    m.letters = [
        cat(sp("qwertyuiop/\"), [kd("⌫", 1, "del")]),
        cat(sp("asdfghjkl.:"), [kd("Pronto", 2, "done")]),
        cat(cat([kd("⇧", 1, "shift")], sp("zxcvbnm,?")), [kd("Limpar", 2, "clear"), kd("⇧", 1, "shift")]),
        [kd("123", 1.4, "mode"), kd("http://", 1.6, ""), kd("espaço", 6, "space"), kd(".com", 1.6, ""), kd("@", 1.4, "")]
    ]
    m.symbols = [
        cat(sp("1234567890-_"), [kd("⌫", 1, "del")]),
        cat(sp("!#$%&*()=+;"), [kd("Pronto", 2, "done")]),
        cat(sp("[]{}<>'" + Chr(34) + "~|"), [kd("Limpar", 2, "clear"), kd("^", 1, "")]),
        [kd("ABC", 1.4, "mode"), kd("https://", 1.6, ""), kd("espaço", 6, "space"), kd(".br", 1.6, ""), kd(":", 1.4, "")]
    ]
    m.sym = false
    m.shift = false
    m.row = 0
    m.col = 0
end sub

function kd(t as string, w as float, a as string) as object
    return { t: t, w: w, a: a }
end function

function cat(a as object, b as object) as object
    out = []
    out.Append(a)
    out.Append(b)
    return out
end function

function sp(s as string) as object
    out = []
    for i = 1 to Len(s)
        out.Push(kd(Mid(s, i, 1), 1, ""))
    end for
    return out
end function

sub onSetup()
    s = m.top.setup
    m.top.text = asText(s.text)
    draw()
end sub

function rows() as object
    if m.sym then return m.symbols
    return m.letters
end function

sub draw()
    m.top.removeChildrenIndex(m.top.getChildCount(), 0)
    W = 1120
    x0 = (1920 - W) / 2
    addRect(m.top, 0, 640, 1920, 440, "0x060510F2")
    s = m.top.setup
    addLabel(m.top, UCase(asText(s.label)), x0, 664, ISEM(), 16, "0x9D9ABBFF", 260)
    addPoster(m.top, "pkg:/images/field_n.9.png", x0 + 270, 650, W - 270, 56)
    t = m.top.text
    if s.password = true then t = String(Len(t), "•")
    addLabel(m.top, t + "|", x0 + 292, 664, IMED(), 22, "0xFFFFFFFF", W - 310)
    rs = rows()
    y = 724
    for r = 0 to rs.Count() - 1
        keys = rs[r]
        tw = 0.0
        for each kk in keys
            tw = tw + kk.w
        end for
        gap = 8
        unit = (W - gap * (keys.Count() - 1)) / tw
        x = x0
        for c = 0 to keys.Count() - 1
            kk = keys[c]
            kw = Int(unit * kk.w)
            f = (r = m.row and c = m.col)
            on = (kk.a = "shift" and m.shift)
            if f then col = "0xFFFFFFFF" else if on then col = "0x3A3650FF" else col = "0x16141FFF"
            addRect(m.top, x, y, kw, 72, col)
            lab = kk.t
            if m.shift and not m.sym and Len(lab) = 1 then lab = UCase(lab)
            if kk.a = "space" then lab = "espaço"
            if f then tc = "0x0B0A12FF" else tc = "0xFFFFFFFF"
            fs = 26
            if Len(lab) > 2 then fs = 20
            addLabel(m.top, lab, x, y + 22, ISEM(), fs, tc, kw, "center")
            x = x + kw + gap
        end for
        y = y + 82
    end for
end sub

sub pressKey(kk as object)
    a = kk.a
    t = m.top.text
    if a = "del"
        if Len(t) > 0 then m.top.text = Left(t, Len(t) - 1)
    else if a = "clear"
        m.top.text = ""
    else if a = "done"
        m.top.result = "done"
        return
    else if a = "shift"
        m.shift = not m.shift
    else if a = "mode"
        m.sym = not m.sym
        rs = rows()
        if m.col > rs[m.row].Count() - 1 then m.col = rs[m.row].Count() - 1
    else if a = "space"
        m.top.text = t + " "
    else
        ch = kk.t
        if m.shift and Len(ch) = 1 then ch = UCase(ch)
        m.top.text = t + ch
        m.shift = false
    end if
    draw()
end sub

' sobe/desce para a tecla mais perto na outra linha
sub moveRow(d as integer)
    rs = rows()
    nr = m.row + d
    if nr < 0 or nr > rs.Count() - 1 then return
    center = 0.0
    for i = 0 to m.col - 1
        center = center + rs[m.row][i].w
    end for
    center = center + rs[m.row][m.col].w / 2
    acc = 0.0
    best = 0
    for i = 0 to rs[nr].Count() - 1
        if acc <= center then best = i
        acc = acc + rs[nr][i].w
    end for
    m.row = nr
    m.col = best
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return true
    rs = rows()
    if key = "left"
        if m.col > 0 then m.col = m.col - 1
    else if key = "right"
        if m.col < rs[m.row].Count() - 1 then m.col = m.col + 1
    else if key = "up"
        moveRow(-1)
    else if key = "down"
        moveRow(1)
    else if key = "OK"
        pressKey(rs[m.row][m.col])
        return true
    else if key = "back"
        m.top.result = "cancel"
        return true
    else if Left(key, 4) = "Lit_"
        ' teclado do celular/app Roku também escreve
        m.top.text = m.top.text + Mid(key, 5)
    else if key = "replay" or key = "rev"
        pressKey(kd("⌫", 1, "del"))
        return true
    else
        return true
    end if
    draw()
    return true
end function
