package edu.ntnu.idatt2003.g23;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import edu.ntnu.idatt2003.g23.ui.HomePageView;
/**
 * Launches the  JavaFX application and manages top-level scene navigation.
 */
public class App extends Application {

    private Scene scene;
    private StackPane homePageRoot;
    private MediaPlayer homePageMusic;

    /**
     * Starts the primary JavaFX stage and loads the home page.
     *
     * @param stage the primary application stage
     */
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        homePageRoot = root;
        root.getChildren().add(HomePageView.build(() -> {}));

        Rectangle splash = new Rectangle();
        splash.setFill(Color.BLACK);
        splash.widthProperty().bind(root.widthProperty());
        splash.heightProperty().bind(root.heightProperty());
        root.getChildren().add(splash);

        scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/home.css").toExternalForm());
        scene.rootProperty().addListener((observable, oldRoot, newRoot) -> handlePageMusic(newRoot));

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        stage.setTitle("App");
        stage.setScene(scene);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMinWidth(860);
        stage.setMinHeight(620);
        stage.setMaximized(true);
        stage.show();

        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), splash);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> root.getChildren().remove(splash));

        playHomePageMusic(fade);
    }

    private void handlePageMusic(Parent currentRoot) {
        if (currentRoot == homePageRoot) {
            if (homePageMusic == null) {
                playHomePageMusic(null);
            }
            return;
        }
        stopHomePageMusic();
    }

    private void playHomePageMusic(FadeTransition fade) {
        stopHomePageMusic();
        try {
            String musicPath = getClass().getResource("/audio/music/idatt2003_sound_test2.mp3").toExternalForm();
            homePageMusic = new MediaPlayer(new Media(musicPath));
            homePageMusic.setCycleCount(MediaPlayer.INDEFINITE);
            if (fade != null) {
                homePageMusic.setOnPlaying(() -> {
                    fade.setDelay(Duration.seconds(2.0));
                    fade.play();
                });
            }
            homePageMusic.play();
        } catch (Exception e) {
            if (fade != null) {
                fade.setDelay(Duration.seconds(0.3));
                fade.play();
            }
        }
    }

    private void stopHomePageMusic() {
        if (homePageMusic == null) {
            return;
        }
        homePageMusic.stop();
        homePageMusic.dispose();
        homePageMusic = null;
    }

    @Override
    public void stop() {
        stopHomePageMusic();
    }


    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
