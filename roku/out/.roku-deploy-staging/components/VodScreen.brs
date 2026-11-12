sub init()
    m.cats = m.top.findNode("cats")
    m.grid = m.top.findNode("grid")
    m.status = m.top.findNode("status")
    m.cats.observeField("itemSelected", "onCat")
    m.grid.observeField("itemSelected", "onItem")
    m.top.observeField("focusedChild", "onFocus")
    m.catData = []
    m.items = []
    m.focusList = m.cats
    m.seq = 0
end sub

sub onParams()
    m.series = (m.top.params.kind = "series")
    if m.series
        m.top.findNode("title").text = "SÉRIES"
        callApi("cats", apiUrl("get_series_categories"))
    else
        m.top.findNode("title").text = "FILMES"
        callApi("cats", apiUrl("get_vod_categories"))
    end if
end sub

sub onFocus()
    if m.top.hasFocus() then m.focusList.setFocus(true)
end sub

sub onApi(event as object)
    r = event.getData()
    if r.id = "cats"
        if r.ok and type(r.data) = "roArray"
            m.catData = r.data
            m.cats.content = listContent(r.data, "category_name")
            if r.data.Count() > 0 then loadCat(0) else m.status.text = "Nenhuma categoria"
        else
            m.status.text = "Não foi possível carregar."
        end if
    else if r.id = "items" + m.seq.ToStr()
        m.status.text = ""
        if r.ok and type(r.data) = "roArray"
            m.items = r.data
            root = CreateObject("roSGNode", "ContentNode")
            for each it in r.data
                n = root.createChild("ContentNode")
                img = asText(it.stream_icon)
                if m.series then img = asText(it.cover)
                n.HDGRIDPOSTERURL = img
                n.FHDGRIDPOSTERURL = img
                n.SHORTDESCRIPTIONLINE1 = asText(it.name)
            end for
            m.grid.content = root
            if r.data.Count() = 0 then m.status.text = "Nada nesta categoria"
            if m.moveRight = true and r.data.Count() > 0
                m.focusList = m.grid
                m.grid.setFocus(true)
            end if
        else
            m.status.text = "Não foi possível carregar."
        end if
    end if
end sub

sub loadCat(i as integer)
    c = m.catData[i]
    m.top.findNode("catTitle").text = UCase(asText(c.category_name))
    m.status.text = "Carregando…"
    m.items = []
    m.grid.content = CreateObject("roSGNode", "ContentNode")
    m.seq = m.seq + 1
    action = "get_vod_streams"
    if m.series then action = "get_series"
    callApi("items" + m.seq.ToStr(), apiUrl(action, "&category_id=" + esc(asText(c.category_id))))
end sub

sub onCat()
    m.moveRight = true
    loadCat(m.cats.itemSelected)
end sub

sub onItem()
    it = m.items[m.grid.itemSelected]
    if m.series
        m.top.action = { type: "open", screen: "SeriesScreen", params: { id: asText(it.series_id), name: asText(it.name) } }
    else
        ext = asText(it.container_extension)
        if ext = "" then ext = "mp4"
        m.top.action = { type: "play", url: streamUrl("movie", asText(it.stream_id), ext), title: asText(it.name), format: formatFor(ext), live: false }
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "right" and m.cats.hasFocus() and m.items.Count() > 0
        m.focusList = m.grid
        m.grid.jumpToItem = 0
        m.grid.setFocus(true)
        return true
    else if key = "left" and m.grid.hasFocus()
        m.focusList = m.cats
        m.cats.setFocus(true)
        return true
    end if
    return false
end function
