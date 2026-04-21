package edu.ntnu.idatt2003.g23.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves global (cross-game) settings to
 * {@code ~/.afterhours/settings.json}.
 *
 * Persisted fields:
 *   - musicVolume
 *   - sfxVolume
 *   - animations
 *   - musicMuted
 *   - sfxMuted
 *   - autosave
 *   - autosaveToast
 *   - fullscreen
 *   - devMode
 */
public final class GlobalSettingsManager {

    private static final Path SETTINGS_FILE =
            Path.of(System.getProperty("user.home"), ".afterhours", "settings.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Default values
    public static final double DEFAULT_MUSIC_VOLUME     = 0.5;
    public static final double DEFAULT_SFX_VOLUME       = 0.5;
    public static final boolean DEFAULT_ANIMATIONS      = true;
    public static final boolean DEFAULT_MUSIC_MUTED     = false;
    public static final boolean DEFAULT_SFX_MUTED       = false;
    public static final boolean DEFAULT_DEV_MODE        = false;
    public static final boolean DEFAULT_AUTOSAVE        = false;
    public static final boolean DEFAULT_AUTOSAVE_TOAST  = true;
    public static final boolean DEFAULT_FULLSCREEN      = false;

    /**
     * All persisted fields.
     */
    public record Settings(
            double  musicVolume,
            double  sfxVolume,
            boolean animations,
            boolean musicMuted,
            boolean sfxMuted,
            boolean autosave,
            boolean autosaveToast,
            boolean fullscreen,
            boolean devMode
    ) {}

    private GlobalSettingsManager() {}

    /**
     * Loads settings from disk. Returns defaults if the file does not exist or
     * cannot be parsed.
     */
    public static Settings load() {
        if (!Files.exists(SETTINGS_FILE)) return defaults();
        try {
            String raw = Files.readString(SETTINGS_FILE, StandardCharsets.UTF_8);
            JsonObject obj = GSON.fromJson(raw, JsonObject.class);

            double  music         = obj.has("musicVolume")    ? obj.get("musicVolume").getAsDouble()    : DEFAULT_MUSIC_VOLUME;
            double  sfx           = obj.has("sfxVolume")      ? obj.get("sfxVolume").getAsDouble()      : DEFAULT_SFX_VOLUME;
            boolean anim          = !obj.has("animations")    || obj.get("animations").getAsBoolean();
            boolean musicMuted    = obj.has("musicMuted")     && obj.get("musicMuted").getAsBoolean();
            boolean sfxMuted      = obj.has("sfxMuted")       && obj.get("sfxMuted").getAsBoolean();
            boolean autosave      = obj.has("autosave")       && obj.get("autosave").getAsBoolean();
            boolean autosaveToast = !obj.has("autosaveToast") || obj.get("autosaveToast").getAsBoolean();
            boolean fullscreen    = obj.has("fullscreen")     && obj.get("fullscreen").getAsBoolean();
            boolean devMode       = obj.has("devMode")        && obj.get("devMode").getAsBoolean();

            return new Settings(
                    clamp(music),
                    clamp(sfx),
                    anim,
                    musicMuted,
                    sfxMuted,
                    autosave,
                    autosaveToast,
                    fullscreen,
                    devMode
            );

        } catch (Exception e) {
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

            obj.addProperty("musicVolume",    s.musicVolume());
            obj.addProperty("sfxVolume",      s.sfxVolume());
            obj.addProperty("animations",     s.animations());
            obj.addProperty("musicMuted",     s.musicMuted());
            obj.addProperty("sfxMuted",       s.sfxMuted());
            obj.addProperty("autosave",       s.autosave());
            obj.addProperty("autosaveToast",  s.autosaveToast());
            obj.addProperty("fullscreen",     s.fullscreen());
            obj.addProperty("devMode",        s.devMode());

            Files.writeString(SETTINGS_FILE, GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
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
                DEFAULT_FULLSCREEN,
                DEFAULT_DEV_MODE
        );
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
