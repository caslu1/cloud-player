/******************************************************************************/
// Cloud Player for Media Station X - shared helpers
// The M3U address is stored only on the TV (localStorage of this plugin), never in the repository.
/******************************************************************************/
var CloudPlayer = (function () {
    var KEY = "cloudplayer_m3u";
    var memory = "";
    // Official M3U service of Media Station X: turns an M3U playlist into MSX channel pages
    var M3U_SERVICE = "http://msx.benzac.de/services/m3u.php?type=video&mode=extended&url=";

    function getM3u() {
        try {
            var v = window.localStorage.getItem(KEY);
            if (v != null) return v;
        } catch (e) { /* storage blocked: keep value in memory */ }
        return memory;
    }

    function setM3u(v) {
        memory = v || "";
        try {
            if (memory) window.localStorage.setItem(KEY, memory); else window.localStorage.removeItem(KEY);
        } catch (e) { /* storage blocked */ }
    }

    function isValid(v) {
        return /^https?:\/\/[^\s\/]+\.[^\s]*$|^https?:\/\/[^\s\/]+:\d+/.test(v || "");
    }

    function channelsUrl(v) {
        return M3U_SERVICE + encodeURIComponent(v);
    }

    function setupAction() {
        return "content:request:interaction:init@" + tvx.Tools.getAbsoluteUrl("setup.html");
    }

    return {
        getM3u: getM3u,
        setM3u: setM3u,
        isValid: isValid,
        channelsUrl: channelsUrl,
        setupAction: setupAction
    };
})();
