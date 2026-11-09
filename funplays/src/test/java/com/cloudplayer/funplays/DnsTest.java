package com.cloudplayer.funplays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** DNS prefix replacement keeps the user/password query intact. */
public class DnsTest {

    @Test
    public void replacesHostKeepingQuery() {
        String url = "http://old.server.com:8080/get.php?username=u1&password=p1&type=m3u_plus";
        assertEquals("http://new.server.com:9090/get.php?username=u1&password=p1&type=m3u_plus",
                Dns.replace(url, "http://old.server.com:8080", "http://new.server.com:9090"));
    }

    @Test
    public void matchesByBareHost() {
        String url = "http://old.server.com:8080/get.php?username=u&password=p";
        // old DNS pasted without scheme, new DNS without scheme -> keeps original scheme
        assertEquals("http://new.server.com:8080/get.php?username=u&password=p",
                Dns.replace(url, "old.server.com:8080", "new.server.com:8080"));
    }

    @Test
    public void ignoresTrailingSlashOnNewDns() {
        String url = "http://a.com:80/x.m3u";
        assertEquals("http://b.com:80/x.m3u", Dns.replace(url, "http://a.com:80", "http://b.com:80/"));
    }

    @Test
    public void nullWhenHostDoesNotMatch() {
        String url = "http://other.com:8080/get.php?username=u&password=p";
        assertNull(Dns.replace(url, "http://old.server.com:8080", "http://new.server.com:8080"));
    }

    @Test
    public void nullWhenPortDiffers() {
        String url = "http://old.server.com:8080/get.php";
        assertNull(Dns.replace(url, "http://old.server.com:9999", "http://new.server.com:8080"));
    }

    @Test
    public void nullWhenResultUnchanged() {
        String url = "http://same.com:80/x";
        assertNull(Dns.replace(url, "http://same.com:80", "http://same.com:80"));
    }
}
