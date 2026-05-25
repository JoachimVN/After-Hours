package edu.ntnu.idatt2003.g23.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class HomePageMusicController {

  private static final String HOME_PAGE_MUSIC = "/audio/music/Theme.mp3";
  private static final List<String> GAME_START_TRACKS = List.of(
      "/audio/sfx/game_start/Game_Start1.wav",
      "/audio/sfx/game_start/Game_Start2.wav",
      // "/audio/sfx/game_start/Game_Start3.wav",
      "/audio/sfx/game_start/Game_Start4.wav"
      // "/audio/sfx/game_start/Game_Start5.wav"
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
  private static final Duration CONTEXT_EQ_FADE_DURATION = Duration.seconds(0.6);
  private static final double DEFAULT_VOLUME = 0.50;
  private static final double NON_LANDING_EQ_CUTOFF_HZ = 1000.0;
  private static final double EQ_FLAT_GAIN_DB = 0.0;
  private static final double EQ_HIGH_CUT_GAIN_DB = -24.0;
  private static final double MAIN_THEME_VOLUME_MULTIPLIER = 0.8;
  private static final double AMBIENCE1_VOLUME_MULTIPLIER = 0.50;
  private static final double AMBIENCE2_VOLUME_MULTIPLIER = 1.0;
  private static final double AMBIENCE3_VOLUME_MULTIPLIER = 0.75;
  private static final double AMBIENCE4_VOLUME_MULTIPLIER = 0.75;
  private static final double AMBIENCE5_VOLUME_MULTIPLIER = 0.50;

  private final Class<?> resourceOwner;
  private final Map<String, Media> mediaCache = new HashMap<>();
  private MediaPlayer mediaPlayer;
  private double volume = DEFAULT_VOLUME;
  private double currentTrackMultiplier = 1.0;
  private double contextVolumeMultiplier = 1.0;
  private boolean currentTrackIsHomeTheme = false;
  private Timeline fadeTimeline;
  private Timeline eqAdjustTimeline;

  private List<String> ambienceQueue = new ArrayList<>();
  private String lastGameStartTrack = null;
  private boolean eqFilterActive = false;
  private final Random random = new Random();

  // Status-change low-pass gains (32 Hz ... 16 kHz).
  // Stronger cutoff: start attenuation earlier and cut highs harder.
  private static final double[] LOW_PASS_GAINS = { 0, 0, 0, -3, -7, -12, -18, -23, -24, -24 };

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
    } catch (Exception _) {
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
      currentTrackIsHomeTheme = true;
      currentTrackMultiplier = MAIN_THEME_VOLUME_MULTIPLIER;
      mediaPlayer.setVolume(effectiveVolume());
      applyThemeEqProfile(mediaPlayer, false);
      mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
      if (onPlaying != null) {
        mediaPlayer.setOnPlaying(onPlaying);
      }
      mediaPlayer.play();
    } catch (Exception _) {
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
      String randomTrack = candidates.get(random.nextInt(candidates.size()));
      lastGameStartTrack = randomTrack;
      Media media = cacheMedia(randomTrack);
      if (media == null) {
        throw new IllegalStateException("Unable to load game start track");
      }
      final MediaPlayer sfxPlayer = new MediaPlayer(media);
      mediaPlayer = sfxPlayer;
      currentTrackIsHomeTheme = false;
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
    } catch (Exception _) {
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
      currentTrackIsHomeTheme = false;
      if (eqFilterActive) {
        applyEqToCurrentPlayer();
      }
      mediaPlayer.setVolume(0.0);
      mediaPlayer.setOnEndOfMedia(this::playNextAmbience);
      mediaPlayer.play();
      if (fadeTimeline != null) {
        fadeTimeline.stop();
      }
      MediaPlayer ambiencePlayer = mediaPlayer;
      double targetVolume = effectiveVolume();
      fadeTimeline = new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(ambiencePlayer.volumeProperty(), 0.0)),
          new KeyFrame(AMBIENCE_FADE_IN_DURATION,
              new KeyValue(ambiencePlayer.volumeProperty(), targetVolume))
      );
      fadeTimeline.play();
    } catch (Exception _) {
      playNextAmbience();
    }
  }

  private void fadeOutThen(Runnable after) {
    stopEqAdjustTimeline();
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
      currentTrackIsHomeTheme = false;
      mediaPlayer.setVolume(effectiveVolume());
      if (eqFilterActive) {
        applyEqToCurrentPlayer();
      }
      mediaPlayer.setOnEndOfMedia(this::playNextAmbience);
      mediaPlayer.play();
    } catch (Exception _) {
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

  /**
   * Fades the low-pass EQ in over {@code durationMs} ms on the current player
   * and sets the flag so future tracks start filtered instantly.
   */
  public void applyLowPassFilter() {
    eqFilterActive = true;
    fadeEqToCurrentPlayer(LOW_PASS_GAINS, 600);
  }

  /**
   * Fades the EQ back to flat over {@code durationMs} ms, then disables it.
   */
  public void removeFilter() {
    eqFilterActive = false;
    if (mediaPlayer == null) {
      return;
    }
    AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
    if (!eq.isEnabled()) {
      return;
    }
    var bands = eq.getBands();
    int count = Math.min(bands.size(), LOW_PASS_GAINS.length);
    java.util.List<KeyValue> kvs = new java.util.ArrayList<>();
    for (int i = 0; i < count; i++) {
      kvs.add(new KeyValue(bands.get(i).gainProperty(), bands.get(i).getGain()));
    }
    java.util.List<KeyValue> kvsEnd = new java.util.ArrayList<>();
    for (int i = 0; i < count; i++) {
      kvsEnd.add(new KeyValue(bands.get(i).gainProperty(), 0.0));
    }
    Timeline eqOut = new Timeline(
        new KeyFrame(Duration.ZERO, kvs.toArray(new KeyValue[0])),
        new KeyFrame(Duration.millis(400), kvsEnd.toArray(new KeyValue[0]))
    );
    eqOut.setOnFinished(e -> eq.setEnabled(false));
    eqOut.play();
  }

  private void fadeEqToCurrentPlayer(double[] targetGains, long durationMs) {
    if (mediaPlayer == null) {
      return;
    }
    AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
    eq.setEnabled(true);
    var bands = eq.getBands();
    int count = Math.min(bands.size(), targetGains.length);
    java.util.List<KeyValue> kvsStart = new java.util.ArrayList<>();
    java.util.List<KeyValue> kvsEnd = new java.util.ArrayList<>();
    for (int i = 0; i < count; i++) {
      kvsStart.add(new KeyValue(bands.get(i).gainProperty(), bands.get(i).getGain()));
      kvsEnd.add(new KeyValue(bands.get(i).gainProperty(), targetGains[i]));
    }
    new Timeline(
        new KeyFrame(Duration.ZERO, kvsStart.toArray(new KeyValue[0])),
        new KeyFrame(Duration.millis(durationMs), kvsEnd.toArray(new KeyValue[0]))
    ).play();
  }

  // Called when a new track starts while filter is active — snap to target immediately.
  private void applyEqToCurrentPlayer() {
    if (mediaPlayer == null) {
      return;
    }
    AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
    eq.setEnabled(true);
    var bands = eq.getBands();
    for (int i = 0; i < bands.size() && i < LOW_PASS_GAINS.length; i++) {
      bands.get(i).setGain(LOW_PASS_GAINS[i]);
    }
  }

  public void stop() {
    stopEqAdjustTimeline();
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
    currentTrackIsHomeTheme = false;
  }

  public void setVolume(double volume) {
    this.volume = volume;
    if (mediaPlayer != null) {
      mediaPlayer.setVolume(effectiveVolume());
    }
  }

  /**
   * Updates Theme.mp3 context profile. Volume is unchanged; EQ profile may change.
   * Use 1.0 for landing profile and a lower value for non-landing profile.
   */
  public void setContextVolumeMultiplier(double multiplier, boolean smooth) {
    contextVolumeMultiplier = Math.max(0.0, multiplier);
    if (mediaPlayer == null || !currentTrackIsHomeTheme) {
      return;
    }
    if (!smooth) {
      applyThemeEqProfile(mediaPlayer, false);
      return;
    }
    applyThemeEqProfile(mediaPlayer, true);
  }

  private double effectiveVolume() {
    return volume * currentTrackMultiplier;
  }

  private void applyThemeEqProfile(MediaPlayer player, boolean smooth) {
    if (player == null || !currentTrackIsHomeTheme) {
      return;
    }
    AudioEqualizer equalizer = player.getAudioEqualizer();
    if (equalizer == null) {
      return;
    }
    equalizer.setEnabled(true);

    if (!smooth) {
      stopEqAdjustTimeline();
      for (EqualizerBand band : equalizer.getBands()) {
        band.setGain(targetGainForBand(band));
      }
      return;
    }

    stopEqAdjustTimeline();
    List<KeyValue> gainTargets = new ArrayList<>();
    for (EqualizerBand band : equalizer.getBands()) {
      gainTargets.add(new KeyValue(band.gainProperty(), targetGainForBand(band)));
    }
    if (gainTargets.isEmpty()) {
      return;
    }
    eqAdjustTimeline = new Timeline(
        new KeyFrame(CONTEXT_EQ_FADE_DURATION, gainTargets.toArray(new KeyValue[0]))
    );
    eqAdjustTimeline.setOnFinished(e -> {
      if (eqAdjustTimeline != null) {
        eqAdjustTimeline = null;
      }
    });
    eqAdjustTimeline.play();
  }

  private double targetGainForBand(EqualizerBand band) {
    if (contextVolumeMultiplier < 0.999 && band.getCenterFrequency() >= NON_LANDING_EQ_CUTOFF_HZ) {
      return EQ_HIGH_CUT_GAIN_DB;
    }
    return EQ_FLAT_GAIN_DB;
  }

  private void stopEqAdjustTimeline() {
    if (eqAdjustTimeline != null) {
      eqAdjustTimeline.stop();
      eqAdjustTimeline = null;
    }
  }

  public double getVolume() {
    return volume;
  }
}

