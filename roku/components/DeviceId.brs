' "MAC" do aparelho (a Roku não expõe o MAC real): gerado a partir do ID do canal, guardado na TV.
function deviceMac() as string
    reg = CreateObject("roRegistrySection", "orbita")
    if reg.Exists("dmac") then return reg.Read("dmac")
    ba = CreateObject("roByteArray")
    ba.FromAsciiString(CreateObject("roDeviceInfo").GetChannelClientId())
    d = CreateObject("roEVPDigest")
    d.Setup("sha256")
    hex = UCase(d.Process(ba))
    first = Val(Left(hex, 2), 16)
    first = (first and &hFC) or &h02
    h = Right("0" + StrI(first, 16), 2)
    mac = UCase(h)
    for i = 1 to 5
        mac = mac + ":" + Mid(hex, i * 2 + 1, 2)
    end for
    reg.Write("dmac", mac)
    reg.Flush()
    return mac
end function

function deviceKey() as string
    reg = CreateObject("roRegistrySection", "orbita")
    if reg.Exists("dkey") then return reg.Read("dkey")
    k = (100000 + Rnd(899999)).ToStr()
    reg.Write("dkey", k)
    reg.Flush()
    return k
end function

