' Funções usadas por várias telas (layout 1920x1080, mesmas medidas do app Android x2).

' --- API Xtream ---------------------------------------------------------------

' (roUrlTransfer não pode ser criado na thread da tela: travava o app no login)
function esc(s as string) as string
    return s.EncodeUriComponent()
end function

function apiUrl(action as string, extra = "" as string) as string
    c = m.global.creds
    u = c.server + "/player_api.php?username=" + esc(c.user) + "&password=" + esc(c.pass)
    if action <> "" then u = u + "&action=" + action
    return u + extra
end function

function streamUrl(kind as string, id as string, ext as string) as string
    c = m.global.creds
    return c.server + "/" + kind + "/" + esc(c.user) + "/" + esc(c.pass) + "/" + id + "." + ext
end function

function formatFor(ext as string) as string
    e = LCase(ext)
    if e = "m3u8" then return "hls"
    if e = "mkv" then return "mkv"
    return "mp4"
end function

' Conteúdo de vídeo com o mesmo "User-Agent" do app Android (muitos servidores bloqueiam o da Roku).
function mkVideo(url as string, fmt as string, live as boolean) as object
    c = CreateObject("roSGNode", "ContentNode")
    c.url = url
    c.streamFormat = fmt
    c.HttpHeaders = ["User-Agent: IPTVSmartersPlayer"]
    if live then c.live = true
    return c
end function

' Endereços para tentar num canal ao vivo: m3u8 (HLS) e depois .ts.
function liveSources(id as string) as object
    return [{ url: streamUrl("live", id, "m3u8"), format: "hls" }, { url: streamUrl("live", id, "ts"), format: "ts" }]
end function

' Dispara uma consulta à API em segundo plano; a resposta chega em onApi(event).
sub callApi(id as string, url as string)
    t = CreateObject("roSGNode", "ApiTask")
    t.request = { id: id, url: url }
    t.observeField("response", "onApi")
    t.control = "RUN"
    if m.tasks = invalid then m.tasks = []
    m.tasks.Push(t)
    if m.tasks.Count() > 12 then m.tasks.Shift()
end sub

' --- valores -------------------------------------------------------------------

function asText(v as dynamic) as string
    if v = invalid then return ""
    t = type(v)
    if t = "roString" or t = "String" then return v
    if t = "roInteger" or t = "Integer" or t = "roInt" or t = "roLongInteger" or t = "LongInteger" then return v.ToStr()
    if t = "roFloat" or t = "Float" or t = "roDouble" or t = "Double"
        if v = Int(v) then return Int(v).ToStr()
        s = Str(Int(v * 10) / 10).Trim()
        return s
    end if
    if t = "roBoolean" or t = "Boolean"
        if v then return "1"
        return "0"
    end if
    return ""
end function

function asNum(v as dynamic) as float
    s = asText(v)
    if s = "" then return 0
    return Val(s)
end function

function fmtCount(n as integer) as string
    s = n.ToStr()
    out = ""
    while Len(s) > 3
        out = "." + Right(s, 3) + out
        s = Left(s, Len(s) - 3)
    end while
    return s + out
end function

function two(n as integer) as string
    if n < 10 then return "0" + n.ToStr()
    return n.ToStr()
end function

function hhmmOf(secs as integer) as string
    d = CreateObject("roDateTime")
    d.FromSeconds(secs)
    d.ToLocalTime()
    return two(d.GetHours()) + ":" + two(d.GetMinutes())
end function

function nowSecs() as integer
    d = CreateObject("roDateTime")
    return d.AsSeconds()
end function

function fmtDuration(secs as integer) as string
    if secs <= 0 then return ""
    h = Int(secs / 3600)
    mi = Int((secs - h * 3600) / 60)
    if h > 0 then return h.ToStr() + "h " + two(mi) + "min"
    return mi.ToStr() + " min"
end function

function b64(s as string) as string
    if s = "" then return ""
    ba = CreateObject("roByteArray")
    ba.FromBase64String(s)
    return ba.ToAsciiString()
end function

' --- desenho -------------------------------------------------------------------

function mkFont(name as string, size as integer) as object
    f = CreateObject("roSGNode", "Font")
    f.uri = "pkg:/fonts/" + name
    f.size = size
    return f
end function

function addLabel(parent as object, text as string, x as float, y as float, fontName as string, size as integer, color as string, width = 0 as integer, align = "left" as string) as object
    l = parent.createChild("Label")
    l.text = text
    l.translation = [x, y]
    l.font = mkFont(fontName, size)
    l.color = color
    if width > 0 then l.width = width
    l.horizAlign = align
    return l
end function

function addPoster(parent as object, uri as string, x as float, y as float, w as integer, h as integer) as object
    p = parent.createChild("Poster")
    p.uri = uri
    p.translation = [x, y]
    p.width = w
    p.height = h
    return p
end function

function addRect(parent as object, x as float, y as float, w as integer, h as integer, color as string) as object
    r = parent.createChild("Rectangle")
    r.translation = [x, y]
    r.width = w
    r.height = h
    r.color = color
    return r
end function

' Imagem com cantos arredondados (MaskGroup).
function addRounded(parent as object, mask as string, x as float, y as float, w as integer, h as integer) as object
    g = parent.createChild("MaskGroup")
    g.maskUri = "pkg:/images/" + mask + ".png"
    g.maskSize = [w, h]
    g.translation = [x, y]
    p = g.createChild("Poster")
    p.width = w
    p.height = h
    p.loadDisplayMode = "scaleToZoom"
    return p
end function

' --- dados guardados na TV ---------------------------------------------------

function regRead(key as string) as object
    reg = CreateObject("roRegistrySection", "orbita")
    if not reg.Exists(key) then return []
    a = ParseJson(reg.Read(key))
    if type(a) <> "roArray" then return []
    return a
end function

sub regWrite(key as string, a as object)
    reg = CreateObject("roRegistrySection", "orbita")
    reg.Write(key, FormatJson(a))
    reg.Flush()
end sub

' Coloca/atualiza um item no início de uma lista guardada (chave "key").
sub regPush(name as string, item as object, max as integer)
    a = regRead(name)
    out = [item]
    for each it in a
        if it.key <> item.key and out.Count() < max then out.Push(it)
    end for
    regWrite(name, out)
end sub

sub regRemove(name as string, key as string)
    a = regRead(name)
    out = []
    for each it in a
        if it.key <> key then out.Push(it)
    end for
    regWrite(name, out)
end sub

function regFind(name as string, key as string) as object
    for each it in regRead(name)
        if it.key = key then return it
    end for
    return invalid
end function

' Fontes
function OUTF() as string
    return "outfit_bold.ttf"
end function
function IREG() as string
    return "inter_regular.otf"
end function
function IMED() as string
    return "inter_medium.otf"
end function
function ISEM() as string
    return "inter_semibold.otf"
end function
