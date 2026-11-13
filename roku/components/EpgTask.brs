' Busca o programa de agora de vários canais (para a linha de cada canal, como no Android).
sub init()
    m.top.functionName = "load"
end sub

sub load()
    out = {}
    port = CreateObject("roMessagePort")
    now = CreateObject("roDateTime").AsSeconds()
    for each id in m.top.ids
        xfer = CreateObject("roUrlTransfer")
        xfer.SetMessagePort(port)
        xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
        xfer.InitClientCertificates()
        xfer.AddHeader("User-Agent", "IPTVSmartersPlayer")
        xfer.SetUrl(apiUrl("get_short_epg", "&stream_id=" + id + "&limit=4"))
        if xfer.AsyncGetToString()
            msg = wait(10000, port)
            if type(msg) = "roUrlEvent" and msg.GetResponseCode() = 200
                d = ParseJson(msg.GetString())
                if type(d) = "roAssociativeArray" and type(d.epg_listings) = "roArray"
                    cur = invalid
                    nxt = invalid
                    for each e in d.epg_listings
                        s = asInt((e.start_timestamp))
                        en = asInt((e.stop_timestamp))
                        p = { title: b64(asText(e.title)), start: s, stop: en }
                        if s <= now and now < en and cur = invalid
                            cur = p
                        else if s >= now and nxt = invalid
                            nxt = p
                        end if
                    end for
                    if cur <> invalid then out[id] = { title: cur.title, start: cur.start, stop: cur.stop, now: cur, nxt: nxt, full: true }
                end if
            end if
        end if
        if not out.DoesExist(id) then out[id] = { title: "", start: 0, stop: 0, full: true }
    end for
    m.top.response = out
end sub
