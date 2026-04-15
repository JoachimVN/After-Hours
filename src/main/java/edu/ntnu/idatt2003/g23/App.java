package edu.ntnu.idatt2003.g23;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import edu.ntnu.idatt2003.g23.audio.HomePageMusicController;
import edu.ntnu.idatt2003.g23.ui.overlay.SplashOverlayController;
import edu.ntnu.idatt2003.g23.ui.views.GameView;
import edu.ntnu.idatt2003.g23.ui.views.HomePageView;
import edu.ntnu.idatt2003.g23.ui.views.ImportCsvView;
import edu.ntnu.idatt2003.g23.ui.views.SettingsView;

/**
 * Launches the JavaFX application and manages top-level scene navigation.
 */
public class App extends Application {

    private StackPane root;
    private Parent homePage;
    private HomePageMusicController homePageMusicController;

    @Override
    public void start(Stage stage) {
        homePageMusicController = new HomePageMusicController(getClass());

        homePage = HomePageView.build(
                () -> navigate(GameView.build(this::goHome)),
                () -> navigate(ImportCsvView.build(this::goHome)),
                () -> navigate(SettingsView.build(
                        this::goHome,
                        homePageMusicController::setVolume,
                        homePageMusicController.getVolume()))
        );

        root = new StackPane(homePage);
        root.getStyleClass().add("app-root");

        SplashOverlayController splashOverlayController = new SplashOverlayController(root);

        Scene scene = new Scene(root, AppConfig.DEFAULT_WIDTH, AppConfig.DEFAULT_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/home.css").toExternalForm());

        configureStage(stage, scene);
        stage.show();

        homePageMusicController.play(
                splashOverlayController::fadeAfterStartup,
                splashOverlayController::fadeAfterFailure);
    }

    private void navigate(Parent page) {
        homePageMusicController.fadeOutThenPlayAmbience();
        root.getChildren().setAll(page);
    }

    private void goHome() {
        homePageMusicController.fadeOutThenPlay(null, null);
        root.getChildren().setAll(homePage);
    }

    private void configureStage(Stage stage, Scene scene) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setTitle(AppConfig.APP_TITLE);
        stage.setScene(scene);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMinWidth(AppConfig.MIN_WIDTH);
        stage.setMinHeight(AppConfig.MIN_HEIGHT);
        stage.setMaximized(true);
    }

    @Override
    public void stop() {
        if (homePageMusicController != null) {
            homePageMusicController.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

