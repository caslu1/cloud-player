' Conversa com o site de ativação: cadastra o aparelho, traz situação + listas e o QR code.
sub init()
    m.top.functionName = "go"
end sub

function http(url as string, body = "" as string) as dynamic
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.SetUrl(url)
    if body <> ""
        xfer.AddHeader("Content-Type", "application/json")
        ok = xfer.AsyncPostFromString(body)
    else
        ok = xfer.AsyncGetToString()
    end if
    if ok
        msg = wait(20000, port)
        if type(msg) = "roUrlEvent" and msg.GetResponseCode() = 200 then return ParseJson(msg.GetString())
        xfer.AsyncCancel()
    end if
    return invalid
end function

sub go()
    r = m.top.request
    di = CreateObject("roDeviceInfo")
    mac = deviceMac()
    key = deviceKey()
    body = FormatJson({ mac: mac, key: key, platform: "roku", model: "Roku " + di.GetModelDisplayName() })
    st = http(r.site + "/api/app/register", body)
    out = { ok: st <> invalid, data: st, mac: mac, key: key }
    if st <> invalid and r.wantQr = true and st.activate_url <> invalid
        q = http(r.site + "/api/qr?t=" + st.activate_url.EncodeUriComponent())
        if q <> invalid then out.qr = q
    end if
    m.top.response = out
end sub

