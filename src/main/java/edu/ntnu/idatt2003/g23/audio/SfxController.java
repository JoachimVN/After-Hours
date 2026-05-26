package edu.ntnu.idatt2003.g23.audio;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages sound-effect playback and the global SFX volume.
 *
 * <p>Uses cached {@link AudioClip} instances for low-latency click/hover SFX.
 */
public class SfxController {

  public static final String SETTINGS = "/audio/sfx/Settings.wav";
  public static final String SETTINGS_ON = "/audio/sfx/Settings_On.wav";
  public static final String SETTINGS_OFF = "/audio/sfx/Settings_Off.wav";
  public static final String BACK = "/audio/sfx/Back.wav";
  public static final String PROFILE = "/audio/sfx/Profile.wav";
  public static final String SELECT = "/audio/sfx/Select.wav";
  public static final String PLAY1 = "/audio/sfx/play/Play1.wav";
  public static final String PLAY2 = "/audio/sfx/play/Play2.wav";
  public static final String PLAY3 = "/audio/sfx/play/Play3.wav";
  public static final String LEVEL_UP = "/audio/sfx/Level_Up.wav";

  private static final double DEFAULT_VOLUME = 0.5;

  private final Class<?> resourceOwner;
  private final Map<String, AudioClip> clipCache = new ConcurrentHashMap<>();
  private double volume = DEFAULT_VOLUME;

  public SfxController(Class<?> resourceOwner) {
    this.resourceOwner = resourceOwner;
    try {
      preload(SELECT, SETTINGS, SETTINGS_ON, SETTINGS_OFF, BACK, PROFILE, PLAY1, PLAY2, PLAY3);
    } catch (Throwable ignored) {
    }
  }

  /**
   * Plays the sound effect at {@code resourcePath} at the current SFX volume.
   *
   * @param resourcePath classpath-relative path, e.g. {@code "/audio/sfx/Back.wav"}
   */
  public void play(String resourcePath) {
    play(resourcePath, volume);
  }

  /**
   * Plays the sound effect at {@code resourcePath} at an explicit {@code volume},
   * ignoring the stored SFX volume. Useful for music-controller-driven stings.
   *
   * @param resourcePath classpath-relative path
   * @param volume       playback volume in [0.0, 1.0]
   */
  public void play(String resourcePath, double volume) {
    try {
      AudioClip clip = getOrCreateClip(resourcePath);
      if (clip == null) {
        return;
      }
      double clamped = Math.max(0.0, Math.min(1.0, volume));
      clip.play(clamped);
    } catch (Throwable ignored) {
    }
  }

  private void preload(String... resourcePaths) {
    for (String resourcePath : resourcePaths) {
      getOrCreateClip(resourcePath);
    }
  }

  private AudioClip getOrCreateClip(String resourcePath) {
    return clipCache.computeIfAbsent(resourcePath, path -> {
      URL resource = resourceOwner.getResource(path);
      if (resource == null) {
        return null;
      }
      try {
        return new AudioClip(resource.toExternalForm());
      } catch (Throwable ignored) {
        return null;
      }
    });
  }

  public void setVolume(double volume) {
    this.volume = volume;
  }

  public double getVolume() {
    return volume;
  }
}
