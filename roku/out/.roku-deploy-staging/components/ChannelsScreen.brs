sub init()
    m.cats = m.top.findNode("cats")
    m.list = m.top.findNode("channels")
    m.status = m.top.findNode("status")
    m.cats.observeField("itemSelected", "onCat")
    m.list.observeField("itemSelected", "onChannel")
    m.top.observeField("focusedChild", "onFocus")
    m.catData = []
    m.chData = []
    m.focusList = m.cats
    m.seq = 0
    callApi("cats", apiUrl("get_live_categories"))
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
            m.status.text = "Não foi possível carregar os canais."
        end if
    else if r.id = "ch" + m.seq.ToStr()
        m.status.text = ""
        if r.ok and type(r.data) = "roArray"
            m.chData = r.data
            root = CreateObject("roSGNode", "ContentNode")
            for each c in r.data
                n = root.createChild("ContentNode")
                num = asText(c.num)
                if num <> "" then num = num + "    "
                n.title = num + asText(c.name)
            end for
            m.list.content = root
            if r.data.Count() = 0 then m.status.text = "Nenhum canal nesta categoria"
            if m.moveRight = true and r.data.Count() > 0
                m.focusList = m.list
                m.list.setFocus(true)
            end if
        else
            m.status.text = "Não foi possível carregar os canais."
        end if
    end if
end sub

sub loadCat(i as integer)
    c = m.catData[i]
    m.top.findNode("catTitle").text = UCase(asText(c.category_name))
    m.status.text = "Carregando…"
    m.list.content = CreateObject("roSGNode", "ContentNode")
    m.seq = m.seq + 1
    callApi("ch" + m.seq.ToStr(), apiUrl("get_live_streams", "&category_id=" + esc(asText(c.category_id))))
end sub

sub onCat()
    m.moveRight = true
    loadCat(m.cats.itemSelected)
end sub

sub onChannel()
    c = m.chData[m.list.itemSelected]
    url = streamUrl("live", asText(c.stream_id), "m3u8")
    m.top.action = { type: "play", url: url, title: asText(c.name), format: "hls", live: true }
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "right" and m.cats.hasFocus() and m.chData.Count() > 0
        m.focusList = m.list
        m.list.jumpToItem = 0
        m.list.setFocus(true)
        return true
    else if key = "left" and m.list.hasFocus()
        m.focusList = m.cats
        m.cats.setFocus(true)
        return true
    end if
    return false
end function
