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
        xfer.SetUrl(apiUrl("get_short_epg", "&stream_id=" + id + "&limit=3"))
        if xfer.AsyncGetToString()
            msg = wait(10000, port)
            if type(msg) = "roUrlEvent" and msg.GetResponseCode() = 200
                d = ParseJson(msg.GetString())
                if type(d) = "roAssociativeArray" and type(d.epg_listings) = "roArray"
                    for each e in d.epg_listings
                        s = Int(asNum(e.start_timestamp))
                        en = Int(asNum(e.stop_timestamp))
                        if s <= now and now < en
                            out[id] = { title: b64(asText(e.title)), start: s, stop: en }
                            exit for
                        end if
                    end for
                end if
            end if
        end if
        if not out.DoesExist(id) then out[id] = { title: "", start: 0, stop: 0 }
    end for
    m.top.response = out
end sub
