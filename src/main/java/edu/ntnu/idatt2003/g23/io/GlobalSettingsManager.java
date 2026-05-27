package edu.ntnu.idatt2003.g23.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Loads and saves global (cross-game) settings from the application's per-user
 * data directory.
 * <p>
 * Persisted fields:
 * - musicVolume
 * - sfxVolume
 * - animations
 * - musicMuted
 * - sfxMuted
 * - autosave
 * - autosaveToast
 * - showTutorial
 * - fullscreen
 * - devMode
 */
public final class GlobalSettingsManager {

      private static final Path SETTINGS_FILE =
        AppDataPaths.appDataDir().resolve("settings.json");

  private static final String KEY_MUSIC_VOLUME = "musicVolume";
  private static final String KEY_SFX_VOLUME = "sfxVolume";
  private static final String KEY_ANIMATIONS = "animations";
  private static final String KEY_MUSIC_MUTED = "musicMuted";
  private static final String KEY_SFX_MUTED = "sfxMuted";
  private static final String KEY_AUTOSAVE = "autosave";
  private static final String KEY_AUTOSAVE_TOAST = "autosaveToast";
  private static final String KEY_SHOW_TUTORIAL = "showTutorial";
  private static final String KEY_FULLSCREEN = "fullscreen";
  private static final String KEY_DEV_MODE = "devMode";
  private static final String KEY_PERFORMANCE_MODE = "performanceMode";
  private static final String KEY_MAX_HISTORY_WEEKS = "maxHistoryWeeks";
  private static final String KEY_WINDOW_WIDTH = "windowWidth";
  private static final String KEY_WINDOW_HEIGHT = "windowHeight";

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  // Default values
  public static final double DEFAULT_MUSIC_VOLUME = 0.5;
  public static final double DEFAULT_SFX_VOLUME = 0.5;
  public static final boolean DEFAULT_ANIMATIONS = true;
  public static final boolean DEFAULT_MUSIC_MUTED = false;
  public static final boolean DEFAULT_SFX_MUTED = false;
  public static final boolean DEFAULT_DEV_MODE = false;
  public static final boolean DEFAULT_AUTOSAVE = false;
  public static final boolean DEFAULT_AUTOSAVE_TOAST = true;
  public static final boolean DEFAULT_SHOW_TUTORIAL = true;
  public static final boolean DEFAULT_FULLSCREEN = false;
  public static final boolean DEFAULT_PERFORMANCE_MODE = false;
  public static final int DEFAULT_MAX_HISTORY_WEEKS = 500;
  /**
   * 0 = no saved size (start maximized).
   */
  public static final int DEFAULT_WINDOW_WIDTH = 0;
  public static final int DEFAULT_WINDOW_HEIGHT = 0;

  /**
   * All persisted fields.
   */
  public record Settings(
      double musicVolume,
      double sfxVolume,
      boolean animations,
      boolean musicMuted,
      boolean sfxMuted,
      boolean autosave,
      boolean autosaveToast,
      boolean showTutorial,
      boolean fullscreen,
      boolean devMode,
        boolean performanceMode,
        int maxHistoryWeeks,
      int windowWidth,
      int windowHeight
  ) {
  }

  private GlobalSettingsManager() {
  }

