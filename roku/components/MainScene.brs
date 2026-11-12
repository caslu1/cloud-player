sub init()
    m.stack = []
    m.screens = m.top.findNode("screens")
    m.video = m.top.findNode("video")
    m.video.observeField("state", "onVideoState")
    m.errBox = m.top.findNode("errBox")
    m.errMsg = m.top.findNode("errMsg")
    m.errMsg.font = mkFont(IMED(), 24)
    m.top.findNode("errTimer").observeField("fire", "hideError")
    ' tela cheia ao vivo: informações do canal e lista lateral (igual ao Android)
    m.liveOv = m.top.findNode("liveOv")
    m.chPanel = m.top.findNode("chPanel")
    m.chList = m.top.findNode("chList")
    m.chList.observeField("itemSelected", "onPanelPick")
    m.top.findNode("ovTimer").observeField("fire", "hideOverlay")
    buildOverlay()
    m.playing = invalid
    m.global.addFields({ creds: {} })
    reg = CreateObject("roRegistrySection", "orbita")
    if reg.Exists("server")
        m.global.creds = { server: reg.Read("server"), user: reg.Read("user"), pass: reg.Read("pass") }
        pushScreen("HomeScreen", {})
        startActTask(false)
    else
        pushScreen("LoginScreen", {})
    end if
end sub

function pushScreen(name as string, params as object) as object
    s = CreateObject("roSGNode", name)
    s.observeField("action", "onAction")
    m.screens.appendChild(s)
    s.params = params
    if m.stack.Count() > 0 then m.stack.Peek().visible = false
    m.stack.Push(s)
    s.setFocus(true)
    return s
end function

sub popScreen()
    s = m.stack.Pop()
    if s.subtype() = "ChannelsScreen" then s.callFunc("cleanup", {})
    m.screens.removeChild(s)
    top = m.stack.Peek()
    top.visible = true
    top.setFocus(true)
end sub

sub resetTo(name as string)
    while m.stack.Count() > 0
        m.screens.removeChild(m.stack.Pop())
    end while
    pushScreen(name, {})
end sub

sub onAction(event as object)
    a = event.getData()
    if a = invalid or a.type = invalid then return
    if a.type = "open"
        pushScreen(a.screen, a.params)
    else if a.type = "play"
        play(a)
    else if a.type = "home"
        resetTo("HomeScreen")
    else if a.type = "logout"
        reg = CreateObject("roRegistrySection", "orbita")
        for each k in ["server", "user", "pass", "progress", "favs", "recents"]
            reg.Delete(k)
        end for
        reg.Flush()
        m.global.creds = {}
        resetTo("LoginScreen")
    end if
end sub

sub play(a as object)
    if a.live = true and a.streamId <> invalid then a.sources = liveSources(a.streamId)
    if a.sources <> invalid
        a.try = 0
        a.url = a.sources[0].url
        a.format = a.sources[0].format
    end if
    m.playing = a
    startVideo()
end sub

sub startVideo()
    a = m.playing
    c = mkVideo(a.url, a.format, a.live = true)
    c.title = a.title
    ' ao vivo: o próprio app cuida das teclas (troca de canal, informações e lista)
    m.video.enableUI = (a.live <> true)
    if a.start <> invalid and a.start > 0 then c.PlayStart = a.start
    m.video.content = c
    m.video.visible = true
    m.video.control = "play"
    m.video.setFocus(true)
    if a.live = true
        if a.list <> invalid then m.liveList = a.list
        if a.index <> invalid then m.liveIdx = a.index
        if a.epg <> invalid then m.liveEpg = a.epg
        showOverlay()
    end if
end sub

sub buildOverlay()
    o = m.liveOv
    addPoster(o, "pkg:/images/player_shade.png", 0, 680, 1920, 400)
    m.ovLogoBg = addPoster(o, "pkg:/images/logo_box.png", 72, 860, 150, 106)
    m.ovLogoBg.blendColor = "0x1B1828FF"
    m.ovLogo = addPoster(o, "", 82, 870, 130, 86)
    m.ovLogo.loadDisplayMode = "scaleToFit"
    m.ovNum = addLabel(o, "", 250, 858, ISEM(), 22, "0x22D3EEFF")
    m.ovName = addLabel(o, "", 250, 888, OUTF(), 40, "0xFFFFFFFF", 1300)
    m.ovProg = addLabel(o, "", 250, 942, IREG(), 22, "0xE7E5F7FF", 1300)
    addPoster(o, "pkg:/images/live_badge.png", 1740, 862, 108, 34)
    m.ovClock = addLabel(o, "", 1640, 40, OUTF(), 30, "0xFFFFFFFF", 220, "right")
end sub

sub showOverlay()
    a = m.playing
    if a = invalid or a.live <> true then return
    c = invalid
    if m.liveList <> invalid and m.liveIdx <> invalid and m.liveIdx < m.liveList.Count() then c = m.liveList[m.liveIdx]
    if c <> invalid
        m.ovNum.text = c.num
        m.ovName.text = c.name
        m.ovLogo.uri = c.logo
        e = invalid
        if m.liveEpg <> invalid then e = m.liveEpg[c.id]
        if e <> invalid and e.title <> "" then m.ovProg.text = e.title + "  ·  " + hhmmOf(e.start) + " – " + hhmmOf(e.stop) else m.ovProg.text = ""
    else
        m.ovNum.text = ""
        m.ovName.text = a.title
        m.ovProg.text = ""
    end if
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    m.ovClock.text = two(d.GetHours()) + ":" + two(d.GetMinutes())
    m.liveOv.visible = true
    t = m.top.findNode("ovTimer")
    t.control = "stop"
    t.control = "start"
