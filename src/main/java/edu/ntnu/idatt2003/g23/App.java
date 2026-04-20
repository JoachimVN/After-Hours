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
import edu.ntnu.idatt2003.g23.ui.views.game.GameView;
import edu.ntnu.idatt2003.g23.ui.views.importcsv.ImportCsvView;
import edu.ntnu.idatt2003.g23.ui.views.landingpage.LandingPageView;
import edu.ntnu.idatt2003.g23.ui.views.settings.SettingsView;
import edu.ntnu.idatt2003.g23.ui.views.setup.SetupView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private boolean animationsEnabled = true;
    private double sfxVolume = 0.5;

    @Override
    public void start(Stage stage) {
        homePageMusicController = new HomePageMusicController(getClass());

        homePage = LandingPageView.build(
                this::goToSetup,
            () -> { Parent s = buildSettingsView(this::goHomeKeepMusic); navigateKeepMusic(s); fadeInPage(s); },
            Platform::exit
        );

        backgroundCanvas = new BackgroundCanvas();
        root = new StackPane(backgroundCanvas, homePage);
        backgroundCanvas.widthProperty().bind(root.widthProperty());
        backgroundCanvas.heightProperty().bind(root.heightProperty());
        root.getStyleClass().add("app-root");

        Font.loadFont(getClass().getResourceAsStream("/fonts/HARLOWSI.TTF"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/SANSSERIFCOLLECTION.TTF"), 14);

        SplashOverlayController splashOverlayController = new SplashOverlayController(root);

        Scene scene = new Scene(root, AppConfig.DEFAULT_WIDTH, AppConfig.DEFAULT_HEIGHT);
        scene.getStylesheets().addAll(
                getClass().getResource("/css/base.css").toExternalForm(),
                getClass().getResource("/css/settings.css").toExternalForm(),
                getClass().getResource("/css/setup.css").toExternalForm(),
                getClass().getResource("/css/import-csv.css").toExternalForm(),
                getClass().getResource("/css/game.css").toExternalForm(),
                getClass().getResource("/css/dialogs.css").toExternalForm()
        );

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
        fadeInPage(currentSetupPage);
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
        GameView gameView = new GameView(player, exchange, this::goHome, () -> navigateKeepMusic(buildSettingsView(() -> navigateKeepMusic(currentGamePage))));
        currentGamePage = gameView.getRoot();

        navigateToGame(currentGamePage);
    }

    private Parent buildSettingsView(Runnable onBack) {
        return SettingsView.build(
                onBack,
                homePageMusicController::setVolume, homePageMusicController.getVolume(),
                v -> sfxVolume = v, sfxVolume,
                enabled -> {
                    animationsEnabled = enabled;
                    backgroundCanvas.setAnimationsEnabled(enabled);
                },
                animationsEnabled
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

    /** Fade a page in from opacity 0 — use only when coming from the home page. */
    private void fadeInPage(Parent page) {
        page.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), page);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.play();
    }

    /** Fade the current page out, then run the navigation action. */
    private void fadeOutThenNavigate(Runnable navigate) {
        if (root.getChildren().size() < 2) {
            navigate.run();
            return;
        }
        Parent current = (Parent) root.getChildren().get(root.getChildren().size() - 1);
        FadeTransition ft = new FadeTransition(Duration.millis(500), current);
        ft.setFromValue(current.getOpacity());
        ft.setToValue(0);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.setOnFinished(e -> navigate.run());
        ft.play();
    }

    /** Return home from game: fade ambience out, restart home music. */
    private void goHome() {
        homePageMusicController.fadeOutThenPlay(null, null);
        fadeOutThenNavigate(() -> root.getChildren().setAll(backgroundCanvas, homePage));
    }

    /** Return home from non-game pages: no music change. */
    private void goHomeKeepMusic() {
        fadeOutThenNavigate(() -> root.getChildren().setAll(backgroundCanvas, homePage));
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

