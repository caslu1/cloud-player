sub init()
    m.top.functionName = "fetch"
end sub

sub fetch()
    req = m.top.request
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.AddHeader("User-Agent", "IPTVSmartersPlayer")
    xfer.EnableEncodings(true)
    xfer.SetUrl(req.url)
    body = ""
    code = 0
    if xfer.AsyncGetToString()
        msg = wait(30000, port)
        if type(msg) = "roUrlEvent"
            code = msg.GetResponseCode()
            if code = 200 then body = msg.GetString()
        else
            xfer.AsyncCancel()
        end if
    end if
    data = invalid
    if body <> "" then data = ParseJson(body)
    m.top.response = { id: req.id, ok: data <> invalid, code: code, data: data }
end sub
