package com.cloudplayer.funplays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {

    @Test
    @Config(sdk = 35)
    public void opensOnAndroid15() {
        assertNotNull(Robolectric.buildActivity(MainActivity.class).setup().get());
    }

    @Test
    @Config(sdk = 29)
    public void opensOnAndroid10() {
        assertNotNull(Robolectric.buildActivity(MainActivity.class).setup().get());
    }

    @Test
    @Config(sdk = 23)
    public void opensOnAndroid6() {
        assertNotNull(Robolectric.buildActivity(MainActivity.class).setup().get());
    }

    @Test
    public void normalizesMac() {
        assertEquals("bd:04:b2:3a:cc:7d", FunplaysApi.normalizeMac(" BD04B23ACC7D "));
        assertEquals("bd:04:b2:3a:cc:7d", FunplaysApi.normalizeMac("BD-04-B2-3A-CC-7D"));
    }
}
