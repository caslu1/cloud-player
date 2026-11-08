package com.cloudplayer.funplays;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** App start with a saved device: it logs in by itself and shows the playlists (server responses faked). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SavedDeviceTest {

    private static final String VALIDATE = "{\"error\":false,\"message\":{\"payed\":false,\"free_trial\":2,"
            + "\"mac\":\"aa:bb:cc:dd:ee:ff\",\"auth_type\":\"device_key\",\"id\":1,\"model\":\"android\","
            + "\"os_version\":\"0.0.0\",\"app_version\":\"1.1.0\"},\"status\":200}";
    private static final String LOGIN = "{\"error\":false,\"message\":\"token123\",\"status\":200}";
    private static final String DEVICE = "{\"error\":false,\"message\":{\"id\":1,\"mac\":\"aa:bb:cc:dd:ee:ff\","
            + "\"playlists\":[{\"url\":\"http://example.com/get.php?username=u&password=p&type=m3u_plus\","
            + "\"id\":10,\"deviceId\":1,\"name\":\"Minha Lista\",\"added_by_web\":true,\"pin\":null,"
            + "\"is_protected\":false,\"categories\":null,\"epg_url\":null,\"status\":null},"
            + "{\"url\":\"http://x.y/z.m3u\",\"id\":11,\"name\":\"\",\"is_protected\":true,\"epg_url\":null}]},"
            + "\"status\":200}";

    @After
    public void reset() { FunplaysApi.testTransport = null; }

    @Test
    public void savedDeviceLoadsPlaylists() throws Exception {
        FunplaysApi.testTransport = (method, path, body) -> {
            if (path.startsWith("validate_mac")) return VALIDATE;
            if (path.startsWith("login_by_mac")) return LOGIN;
            if (path.startsWith("device")) return DEVICE;
            return "{\"error\":false,\"message\":{},\"status\":200}";
        };
        Context ctx = ApplicationProvider.getApplicationContext();
        ctx.getSharedPreferences("funplays", Context.MODE_PRIVATE).edit()
                .putString("mac", "aa:bb:cc:dd:ee:ff").putString("key", "123456").commit();

        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        // Let the background login finish, then run what it posted to the screen.
        for (int i = 0; i < 50 && !containsText(a.getWindow().getDecorView(), "Minha Lista"); i++) {
            Thread.sleep(100);
            ShadowLooper.idleMainLooper();
        }
        assertTrue("playlist not shown", containsText(a.getWindow().getDecorView(), "Minha Lista"));
    }

    private static boolean containsText(View v, String s) {
        if (v instanceof TextView && ((TextView) v).getText().toString().contains(s)) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) if (containsText(g.getChildAt(i), s)) return true;
        }
        return false;
    }
}
