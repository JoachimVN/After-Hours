package edu.ntnu.idatt2003.g23.audio;

import javafx.scene.Parent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class HomePageMusicController {

    private static final String HOME_PAGE_MUSIC = "/audio/music/idatt2003_sound_test2.mp3";

    private final Parent homePageRoot;
    private final Class<?> resourceOwner;
    private MediaPlayer mediaPlayer;

    public HomePageMusicController(Parent homePageRoot, Class<?> resourceOwner) {
        this.homePageRoot = homePageRoot;
        this.resourceOwner = resourceOwner;
    }

    public void handleRootChange(Parent currentRoot) {
        if (currentRoot == homePageRoot) {
            if (mediaPlayer == null) {
                play(null, null);
            }
            return;
        }
        stop();
    }

    public void play(Runnable onPlaying, Runnable onFailure) {
        stop();
        try {
            String musicPath = resourceOwner.getResource(HOME_PAGE_MUSIC).toExternalForm();
            mediaPlayer = new MediaPlayer(new Media(musicPath));
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

    public void stop() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
        mediaPlayer.dispose();
        mediaPlayer = null;
    }
}
