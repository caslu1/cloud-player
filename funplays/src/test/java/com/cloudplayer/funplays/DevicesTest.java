package com.cloudplayer.funplays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DevicesTest {

    private Devices devices;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SharedPreferences p = ctx.getSharedPreferences("funplays-test", Context.MODE_PRIVATE);
        p.edit().clear().commit();
        devices = new Devices(p);
    }

    @Test
    public void addsEditsAndDeletes() {
        String id1 = devices.save(null, "Sala", "aa:bb:cc:dd:ee:01", "111");
        String id2 = devices.save(null, "Quarto", "aa:bb:cc:dd:ee:02", "222");
        assertEquals(2, devices.count());

        devices.save(id1, "Sala TV", "aa:bb:cc:dd:ee:01", "999");
        JSONObject d = devices.get(id1);
        assertEquals("Sala TV", Devices.name(d));
        assertEquals("999", Devices.key(d));

        devices.select(id2);
        assertEquals("Quarto", Devices.name(devices.selected()));

        devices.delete(id2);
        assertEquals(1, devices.count());
        // Deleting the selected device falls back to the remaining one.
        assertEquals("Sala TV", Devices.name(devices.selected()));

        devices.delete(id1);
        assertEquals(0, devices.count());
        assertNull(devices.selected());
    }

    @Test
    public void migratesLegacySingleDevice() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SharedPreferences p = ctx.getSharedPreferences("funplays-legacy", Context.MODE_PRIVATE);
        p.edit().clear().putString("mac", "aa:bb:cc:dd:ee:ff").putString("key", "123456").commit();
        Devices d = new Devices(p);
        d.migrateLegacy();
        assertEquals(1, d.count());
        assertEquals("aa:bb:cc:dd:ee:ff", Devices.mac(d.selected()));
    }
}
