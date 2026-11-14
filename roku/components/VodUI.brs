' Player de filmes e séries (igual ao Android): capa, FILME/SÉRIE em vermelho, título, barra branca,
' ⏪ ▶ ⏩ no centro e os botões Áudio e legendas | Tela | Próximo episódio. Sem cores no resto.
sub init()
    m.ov = m.top.findNode("ov")
    m.pan = m.top.findNode("pan")
    m.top.findNode("tick").observeField("fire", "onTick")
    m.top.findNode("hide").observeField("fire", "autoHide")
    m.btn = -1
    m.seek = -1
    m.screen = 0
    m.panelOpen = false
end sub

sub onItem()
    it = m.top.item
    m.btn = -1
    m.seek = -1
    m.ov.removeChildrenIndex(m.ov.getChildCount(), 0)
    o = m.ov
    addPoster(o, "pkg:/images/player_shade.png", 0, 520, 1920, 560)
    m.clock = addLabel(o, "", 1660, 40, ISEM(), 24, "0xFFFFFFFF", 200, "right")
    x = 80
    if asText(it.image) <> ""
        p = addRounded(o, "mask_poster", 80, 650, 124, 184)
        p.uri = asText(it.image)
        x = 236
    end if
    if it.kind = "episode" then tag = "SÉRIE" else tag = "FILME"
    addLabel(o, tag, x, 716, ISEM(), 16, "0xE5303AFF")
    addLabel(o, asText(it.title), x, 742, OUTF(), 44, "0xFFFFFFFF", 1200)
    if asText(it.subtitle) <> "" then addLabel(o, asText(it.subtitle), x, 802, IMED(), 21, "0xE7E5F7FF", 1200)
    m.ends = addLabel(o, "", 1340, 760, ISEM(), 20, "0xE7E5F7FF", 500, "right")
    m.durL = addLabel(o, "", 1340, 790, IREG(), 17, "0x9D9ABBFF", 500, "right")
    addRect(o, 80, 868, 1760, 10, "0xFFFFFF40")
    m.bar = addRect(o, 80, 868, 0, 10, "0xFFFFFFFF")
    m.knob = addPoster(o, "pkg:/images/vp_knob.png", 66, 859, 28, 28)
    m.posL = addLabel(o, "0:00", 80, 888, ISEM(), 19, "0xFFFFFFFF")
    m.remL = addLabel(o, "", 1540, 888, IMED(), 19, "0x9D9ABBFF", 300, "right")
    ' controles no centro
    addPoster(o, "pkg:/images/vp_small.png", 820, 938, 68, 68)
    addPoster(o, "pkg:/images/vi_rew.png", 834, 952, 40, 40)
    addPoster(o, "pkg:/images/vp_circle.png", 912, 924, 96, 96)
    m.pp = addPoster(o, "pkg:/images/vi_pause.png", 936, 948, 48, 48)
    m.pp.blendColor = "0x0B0A12FF"
    addPoster(o, "pkg:/images/vp_small.png", 1032, 938, 68, 68)
    addPoster(o, "pkg:/images/vi_fwd.png", 1046, 952, 40, 40)
    ' botões
    m.btns = []
    m.btns.Push(pill(o, "opts", "vi_gear", "Áudio e legendas", 80))
    m.btns.Push(pill(o, "screen", "vi_expand", "Tela: Original", 400))
    if it.queue <> invalid and it.queue.Count() > 0 then m.btns.Push(pill(o, "next", "vi_fwd", "Próximo episódio", -1))
    m.hint = addLabel(o, "", 0, 1036, IREG(), 16, "0x6E6B8CFF", 1920, "center")
    applyScreen()
    paint()
    show()
    m.top.findNode("tick").control = "start"
end sub

function pill(o as object, id as string, ic as string, t as string, x as integer) as object
    w = Len(t) * 12 + 92
    if x < 0 then x = 1840 - w
    g = o.createChild("Group")
    g.translation = [x, 944]
    bg = addPoster(g, "pkg:/images/pill_w.9.png", 0, 0, w, 56)
    i = addPoster(g, "pkg:/images/" + ic + ".png", 22, 14, 28, 28)
    l = addLabel(g, t, 62, 15, ISEM(), 19, "0xFFFFFFFF", w - 70)
    return { id: id, g: g, bg: bg, ic: i, lb: l, w: w }
