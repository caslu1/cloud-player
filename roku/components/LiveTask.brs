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

function qualityOf(name as string) as string
    if CreateObject("roRegex", "\b(uhd|4k|2160p?)\b", "i").IsMatch(name) then return "UHD"
    if CreateObject("roRegex", "\b(fhd|full\s?hd|1080p?)\b", "i").IsMatch(name) then return "FHD"
    if CreateObject("roRegex", "\b(hd|720p?)\b", "i").IsMatch(name) then return "HD"
    if CreateObject("roRegex", "\b(sd|480p?)\b", "i").IsMatch(name) then return "SD"
    return ""
end function

' Preferência igual ao Android: FHD, HD, 4K, sem marcação, SD (a principal antes da "²")
function rank(name as string, q as string) as integer
    r = 30
    if q = "FHD" then r = 0
    if q = "HD" then r = 10
    if q = "UHD" then r = 20
    if q = "SD" then r = 40
    if Instr(1, name, "²") > 0 then r = r + 5
    return r
end function

' Baixa categorias e canais, junta as versões SD/HD/FHD do mesmo canal numa linha (a qualidade se escolhe no player).
sub load()
    cats = getJson(apiUrl("get_live_categories"))
    list = getJson(apiUrl("get_live_streams"))
    if type(cats) <> "roArray" or type(list) <> "roArray"
        m.top.response = { ok: false }
        return
    end if
    strip = CreateObject("roRegex", "\b(fhd|uhd|hd|sd|4k|hevc|h265|h\.265|full\s?hd|1080p?|720p?|2160p?|480p?|alt|backup)\b|²|\[[^\]]*\]|\([^)]*\)", "i")
    sp = CreateObject("roRegex", "[^a-z0-9]+", "i")
    groups = {}
    order = []
    variants = {}
    for each c in list
        name = asText(c.name)
        base = LCase(sp.ReplaceAll(strip.ReplaceAll(name, " "), " ")).Trim()
        if base = "" then base = LCase(name)
        cat = asText(c.category_id)
        q = qualityOf(name)
        it = { id: asText(c.stream_id), name: name, num: asText(c.num), logo: asText(c.stream_icon), cat: cat, vkey: base, q: q, r: rank(name, q) }
        if not variants.DoesExist(base) then variants[base] = {}
        if q <> "" and not variants[base].DoesExist(q) then variants[base][q] = it.id
        k = base + "|" + cat
        if not groups.DoesExist(k)
            groups[k] = it
            order.Push(k)
        else if it.r < groups[k].r
            ' mantém a posição/número da primeira, troca pela versão preferida
            it.num = groups[k].num
            groups[k] = it
        end if
    end for
    items = []
    counts = {}
    for each k in order
        it = groups[k]
        items.Push(it)
        if counts.DoesExist(it.cat) then counts[it.cat] = counts[it.cat] + 1 else counts[it.cat] = 1
    end for
    outCats = []
    for each c in cats
        id = asText(c.category_id)
        if counts.DoesExist(id) then outCats.Push({ id: id, name: asText(c.category_name), count: counts[id] })
    end for
    m.top.response = { ok: true, cats: outCats, items: items, variants: variants }
end sub
