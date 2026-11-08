/******************************************************************************/
// Cloud Player for Media Station X - main menu
/******************************************************************************/
function createMenu() {
    var m3u = CloudPlayer.getM3u();
    var configured = CloudPlayer.isValid(m3u);

    var notConfigured = {
        type: "list",
        headline: "Cloud Player",
        template: {
            type: "separate",
            layout: "0,0,4,2",
            color: "msx-glass"
        },
        items: [{
            icon: "playlist-add",
            title: "Adicionar minha lista M3U",
            titleFooter: "Digite o link da sua lista uma única vez",
            action: CloudPlayer.setupAction()
        }]
    };

    var settings = {
        type: "list",
        headline: "Configurações",
        template: {
            type: "separate",
            layout: "0,0,4,2",
            color: "msx-glass"
        },
        items: [{
            icon: "edit",
            title: configured ? "Trocar lista M3U" : "Adicionar lista M3U",
            titleFooter: configured ? "Lista atual salva nesta TV" : "Nenhuma lista configurada",
            action: CloudPlayer.setupAction()
        }, {
            icon: "info",
            title: "Sobre",
            titleFooter: "Cloud Player para Media Station X",
            action: "panel:data",
            data: {
                pages: [{
                    items: [{
                        type: "default",
                        layout: "0,0,8,4",
                        color: "msx-glass",
                        headline: "Cloud Player",
                        text: "Versão para Media Station X. O app não fornece nenhum conteúdo: ele só mostra a lista M3U que você adicionar. O link da lista fica salvo apenas nesta TV e é convertido pelo serviço de listas M3U do Media Station X (msx.benzac.de).",
                        action: "back"
                    }]
                }]
            }
        }]
    };

    return {
        name: "Cloud Player",
        version: "1.0.0",
        headline: "Cloud Player",
        menu: [{
            icon: "live-tv",
            label: "TV ao vivo",
            // Configured: the menu opens the channel pages of the M3U service directly
            data: configured ? CloudPlayer.channelsUrl(m3u) : notConfigured
        }, {
            type: "separator"
        }, {
            icon: "settings",
            label: "Configurações",
            data: settings
        }]
    };
}

function MenuHandler() {
    this.init = function () { };
    this.ready = function () { };
    this.handleRequest = function (dataId, data, callback) {
        if (dataId == "init") {
            callback(createMenu());
        } else {
            callback(null);
        }
    };
}

tvx.PluginTools.onReady(function () {
    tvx.InteractionPlugin.setupHandler(new MenuHandler());
    tvx.InteractionPlugin.init();
});
