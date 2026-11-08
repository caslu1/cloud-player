/******************************************************************************/
// Cloud Player for Media Station X - on-screen keyboard to enter the M3U address
// (follows the "Search Example" of the official MSX interaction plugin examples)
/******************************************************************************/
var MAX_INPUT_LENGTH = 1000;
var SHOWN_CHARS = 56;

function SetupHandler() {
    var input = CloudPlayer.getM3u() || "http://";
    var upper = false;
    var state = "edit"; // "edit" | "saved"
    var message = null;

    function key(label, value, x, y, w) {
        return {
            type: "button",
            layout: x + "," + y + "," + (w || 1) + ",1",
            label: label,
            action: "interaction:commit",
            data: { action: "setup:input:" + value }
        };
    }

    function control(label, name, x, y, w) {
        return {
            type: "button",
            layout: x + "," + y + "," + w + ",1",
            label: label,
            action: "interaction:commit",
            data: { action: "setup:control:" + name }
        };
    }

    function shown() {
        if (input.length <= SHOWN_CHARS) return input;
        return "…" + input.substr(input.length - SHOWN_CHARS + 1);
    }

    function editPage() {
        var items = [];
        var letters = "abcdefghijklmnopqrstuvwxyz";
        var row0 = letters.substr(0, 12), row1 = letters.substr(12, 12);
        var row2 = letters.substr(24) + "0123456789";
        var row3 = ".:/-_?=&%~+@";
        var rows = [row0, row1, row2, row3];
        for (var y = 0; y < rows.length; y++) {
            for (var x = 0; x < rows[y].length; x++) {
                var c = rows[y].charAt(x);
                if (upper && /[a-z]/.test(c)) c = c.toUpperCase();
                items.push(key(c, c, x, y));
            }
        }
        items.push(control(upper ? "{ico:keyboard-capslock} ABC" : "{ico:keyboard-capslock} abc", "shift", 0, 4, 2));
        items.push(control("{ico:backspace}", "back", 2, 4, 2));
        items.push(control("{ico:clear} Limpar", "clear", 4, 4, 2));
        items.push(key("http://", "http://", 6, 4, 2));
        items.push(key("https://", "https://", 8, 4, 2));
        items.push(key(".com", ".com", 10, 4, 2));
        items.push(key("get.php?username=", "get.php?username=", 0, 5, 4));
        items.push(key("&password=", "&password=", 4, 5, 3));
        items.push(key("&type=m3u_plus", "&type=m3u_plus", 7, 5, 3));
        items.push(control("{ico:check} Salvar", "save", 10, 5, 2));
        return {
            headline: message || "{txt:msx-white-soft:Link M3U:} " + (shown() || "…"),
            items: items
        };
    }

    function savedPage() {
        return {
            headline: "{ico:check-circle} Lista salva nesta TV",
            items: [{
                type: "default",
                layout: "0,0,6,3",
                color: "msx-glass",
                icon: "live-tv",
                iconSize: "medium",
                label: "Abrir canais agora",
                action: "content:" + CloudPlayer.channelsUrl(input)
            }, {
                type: "default",
                layout: "6,0,6,3",
                color: "msx-glass",
                icon: "edit",
                iconSize: "medium",
                label: "Editar o link",
                action: "interaction:commit",
                data: { action: "setup:control:edit" }
            }, {
                type: "space",
                layout: "0,3,12,2",
                text: "Na próxima vez que abrir o Media Station X, o item \"TV ao vivo\" do menu abre a sua lista direto."
            }]
        };
    }

    function content() {
        return {
            cache: false,
            type: "list",
            important: true,
            headline: "Cloud Player – lista M3U",
            pages: [state == "saved" ? savedPage() : editPage()]
        };
    }

    function reload() {
        tvx.InteractionPlugin.executeAction("reload:content");
    }

    function handleControl(name) {
        message = null;
        if (name == "shift") {
            upper = !upper;
        } else if (name == "back") {
            if (input.length > 0) input = input.substr(0, input.length - 1);
        } else if (name == "clear") {
            input = "";
        } else if (name == "edit") {
            state = "edit";
        } else if (name == "save") {
            if (CloudPlayer.isValid(input)) {
                CloudPlayer.setM3u(input);
                state = "saved";
            } else {
                message = "{ico:error} Link inválido – ele deve começar com http:// ou https://";
            }
        }
    }

    this.init = function () { };
    this.ready = function () { };

    this.handleData = function (data) {
        var action = data != null && data.data != null ? data.data.action : null;
        if (typeof action != "string" || action.indexOf("setup:") != 0) return;
        action = action.substr(6);
        if (action.indexOf("input:") == 0) {
            message = null;
            if (input.length < MAX_INPUT_LENGTH) input += action.substr(6);
        } else if (action.indexOf("control:") == 0) {
            handleControl(action.substr(8));
        }
        reload();
    };

    this.handleRequest = function (dataId, data, callback) {
        if (dataId == "init") {
            callback(content());
        } else {
            callback(null);
        }
    };
}

tvx.PluginTools.onReady(function () {
    tvx.InteractionPlugin.setupHandler(new SetupHandler());
    tvx.InteractionPlugin.init();
});
