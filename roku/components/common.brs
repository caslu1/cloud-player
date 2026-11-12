' Funções usadas por várias telas.

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

function asText(v as dynamic) as string
    if v = invalid then return ""
    t = type(v)
    if t = "roString" or t = "String" then return v
    if t = "roInteger" or t = "Integer" or t = "roInt" or t = "roLongInteger" or t = "LongInteger" then return v.ToStr()
    if t = "roFloat" or t = "Float" or t = "roDouble" or t = "Double" then return Int(v).ToStr()
    return ""
end function

' Dispara uma consulta à API em segundo plano; a resposta chega em onApi(event).
sub callApi(id as string, url as string)
    t = CreateObject("roSGNode", "ApiTask")
    t.request = { id: id, url: url }
    t.observeField("response", "onApi")
    t.control = "RUN"
    if m.tasks = invalid then m.tasks = []
    m.tasks.push(t)
end sub

function listContent(items as object, labelKey as string) as object
    root = CreateObject("roSGNode", "ContentNode")
    for each it in items
        n = root.createChild("ContentNode")
        n.title = asText(it[labelKey])
    end for
    return root
end function
