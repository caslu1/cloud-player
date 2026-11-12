sub init()
    m.seasons = m.top.findNode("seasons")
    m.eps = m.top.findNode("episodes")
    m.status = m.top.findNode("status")
    m.seasons.observeField("itemFocused", "onSeason")
    m.seasons.observeField("itemSelected", "onSeasonSelect")
    m.eps.observeField("itemSelected", "onEpisode")
    m.top.observeField("focusedChild", "onFocus")
    m.seasonKeys = []
    m.cur = []
    m.focusList = m.seasons
end sub

sub onParams()
    m.top.findNode("title").text = m.top.params.name
    callApi("info", apiUrl("get_series_info", "&series_id=" + esc(m.top.params.id)))
end sub

sub onFocus()
    if m.top.hasFocus() then m.focusList.setFocus(true)
end sub

sub onApi(event as object)
    r = event.getData()
    if not r.ok or type(r.data) <> "roAssociativeArray"
        m.status.text = "Não foi possível carregar os episódios."
        return
    end if
    info = r.data.info
    if type(info) = "roAssociativeArray"
        m.top.findNode("plot").text = asText(info.plot)
        bd = info.backdrop_path
        if type(bd) = "roArray" and bd.Count() > 0 then m.top.findNode("backdrop").uri = asText(bd[0])
    end if
    m.data = r.data.episodes
    if type(m.data) <> "roAssociativeArray"
        m.status.text = "Nenhum episódio disponível"
        return
    end if
    keys = []
    for each k in m.data
        keys.Push(k)
    end for
    ' ordena as temporadas pelo número
    for i = 0 to keys.Count() - 1
        for j = i + 1 to keys.Count() - 1
            if Val(keys[j]) < Val(keys[i])
                t = keys[i]
                keys[i] = keys[j]
                keys[j] = t
            end if
        end for
    end for
    m.seasonKeys = keys
    root = CreateObject("roSGNode", "ContentNode")
    for each k in keys
        n = root.createChild("ContentNode")
        n.title = "Temporada " + k
    end for
    m.seasons.content = root
    m.status.text = ""
    if keys.Count() > 0 then showSeason(0)
end sub

sub showSeason(i as integer)
    if i < 0 or i >= m.seasonKeys.Count() then return
    m.cur = m.data[m.seasonKeys[i]]
    if type(m.cur) <> "roArray" then m.cur = []
    root = CreateObject("roSGNode", "ContentNode")
    for each e in m.cur
        n = root.createChild("ContentNode")
        n.title = "E" + asText(e.episode_num) + "    " + asText(e.title)
    end for
    m.eps.content = root
end sub

sub onSeason()
    showSeason(m.seasons.itemFocused)
end sub

sub onSeasonSelect()
    if m.cur.Count() > 0
        m.focusList = m.eps
        m.eps.jumpToItem = 0
        m.eps.setFocus(true)
    end if
end sub

sub onEpisode()
    e = m.cur[m.eps.itemSelected]
    ext = asText(e.container_extension)
    if ext = "" then ext = "mp4"
    m.top.action = { type: "play", url: streamUrl("series", asText(e.id), ext), title: m.top.params.name + " · " + asText(e.title), format: formatFor(ext), live: false }
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "right" and m.seasons.hasFocus() and m.cur.Count() > 0
        m.focusList = m.eps
        m.eps.jumpToItem = 0
        m.eps.setFocus(true)
        return true
    else if key = "left" and m.eps.hasFocus()
        m.focusList = m.seasons
        m.seasons.setFocus(true)
        return true
    end if
    return false
end function
