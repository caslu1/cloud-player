package com.cloudplayer.funplays;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** Saved devices (name + MAC + device key), stored as JSON in SharedPreferences. */
final class Devices {

    private final SharedPreferences prefs;

    Devices(SharedPreferences prefs) { this.prefs = prefs; }

    JSONArray all() {
        try {
            String raw = prefs.getString("devices", "[]");
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    int count() { return all().length(); }

    boolean hasMac(String mac) {
        if (mac == null) return false;
        JSONArray list = all();
        for (int i = 0; i < list.length(); i++) {
            JSONObject d = list.optJSONObject(i);
            if (d != null && mac.equalsIgnoreCase(d.optString("mac"))) return true;
        }
        return false;
    }

    /** Adds several devices (key may be empty, to fill in later). Returns how many were added. */
    int addMany(java.util.List<String[]> macsAndNames) {
        JSONArray list = all();
        int added = 0;
        try {
            for (String[] pair : macsAndNames) {
                String mac = pair[0];
                String name = pair.length > 1 ? pair[1] : "";
                if (mac == null || mac.isEmpty() || hasMacIn(list, mac)) continue;
                JSONObject d = new JSONObject()
                        .put("id", Long.toString(System.currentTimeMillis()) + "-" + added)
                        .put("name", name == null || name.isEmpty() ? macTail(mac) : name)
                        .put("mac", mac).put("key", "");
                list.put(d);
                added++;
            }
            prefs.edit().putString("devices", list.toString()).apply();
        } catch (Exception ignored) {
        }
        return added;
    }

    private static boolean hasMacIn(JSONArray list, String mac) {
        for (int i = 0; i < list.length(); i++) {
            JSONObject d = list.optJSONObject(i);
            if (d != null && mac.equalsIgnoreCase(d.optString("mac"))) return true;
        }
        return false;
    }

    /** Last two groups of a MAC, e.g. "CC:7D", for a short default name. */
    static String macTail(String mac) {
        if (mac == null) return "Aparelho";
        String[] parts = mac.split(":");
        if (parts.length >= 2) return (parts[parts.length - 2] + ":" + parts[parts.length - 1]).toUpperCase(java.util.Locale.ROOT);
        return mac.toUpperCase(java.util.Locale.ROOT);
    }

    static boolean hasKey(JSONObject d) { return d != null && !d.optString("key", "").trim().isEmpty(); }

    JSONObject get(String id) {
        JSONArray list = all();
        for (int i = 0; i < list.length(); i++) {
            JSONObject d = list.optJSONObject(i);
            if (d != null && id != null && id.equals(d.optString("id"))) return d;
        }
        return null;
    }

    /** First device, or null if none saved. */
    JSONObject first() {
        JSONArray list = all();
        return list.length() > 0 ? list.optJSONObject(0) : null;
    }

    String selectedId() { return prefs.getString("selected", null); }

    void select(String id) { prefs.edit().putString("selected", id).apply(); }

    /** The selected device, falling back to the first saved one. */
    JSONObject selected() {
        JSONObject d = get(selectedId());
        return d != null ? d : first();
    }

    /** Adds a new device (new id) or updates the one with the given id. Returns its id. */
    String save(String id, String name, String mac, String key) {
        JSONArray list = all();
        try {
            if (id == null || id.isEmpty()) {
                id = Long.toString(System.currentTimeMillis());
                JSONObject d = new JSONObject().put("id", id);
                writeInto(d, name, mac, key);
                list.put(d);
            } else {
                boolean found = false;
                for (int i = 0; i < list.length(); i++) {
                    JSONObject d = list.optJSONObject(i);
                    if (d != null && id.equals(d.optString("id"))) {
                        writeInto(d, name, mac, key);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    JSONObject d = new JSONObject().put("id", id);
                    writeInto(d, name, mac, key);
                    list.put(d);
                }
            }
            prefs.edit().putString("devices", list.toString()).apply();
        } catch (Exception ignored) {
        }
        return id;
    }

    private void writeInto(JSONObject d, String name, String mac, String key) throws Exception {
        d.put("name", name).put("mac", mac).put("key", key);
    }

    void delete(String id) {
        JSONArray list = all();
        JSONArray out = new JSONArray();
        for (int i = 0; i < list.length(); i++) {
            JSONObject d = list.optJSONObject(i);
            if (d != null && !id.equals(d.optString("id"))) out.put(d);
        }
        prefs.edit().putString("devices", out.toString()).apply();
        if (id.equals(selectedId())) {
            JSONObject f = out.length() > 0 ? out.optJSONObject(0) : null;
            if (f != null) select(f.optString("id")); else prefs.edit().remove("selected").apply();
        }
    }

    /** One-time move from the old single-device keys ("mac"/"key") to the device list. */
    void migrateLegacy() {
        if (prefs.contains("devices")) return;
        String mac = prefs.getString("mac", "");
        String key = prefs.getString("key", "");
        if (!mac.isEmpty() && !key.isEmpty()) {
            String id = save(null, "Meu aparelho", mac, key);
            select(id);
        }
    }

    static String name(JSONObject d) { return d == null ? "" : d.optString("name", "Aparelho"); }
    static String mac(JSONObject d) { return d == null ? "" : d.optString("mac", ""); }
    static String key(JSONObject d) { return d == null ? "" : d.optString("key", ""); }
    static String id(JSONObject d) { return d == null ? "" : d.optString("id", ""); }
}
