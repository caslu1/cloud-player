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
    m.live = m.top.findNode("liveUI")
    m.live.observeField("action", "onLiveAction")
    m.playing = invalid
    m.global.addFields({ creds: {}, adultUnlocked: false })
    m.top.findNode("bg").uri = "pkg:/images/wall_" + prefGet("wall", "1") + ".png"
    reg = CreateObject("roRegistrySection", "orbita")
    if reg.Exists("server")
        m.global.creds = { server: reg.Read("server"), user: reg.Read("user"), pass: reg.Read("pass") }
        pushScreen(homeName(), {})
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
    ' layout da tela inicial trocado nas Configurações
    st = top.subtype()
    if (st = "HomeScreen" or st = "HomeLite") and st <> homeName() and m.stack.Count() = 1
        resetTo(homeName())
        return
    end if
    top.visible = true
    top.setFocus(true)
end sub

function homeName() as string
    if prefGet("layout", "default") = "lite" then return "HomeLite"
    return "HomeScreen"
end function

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
        resetTo(homeName())
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
    m.screens.visible = false
    m.video.control = "play"
    m.video.setFocus(true)
    if a.live = true
        d = m.live.data
        if d = invalid then d = {}
        if a.list <> invalid then d.list = a.list
        if a.index <> invalid then d.idx = a.index
        if a.all <> invalid then d.all = a.all
        if a.cats <> invalid then d.cats = a.cats
        if a.variants <> invalid then d.variants = a.variants
        if a.catId <> invalid then d.catId = a.catId
        if d.all = invalid then d.all = []
        if d.cats = invalid then d.cats = []
        m.live.data = d
        showOverlay()
    end if
end sub

sub showOverlay()
    if m.live.mode = "overlay" then m.live.callFunc("refresh", {}) else m.live.mode = "overlay"
end sub

sub hideOverlay()
    m.live.mode = "hidden"
    m.video.setFocus(true)
end sub

' Troca de canal no player (▲▼)
sub zap(dirn as integer)
    d = m.live.data
    if d = invalid or d.list = invalid or d.list.Count() = 0 then return
    n = d.list.Count()
    playIdx((d.idx + dirn + n) mod n, d.list, invalid)
end sub

sub playIdx(idx as integer, list as object, catId as dynamic)
    d = m.live.data
    d.list = list
    d.idx = idx
    if catId <> invalid then d.catId = catId
    m.live.data = d
    c = list[idx]
    id = c.id
    if id = invalid then id = c.key
    regPush("recents", { key: id, id: id, name: c.name, num: asText(c.num), logo: asText(c.logo), cat: asText(c.cat), vkey: asText(c.vkey), q: asText(c.q) }, 30)
    play({ type: "play", streamId: id, title: c.name, live: true })
end sub

sub onLiveAction(event as object)
    a = event.getData()
    if a.type = "hide"
        hideOverlay()
    else if a.type = "zap"
        if a.dir = "up" then zap(-1) else zap(1)
    else if a.type = "pick"
        m.live.mode = "hidden"
        playIdx(a.idx, a.list, a.catId)
    else if a.type = "quality"
        ' troca pela versão escolhida (FHD/HD/SD…) do mesmo canal
        d = m.live.data
        c = d.list[d.idx]
        nc = {}
        nc.Append(c)
        nc.id = a.id
        nc.q = a.q
        for each it in d.all
            if it.id = a.id then nc.name = it.name
        end for
        d.list[d.idx] = nc
        m.live.data = d
        play({ type: "play", streamId: a.id, title: nc.name, live: true })
    end if
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
    m.live.mode = "hidden"
    m.video.control = "stop"
    m.video.visible = false
    m.screens.visible = true
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
        a = m.playing
        ' próximo episódio automático (Configurações → Reprodução)
        if a <> invalid and a.queue <> invalid and a.queue.Count() > 0 and prefGet("autoNext", "1") = "1"
            regRemove("progress", a.key)
            nxt = a.queue[0]
            rest = []
            for i = 1 to a.queue.Count() - 1
                rest.Push(a.queue[i])
            end for
            nxt.queue = rest
            play(nxt)
            return
        end if
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
    if m.video.visible and m.playing <> invalid and m.playing.live = true and m.live.mode = "hidden"
        d = m.live.data
        hasList = (d <> invalid and d.list <> invalid and d.list.Count() > 0)
        if key = "up" and hasList
            zap(-1)
            return true
        else if key = "down" and hasList
            zap(1)
            return true
        else if key = "left" and hasList
            m.live.mode = "panel"
            return true
        else if key = "OK" or key = "info"
            showOverlay()
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
        if m.exitBox = invalid and m.stack.Count() = 1 and (m.stack[0].subtype() = "HomeScreen" or m.stack[0].subtype() = "HomeLite")
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
