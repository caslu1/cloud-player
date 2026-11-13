' Janela do Recarregar (igual ao Android): "Carregando…" e depois "Atualização concluída".
sub init()
    m.top.findNode("title").font = mkFont(OUTF(), 34)
    m.top.findNode("title").color = "0xFFFFFFFF"
    m.top.findNode("sub").font = mkFont(IREG(), 20)
    m.top.findNode("sub").color = "0x9D9ABBFF"
    sp = m.top.findNode("spin")
    sp.poster.uri = "pkg:/images/spinner.png"
    sp.poster.width = 88
    sp.poster.height = 88
    sp.control = "start"
    m.pending = 4
    callApi("acc", apiUrl(""))
    callApi("lc", apiUrl("get_live_categories"))
    callApi("vc", apiUrl("get_vod_categories"))
    callApi("sc", apiUrl("get_series_categories"))
    m.top.findNode("closeT").observeField("fire", "finish")
end sub

sub onApi(event as object)
    m.pending = m.pending - 1
    if m.pending > 0 then return
    sp = m.top.findNode("spin")
    sp.control = "stop"
    sp.visible = false
    m.top.findNode("check").visible = true
    m.top.findNode("title").text = "Atualização concluída"
    m.top.findNode("sub").text = "Sua lista está em dia."
    m.top.findNode("closeT").control = "start"
end sub

sub finish()
    m.top.done = true
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    return true
end function