end sub

sub hideOverlay()
    m.liveOv.visible = false
end sub

' Troca de canal no player (▲▼ e lista lateral)
sub zap(idx as integer)
    if m.liveList = invalid or m.liveList.Count() = 0 then return
    n = m.liveList.Count()
    idx = (idx + n) mod n
    m.liveIdx = idx
    c = m.liveList[idx]
    regPush("recents", { key: c.id, id: c.id, name: c.name, num: c.num, logo: c.logo }, 30)
    play({ type: "play", streamId: c.id, title: c.name, live: true })
end sub

sub openPanel()
    if m.liveList = invalid then return
    root = CreateObject("roSGNode", "ContentNode")
    for each c in m.liveList
        n = root.createChild("ItemData")
        n.key = c.id
        n.title = c.name
        n.num = c.num
        n.logo = c.logo
        n.playing = (c.id = m.liveList[m.liveIdx].id)
        n.icon = "nostar"
        if m.liveEpg <> invalid and m.liveEpg[c.id] <> invalid and m.liveEpg[c.id].title <> "" then n.prog = m.liveEpg[c.id].title
    end for
    m.chList.content = root
    m.chList.jumpToItem = m.liveIdx
    m.chPanel.visible = true
    m.liveOv.visible = false
    m.chList.setFocus(true)
end sub

sub closePanel()
    m.chPanel.visible = false
    m.video.setFocus(true)
end sub

sub onPanelPick()
    closePanel()
    zap(m.chList.itemSelected)
end sub

' Guarda onde parou (filmes e episódios) para o "Continuar assistindo".
sub saveProgress()
    a = m.playing
    if a = invalid or a.live = true or a.key = invalid then return
    curPos = Int(m.video.position)
    dur = Int(m.video.duration)
    if dur > 0 and curPos > dur * 0.92
        regRemove("progress", a.key)
    else if curPos > 30
        regPush("progress", { key: a.key, kind: a.kind, title: a.title, subtitle: a.subtitle, image: a.image, url: a.url, format: a.format, pos: curPos, dur: dur, seriesId: a.seriesId, seriesName: a.seriesName }, 20)
    end if
end sub

sub stopVideo()
    saveProgress()
    m.liveOv.visible = false
    m.chPanel.visible = false
    m.video.control = "stop"
    m.video.visible = false
    m.playing = invalid
    if m.stack.Count() > 0 then m.stack.Peek().setFocus(true)
end sub

sub onVideoState(event as object)
    st = event.getData()
    if st = "error"
        a = m.playing
        ' tenta o próximo formato do canal (m3u8 → ts)
        if a <> invalid and a.sources <> invalid and a.try + 1 < a.sources.Count()
            a.try = a.try + 1
            a.url = a.sources[a.try].url
            a.format = a.sources[a.try].format
            m.playing = a
            startVideo()
            return
        end if
        showError("Não foi possível abrir o vídeo." + Chr(10) + "Erro " + m.video.errorCode.ToStr() + ": " + m.video.errorMsg)
        stopVideo()
    else if st = "finished"
        stopVideo()
    end if
end sub

' Situação no site: vencido/bloqueado volta para a tela de ativação.
sub onAct(event as object)
    r = event.getData()
    if not r.ok or type(r.data) <> "roAssociativeArray" then return
    st = asText(r.data.status)
    if st = "expired" or st = "blocked"
        if m.video.visible then stopVideo()
        resetTo("LoginScreen")
    end if
end sub

sub onExitAsk()
    res = m.exitBox.result
    m.top.removeChild(m.exitBox)
    m.exitBox = invalid
    if res = "ok"
        m.top.exitApp = true
    else
        m.stack.Peek().setFocus(true)
    end if
end sub

sub showError(t as string)
    m.errMsg.text = t
    m.errBox.visible = true
    tm = m.top.findNode("errTimer")
    tm.control = "stop"
    tm.control = "start"
end sub

sub hideError()
    m.errBox.visible = false
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.video.visible and m.playing <> invalid and m.playing.live = true
        if m.chPanel.visible
            if key = "back" or key = "right"
                closePanel()
                return true
            end if
            return false
        end if
        hasList = (m.liveIdx <> invalid and m.liveList <> invalid)
        if (key = "up" or key = "down" or key = "left") and not hasList
            return true
        else if key = "up"
            zap(m.liveIdx - 1)
            return true
        else if key = "down"
            zap(m.liveIdx + 1)
            return true
        else if key = "left"
            openPanel()
            return true
        else if key = "OK" or key = "info"
            if m.liveOv.visible then hideOverlay() else showOverlay()
            return true
        else if key = "back" and m.liveOv.visible
            hideOverlay()
            return true
        end if
    end if
    if key = "back"
        if m.video.visible
            stopVideo()
            return true
        end if
        if m.stack.Count() > 1
            popScreen()
            return true
        end if
        ' na tela inicial: pergunta antes de sair do app (igual ao Android)
        if m.exitBox = invalid and m.stack.Count() = 1 and m.stack[0].subtype() = "HomeScreen"
            d = CreateObject("roSGNode", "ConfirmBox")
            d.setup = { title: "Sair do Orbita Play?", message: "O que estiver tocando será fechado.", confirm: "Sair", icon: "ic_logout", danger: false }
            d.observeField("result", "onExitAsk")
            m.top.appendChild(d)
            m.exitBox = d
            d.setFocus(true)
            return true
        end if
    end if
    return false
end function
