' Busca geral (igual ao Android): teclado à esquerda, resultados de canais, filmes e séries à direita.
sub init()
    r = m.top.findNode("root")
    addLabel(r, "Buscar", 60, 60, OUTF(), 48, "0xFFFFFFFF")
    addLabel(r, "Canais, filmes e séries da sua lista", 60, 124, IREG(), 20, "0x9D9ABBFF")
    m.status = addLabel(r, "Carregando o catálogo…", 652, 120, ISEM(), 20, "0x9D9ABBFF", 1240)
    m.kb = m.top.findNode("kb")
    m.kb.observeField("text", "onText")
    m.grid = m.top.findNode("grid")
    m.grid.observeField("itemSelected", "onPick")
    m.task = CreateObject("roSGNode", "SearchTask")
    m.task.creds = m.global.creds
    m.task.observeField("phase", "onState")
    m.task.observeField("results", "onResults")
    m.task.control = "RUN"
    m.results = []
    m.top.observeField("focusedChild", "onFocus")
end sub

sub onFocus()
    if m.top.hasFocus() then m.kb.setFocus(true)
end sub

sub onState()
    if m.task.phase = "ready" then onText()
end sub

sub onText()
    q = m.kb.text
    if m.task.phase <> "ready"
        m.status.text = "Carregando o catálogo… (" + q + ")"
        return
    end if
    if Len(q.Trim()) < 2
        m.status.text = "Digite pelo menos 2 letras"
        m.grid.content = CreateObject("roSGNode", "ContentNode")
        m.results = []
        return
    end if
    m.task.query = q
end sub

sub onResults()
    m.results = m.task.results
    root = CreateObject("roSGNode", "ContentNode")
    for each it in m.results
        n = root.createChild("ContentNode")
        n.addFields({ stars: "" })
        n.title = it.name
        n.HDPOSTERURL = it.icon
        rv = Val(asText(it.r))
        if rv > 0 and rv <= 10 then n.stars = asText(Int(rv * 10) / 10)
    end for
    m.grid.content = root
    cnt = { live: 0, movie: 0, series: 0 }
    for each it in m.results
        cnt[it.k] = cnt[it.k] + 1
    end for
    if m.results.Count() = 0
        m.status.text = "Nada encontrado para " + Chr(34) + m.kb.text + Chr(34)
    else
        m.status.text = "Canais (" + cnt.live.ToStr() + ")   ·   Filmes (" + cnt.movie.ToStr() + ")   ·   Séries (" + cnt.series.ToStr() + ")"
    end if
end sub

sub onPick()
    it = m.results[m.grid.itemSelected]
    if it = invalid then return
    if it.k = "live"
        m.top.action = { type: "play", streamId: it.id, title: it.name, live: true }
    else if it.k = "series"
        m.top.action = { type: "open", screen: "SeriesScreen", params: { id: it.id, name: it.name, cover: it.icon, rating: it.r } }
    else
        m.top.action = { type: "open", screen: "MovieDetail", params: { id: it.id, name: it.name, icon: it.icon, ext: it.ext, rating: it.r } }
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "right" and m.kb.isInFocusChain() and m.results.Count() > 0
        m.grid.setFocus(true)
        return true
    else if key = "left" and m.grid.hasFocus() and (m.grid.itemFocused mod 4) = 0
        m.kb.setFocus(true)
        return true
    end if
    return false
end function
