package com.cloudplayer.funplays;

import java.util.Locale;

/**
 * Swaps the DNS/server part at the start of an M3U link.
 *
 * An IPTV link looks like {@code http://host:port/get.php?username=...&password=...}.
 * When the server (host[:port]) changes, only that prefix is replaced; the path and
 * query (user, password, type) are kept exactly.
 */
final class Dns {

    private Dns() {}

    /** host[:port] in lower case, from a full URL or a bare host string; "" if none. */
    static String hostPort(String s) {
        if (s == null) return "";
        String t = s.trim();
        int scheme = t.indexOf("://");
        if (scheme >= 0) t = t.substring(scheme + 3);
        int slash = t.indexOf('/');
        if (slash >= 0) t = t.substring(0, slash);
        int q = t.indexOf('?');
        if (q >= 0) t = t.substring(0, q);
        return t.toLowerCase(Locale.ROOT);
    }

    private static String scheme(String url) {
        if (url == null) return "http://";
        int i = url.indexOf("://");
        return i < 0 ? "http://" : url.substring(0, i + 3);
    }

    private static String trimTrailingSlash(String s) {
        String t = s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    /** The path + query of a URL (everything from the first '/' after the host), or "". */
    private static String path(String url) {
        if (url == null) return "";
        int scheme = url.indexOf("://");
        String afterScheme = scheme < 0 ? url : url.substring(scheme + 3);
        int slash = afterScheme.indexOf('/');
        return slash < 0 ? "" : afterScheme.substring(slash);
    }

    /**
     * If {@code url}'s host[:port] matches {@code oldDns}'s, returns the URL with the server
     * replaced by {@code newDns} (keeping path and query). Returns null if it does not match
     * or the result would be unchanged.
     */
    static String replace(String url, String oldDns, String newDns) {
        if (url == null) return null;
        String oldHost = hostPort(oldDns);
        String urlHost = hostPort(url);
        if (oldHost.isEmpty() || urlHost.isEmpty() || !urlHost.equals(oldHost)) return null;

        String newHostFull = trimTrailingSlash(newDns);
        if (newHostFull.isEmpty()) return null;
        if (!newHostFull.contains("://")) newHostFull = scheme(url) + newHostFull;

        String result = newHostFull + path(url);
        return result.equals(url) ? null : result;
    }
}
