sub init()
    m.stack = []
    m.screens = m.top.findNode("screens")
    m.video = m.top.findNode("video")
    m.video.observeField("state", "onVideoState")
    m.playing = invalid
    m.global.addFields({ creds: {} })
    reg = CreateObject("roRegistrySection", "orbita")
    if reg.Exists("server")
        m.global.creds = { server: reg.Read("server"), user: reg.Read("user"), pass: reg.Read("pass") }
        pushScreen("HomeScreen", {})
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
    c = CreateObject("roSGNode", "ContentNode")
    c.url = a.url
    c.title = a.title
    c.streamFormat = a.format
    if a.live = true then c.live = true
    if a.start <> invalid and a.start > 0 then c.PlayStart = a.start
    m.playing = a
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
    if st = "finished" or st = "error" then stopVideo()
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