  /**
   * Loads settings from disk. Returns defaults if the file does not exist or
   * cannot be parsed.
   */
  public static Settings load() {
    if (!Files.exists(SETTINGS_FILE)) {
      return defaults();
    }
    try {
      String raw = Files.readString(SETTINGS_FILE, StandardCharsets.UTF_8);
      JsonObject obj = GSON.fromJson(raw, JsonObject.class);

      double music =
          obj.has(KEY_MUSIC_VOLUME) ? obj.get(KEY_MUSIC_VOLUME).getAsDouble() : DEFAULT_MUSIC_VOLUME;
      double sfx = obj.has(KEY_SFX_VOLUME) ? obj.get(KEY_SFX_VOLUME).getAsDouble() : DEFAULT_SFX_VOLUME;
      boolean anim = !obj.has(KEY_ANIMATIONS) || obj.get(KEY_ANIMATIONS).getAsBoolean();
      boolean musicMuted = obj.has(KEY_MUSIC_MUTED) && obj.get(KEY_MUSIC_MUTED).getAsBoolean();
      boolean sfxMuted = obj.has(KEY_SFX_MUTED) && obj.get(KEY_SFX_MUTED).getAsBoolean();
      boolean autosave = obj.has(KEY_AUTOSAVE) && obj.get(KEY_AUTOSAVE).getAsBoolean();
      boolean autosaveToast = !obj.has(KEY_AUTOSAVE_TOAST) || obj.get(KEY_AUTOSAVE_TOAST).getAsBoolean();
      boolean showTutorial = !obj.has(KEY_SHOW_TUTORIAL) || obj.get(KEY_SHOW_TUTORIAL).getAsBoolean();
      boolean fullscreen = obj.has(KEY_FULLSCREEN) && obj.get(KEY_FULLSCREEN).getAsBoolean();
      boolean devMode = obj.has(KEY_DEV_MODE) && obj.get(KEY_DEV_MODE).getAsBoolean();
        boolean performanceMode =
          obj.has(KEY_PERFORMANCE_MODE) && obj.get(KEY_PERFORMANCE_MODE).getAsBoolean();
        int maxHistoryWeeks = obj.has(KEY_MAX_HISTORY_WEEKS)
          ? Math.max(50, obj.get(KEY_MAX_HISTORY_WEEKS).getAsInt())
          : DEFAULT_MAX_HISTORY_WEEKS;

      return new Settings(
          clamp(music),
          clamp(sfx),
          anim,
          musicMuted,
          sfxMuted,
          autosave,
          autosaveToast,
          showTutorial,
          fullscreen,
          devMode,
            performanceMode,
            maxHistoryWeeks,
          windowWidth(obj),
          windowHeight(obj)
      );

    } catch (Exception _) {
      return defaults();
    }
  }

  /**
   * Persists settings to disk.
   */
  public static void save(Settings s) {
    try {
      Files.createDirectories(SETTINGS_FILE.getParent());
      JsonObject obj = new JsonObject();

      obj.addProperty(KEY_MUSIC_VOLUME, s.musicVolume());
      obj.addProperty(KEY_SFX_VOLUME, s.sfxVolume());
      obj.addProperty(KEY_ANIMATIONS, s.animations());
      obj.addProperty(KEY_MUSIC_MUTED, s.musicMuted());
      obj.addProperty(KEY_SFX_MUTED, s.sfxMuted());
      obj.addProperty(KEY_AUTOSAVE, s.autosave());
      obj.addProperty(KEY_AUTOSAVE_TOAST, s.autosaveToast());
      obj.addProperty(KEY_SHOW_TUTORIAL, s.showTutorial());
      obj.addProperty(KEY_FULLSCREEN, s.fullscreen());
      obj.addProperty(KEY_DEV_MODE, s.devMode());
      obj.addProperty(KEY_PERFORMANCE_MODE, s.performanceMode());
      obj.addProperty(KEY_MAX_HISTORY_WEEKS, Math.max(50, s.maxHistoryWeeks()));
      obj.addProperty(KEY_WINDOW_WIDTH, s.windowWidth());
      obj.addProperty(KEY_WINDOW_HEIGHT, s.windowHeight());

      Files.writeString(SETTINGS_FILE, GSON.toJson(obj), StandardCharsets.UTF_8);
    } catch (IOException _) {
    }
  }

  /**
   * Defaults for all fields.
   */
  private static Settings defaults() {
    return new Settings(
        DEFAULT_MUSIC_VOLUME,
        DEFAULT_SFX_VOLUME,
        DEFAULT_ANIMATIONS,
        DEFAULT_MUSIC_MUTED,
        DEFAULT_SFX_MUTED,
        DEFAULT_AUTOSAVE,
        DEFAULT_AUTOSAVE_TOAST,
        DEFAULT_SHOW_TUTORIAL,
        DEFAULT_FULLSCREEN,
        DEFAULT_DEV_MODE,
        DEFAULT_PERFORMANCE_MODE,
        DEFAULT_MAX_HISTORY_WEEKS,
        DEFAULT_WINDOW_WIDTH,
        DEFAULT_WINDOW_HEIGHT
    );
  }

  private static double clamp(double v) {
    return Math.clamp(v, 0.0, 1.0);
  }

  private static int windowWidth(JsonObject obj) {
    if (!obj.has(KEY_WINDOW_WIDTH)) {
      return DEFAULT_WINDOW_WIDTH;
    }
    int v = obj.get(KEY_WINDOW_WIDTH).getAsInt();
    return v >= 860 ? v : DEFAULT_WINDOW_WIDTH;
  }

  private static int windowHeight(JsonObject obj) {
    if (!obj.has(KEY_WINDOW_HEIGHT)) {
      return DEFAULT_WINDOW_HEIGHT;
    }
    int v = obj.get(KEY_WINDOW_HEIGHT).getAsInt();
    return v >= 620 ? v : DEFAULT_WINDOW_HEIGHT;
  }
}
