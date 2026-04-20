package edu.ntnu.idatt2003.g23;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @AfterEach
    void resetDevMode() {
        AppConfig.DEV_MODE.set(false);
    }

    @Test
    @DisplayName("APP_TITLE is \"After Hours\"")
    void testAppTitle() {
        assertEquals("After Hours", AppConfig.APP_TITLE);
    }

    @Test
    @DisplayName("DEFAULT_WIDTH is 1024")
    void testDefaultWidth() {
        assertEquals(1024.0, AppConfig.DEFAULT_WIDTH);
    }

    @Test
    @DisplayName("DEFAULT_HEIGHT is 768")
    void testDefaultHeight() {
        assertEquals(768.0, AppConfig.DEFAULT_HEIGHT);
    }

    @Test
    @DisplayName("MIN_WIDTH is 860")
    void testMinWidth() {
        assertEquals(860.0, AppConfig.MIN_WIDTH);
    }

    @Test
    @DisplayName("MIN_HEIGHT is 620")
    void testMinHeight() {
        assertEquals(620.0, AppConfig.MIN_HEIGHT);
    }

    @Test
    @DisplayName("SPLASH_FADE_DURATION is 0.6 seconds")
    void testSplashFadeDuration() {
        assertEquals(0.6, AppConfig.SPLASH_FADE_DURATION.toSeconds(), 0.001);
    }

    @Test
    @DisplayName("SPLASH_DELAY is positive")
    void testSplashDelayPositive() {
        assertTrue(AppConfig.SPLASH_DELAY.toSeconds() > 0);
    }

    @Test
    @DisplayName("SPLASH_FALLBACK_DELAY is 0.3 seconds")
    void testSplashFallbackDelay() {
        assertEquals(0.3, AppConfig.SPLASH_FALLBACK_DELAY.toSeconds(), 0.001);
    }

    @Test
    @DisplayName("DEV_MODE defaults to false")
    void testDevModeDefault() {
        assertFalse(AppConfig.DEV_MODE.get());
    }

    @Test
    @DisplayName("DEV_MODE can be set to true")
    void testDevModeSetTrue() {
        AppConfig.DEV_MODE.set(true);
        assertTrue(AppConfig.DEV_MODE.get());
    }

    @Test
    @DisplayName("Private constructor does not throw")
    void testPrivateConstructor() throws Exception {
        var constructor = AppConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertDoesNotThrow(() -> { constructor.newInstance(); });
    }
}
