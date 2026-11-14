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
    ' filmes e séries: controles do app (igual ao Android)
    m.vod = m.top.findNode("vodUI")
    m.vod.video = m.video
    m.vod.observeField("action", "onVodAction")
    m.playing = invalid
    m.global.addFields({ creds: {}, adultUnlocked: false })
    m.top.findNode("bg").uri = "pkg:/images/wall_" + prefGet("wall", "3") + ".png"
    reg = CreateObject("roRegistrySection", "orbita")
    if reg.Exists("server")
        m.global.creds = { server: reg.Read("server"), user: reg.Read("user"), pass: reg.Read("pass") }
        ' quem já usava o app antes das Playlists: a lista atual vira a "Lista 1"
        if regRead("playlists").Count() = 0 then savePlaylist("", m.global.creds.server, m.global.creds.user, m.global.creds.pass)
        pushScreen(homeName(), {})
        startActTask(false)
    else
        pushScreen("LoginScreen", {})
    end if
    startSplash()
end sub

' Vídeo de abertura (igual ao Android): sempre toca até o fim; só segue antes se der erro ou travar.
sub startSplash()
    v = CreateObject("roSGNode", "Video")
    v.width = 1920
    v.height = 1080
    v.enableUI = false
    c = CreateObject("roSGNode", "ContentNode")
    c.url = "pkg:/images/splash.mp4"
    c.streamFormat = "mp4"
    v.content = c
    v.observeField("state", "onSplashState")
    m.top.appendChild(v)
    m.splash = v
    m.screens.visible = false
    t = CreateObject("roSGNode", "Timer")
    t.duration = 15
    t.observeField("fire", "endSplash")
    m.top.appendChild(t)
    m.splashTimer = t
    t.control = "start"
    v.control = "play"
    v.setFocus(true)
end sub

sub onSplashState(event as object)
    st = event.getData()
    if st = "finished" or st = "error" then endSplash()
end sub

sub endSplash()
    if m.splash = invalid then return
    m.splash.unobserveField("state")
    m.splash.control = "stop"
    m.top.removeChild(m.splash)
    m.splash = invalid
    if m.splashTimer <> invalid
        m.splashTimer.control = "stop"
        m.top.removeChild(m.splashTimer)
        m.splashTimer = invalid
    end if
    m.screens.visible = true
    if m.stack.Count() > 0 then m.stack.Peek().setFocus(true)
    ' aviso importante: só no primeiro acesso
    if prefGet("disclaimer", "0") <> "1"
        d = CreateObject("roSGNode", "Disclaimer")
        d.observeField("done", "onDisclaimer")
        m.top.appendChild(d)
        m.disc = d
        d.setFocus(true)
    end if
end sub

sub onDisclaimer()
    prefSet("disclaimer", "1")
    m.top.removeChild(m.disc)
    m.disc = invalid
    if m.stack.Count() > 0 then m.stack.Peek().setFocus(true)
end sub

function pushScreen(name as string, params as object) as object
    s = CreateObject("roSGNode", name)
    s.observeField("action", "onAction")
    m.screens.appendChild(s)
    s.params = params
    if m.stack.Count() > 0 then m.stack.Peek().visible = false
    m.stack.Push(s)
    if name = homeName() and m.stack.Count() = 1 then m.homeLayout = prefGet("layout", "cards")
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
    if isHome(st) and m.stack.Count() = 1 and prefGet("layout", "cards") <> m.homeLayout
        resetTo(homeName())
        return
    end if
    top.visible = true
    top.setFocus(true)
end sub

function homeName() as string
    l = prefGet("layout", "cards")
    if l = "lite" then return "HomeLite"
    if l = "cards" or l = "round" or l = "square" then return "HomeCards"
    return "HomeScreen"
end function

function isHome(st as string) as boolean
    return st = "HomeScreen" or st = "HomeLite" or st = "HomeCards"
end function

' Recarregar: janela "Carregando…" → "Atualização concluída"
sub showReload()
    if m.reloadBox <> invalid then return
    d = CreateObject("roSGNode", "ReloadBox")
    d.observeField("done", "onReloadDone")
    m.top.appendChild(d)
    m.reloadBox = d
    d.setFocus(true)
end sub

sub onReloadDone()
    m.top.removeChild(m.reloadBox)
    m.reloadBox = invalid
    resetTo(homeName())
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
        resetTo(homeName())
    else if a.type = "reload"
        showReload()
    else if a.type = "switch"
        ' troca de playlist (tela Playlists)
        reg = CreateObject("roRegistrySection", "orbita")
        reg.Write("server", a.server)
        reg.Write("user", a.user)
        reg.Write("pass", a.pass)
        reg.Flush()
        m.global.creds = { server: a.server, user: a.user, pass: a.pass }
        resetTo(homeName())
    else if a.type = "logout"
        reg = CreateObject("roRegistrySection", "orbita")
        for each k in ["server", "user", "pass", "progress", "favs", "recents", "playlists"]
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
    m.video.enableUI = false
    m.video.scale = [1.0, 1.0]
    if a.start <> invalid and a.start > 0 then c.PlayStart = a.start
    m.video.content = c
    m.video.visible = true
    m.screens.visible = false
    m.video.control = "play"
    m.video.setFocus(true)
    if a.live <> true
        m.vod.visible = true
        m.vod.item = a
        m.vod.setFocus(true)
    else
        m.vod.visible = false
    end if
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

sub onVodAction(event as object)
    a = event.getData()
    p = m.playing
    if a.type = "next" and p <> invalid and p.queue <> invalid and p.queue.Count() > 0
        saveProgress()
        nxt = p.queue[0]
        rest = []
        for i = 1 to p.queue.Count() - 1
            rest.Push(p.queue[i])
        end for
        nxt.queue = rest
        play(nxt)
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
    m.vod.visible = false
    m.video.scale = [1.0, 1.0]
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
    if m.splash <> invalid or m.disc <> invalid then return true
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
        if m.exitBox = invalid and m.stack.Count() = 1 and isHome(m.stack[0].subtype())
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
