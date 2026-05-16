package edu.ntnu.idatt2003.g23.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlobalSettingsManager}.
 *
 * <p>The class writes to {@code ~/.afterhours/settings.json}. Tests that
 * exercise save/load back up any existing file before the test and restore it
 * afterwards so that developer settings are never clobbered.
 */
class GlobalSettingsManagerTest {

  private static final Path SETTINGS_FILE =
      Path.of(System.getProperty("user.home"), ".afterhours", "settings.json");

  private boolean hadExistingFile;
  private byte[] backup;

  @BeforeEach
  void backupSettings() throws IOException {
    hadExistingFile = Files.exists(SETTINGS_FILE);
    if (hadExistingFile) {
      backup = Files.readAllBytes(SETTINGS_FILE);
      Files.delete(SETTINGS_FILE);
    }
  }

  @AfterEach
  void restoreSettings() throws IOException {
    if (hadExistingFile && backup != null) {
      Files.createDirectories(SETTINGS_FILE.getParent());
      Files.write(SETTINGS_FILE, backup);
    } else if (Files.exists(SETTINGS_FILE)) {
      Files.delete(SETTINGS_FILE);
    }
  }

  // ── defaults ─────────────────────────────────────────────────────────────

  @Test
  void load_withNoFile_returnsDefaults() {
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(GlobalSettingsManager.DEFAULT_MUSIC_VOLUME, s.musicVolume());
    assertEquals(GlobalSettingsManager.DEFAULT_SFX_VOLUME, s.sfxVolume());
    assertTrue(s.animations());
    assertFalse(s.musicMuted());
    assertFalse(s.sfxMuted());
    assertFalse(s.autosave());
    assertTrue(s.autosaveToast());
    assertTrue(s.showTutorial());
    assertFalse(s.fullscreen());
    assertFalse(s.devMode());
    assertEquals(GlobalSettingsManager.DEFAULT_WINDOW_WIDTH, s.windowWidth());
    assertEquals(GlobalSettingsManager.DEFAULT_WINDOW_HEIGHT, s.windowHeight());
  }

  // ── save / load round-trip ────────────────────────────────────────────────

  @Test
  void saveAndLoad_roundtrip_allFields() {
    GlobalSettingsManager.Settings original = new GlobalSettingsManager.Settings(
        0.75, 0.3, false, true, false, true, false, true, true, true, true, 640, 1280, 720
    );
    GlobalSettingsManager.save(original);
    GlobalSettingsManager.Settings loaded = GlobalSettingsManager.load();

    assertEquals(0.75, loaded.musicVolume(), 1e-9);
    assertEquals(0.3, loaded.sfxVolume(), 1e-9);
    assertFalse(loaded.animations());
    assertTrue(loaded.musicMuted());
    assertFalse(loaded.sfxMuted());
    assertTrue(loaded.autosave());
    assertFalse(loaded.autosaveToast());
    assertTrue(loaded.showTutorial());
    assertTrue(loaded.fullscreen());
    assertTrue(loaded.devMode());
    assertTrue(loaded.performanceMode());
    assertEquals(640, loaded.maxHistoryWeeks());
    assertEquals(1280, loaded.windowWidth());
    assertEquals(720, loaded.windowHeight());
  }

  @Test
  void save_createsParentDirectoriesIfMissing() {
    GlobalSettingsManager.save(GlobalSettingsManager.load());
    assertTrue(Files.exists(SETTINGS_FILE.getParent()));
  }

  // ── windowWidth / windowHeight validation ─────────────────────────────────

  @Test
  void load_windowWidthBelowMinimum_returnsZero() throws IOException {
    writeRawJson("{\"windowWidth\":500,\"windowHeight\":640}");
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(0, s.windowWidth(), "Width below 860 should fall back to 0");
  }

  @Test
  void load_windowHeightBelowMinimum_returnsZero() throws IOException {
    writeRawJson("{\"windowWidth\":1024,\"windowHeight\":400}");
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(0, s.windowHeight(), "Height below 620 should fall back to 0");
  }

  @Test
  void load_validWindowDimensions_preservedAsIs() throws IOException {
    writeRawJson("{\"windowWidth\":1920,\"windowHeight\":1080}");
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(1920, s.windowWidth());
    assertEquals(1080, s.windowHeight());
  }

  // ── clamp ─────────────────────────────────────────────────────────────────

  @Test
  void load_musicVolumeAboveOne_clampedToOne() throws IOException {
    writeRawJson("{\"musicVolume\":2.5}");
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(1.0, s.musicVolume(), 1e-9);
  }

  @Test
  void load_sfxVolumeNegative_clampedToZero() throws IOException {
    writeRawJson("{\"sfxVolume\":-0.5}");
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(0.0, s.sfxVolume(), 1e-9);
  }

  // ── corrupt file ─────────────────────────────────────────────────────────

  @Test
  void load_corruptFile_returnsDefaults() throws IOException {
    Files.createDirectories(SETTINGS_FILE.getParent());
    Files.writeString(SETTINGS_FILE, "not valid json!", StandardCharsets.UTF_8);
    GlobalSettingsManager.Settings s = GlobalSettingsManager.load();
    assertEquals(GlobalSettingsManager.DEFAULT_MUSIC_VOLUME, s.musicVolume());
  }

  // ── helper ────────────────────────────────────────────────────────────────

  private void writeRawJson(String json) throws IOException {
    Files.createDirectories(SETTINGS_FILE.getParent());
    Files.writeString(SETTINGS_FILE, json, StandardCharsets.UTF_8);
  }
}
