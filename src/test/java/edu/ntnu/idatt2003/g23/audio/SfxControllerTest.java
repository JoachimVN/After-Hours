package edu.ntnu.idatt2003.g23.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SfxController")
class SfxControllerTest {

  private SfxController sfx;

  @BeforeEach
  void setUp() {
    sfx = new SfxController(getClass());
  }

  @Test
  @DisplayName("Default volume is 0.5")
  void testDefaultVolume() {
    assertEquals(0.5, sfx.getVolume(), 1e-9);
  }

  @Test
  @DisplayName("setVolume updates getVolume")
  void testSetVolume() {
    sfx.setVolume(0.8);
    assertEquals(0.8, sfx.getVolume(), 1e-9);
  }

  @Test
  @DisplayName("SETTINGS constant is correct path")
  void testSettingsConstant() {
    assertEquals("/audio/sfx/Settings.wav", SfxController.SETTINGS);
  }

  @Test
  @DisplayName("BACK constant is correct path")
  void testBackConstant() {
    assertEquals("/audio/sfx/Back.wav", SfxController.BACK);
  }

  @Test
  @DisplayName("play(String) with missing resource does not throw")
  void testPlayMissingResourceNoThrow() {
    // getResource returns null → NPE on .toExternalForm() → caught by catch(Exception)
    assertDoesNotThrow(() -> sfx.play("/nonexistent/sound.wav"));
  }

  @Test
  @DisplayName("play(String, double) with missing resource does not throw")
  void testPlayExplicitVolumeNoThrow() {
    assertDoesNotThrow(() -> sfx.play("/nonexistent/sound.wav", 0.3));
  }
}
