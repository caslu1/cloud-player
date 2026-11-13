' Busca geral (canais, filmes e séries) fora da tela.
' Leve para a Roku: guarda o catálogo como texto dividido por item (sem montar 30 mil objetos)
' e só converte em objeto os itens que batem com a busca.
sub init()
    m.top.functionName = "loadAll"
end sub

function getText(url as string) as string
    x = CreateObject("roUrlTransfer")
    x.SetCertificatesFile("common:/certs/ca-bundle.crt")
    x.InitClientCertificates()
    x.AddHeader("User-Agent", "IPTVSmartersPlayer")
    x.EnableEncodings(true)
    x.SetUrl(url)
    return x.GetToString()
end function

' minúsculas e sem acento (o teclado da Roku não tem acentos)
function fold(s as string) as string
    s = LCase(s)
    for each p in [["á", "a"], ["à", "a"], ["â", "a"], ["ã", "a"], ["é", "e"], ["ê", "e"], ["í", "i"], ["ó", "o"], ["ô", "o"], ["õ", "o"], ["ú", "u"], ["ç", "c"], ["Á", "a"], ["Ã", "a"], ["É", "e"], ["Í", "i"], ["Ó", "o"], ["Ú", "u"], ["Ç", "c"]]
        s = s.Replace(p[0], p[1])
    end for
    return s
end function

sub loadAll()
    c = m.top.creds
    base = c.server + "/player_api.php?username=" + c.user + "&password=" + c.pass + "&action="
    m.top.phase = "loading"
    m.sets = []
    for each l in [["live", "get_live_streams"], ["movie", "get_vod_streams"], ["series", "get_series"]]
        raw = getText(base + l[1])
        if Len(raw) > 2
            m.sets.Push({ k: l[0], raw: raw.Split("{""num"":"), low: fold(raw).Split("{""num"":") })
        end if
        raw = ""
    end for
    m.top.phase = "ready"
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
        for each st in m.sets
            low = st.low
            per = 0
            for i = 1 to low.Count() - 1
                if Instr(1, low[i], q) > 0
                    ' só vale se a palavra estiver no nome (não na sinopse)
                    t = st.raw[i].Trim()
                    if Right(t, 1) = "]" then t = Left(t, Len(t) - 1)
                    if Right(t, 1) = "," then t = Left(t, Len(t) - 1)
                    it = ParseJson("{""num"":" + t)
                    if type(it) = "roAssociativeArray" and Instr(1, fold(asStr(it.name)), q) > 0
                        if st.k = "series"
                            out.Push({ k: "series", name: asStr(it.name), id: asStr(it.series_id), icon: asStr(it.cover), r: asStr(it.rating), ext: "" })
                        else
                            out.Push({ k: st.k, name: asStr(it.name), id: asStr(it.stream_id), icon: asStr(it.stream_icon), r: asStr(it.rating), ext: asStr(it.container_extension) })
                        end if
                        per = per + 1
                        if per >= 40 then exit for
                    end if
                end if
            end for
        end for
    end if
    m.top.results = out
end sub
