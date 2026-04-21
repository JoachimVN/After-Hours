package edu.ntnu.idatt2003.g23.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HomePageMusicController")
class HomePageMusicControllerTest {

    private HomePageMusicController music;

    @BeforeEach
    void setUp() {
        music = new HomePageMusicController(getClass());
    }

    @Test
    @DisplayName("Default volume is 0.5")
    void testDefaultVolume() {
        assertEquals(0.5, music.getVolume(), 1e-9);
    }

    @Test
    @DisplayName("setVolume updates getVolume when no media is playing")
    void testSetVolumeNoPlayer() {
        music.setVolume(0.9);
        assertEquals(0.9, music.getVolume(), 1e-9);
    }

    @Test
    @DisplayName("stop() when nothing is playing does not throw")
    void testStopWhenIdle() {
        assertDoesNotThrow(() -> music.stop());
    }

    @Test
    @DisplayName("stop() called twice does not throw")
    void testStopTwiceDoesNotThrow() {
        assertDoesNotThrow(() -> {
            music.stop();
            music.stop();
        });
    }
}
