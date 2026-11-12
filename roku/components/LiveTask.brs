' Baixa categorias e canais ao vivo, junta as versões SD/HD/FHD do mesmo canal (como no Android) e conta por categoria.
sub init()
    m.top.functionName = "load"
end sub

function getJson(url as string) as dynamic
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.AddHeader("User-Agent", "IPTVSmartersPlayer")
    xfer.EnableEncodings(true)
    xfer.SetUrl(url)
    if xfer.AsyncGetToString()
        msg = wait(45000, port)
        if type(msg) = "roUrlEvent" and msg.GetResponseCode() = 200 then return ParseJson(msg.GetString())
        xfer.AsyncCancel()
    end if
    return invalid
end function

sub load()
    cats = getJson(apiUrl("get_live_categories"))
    list = getJson(apiUrl("get_live_streams"))
    if type(cats) <> "roArray" or type(list) <> "roArray"
        m.top.response = { ok: false }
        return
    end if
    q = CreateObject("roRegex", "\b(fhd|uhd|hd|sd|4k|hevc|h265|h\.265|1080p?|720p?|2160p?|alt|backup)\b|²|\[[^\]]*\]|\([^)]*\)", "i")
    sp = CreateObject("roRegex", "[^a-z0-9]+", "i")
    seen = {}
    items = []
    counts = {}
    for each c in list
        name = asText(c.name)
        k = LCase(sp.ReplaceAll(q.ReplaceAll(name, " "), " ")).Trim() + "|" + asText(c.category_id)
        if not seen.DoesExist(k)
            seen[k] = true
            cat = asText(c.category_id)
            items.Push({ id: asText(c.stream_id), name: name, num: asText(c.num), logo: asText(c.stream_icon), cat: cat })
            if counts.DoesExist(cat) then counts[cat] = counts[cat] + 1 else counts[cat] = 1
        end if
    end for
    outCats = []
    for each c in cats
        id = asText(c.category_id)
        if counts.DoesExist(id) then outCats.Push({ id: id, name: asText(c.category_name), count: counts[id] })
    end for
    m.top.response = { ok: true, cats: outCats, items: items }
end sub
