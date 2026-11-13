' Busca geral (canais, filmes e séries): baixa o catálogo uma vez e procura pelo nome, fora da tela.
sub init()
    m.top.functionName = "loadAll"
end sub

function getJson(url as string) as dynamic
    x = CreateObject("roUrlTransfer")
    x.SetCertificatesFile("common:/certs/ca-bundle.crt")
    x.InitClientCertificates()
    x.AddHeader("User-Agent", "IPTVSmartersPlayer")
    x.EnableEncodings(true)
    x.SetUrl(url)
    return ParseJson(x.GetToString())
end function

function fold(s as string) as string
    s = LCase(s)
    pairs = [["á", "a"], ["à", "a"], ["â", "a"], ["ã", "a"], ["é", "e"], ["ê", "e"], ["í", "i"], ["ó", "o"], ["ô", "o"], ["õ", "o"], ["ú", "u"], ["ç", "c"], ["Á", "a"], ["É", "e"], ["Í", "i"], ["Ó", "o"], ["Ú", "u"], ["Ç", "c"]]
    for each p in pairs
        s = s.Replace(p[0], p[1])
    end for
    return s
end function

sub loadAll()
    c = m.top.creds
    base = c.server + "/player_api.php?username=" + c.user + "&password=" + c.pass + "&action="
    m.top.state = "loading"
    m.items = []
    lists = [["live", "get_live_streams"], ["movie", "get_vod_streams"], ["series", "get_series"]]
    for each l in lists
        data = getJson(base + l[1])
        if type(data) = "roArray"
            for each it in data
                if l[0] = "series"
                    m.items.Push({ k: "series", n: fold(asStr(it.name)), name: asStr(it.name), id: asStr(it.series_id), icon: asStr(it.cover), r: asStr(it.rating) })
                else if l[0] = "movie"
                    m.items.Push({ k: "movie", n: fold(asStr(it.name)), name: asStr(it.name), id: asStr(it.stream_id), icon: asStr(it.stream_icon), r: asStr(it.rating), ext: asStr(it.container_extension) })
                else
                    m.items.Push({ k: "live", n: fold(asStr(it.name)), name: asStr(it.name), id: asStr(it.stream_id), icon: asStr(it.stream_icon), r: "" })
                end if
            end for
        end if
        data = invalid
    end for
    m.top.state = "ready"
    port = CreateObject("roMessagePort")
    m.top.observeField("query", port)
    find(m.top.query)
    while true
        msg = wait(0, port)
        if type(msg) = "roSGNodeEvent" then find(msg.getData())
    end while
end sub

function asStr(v as dynamic) as string
    if v = invalid then return ""
    t = type(v)
    if t = "roString" or t = "String" then return v
    if t = "roInteger" or t = "Integer" or t = "roFloat" or t = "Float" or t = "Double" or t = "roDouble" or t = "LongInteger" then return v.ToStr()
    return ""
end function

sub find(q as string)
    q = fold(q.Trim())
    out = []
    if Len(q) >= 2
        for each it in m.items
            if Instr(1, it.n, q) > 0
                out.Push({ k: it.k, name: it.name, id: it.id, icon: it.icon, r: it.r, ext: it.ext })
                if out.Count() >= 80 then exit for
            end if
        end for
    end if
    m.top.results = out
end sub
