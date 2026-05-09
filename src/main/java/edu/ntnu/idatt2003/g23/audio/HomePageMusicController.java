package edu.ntnu.idatt2003.g23.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class HomePageMusicController {

  private static final String HOME_PAGE_MUSIC = "/audio/music/Theme.mp3";
  private static final List<String> GAME_START_TRACKS = List.of(
      "/audio/sfx/game_start/Game_Start1.mp3",
      "/audio/sfx/game_start/Game_Start2.mp3",
      "/audio/sfx/game_start/Game_Start4.mp3"
      // "/audio/sfx/game_start/Game_Start3.mp3",
      // "/audio/sfx/game_start/Game_Start5.mp3"
  );
  private static final List<String> AMBIENCE_TRACKS = List.of(
      "/audio/music/ambience/Ambience1.mp3",
      "/audio/music/ambience/Ambience2.mp3",
      "/audio/music/ambience/Ambience3.mp3",
      "/audio/music/ambience/Ambience4.mp3",
      "/audio/music/ambience/Ambience5.mp3"
  );
  private static final Duration FADE_DURATION = Duration.seconds(1.0);
  private static final Duration AMBIENCE_FADE_IN_DURATION = Duration.seconds(0.1);
  private static final double DEFAULT_VOLUME = 0.50;
  private static final double MAIN_THEME_VOLUME_MULTIPLIER = 0.8;
  private static final double AMBIENCE1_VOLUME_MULTIPLIER = 0.50;
  private static final double AMBIENCE2_VOLUME_MULTIPLIER = 1.0;
  private static final double AMBIENCE3_VOLUME_MULTIPLIER = 0.75;
  private static final double AMBIENCE4_VOLUME_MULTIPLIER = 0.75;
  private static final double AMBIENCE5_VOLUME_MULTIPLIER = 0.75;

  private final Class<?> resourceOwner;
  private final Map<String, Media> mediaCache = new HashMap<>();
  private MediaPlayer mediaPlayer;
  private double volume = DEFAULT_VOLUME;
  private double currentTrackMultiplier = 1.0;
  private Timeline fadeTimeline;

  private List<String> ambienceQueue = new ArrayList<>();
  private String lastGameStartTrack = null;

  public HomePageMusicController(Class<?> resourceOwner) {
    this.resourceOwner = resourceOwner;
  }

  /**
   * Preloads audio assets that should start instantly on slower machines.
   */
  public void preloadStartupAudio() {
    cacheMedia(HOME_PAGE_MUSIC);
    for (String track : GAME_START_TRACKS) {
      cacheMedia(track);
    }
  }

  private Media cacheMedia(String resourcePath) {
    Media cached = mediaCache.get(resourcePath);
    if (cached != null) {
      return cached;
    }
    try {
      String path = resourceOwner.getResource(resourcePath).toExternalForm();
      Media media = new Media(path);
      mediaCache.put(resourcePath, media);
      return media;
    } catch (Exception e) {
      return null;
    }
  }

  public void play(Runnable onPlaying, Runnable onFailure) {
    stop();
    try {
      Media media = cacheMedia(HOME_PAGE_MUSIC);
      if (media == null) {
        throw new IllegalStateException("Unable to load home music");
      }
      mediaPlayer = new MediaPlayer(media);
      currentTrackMultiplier = MAIN_THEME_VOLUME_MULTIPLIER;
      mediaPlayer.setVolume(volume * currentTrackMultiplier);
      mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
      if (onPlaying != null) {
        mediaPlayer.setOnPlaying(onPlaying);
      }
      mediaPlayer.play();
    } catch (Exception exception) {
      if (onFailure != null) {
        onFailure.run();
      }
    }
  }

  public void fadeOutThenPlay(Runnable onPlaying, Runnable onFailure) {
    fadeOutThen(() -> play(onPlaying, onFailure));
  }

  public void playAmbience() {
    stop();
    playNextAmbience();
  }

  public void fadeOutThenPlayAmbience() {
    fadeOutThen(this::playAmbience);
  }

  /**
   * Fades out current music then plays ambience, forcing {@code firstTrack} to be played first.
   * The remaining tracks continue in shuffled order.
   */
  public void fadeOutThenPlayAmbienceStartingWith(String firstTrack) {
    fadeOutThen(() -> playAmbienceStartingWith(firstTrack));
  }

  public void playAmbienceStartingWith(String firstTrack) {
    stop();
    // Rebuild queue with remaining tracks shuffled, firstTrack goes first
    ambienceQueue = new ArrayList<>(AMBIENCE_TRACKS);
    ambienceQueue.remove(firstTrack);
    Collections.shuffle(ambienceQueue);
    ambienceQueue.add(0, firstTrack);
    playNextAmbience();
  }

  /**
   * Instantly cuts the main theme, plays the game-start sting at {@code sfxVolume},
   * then fades ambience in once the sting ends.
   */
  public void playGameStartThenAmbience(double sfxVolume) {
    playGameStartThenAmbience(sfxVolume, true);
  }

  /**
   * Instantly cuts the main theme and plays the game-start sting at {@code sfxVolume}.
   *
   * @param continueWithAmbience whether ambience should start after the sting
   */
  public void playGameStartThenAmbience(double sfxVolume, boolean continueWithAmbience) {
    stop(); // cut main theme immediately
    try {
      List<String> candidates = new ArrayList<>(GAME_START_TRACKS);
      if (lastGameStartTrack != null && candidates.size() > 1) {
        candidates.remove(lastGameStartTrack);
      }
      String randomTrack = candidates.get((int) (Math.random() * candidates.size()));
      lastGameStartTrack = randomTrack;
      Media media = cacheMedia(randomTrack);
      if (media == null) {
        throw new IllegalStateException("Unable to load game start track");
      }
      final MediaPlayer sfxPlayer = new MediaPlayer(media);
      mediaPlayer = sfxPlayer;
      sfxPlayer.setVolume(sfxVolume);
      sfxPlayer.setOnEndOfMedia(() -> {
        sfxPlayer.stop();
        sfxPlayer.dispose();
        if (mediaPlayer == sfxPlayer) {
          mediaPlayer = null;
        }
        if (continueWithAmbience) {
          fadeInAmbience();
        }
      });
      sfxPlayer.play();
    } catch (Exception ignored) {
      if (continueWithAmbience) {
        fadeInAmbience();
      }
    }
  }

  private void fadeInAmbience() {
    if (ambienceQueue.isEmpty()) {
      ambienceQueue = new ArrayList<>(AMBIENCE_TRACKS);
      Collections.shuffle(ambienceQueue);
    }
    String track = ambienceQueue.remove(0);
    try {
      Media media = cacheMedia(track);
      if (media == null) {
        throw new IllegalStateException("Unable to load ambience track");
      }
      currentTrackMultiplier = multiplierFor(track);
      mediaPlayer = new MediaPlayer(media);
      mediaPlayer.setVolume(0.0);
      mediaPlayer.setOnEndOfMedia(this::playNextAmbience);
      mediaPlayer.play();
      if (fadeTimeline != null) {
        fadeTimeline.stop();
      }
      MediaPlayer ambiencePlayer = mediaPlayer;
      double targetVolume = volume * currentTrackMultiplier;
      fadeTimeline = new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(ambiencePlayer.volumeProperty(), 0.0)),
          new KeyFrame(AMBIENCE_FADE_IN_DURATION,
              new KeyValue(ambiencePlayer.volumeProperty(), targetVolume))
      );
      fadeTimeline.play();
    } catch (Exception ignored) {
      playNextAmbience();
    }
  }

  private void fadeOutThen(Runnable after) {
    if (fadeTimeline != null) {
      fadeTimeline.stop();
    }
    if (mediaPlayer == null) {
      after.run();
      return;
    }
    MediaPlayer playerToFade = mediaPlayer;
    fadeTimeline = new Timeline(
        new KeyFrame(Duration.ZERO,
            new KeyValue(playerToFade.volumeProperty(), playerToFade.getVolume())),
        new KeyFrame(FADE_DURATION,
            new KeyValue(playerToFade.volumeProperty(), 0.0))
    );
    fadeTimeline.setOnFinished(e -> {
      playerToFade.stop();
      playerToFade.dispose();
      if (mediaPlayer == playerToFade) {
        mediaPlayer = null;
      }
      after.run();
    });
    fadeTimeline.play();
  }

  private void playNextAmbience() {
    if (ambienceQueue.isEmpty()) {
      ambienceQueue = new ArrayList<>(AMBIENCE_TRACKS);
      Collections.shuffle(ambienceQueue);
    }
    String track = ambienceQueue.remove(0);
    try {
      Media media = cacheMedia(track);
      if (media == null) {
        throw new IllegalStateException("Unable to load ambience track");
      }
      currentTrackMultiplier = multiplierFor(track);
      mediaPlayer = new MediaPlayer(media);
      mediaPlayer.setVolume(volume * currentTrackMultiplier);
      mediaPlayer.setOnEndOfMedia(this::playNextAmbience);
      mediaPlayer.play();
    } catch (Exception ignored) {
      playNextAmbience();
    }
  }

  private double multiplierFor(String track) {
    if (track.contains("Ambience1")) {
      return AMBIENCE1_VOLUME_MULTIPLIER;
    }
    if (track.contains("Ambience2")) {
      return AMBIENCE2_VOLUME_MULTIPLIER;
    }
    if (track.contains("Ambience3")) {
      return AMBIENCE3_VOLUME_MULTIPLIER;
    }
    if (track.contains("Ambience4")) {
      return AMBIENCE4_VOLUME_MULTIPLIER;
    }
    if (track.contains("Ambience5")) {
      return AMBIENCE5_VOLUME_MULTIPLIER;
    }
    return 1.0;
  }

  public void stop() {
    if (fadeTimeline != null) {
      fadeTimeline.stop();
      fadeTimeline = null;
    }
    if (mediaPlayer == null) {
      return;
    }
    mediaPlayer.stop();
    mediaPlayer.dispose();
    mediaPlayer = null;
  }

  public void setVolume(double volume) {
    this.volume = volume;
    if (mediaPlayer != null) {
      mediaPlayer.setVolume(volume * currentTrackMultiplier);
    }
  }

  public double getVolume() {
    return volume;
  }
}