end function

sub paint()
    for i = 0 to m.btns.Count() - 1
        b = m.btns[i]
        f = (i = m.btn)
        if f then b.bg.blendColor = "0xFFFFFFFF" else b.bg.blendColor = "0xFFFFFF30"
        if f then b.lb.color = "0x0B0A12FF" else b.lb.color = "0xFFFFFFFF"
        if f then b.ic.blendColor = "0x0B0A12FF" else b.ic.blendColor = "0xFFFFFFFF"
    end for
    if m.btn >= 0 then m.hint.text = "OK abre a opção  ·  ◀ ▶ trocam  ·  ▲ volta" else m.hint.text = "OK pausa  ·  ◀ ▶ voltam/avançam  ·  ▼ opções"
end sub

function fmtT(s as integer) as string
    if s < 0 then s = 0
    h = Int(s / 3600)
    mi = Int((s mod 3600) / 60)
    se = s mod 60
    if h > 0 then return h.ToStr() + ":" + two(mi) + ":" + two(se)
    return mi.ToStr() + ":" + two(se)
end function

sub onTick()
    v = m.top.video
    if v = invalid then return
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    m.clock.text = two(d.GetHours()) + ":" + two(d.GetMinutes())
    dur = Int(v.duration)
    cur = Int(v.position)
    if m.seek >= 0 then cur = m.seek
    f = 0.0
    if dur > 0 then f = cur / dur
    if f > 1 then f = 1
    m.bar.width = Int(1760 * f)
    m.knob.translation = [66 + Int(1760 * f), 859]
    m.posL.text = fmtT(cur)
    if dur > 0
        m.remL.text = "-" + fmtT(dur - cur)
        m.durL.text = "Duração " + fmtT(dur)
        e = CreateObject("roDateTime")
        e.FromSeconds(e.AsSeconds() + (dur - cur))
        e.ToLocalTime()
        m.ends.text = "Termina às " + two(e.GetHours()) + ":" + two(e.GetMinutes())
    end if
    if v.state = "paused" then m.pp.uri = "pkg:/images/vi_play.png" else m.pp.uri = "pkg:/images/vi_pause.png"
end sub

sub show()
    m.ov.visible = true
    t = m.top.findNode("hide")
    t.control = "stop"
    t.control = "start"
end sub

sub autoHide()
    v = m.top.video
    if m.btn >= 0 or m.panelOpen then return
    if v <> invalid and v.state = "paused" then return
    m.ov.visible = false
end sub

' Tela: Original → Expandir → Esticar
sub applyScreen()
    v = m.top.video
    if v = invalid then return
    names = ["Original", "Expandir", "Esticar"]
    v.scaleRotateCenter = [960, 540]
    if m.screen = 0 then v.scale = [1.0, 1.0]
    if m.screen = 1 then v.scale = [1.2, 1.2]
    if m.screen = 2 then v.scale = [1.33, 1.0]
    for each b in m.btns
        if b.id = "screen" then b.lb.text = "Tela: " + names[m.screen]
    end for
end sub

