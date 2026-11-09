package com.cloudplayer.funplays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BulkAddTest {

    private Devices devices;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SharedPreferences p = ctx.getSharedPreferences("funplays-bulk", Context.MODE_PRIVATE);
        p.edit().clear().commit();
        devices = new Devices(p);
    }

    @Test
    public void addsManyWithoutKeyAndDedups() {
        List<String[]> macs = Arrays.asList(
                new String[]{"aa:bb:cc:dd:ee:01", "Sala"},
                new String[]{"aa:bb:cc:dd:ee:02", ""},
                new String[]{"aa:bb:cc:dd:ee:01", "Dup"}); // duplicate MAC skipped
        int added = devices.addMany(macs);
        assertEquals(2, added);
        assertEquals(2, devices.count());
        // No key yet.
        assertFalse(Devices.hasKey(devices.first()));
        // Empty name falls back to the MAC tail.
        assertEquals("EE:02", Devices.name(devices.all().optJSONObject(1)));
    }

    @Test
    public void keyCanBeAddedLater() {
        devices.addMany(Arrays.asList(new String[]{"aa:bb:cc:dd:ee:09", "Quarto"}));
        String id = Devices.id(devices.first());
        assertFalse(Devices.hasKey(devices.get(id)));
        devices.save(id, "Quarto", "aa:bb:cc:dd:ee:09", "123456");
        assertTrue(Devices.hasKey(devices.get(id)));
    }
}
