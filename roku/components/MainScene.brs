sub init()
    m.stack = []
    m.screens = m.top.findNode("screens")
    m.video = m.top.findNode("video")
    m.video.observeField("state", "onVideoState")
    m.errBox = m.top.findNode("errBox")
    m.errMsg = m.top.findNode("errMsg")
    m.errMsg.font = mkFont(IMED(), 24)
    m.top.findNode("errTimer").observeField("fire", "hideError")
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
    if a.live = true and a.streamId <> invalid and a.sources = invalid then a.sources = liveSources(a.streamId)
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
    if a.start <> invalid and a.start > 0 then c.PlayStart = a.start
    m.video.content = c
    m.video.visible = true
    m.video.control = "play"
    m.video.setFocus(true)
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
    if key = "back"
        if m.video.visible
            stopVideo()
            return true
        end if
        if m.stack.Count() > 1
            popScreen()
            return true
        end if
    end if
    return false
end function
