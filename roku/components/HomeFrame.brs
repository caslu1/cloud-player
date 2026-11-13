' Moldura comum de todos os temas da tela inicial (igual ao Android/Vitrine):
' relógio no canto superior esquerdo, logo no centro, Buscar/Recarregar/Configurações à direita
' e o rodapé com versão, status, endereço MAC, chave do dispositivo e usuário.
sub frameInit(r as object)
    m.fClock = addLabel(r, "", 60, 38, OUTF(), 36, "0xFFFFFFFF")
    m.fDate = addLabel(r, "", 60, 84, IREG(), 16, "0x9D9ABBFF")
    addPoster(r, "pkg:/images/logo_big.png", 780, 116, 361, 72)
    m.topBtns = []
    names = ["search", "refresh", "settings"]
    for i = 0 to 2
        p = addPoster(r, "pkg:/images/rbtn_" + names[i] + "_n.png", 1616 + i * 88, 44, 68, 68)
        m.topBtns.Push({ poster: p, name: names[i] })
    end for
    m.topIdx = 2
    ver = CreateObject("roAppInfo").GetVersion()
    addLabel(r, "Versão: " + ver, 60, 1012, IREG(), 15, "0x9D9ABBFF")
    m.fStatus = addLabel(r, "Status: …", 60, 1034, IREG(), 15, "0x9D9ABBFF")
    user = asText(m.global.creds.user)
    y = 1034
    if user <> ""
        addLabel(r, "Usuário: " + user, 1160, y, IREG(), 15, "0x9D9ABBFF", 700, "right")
        y = y - 22
    end if
    addLabel(r, "Chave do dispositivo: " + deviceKey(), 1160, y, IREG(), 15, "0x9D9ABBFF", 700, "right")
    addLabel(r, "Endereço MAC: " + deviceMac(), 1160, y - 22, IREG(), 15, "0x9D9ABBFF", 700, "right")
    t = r.createChild("Timer")
    t.repeat = true
    t.duration = 20
    t.observeField("fire", "frameTick")
    t.control = "start"
    m.fTimer = t
    frameTick()
    callApi("acc", apiUrl(""))
end sub

sub frameTick()
    d = CreateObject("roDateTime")
    d.ToLocalTime()
    m.fClock.text = two(d.GetHours()) + ":" + two(d.GetMinutes())
    days = ["dom.", "seg.", "ter.", "qua.", "qui.", "sex.", "sáb."]
    months = ["jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."]
    m.fDate.text = days[d.GetDayOfWeek()] + ", " + d.GetDayOfMonth().ToStr() + " " + months[d.GetMonth() - 1]
end sub

' Resposta da conta (status e vencimento) para o rodapé. Devolve true se tratou.
function frameOnApi(r as object) as boolean
    if r.id <> "acc" then return false
    if not r.ok or type(r.data) <> "roAssociativeArray" or type(r.data.user_info) <> "roAssociativeArray" then return true
    u = r.data.user_info
    st = asText(u.status)
    if LCase(st) = "active" or st = "" then st = "Ativa"
    exp = asText(u.exp_date)
    if exp <> "" and exp <> "0"
        d = CreateObject("roDateTime")
        d.FromSeconds(asInt(exp))
        st = st + " · vence " + two(d.GetDayOfMonth()) + "/" + two(d.GetMonth()) + "/" + d.GetYear().ToStr()
    end if
    m.fStatus.text = "Status: " + st
    return true
end function

sub framePaint(focused as boolean)
    for i = 0 to m.topBtns.Count() - 1
        b = m.topBtns[i]
        if focused and i = m.topIdx then st = "_f" else st = "_n"
        b.poster.uri = "pkg:/images/rbtn_" + b.name + st + ".png"
    end for
end sub

' Teclas na barra de cima. Devolve "down" quando o foco deve voltar ao conteúdo.
function frameKey(key as string) as string
    if key = "left" and m.topIdx > 0
        m.topIdx = m.topIdx - 1
    else if key = "right" and m.topIdx < m.topBtns.Count() - 1
        m.topIdx = m.topIdx + 1
    else if key = "down"
        return "down"
    else if key = "OK"
        n = m.topBtns[m.topIdx].name
        if n = "settings"
            m.top.action = { type: "open", screen: "SettingsScreen", params: {} }
        else if n = "search"
            m.top.action = { type: "open", screen: "SearchScreen", params: {} }
        else
            m.top.action = { type: "reload" }
        end if
    else
        return "no"
    end if
    return "ok"
end function

' Contagens leves (categorias) para os blocos
sub frameCounts()
    callApi("lc", apiUrl("get_live_categories"))
    callApi("vc", apiUrl("get_vod_categories"))
    callApi("sc", apiUrl("get_series_categories"))
end sub

function catsLabel(r as object) as string
    if not r.ok or type(r.data) <> "roArray" then return ""
    n = r.data.Count()
    if n = 1 then return "1 categoria"
    return fmtCount(n) + " categorias"
end function

function playlistsLabel() as string
    n = regRead("playlists").Count()
    if n = 1 then return "1 playlist"
    return n.ToStr() + " playlists"
end function

' Os 4 destinos da tela inicial (iguais em todos os temas)
function homeItems() as object
    return [
        { key: "tv", title: "Canais ao vivo", short: "Canais", sic: "sic_tv", ln: "ln_tv", screen: "ChannelsScreen", params: {}, sub: "Ao vivo" },
        { key: "film", title: "Filmes", short: "Filmes", sic: "sic_film", ln: "ln_movie", screen: "VodScreen", params: { kind: "movie" }, sub: "Filmes da sua lista" },
        { key: "series", title: "Séries", short: "Séries", sic: "sic_clap", ln: "ln_series", screen: "VodScreen", params: { kind: "series" }, sub: "Séries da sua lista" },
        { key: "playlist", title: "Playlists", short: "Playlists", sic: "sic_list", ln: "ln_list", screen: "PlaylistsScreen", params: {}, sub: "Suas listas" },
    ]
end function

function currentPlaylistName() as string
    c = m.global.creds
    for each p in regRead("playlists")
        if asText(p.server) = asText(c.server) and asText(p.user) = asText(c.user) then return asText(p.name)
    end for
    return ""
end function
