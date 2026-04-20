package edu.ntnu.idatt2003.g23.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages sound-effect playback and the global SFX volume.
 *
 * <p>Each call to {@link #play(String)} fires a fire-and-forget {@link MediaPlayer}
 * that disposes itself when finished, leaving music playback unaffected.
 */
public class SfxController {

    public static final String SETTINGS = "/audio/sfx/Settings.mp3";
    public static final String BACK     = "/audio/sfx/Back.mp3";

    private static final double DEFAULT_VOLUME = 0.5;

    private final Class<?> resourceOwner;
    private double volume = DEFAULT_VOLUME;
    // Keeps a strong reference to each active player so the GC cannot collect
    // it before playback finishes.
    private final Set<MediaPlayer> activePlayers = new HashSet<>();

    public SfxController(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    /**
     * Plays the sound effect at {@code resourcePath} at the current SFX volume.
     *
     * @param resourcePath classpath-relative path, e.g. {@code "/audio/sfx/Back.mp3"}
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
            String path = resourceOwner.getResource(resourcePath).toExternalForm();
            MediaPlayer sfx = new MediaPlayer(new Media(path));
            sfx.setVolume(volume);
            activePlayers.add(sfx);
            sfx.setOnEndOfMedia(() -> {
                sfx.stop();
                sfx.dispose();
                activePlayers.remove(sfx);
            });
            sfx.play();
        } catch (Exception ignored) {
        }
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getVolume() {
        return volume;
    }
}
