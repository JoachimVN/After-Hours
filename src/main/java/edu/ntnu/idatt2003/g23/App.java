package edu.ntnu.idatt2003.g23;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import edu.ntnu.idatt2003.g23.audio.HomePageMusicController;
import edu.ntnu.idatt2003.g23.audio.SfxController;
import edu.ntnu.idatt2003.g23.io.CsvParseResult;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.ui.BackgroundCanvas;
import edu.ntnu.idatt2003.g23.ui.overlay.SplashOverlayController;
import edu.ntnu.idatt2003.g23.ui.views.csveditor.CsvEditorView;
import edu.ntnu.idatt2003.g23.ui.views.nostocks.NoStocksView;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.text.Font;
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
    private SfxController sfxController;

    @Override
    public void start(Stage stage) {
        homePageMusicController = new HomePageMusicController(getClass());
        sfxController = new SfxController(getClass());

        homePage = LandingPageView.build(
                this::goToSetup,
            () -> { sfxController.play(SfxController.SETTINGS); Parent s = buildSettingsView(this::goHomeKeepMusic); navigateKeepMusic(s); fadeInPage(s); },
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
            getClass().getResource("/css/dialogs.css").toExternalForm(),
            getClass().getResource("/css/csv-editor.css").toExternalForm(),
            getClass().getResource("/css/no-stocks.css").toExternalForm(),
            getClass().getResource("/css/scrollbar.css").toExternalForm()
        );

        configureStage(stage, scene);
        stage.show();

        homePageMusicController.play(
                splashOverlayController::fadeAfterStartup,
                splashOverlayController::fadeAfterFailure);
    }

    // ── Navigation targets ────────────────────────────────────────────────────

    private Runnable withBack(Runnable r) {
        return () -> { sfxController.play(SfxController.BACK); r.run(); };
    }

    private void goToSetup() {
        currentSetupPage = SetupView.build(
                withBack(this::goHomeKeepMusic),
                (name, cash) -> startGame(name, cash),
                (name, cash) -> goToImportCsv(name, cash)
        );
        navigateKeepMusic(currentSetupPage);
        fadeInPage(currentSetupPage);
    }

    private void goToImportCsv(String name, double cash) {
        goToImportCsv(name, cash, null);
    }

    private void goToImportCsv(String name, double cash, File selectedFile) {
        Parent importPage = ImportCsvView.build(
                withBack(() -> navigateKeepMusic(currentSetupPage)),
                () -> openCsvEditorFromImport(new CsvParseResult(List.of()), name, cash, selectedFile),
                file -> openCsvEditorFromImport(file, name, cash),
                file -> startGameWithCsv(name, cash, file),
                selectedFile
        );
        navigateKeepMusic(importPage);
        fadeInPage(importPage);
    }

    private void startGame(String name, double cash) {
        new Thread(() -> {
            CsvParseResult result = StockCsvLoader.loadFromResourceWithErrors("data/stocks/sp500_stocks.csv");
            Platform.runLater(() -> {
                if (result.hasErrors()) {
                    openCsvEditor(result, name, cash);
                } else {
                    List<Stock> stocks = result.getRows().stream()
                            .map(StockCsvLoader::rowToStock)
                            .toList();
                    buildAndStartGame(name, cash, stocks, false);
                }
            });
        }, "stock-loader").start();
    }


    private void startGameWithCsv(String name, double cash, File csvFile) {
        new Thread(() -> {
            CsvParseResult result;
            try {
                result = StockCsvLoader.parseWithErrors(
                        new FileReader(csvFile, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("CSV Error");
                    alert.setHeaderText("Could not read file");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
                return;
            }
            Platform.runLater(() -> {
                if (result.hasErrors()) {
                    openCsvEditorFromImport(result, name, cash, csvFile);
                } else {
                    List<Stock> stocks = result.getRows().stream()
                            .map(StockCsvLoader::rowToStock)
                            .toList();
                    buildAndStartGame(name, cash, stocks, false);
                }
            });
        }, "stock-loader").start();
    }

    private void openCsvEditor(CsvParseResult result, String name, double cash) {
        Parent editorPage = CsvEditorView.build(
                result,
                withBack(this::goHomeKeepMusic),
                stocks -> buildAndStartGame(name, cash, stocks, true)
        );
        navigateKeepMusic(editorPage);
        fadeInPage(editorPage);
    }

    private void openCsvEditorFromImport(File csvFile, String name, double cash) {
        CsvParseResult result;
        try {
            result = StockCsvLoader.parseWithErrors(
                    new FileReader(csvFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("CSV Error");
            alert.setHeaderText("Could not read file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return;
        }

        openCsvEditorFromImport(result, name, cash, csvFile);
    }

    private void openCsvEditorFromImport(CsvParseResult result, String name, double cash, File selectedFile) {
        Parent editorPage = CsvEditorView.build(
                result,
                withBack(() -> goToImportCsv(name, cash, selectedFile)),
                stocks -> buildAndStartGame(name, cash, stocks, true)
        );
        navigateKeepMusic(editorPage);
        fadeInPage(editorPage);
    }

    private void buildAndStartGame(String name, double cash, List<Stock> stocks, boolean fromEditor) {
        if (stocks.isEmpty()) {
            showNoStocksPage(fromEditor);
            return;
        }
        Player player = new Player(
                name == null || name.isBlank() ? "Player" : name,
                BigDecimal.valueOf(cash));
        Exchange exchange = new Exchange("S&P 500", stocks);
        currentGamePage = GameView.build(
                withBack(this::goHome),
                () -> { sfxController.play(SfxController.SETTINGS); navigateKeepMusic(buildSettingsView(() -> navigateKeepMusic(currentGamePage))); },
                player,
                exchange,
                sfxController::getVolume
        );
        navigateToGame(currentGamePage);
        Platform.runLater(() -> homePageMusicController.playGameStartThenAmbience(sfxController.getVolume()));
    }

    private void showNoStocksPage(boolean fromEditor) {
        Parent page = NoStocksView.build(NoStocksView.DEFAULT_MONOLOGUE, fromEditor, withBack(this::goHome));
        navigateToGame(page);
        Platform.runLater(() -> homePageMusicController.fadeOutThenPlayAmbienceStartingWith(
                "/audio/music/ambience/After_Hours_Ambience3_demo.mp3"));
    }

    private Parent buildSettingsView(Runnable onBack) {
        Runnable onBackWithSfx = () -> { sfxController.play(SfxController.BACK); onBack.run(); };
        return SettingsView.build(
                onBackWithSfx,
                homePageMusicController::setVolume, homePageMusicController.getVolume(),
                sfxController::setVolume, sfxController.getVolume(),
                enabled -> {
                    animationsEnabled = enabled;
                    backgroundCanvas.setAnimationsEnabled(enabled);
                },
                animationsEnabled
        );
    }

    // ── Music-aware navigation primitives ────────────────────────────────────

    /** Swap page — music was already started at the top of the start-game call. */
    private void navigateToGame(Parent page) {
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

