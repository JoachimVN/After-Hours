package edu.ntnu.idatt2003.g23.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class HomePageMusicController {

    private static final String HOME_PAGE_MUSIC = "/audio/music/After_Hours_Theme_demo.mp3";
    private static final List<String> AMBIENCE_TRACKS = List.of(
            "/audio/music/ambience/After_Hours_Ambience2_demo.mp3",
            "/audio/music/ambience/After_Hours_Ambience1_demo.mp3"
    );
    private static final Duration FADE_DURATION = Duration.seconds(1.0);

    private final Class<?> resourceOwner;
    private MediaPlayer mediaPlayer;
    private double volume = 0.50;
    private Timeline fadeTimeline;

    private List<String> ambienceQueue = new ArrayList<>();

    public HomePageMusicController(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public void play(Runnable onPlaying, Runnable onFailure) {
        stop();
        try {
            String musicPath = resourceOwner.getResource(HOME_PAGE_MUSIC).toExternalForm();
            mediaPlayer = new MediaPlayer(new Media(musicPath));
            mediaPlayer.setVolume(volume);
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
            String path = resourceOwner.getResource(track).toExternalForm();
            mediaPlayer = new MediaPlayer(new Media(path));
            mediaPlayer.setVolume(volume);
            mediaPlayer.setOnEndOfMedia(this::playNextAmbience);
            mediaPlayer.play();
        } catch (Exception ignored) {
            playNextAmbience();
        }
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
            mediaPlayer.setVolume(volume);
        }
    }

    public double getVolume() {
        return volume;
    }
}

