' Detalhe do filme igual ao Android: capa grande à esquerda, informações e botões ao lado.
sub init()
    m.root = m.top.findNode("root")
    r = m.root
    addPoster(r, "pkg:/images/poster_big_bg.png", 140, 210, 440, 660)
    m.poster = addRounded(r, "mask_poster_big", 140, 210, 440, 660)
    addLabel(r, "F I L M E", 648, 322, ISEM(), 19, "0x22D3EEFF")
    m.title = addLabel(r, "", 648, 360, OUTF(), 64, "0xFFFFFFFF", 1180)
    m.title.maxLines = 1
    m.chips = r.createChild("Group")
    m.genre = addLabel(r, "", 648, 512, IMED(), 23, "0x9D9ABBFF", 1180)
    m.plot = addLabel(r, "", 648, 562, IREG(), 23, "0xE7E5F7FF", 1140)
    m.plot.wrap = true
    m.plot.maxLines = 3
    m.btnBox = r.createChild("Group")
    m.buttons = []
    m.idx = 0
    m.ext = "mp4"
    m.top.observeField("focusedChild", "onFocus")
end sub

sub onParams()
    p = m.top.params
    m.title.text = p.name
    m.poster.uri = p.icon
    m.ext = p.ext
    if m.ext = "" then m.ext = "mp4"
    m.key = "mv:" + p.id
    setChips([], asNum(p.rating), "")
    buildButtons()
    callApi("info", apiUrl("get_vod_info", "&vod_id=" + p.id))
end sub

sub onFocus()
    if m.top.hasFocus() then buildButtons()
end sub

sub onApi(event as object)
    r = event.getData()
    if not r.ok or type(r.data) <> "roAssociativeArray" then return
    info = r.data.info
    if type(info) = "roAssociativeArray"
        m.plot.text = asText(info.plot)
        bd = info.backdrop_path
        if type(bd) = "roArray" and bd.Count() > 0 then m.top.findNode("backdrop").uri = asText(bd[0])
        img = asText(info.movie_image)
        if img <> "" and m.poster.uri = "" then m.poster.uri = img
        setChips([Left(asText(info.releasedate), 4), fmtDuration(asInt((info.duration_secs)))], asNum(info.rating), asText(info.genre))
    end if
    md = r.data.movie_data
    if type(md) = "roAssociativeArray" and asText(md.container_extension) <> "" then m.ext = asText(md.container_extension)
end sub

sub setChips(texts as object, rating as float, genre as string)
    m.chips.removeChildrenIndex(m.chips.getChildCount(), 0)
    x = 648
    y = 460
    for each t in texts
        if t <> "" and t <> "0"
            w = Len(t) * 13 + 34
            addPoster(m.chips, "pkg:/images/chip.9.png", x, y, w, 40)
            addLabel(m.chips, t, x, y + 8, ISEM(), 20, "0xFFFFFFFF", w, "center")
            x = x + w + 14
        end if
    end for
    if rating > 0
        txt = "★ " + asText(Int(rating * 10) / 10)
        w = Len(txt) * 13 + 34
        addPoster(m.chips, "pkg:/images/chip_star.9.png", x, y, w, 40)
        addLabel(m.chips, txt, x, y + 8, ISEM(), 20, "0xFBBF24FF", w, "center")
    end if
    m.genre.text = genre.Replace("/", "·").Replace(",", " ·")
end sub

sub buildButtons()
    m.btnBox.removeChildrenIndex(m.btnBox.getChildCount(), 0)
    m.buttons = []
    m.prog = regFind("progress", m.key)
    x = 648
    y = 690
    if m.prog <> invalid
        left = Int((m.prog.dur - m.prog.pos) / 60)
        b = addButton(m.btnBox, "Continuar · faltam " + left.ToStr() + " min", "ic_play", true, x, y)
        b.action = "resume"
        m.buttons.Push(b)
        x = x + b.width + 16
        b = addButton(m.btnBox, "Do início", "ic_replay", false, x, y)
        b.action = "start"
        m.buttons.Push(b)
    else
        b = addButton(m.btnBox, "Assistir", "ic_play", true, x, y)
        b.action = "start"
        m.buttons.Push(b)
        x = x + b.width + 16
    end if
    ' Minha lista (igual ao Android)
    inList = regFind("mylist", m.key) <> invalid
    if inList then t = "Na minha lista" else t = "Minha lista"
    if inList then ic = "ic_check" else ic = "ic_add"
    if m.prog <> invalid then x = x + m.buttons[m.buttons.Count() - 1].width + 16
    b = addButton(m.btnBox, t, ic, false, x, y)
    b.action = "list"
    m.buttons.Push(b)
    if m.idx >= m.buttons.Count() then m.idx = 0
    paint()
end sub

sub paint()
    for i = 0 to m.buttons.Count() - 1
        paintButton(m.buttons[i], i = m.idx)
    end for
end sub

sub playMovie(fromStart as boolean)
    p = m.top.params
    start = 0
    if not fromStart and m.prog <> invalid then start = m.prog.pos
    m.top.action = { type: "play", key: m.key, kind: "movie", title: p.name, subtitle: "", image: p.icon, url: streamUrl("movie", p.id, m.ext), format: formatFor(m.ext), live: false, start: start }
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "left" and m.idx > 0
        m.idx = m.idx - 1
    else if key = "right" and m.idx < m.buttons.Count() - 1
        m.idx = m.idx + 1
    else if key = "OK"
        act = m.buttons[m.idx].action
        if act = "list"
            p = m.top.params
            if regFind("mylist", m.key) <> invalid then regRemove("mylist", m.key) else regPush("mylist", { key: m.key, kind: "movie", id: p.id, name: p.name, icon: p.icon, ext: m.ext, rating: p.rating }, 500)
            buildButtons()
        else
            playMovie(act = "start")
        end if
        return true
    else
        return false
    end if
    paint()
    return true
end function
