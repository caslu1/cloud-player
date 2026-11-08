package com.cloudplayer.funplays;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The same calls the funplays.app website makes:
 * validate_mac -> login_by_mac (mac + key -> token) -> playlist_with_mac -> device.
 */
final class FunplaysApi {
    private static final String API = "https://api.funplays.app/api/";

    private FunplaysApi() {}

    /** Test hook: replaces the network (method, path, body) -> raw JSON response. */
    interface Transport { String send(String method, String path, String body) throws Exception; }
    static volatile Transport testTransport;

    static String normalizeMac(String raw) {
        String mac = raw.trim().toLowerCase(Locale.ROOT).replace('-', ':').replace(" ", "");
        if (!mac.contains(":") && mac.length() == 12) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i += 2) {
                if (i > 0) sb.append(':');
                sb.append(mac, i, i + 2);
            }
            mac = sb.toString();
        }
        return mac;
    }

    static JSONObject validateMac(String mac) throws Exception {
        Object m = call("GET", "validate_mac?mac=" + URLEncoder.encode(mac, "UTF-8"), null, null, null);
        if (!(m instanceof JSONObject)) throw new IOException("MAC não encontrado");
        return (JSONObject) m;
    }

    static String login(String mac, String key) throws Exception {
        JSONObject body = new JSONObject().put("mac", mac).put("key", key);
        Object m = call("POST", "login_by_mac", null, body.toString(), null);
        String token = String.valueOf(m);
        if (token.isEmpty() || "null".equals(token)) throw new IOException("Login falhou");
        return token;
    }

    static JSONObject addPlaylist(String token, String mac, String url, String name, String epg) throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", name);
        form.put("mac", mac);
        form.put("url", url);
        form.put("is_protected", "false");
        if (epg != null && !epg.isEmpty()) form.put("epg_url", epg);
        Object m = call("POST", "playlist_with_mac", token, null, form);
        return m instanceof JSONObject ? (JSONObject) m : new JSONObject().put("name", name);
    }

    static void editPlaylist(String token, long id, String name, String url, String epg) throws Exception {
        JSONObject body = new JSONObject().put("id", id).put("name", name).put("url", url)
                .put("epg_url", epg == null ? "" : epg);
        call("PUT", "playlist", token, body.toString(), null);
    }

    static void deletePlaylist(String token, long id) throws Exception {
        // Endpoint name is misspelled on the server ("palylist"), exactly as the website uses it.
        call("DELETE", "palylist_from_web", token, new JSONObject().put("id", id).toString(), null);
    }

    static JSONArray listPlaylists(String token) throws Exception {
        Object m = call("GET", "device", token, null, null);
        if (m instanceof JSONObject) {
            JSONArray a = ((JSONObject) m).optJSONArray("playlists");
            return a != null ? a : new JSONArray();
        }
        return m instanceof JSONArray ? (JSONArray) m : new JSONArray();
    }

    /** Returns the "message" field of the response, or throws with the server's error message. */
    private static Object call(String method, String path, String token, String json,
                               Map<String, String> form) throws Exception {
        if (testTransport != null) {
            String fake = testTransport.send(method, path, json != null ? json : String.valueOf(form));
            return parse(fake, 200);
        }
        HttpURLConnection c = (HttpURLConnection) new URL(API + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Accept", "application/json");
        if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);

        byte[] payload = null;
        if (json != null) {
            payload = json.getBytes(StandardCharsets.UTF_8);
            c.setRequestProperty("Content-Type", "application/json");
        } else if (form != null) {
            String boundary = UUID.randomUUID().toString().replace("-", "");
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : form.entrySet()) {
                sb.append("--").append(boundary).append("\r\n")
                  .append("Content-Disposition: form-data; name=\"").append(e.getKey()).append("\"\r\n\r\n")
                  .append(e.getValue()).append("\r\n");
            }
            sb.append("--").append(boundary).append("--\r\n");
            payload = sb.toString().getBytes(StandardCharsets.UTF_8);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        }
        if (payload != null) {
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(payload.length);
            try (OutputStream os = c.getOutputStream()) { os.write(payload); }
        }

        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String body = in == null ? "" : readAll(in);
        c.disconnect();
        return parse(body, code);
    }

    private static Object parse(String body, int code) throws Exception {
        JSONObject r;
        try {
            r = new JSONObject(body);
        } catch (Exception e) {
            throw new IOException("Resposta inesperada do servidor (HTTP " + code + ")");
        }
        Object message = r.opt("message");
        if (r.optBoolean("error", false) || code >= 400) {
            String msg = message instanceof JSONObject
                    ? ((JSONObject) message).optString("message", message.toString())
                    : String.valueOf(message);
            throw new IOException(msg == null || msg.isEmpty() ? "HTTP " + code : msg);
        }
        return message;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }
}
