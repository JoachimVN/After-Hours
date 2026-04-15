package edu.ntnu.idatt2003.g23;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import edu.ntnu.idatt2003.g23.audio.HomePageMusicController;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.ui.BackgroundCanvas;
import edu.ntnu.idatt2003.g23.ui.overlay.SplashOverlayController;
import edu.ntnu.idatt2003.g23.ui.views.GameView;
import edu.ntnu.idatt2003.g23.ui.views.HomePageView;
import edu.ntnu.idatt2003.g23.ui.views.ImportCsvView;
import edu.ntnu.idatt2003.g23.ui.views.SetupView;
import edu.ntnu.idatt2003.g23.ui.views.SettingsView;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Launches the JavaFX application and manages top-level scene navigation.
 *
 * Music rules:
 *   - Home music plays on: Home, Setup, ImportCsv, Settings
 *   - Ambience plays on:   GameView only
 *   - navigateKeepMusic()  never touches music
 *   - navigateToGame()     fades current → ambience
 *   - goHome()             fades ambience → home music  (from game context)
 *   - goHomeKeepMusic()    no music change              (from non-game contexts)
 */
public class App extends Application {

    private StackPane root;
    private Parent homePage;
    private HomePageMusicController homePageMusicController;
    private BackgroundCanvas backgroundCanvas;

    /** Retained so back-navigation can return without recreating the form. */
    private Parent currentSetupPage;
    /** Retained so Settings can return to the game without recreating it. */
    private Parent currentGamePage;

    @Override
    public void start(Stage stage) {
        homePageMusicController = new HomePageMusicController(getClass());

        homePage = HomePageView.build(
                this::goToSetup,
                () -> navigateKeepMusic(buildSettingsView(this::goHomeKeepMusic))
        );

        backgroundCanvas = new BackgroundCanvas();
        root = new StackPane(backgroundCanvas, homePage);
        backgroundCanvas.widthProperty().bind(root.widthProperty());
        backgroundCanvas.heightProperty().bind(root.heightProperty());
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

    // ── Navigation targets ────────────────────────────────────────────────────

    private void goToSetup() {
        currentSetupPage = SetupView.build(
                this::goHomeKeepMusic,
                (name, cash) -> startGame(name, cash),
                (name, cash) -> goToImportCsv(name, cash)
        );
        navigateKeepMusic(currentSetupPage);
    }

    private void goToImportCsv(String name, double cash) {
        navigateKeepMusic(ImportCsvView.build(
                () -> navigateKeepMusic(currentSetupPage),
                file -> startGameWithCsv(name, cash, file)
        ));
    }

    private void startGame(String name, double cash) {
        List<Stock> stocks = StockCsvLoader.loadFromResource("data/stocks/sp500_stocks.csv");
        buildAndStartGame(name, cash, stocks);
    }

    private void startGameWithCsv(String name, double cash, File csvFile) {
        List<Stock> stocks;
        try {
            stocks = StockCsvLoader.parse(new FileReader(csvFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("CSV Error");
            alert.setHeaderText("Could not load stock data");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return;
        }
        buildAndStartGame(name, cash, stocks);
    }

    private void buildAndStartGame(String name, double cash, List<Stock> stocks) {
        Player player = new Player(
                name == null || name.isBlank() ? "Player" : name,
                BigDecimal.valueOf(cash));
        Exchange exchange = new Exchange("S&P 500", stocks);
        currentGamePage = GameView.build(
                this::goHome,
                () -> navigateKeepMusic(buildSettingsView(() -> navigateKeepMusic(currentGamePage))),
                player,
                exchange
        );
        navigateToGame(currentGamePage);
    }

    private Parent buildSettingsView(Runnable onBack) {
        return SettingsView.build(
                onBack,
                homePageMusicController::setVolume,
                homePageMusicController.getVolume()
        );
    }

    // ── Music-aware navigation primitives ────────────────────────────────────

    /** Swap page and start ambience — entering the game. */
    private void navigateToGame(Parent page) {
        homePageMusicController.fadeOutThenPlayAmbience();
        root.getChildren().setAll(backgroundCanvas, page);
    }

    /** Swap page without touching music (home, setup, CSV, settings contexts). */
    private void navigateKeepMusic(Parent page) {
        root.getChildren().setAll(backgroundCanvas, page);
    }

    /** Return home from game: fade ambience out, restart home music. */
    private void goHome() {
        homePageMusicController.fadeOutThenPlay(null, null);
        root.getChildren().setAll(backgroundCanvas, homePage);
    }

    /** Return home from non-game pages: no music change. */
    private void goHomeKeepMusic() {
        root.getChildren().setAll(backgroundCanvas, homePage);
    }

    // ─────────────────────────────────────────────────────────────────────────

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
        if (homePageMusicController != null) homePageMusicController.stop();
        if (backgroundCanvas != null) backgroundCanvas.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

