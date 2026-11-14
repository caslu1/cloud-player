' Busca geral (igual ao Android): teclado à esquerda, resultados de canais, filmes e séries à direita.
sub init()
    r = m.top.findNode("root")
    addLabel(r, "Buscar", 60, 60, OUTF(), 48, "0xFFFFFFFF")
    addLabel(r, "Canais, filmes e séries da sua lista", 60, 124, IREG(), 20, "0x9D9ABBFF")
    ' campo de busca: OK abre o teclado do app (igual ao Android); resultados aparecem enquanto digita
    m.field = addPoster(r, "pkg:/images/field_f.9.png", 60, 166, 1800, 76)
    addPoster(r, "pkg:/images/ic_search.png", 86, 190, 28, 28)
    m.fieldLbl = addLabel(r, "Canal, filme ou série", 130, 188, IMED(), 24, "0x6E6B8CFF", 1700)
    m.status = addLabel(r, "Carregando o catálogo…", 60, 262, ISEM(), 19, "0x9D9ABBFF", 1800)
    m.q = ""
    m.kb = invalid
    m.zone = "field"
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
    if not m.top.hasFocus() then return
    if m.kb <> invalid
        m.kb.setFocus(true)
    else if m.zone = "field" and m.q = ""
        openKb()
    end if
end sub

sub openKb()
    k = CreateObject("roSGNode", "TvKeyboard")
    k.setup = { label: "Buscar", text: m.q, password: false }
    k.observeField("text", "onKbText")
    k.observeField("result", "onKbDone")
    m.top.appendChild(k)
    m.kb = k
    k.setFocus(true)
end sub

sub onKbText()
    m.q = m.q
    if m.q = "" then m.fieldLbl.text = "Canal, filme ou série" else m.fieldLbl.text = m.q
    if m.q = "" then m.fieldLbl.color = "0x6E6B8CFF" else m.fieldLbl.color = "0xFFFFFFFF"
    onText()
end sub

sub onKbDone()
    m.top.removeChild(m.kb)
    m.kb = invalid
    if m.results.Count() > 0
        m.zone = "grid"
        m.field.uri = "pkg:/images/field_n.9.png"
        m.grid.setFocus(true)
    else
        m.zone = "field"
        m.top.setFocus(true)
    end if
end sub

sub onState()
    if m.task.phase = "ready" then onText()
end sub

sub onText()
    q = m.q
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
    if Len(m.q.Trim()) < 2
        m.status.text = "Digite pelo menos 2 letras"
    else if m.results.Count() = 0
        m.status.text = "Nada encontrado para " + Chr(34) + m.q + Chr(34)
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
    if m.zone = "field"
        if key = "OK"
            openKb()
            return true
        else if key = "down" and m.results.Count() > 0
            m.zone = "grid"
            m.field.uri = "pkg:/images/field_n.9.png"
            m.grid.setFocus(true)
            return true
        end if
    else if m.zone = "grid" and key = "up"
        m.zone = "field"
        m.field.uri = "pkg:/images/field_f.9.png"
        m.top.setFocus(true)
        return true
    end if
    return false
end function