' painel de áudio e legendas
sub openPanel()
    v = m.top.video
    m.pan.removeChildrenIndex(m.pan.getChildCount(), 0)
    addRect(m.pan, 1320, 0, 600, 1080, "0x0C0A16F5")
    addLabel(m.pan, "Áudio e legendas", 1360, 60, OUTF(), 32, "0xFFFFFFFF")
    m.opts = []
    y = 130
    addLabel(m.pan, "ÁUDIO", 1360, y, ISEM(), 15, "0x9D9ABBFF")
    y = y + 34
    at = v.availableAudioTracks
    if type(at) = "roArray"
        for each t in at
            nm = asText(t.Name)
            if nm = "" then nm = asText(t.Language)
            if nm = "" then nm = "Faixa " + (m.opts.Count() + 1).ToStr()
            m.opts.Push({ k: "a", id: asText(t.Track), t: nm, on: asText(v.audioTrack) = asText(t.Track), y: y })
            y = y + 62
        end for
    end if
    y = y + 20
    addLabel(m.pan, "LEGENDAS", 1360, y, ISEM(), 15, "0x9D9ABBFF")
    y = y + 34
    m.opts.Push({ k: "s", id: "", t: "Desligada", on: v.globalCaptionMode <> "On", y: y })
    y = y + 62
    st = v.availableSubtitleTracks
    if type(st) = "roArray"
        for each t in st
            nm = asText(t.Description)
            if nm = "" then nm = asText(t.Language)
            m.opts.Push({ k: "s", id: asText(t.TrackName), t: nm, on: v.globalCaptionMode = "On" and asText(v.subtitleTrack) = asText(t.TrackName), y: y })
            y = y + 62
        end for
    end if
    m.oi = 0
    m.optBox = m.pan.createChild("Group")
    drawOpts()
    m.pan.visible = true
    m.panelOpen = true
end sub

sub drawOpts()
    m.optBox.removeChildrenIndex(m.optBox.getChildCount(), 0)
    for i = 0 to m.opts.Count() - 1
        o = m.opts[i]
        if i = m.oi then addRect(m.optBox, 1350, o.y - 10, 540, 54, "0xFFFFFFFF")
        if i = m.oi then c = "0x0B0A12FF" else c = "0xFFFFFFFF"
        mark = "   "
        if o.on then mark = "✓ "
        addLabel(m.optBox, mark + o.t, 1370, o.y, IMED(), 21, c, 500)
    end for
end sub

sub closePanel()
    m.pan.visible = false
    m.panelOpen = false
    show()
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    v = m.top.video
    if m.panelOpen
        if not press then return true
        if key = "up" and m.oi > 0 then m.oi = m.oi - 1
        if key = "down" and m.oi < m.opts.Count() - 1 then m.oi = m.oi + 1
        if key = "OK"
            o = m.opts[m.oi]
            if o.k = "a"
                v.audioTrack = o.id
            else if o.id = ""
                v.globalCaptionMode = "Off"
            else
                v.subtitleTrack = o.id
                v.globalCaptionMode = "On"
            end if
            for each x in m.opts
                if x.k = o.k then x.on = (x.id = o.id)
            end for
        end if
        if key = "back" or key = "left"
            closePanel()
            return true
        end if
        drawOpts()
        return true
    end if
    ' soltou a tecla de avançar/voltar: aplica o pulo
    if not press
        if (key = "left" or key = "right") and m.seek >= 0
            v.seek = m.seek
            m.seek = -1
        end if
        return false
    end if
    if m.btn >= 0
        if key = "left" and m.btn > 0 then m.btn = m.btn - 1
        if key = "right" and m.btn < m.btns.Count() - 1 then m.btn = m.btn + 1
        if key = "up" then m.btn = -1
        if key = "back"
            m.btn = -1
            paint()
            return true
        end if
        if key = "OK"
            id = m.btns[m.btn].id
            if id = "screen"
                m.screen = (m.screen + 1) mod 3
                applyScreen()
            else if id = "opts"
                m.btn = -1
                openPanel()
            else if id = "next"
                m.top.action = { type: "next" }
            end if
        end if
        paint()
        show()
        return true
    end if
    if not m.ov.visible and key <> "back"
        show()
        if key = "OK" or key = "down" or key = "info" then return true
    end if
    if key = "OK" or key = "play"
        if v.state = "paused" then v.control = "resume" else v.control = "pause"
        show()
        return true
    else if key = "left" or key = "right" or key = "rewind" or key = "fastforward"
        base = m.seek
        if base < 0 then base = Int(v.position)
        if key = "left" or key = "rewind" then base = base - 10 else base = base + 10
        if base < 0 then base = 0
        if v.duration > 0 and base > v.duration - 2 then base = Int(v.duration) - 2
        m.seek = base
        onTick()
        show()
        return true
    else if key = "down"
        m.btn = 0
        paint()
        show()
        return true
    else if key = "back" and m.ov.visible and v.state <> "paused"
        m.ov.visible = false
        return true
    end if
    return false
end function
