package edu.ntnu.idatt2003.g23;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import edu.ntnu.idatt2003.g23.audio.HomePageMusicController;
import edu.ntnu.idatt2003.g23.ui.overlay.SplashOverlayController;
import edu.ntnu.idatt2003.g23.ui.views.HomePageView;
/**
 * Launches the  JavaFX application and manages top-level scene navigation.
 */
public class App extends Application {

    private Scene scene;
    private HomePageMusicController homePageMusicController;

    /**
     * Starts the primary JavaFX stage and loads the home page.
     *
     * @param stage the primary application stage
     */
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(HomePageView.build(() -> {}));
        root.getStyleClass().add("app-root");

        homePageMusicController = new HomePageMusicController(root, getClass());
        SplashOverlayController splashOverlayController = new SplashOverlayController(root);

        scene = new Scene(root, AppConfig.DEFAULT_WIDTH, AppConfig.DEFAULT_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/home.css").toExternalForm());
        scene.rootProperty().addListener((observable, oldRoot, newRoot) -> homePageMusicController.handleRootChange(newRoot));

        configureStage(stage);
        stage.show();

        homePageMusicController.play(
                splashOverlayController::fadeAfterStartup,
                splashOverlayController::fadeAfterFailure);
    }

    private void configureStage(Stage stage) {
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


    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
