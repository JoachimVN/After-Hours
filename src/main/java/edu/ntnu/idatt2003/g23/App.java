package edu.ntnu.idatt2003.g23;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import edu.ntnu.idatt2003.g23.audio.HomePageMusicController;
import edu.ntnu.idatt2003.g23.audio.SfxController;
import edu.ntnu.idatt2003.g23.io.GameSaveExporter;
import edu.ntnu.idatt2003.g23.io.GameUiState;
import edu.ntnu.idatt2003.g23.io.GlobalSettingsManager;
import edu.ntnu.idatt2003.g23.ui.views.saveselect.SaveSelectController;
import edu.ntnu.idatt2003.g23.ui.views.saveselect.SaveSelectView;
import edu.ntnu.idatt2003.g23.io.CsvParseResult;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.ui.BackgroundCanvas;
import edu.ntnu.idatt2003.g23.ui.overlay.SplashOverlayController;
import edu.ntnu.idatt2003.g23.ui.views.csveditor.CsvEditorView;
import edu.ntnu.idatt2003.g23.ui.views.nostocks.NoStocksView;
import edu.ntnu.idatt2003.g23.ui.views.game.GameController;
import edu.ntnu.idatt2003.g23.ui.views.game.GameView;
import edu.ntnu.idatt2003.g23.ui.views.importcsv.ImportCsvView;
import edu.ntnu.idatt2003.g23.ui.views.landingpage.LandingPageView;
import edu.ntnu.idatt2003.g23.ui.views.settings.SettingsView;
import edu.ntnu.idatt2003.g23.ui.views.setup.SetupView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    /** Save folder of the currently loaded save; null for a new game. */
    private java.nio.file.Path currentSavePath;
    /** The active player and exchange (needed for in-game save). */
    private Player  currentPlayer;
    private Exchange currentExchange;
    private boolean animationsEnabled = true;
    private boolean musicMuted        = false;
    private boolean sfxMuted          = false;
    private boolean devModeEnabled    = false;
    private boolean autosaveEnabled   = false;
    private boolean autosaveToast     = true;
    private boolean fullscreenEnabled = false;
    private double  musicVolume       = GlobalSettingsManager.DEFAULT_MUSIC_VOLUME;
    private double  sfxVolume         = GlobalSettingsManager.DEFAULT_SFX_VOLUME;
    /** Non-maximised window dimensions (0 = not set, start maximised). */
    private int     windowWidth       = GlobalSettingsManager.DEFAULT_WINDOW_WIDTH;
    private int     windowHeight      = GlobalSettingsManager.DEFAULT_WINDOW_HEIGHT;
    private Stage   primaryStage;
    private Timeline autosaveTimer;
    private SfxController sfxController;
    private GameView currentGameView;
    private GameUiState currentUiState;

    @Override
    public void start(Stage stage) {
        homePageMusicController = new HomePageMusicController(getClass());
        sfxController = new SfxController(getClass());

        GlobalSettingsManager.Settings gs = GlobalSettingsManager.load();
        musicVolume       = gs.musicVolume();
        sfxVolume         = gs.sfxVolume();
        homePageMusicController.setVolume(gs.musicMuted() ? 0.0 : musicVolume);
        sfxController.setVolume(gs.sfxMuted() ? 0.0 : sfxVolume);
        animationsEnabled = gs.animations();
        musicMuted        = gs.musicMuted();
        sfxMuted          = gs.sfxMuted();
        devModeEnabled    = gs.devMode();
        autosaveEnabled   = gs.autosave();
        autosaveToast     = gs.autosaveToast();
        fullscreenEnabled = gs.fullscreen();
        windowWidth       = gs.windowWidth();
        windowHeight      = gs.windowHeight();
        AppConfig.DEV_MODE.set(gs.devMode());

        primaryStage = stage;

        homePage = LandingPageView.build(
                this::goToSaveSelect,
            () -> { sfxController.play(SfxController.SETTINGS); Parent s = buildSettingsView(this::goHomeKeepMusic, null); navigateKeepMusic(s); fadeInPage(s); },
            Platform::exit
        );

        backgroundCanvas = new BackgroundCanvas();
        backgroundCanvas.setAnimationsEnabled(animationsEnabled);
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
        // Apply saved window size (if any) — must come after configureStage
        if (fullscreenEnabled) {
            stage.setFullScreen(true);
        } else if (windowWidth > 0 && windowHeight > 0) {
            stage.setMaximized(false);
            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
        }
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Track window size changes so we can persist them
        stage.widthProperty().addListener((obs, o, w) -> {
            if (!stage.isFullScreen() && !stage.isMaximized()) windowWidth  = w.intValue();
        });
        stage.heightProperty().addListener((obs, o, h) -> {
            if (!stage.isFullScreen() && !stage.isMaximized()) windowHeight = h.intValue();
        });

        stage.show();

        homePageMusicController.play(
                splashOverlayController::fadeAfterStartup,
                splashOverlayController::fadeAfterFailure);
    }

    // ── Navigation targets ────────────────────────────────────────────────────

    private Runnable withBack(Runnable r) {
        return () -> { sfxController.play(SfxController.BACK, Math.min(sfxController.getVolume() * 1.5, 1.0)); r.run(); };
    }

    private void goToSaveSelect() {
        SaveSelectController ctrl = new SaveSelectController(
                this::goToSetup,
                withBack(this::goHomeKeepMusic),
                this::loadFromSave,
                currentPlayer,
                currentExchange,
                currentSavePath,
                currentUiState
        );
        Parent saveSelectPage = new SaveSelectView(ctrl).getRoot();
        navigateKeepMusic(saveSelectPage);
        fadeInPage(saveSelectPage);
    }

    private void loadFromSave(Object[] data) {
        Player player    = (Player)   data[0];
        Exchange exchange = (Exchange) data[1];
        java.nio.file.Path savePath = (java.nio.file.Path) data[2];
        GameUiState uiState = data.length > 3 ? (GameUiState) data[3] : null;
        buildAndStartGameFromSave(player, exchange, savePath, uiState);
    }

    private void goToSetup() {
        // Starting a new game — clear the in-memory session
        currentPlayer   = null;
        currentExchange = null;
        currentSavePath = null;
        currentUiState  = null;
        currentSetupPage = new SetupView(
                withBack(this::goHomeKeepMusic),
                (name, cash, csvResource) -> startGame(name, cash, csvResource),
                (name, cash) -> goToImportCsv(name, cash)
        ).getRoot();
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

    private void startGame(String name, double cash, String csvResource) {
        new Thread(() -> {
            CsvParseResult result = StockCsvLoader.loadFromResourceWithErrors(csvResource);
            Platform.runLater(() -> {
                if (result.hasErrors()) {
                    openCsvEditor(result, name, cash);
                } else {
                    List<Stock> stocks = result.getRows().stream()
                            .map(StockCsvLoader::rowToStock)
                            .toList();
                    String exchangeName = marketName(csvResource);
                    buildAndStartGame(name, cash, stocks, false, exchangeName);
                }
            });
        }, "stock-loader").start();
    }

    private static String marketName(String csvResource) {
        return AppConfig.BUILT_IN_MARKETS.stream()
                .filter(m -> m.csvResource().equals(csvResource))
                .map(m -> m.name())
                .findFirst().orElse("Market");
    }


    private void startGameWithCsv(String name, double cash, File csvFile) {
        new Thread(() -> {
            CsvParseResult result;
            try {
                result = StockCsvLoader.parseWithErrors(
                        new FileReader(csvFile, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Platform.runLater(() -> showAppNotification(
                        "CSV Error", "Could not read file:\n" + e.getMessage(), false));
                return;
            }
            Platform.runLater(() -> {
                if (result.hasErrors()) {
                    openCsvEditorFromImport(result, name, cash, csvFile);
                } else {
                    List<Stock> stocks = result.getRows().stream()
                            .map(StockCsvLoader::rowToStock)
                            .toList();
                    buildAndStartGame(name, cash, stocks, false, "Custom Market");
                }
            });
        }, "stock-loader").start();
    }

    private void openCsvEditor(CsvParseResult result, String name, double cash) {
        Parent editorPage = CsvEditorView.build(
                result,
                withBack(this::goHomeKeepMusic),
                stocks -> buildAndStartGame(name, cash, stocks, true, "Custom Market")
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
            showAppNotification("CSV Error", "Could not read file:\n" + e.getMessage(), false);
            return;
        }

        openCsvEditorFromImport(result, name, cash, csvFile);
    }

    private void openCsvEditorFromImport(CsvParseResult result, String name, double cash, File selectedFile) {
        Parent editorPage = CsvEditorView.build(
                result,
                withBack(() -> goToImportCsv(name, cash, selectedFile)),
                stocks -> buildAndStartGame(name, cash, stocks, true, "Custom Market")
        );
        navigateKeepMusic(editorPage);
        fadeInPage(editorPage);
    }

    private void buildAndStartGame(String name, double cash, List<Stock> stocks, boolean fromEditor) {
        buildAndStartGame(name, cash, stocks, fromEditor, "Custom Market");
    }

    private void buildAndStartGame(String name, double cash, List<Stock> stocks, boolean fromEditor, String exchangeName) {
        if (stocks.isEmpty()) {
            showNoStocksPage(fromEditor);
            return;
        }
        Player player = new Player(
                name == null || name.isBlank() ? "Player" : name,
                BigDecimal.valueOf(cash));
        Exchange exchange = new Exchange(exchangeName, stocks);
        buildAndStartGameFromSave(player, exchange, null, null);
    }

    private void buildAndStartGameFromSave(Player player, Exchange exchange,
                                            java.nio.file.Path savePath, GameUiState uiState) {
        if (exchange.getStocks().isEmpty()) {
            showNoStocksPage(false);
            return;
        }
        currentPlayer   = player;
        currentExchange = exchange;
        currentSavePath = savePath;
        GameController gameController = new GameController(player, exchange);
        GameView gameview = new GameView(gameController, withBack(this::goHome),
                () -> {
                    sfxController.play(SfxController.SETTINGS);
                    navigateKeepMusic(buildSettingsView(
                            () -> navigateKeepMusic(currentGamePage),
                            this::performSave));
                },
                sfxController::getVolume,
                uiState);
        currentGameView = gameview;
        currentGamePage = gameview.getRoot();
        navigateToGame(currentGamePage);
        Platform.runLater(() -> homePageMusicController.playGameStartThenAmbience(sfxController.getVolume()));
        if (autosaveEnabled) startAutosaveTimer();
    }

    private void performSave() {
        if (currentPlayer == null || currentExchange == null) return;
        GameUiState uiState = currentGameView != null ? currentGameView.getUiState() : null;
        try {
            if (currentSavePath != null) {
                GameSaveExporter.overwrite(currentSavePath, currentPlayer, currentExchange, uiState);
            } else {
                currentSavePath = GameSaveExporter.save(currentPlayer, currentExchange, uiState);
            }
            showAppNotification("Game Saved", "Your progress has been saved.", true);
        } catch (IOException e) {
            showAppNotification("Save Failed", "Could not save the game:\n" + e.getMessage(), false);
        }
    }

    private void performAutosave() {
        if (currentPlayer == null || currentExchange == null) return;
        GameUiState uiState = currentGameView != null ? currentGameView.getUiState() : null;
        try {
            GameSaveExporter.autosave(currentPlayer, currentExchange, uiState);
            if (autosaveToast) showTimedNotification("Autosaved", "Progress autosaved.", true);
        } catch (IOException e) {
            showAppNotification("Autosave Failed", "Could not autosave:\n" + e.getMessage(), false);
        }
    }

    private void startAutosaveTimer() {
        stopAutosaveTimer();
        autosaveTimer = new Timeline(new KeyFrame(Duration.minutes(1), e -> performAutosave()));
        autosaveTimer.setCycleCount(Timeline.INDEFINITE);
        autosaveTimer.play();
    }

    private void stopAutosaveTimer() {
        if (autosaveTimer != null) { autosaveTimer.stop(); autosaveTimer = null; }
    }

    private void showNoStocksPage(boolean fromEditor) {
        Parent page = NoStocksView.build(NoStocksView.DEFAULT_MONOLOGUE, fromEditor, withBack(this::goHome));
        navigateToGame(page);
        Platform.runLater(() -> homePageMusicController.fadeOutThenPlayAmbienceStartingWith(
                "/audio/music/ambience/After_Hours_Ambience3_demo.mp3"));
    }

    private Parent buildSettingsView(Runnable onBack, Runnable onSave) {
        Runnable onBackWithSfx = () -> {
            sfxController.play(SfxController.BACK, Math.min(sfxController.getVolume() * 1.5, 1.0));
            onBack.run();
        };
        Runnable onResetAll = () -> {
            musicVolume       = GlobalSettingsManager.DEFAULT_MUSIC_VOLUME;
            sfxVolume         = GlobalSettingsManager.DEFAULT_SFX_VOLUME;
            animationsEnabled = GlobalSettingsManager.DEFAULT_ANIMATIONS;
            musicMuted        = GlobalSettingsManager.DEFAULT_MUSIC_MUTED;
            sfxMuted          = GlobalSettingsManager.DEFAULT_SFX_MUTED;
            devModeEnabled    = GlobalSettingsManager.DEFAULT_DEV_MODE;
            autosaveEnabled   = GlobalSettingsManager.DEFAULT_AUTOSAVE;
            autosaveToast     = GlobalSettingsManager.DEFAULT_AUTOSAVE_TOAST;
            fullscreenEnabled = GlobalSettingsManager.DEFAULT_FULLSCREEN;
            windowWidth       = GlobalSettingsManager.DEFAULT_WINDOW_WIDTH;
            windowHeight      = GlobalSettingsManager.DEFAULT_WINDOW_HEIGHT;
            homePageMusicController.setVolume(musicVolume);
            sfxController.setVolume(sfxVolume);
            backgroundCanvas.setAnimationsEnabled(animationsEnabled);
            primaryStage.setFullScreen(false);
            primaryStage.setMaximized(true);
            AppConfig.DEV_MODE.set(false);
            if (autosaveEnabled) startAutosaveTimer(); else stopAutosaveTimer();
            saveSettings();
            navigateKeepMusic(buildSettingsView(onBack, onSave));
        };
        return SettingsView.build(
                onBackWithSfx,
                primaryStage,
                vol -> { musicVolume = vol; homePageMusicController.setVolume(musicMuted ? 0.0 : vol); saveSettings(); },
                musicVolume,
                musicMuted,
                muted -> { musicMuted = muted; homePageMusicController.setVolume(muted ? 0.0 : musicVolume); saveSettings(); },
                vol -> { sfxVolume = vol; sfxController.setVolume(sfxMuted ? 0.0 : vol); saveSettings(); },
                sfxVolume,
                sfxMuted,
                muted -> { sfxMuted = muted; sfxController.setVolume(muted ? 0.0 : sfxVolume); saveSettings(); },
                enabled -> { animationsEnabled = enabled; backgroundCanvas.setAnimationsEnabled(enabled); saveSettings(); },
                animationsEnabled,
                fullscreenEnabled,
                enabled -> { fullscreenEnabled = enabled; primaryStage.setFullScreen(enabled); saveSettings(); },
                dims   -> { primaryStage.setFullScreen(false); primaryStage.setMaximized(false);
                            primaryStage.setWidth(dims[0]); primaryStage.setHeight(dims[1]); },
                ()     -> { primaryStage.setFullScreen(false); primaryStage.setMaximized(true); },
                file   -> { /* export already completed in view; reserved for future controller logic */ },
                enabled -> { devModeEnabled = enabled; saveSettings(); },
                devModeEnabled,
                enabled -> { autosaveEnabled = enabled; if (enabled) startAutosaveTimer(); else stopAutosaveTimer(); saveSettings(); },
                autosaveEnabled,
                enabled -> { autosaveToast = enabled; saveSettings(); },
                autosaveToast,
                currentSavePath,
                onResetAll,
                onSave
        );
    }

    private void saveSettings() {
        // Only persist a custom size when we actually have one (non-fullscreen, non-maximised)
        int savedW = (primaryStage.isFullScreen() || primaryStage.isMaximized()) ? windowWidth  : (int) primaryStage.getWidth();
        int savedH = (primaryStage.isFullScreen() || primaryStage.isMaximized()) ? windowHeight : (int) primaryStage.getHeight();
        GlobalSettingsManager.save(new GlobalSettingsManager.Settings(
                musicVolume,
                sfxVolume,
                animationsEnabled,
                musicMuted,
                sfxMuted,
                autosaveEnabled,
                autosaveToast,
                fullscreenEnabled,
                devModeEnabled,
                savedW,
                savedH));
    }

    // ── Music-aware navigation primitives ────────────────────────────────────

    /** Styled in-app notification overlay — replaces all OS Alert dialogs. */
    private void showAppNotification(String title, String message, boolean success) {
        Label iconLbl = new Label(success ? "\u2713" : "\u2715");
        iconLbl.getStyleClass().add(success ? "receipt-check" : "error-dialog-icon");
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add(success ? "app-success-title" : "error-dialog-title");

        HBox header = new HBox(12, iconLbl, titleLbl);
        header.getStyleClass().add(success ? "app-success-header" : "error-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add(success ? "app-success-message" : "error-dialog-message");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(320);

        VBox body = new VBox(msgLbl);
        body.getStyleClass().add(success ? "app-success-body" : "error-dialog-body");

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add(success ? "dialog-confirm-buy-btn" : "dialog-cancel-btn");
        HBox btnRow = new HBox(okBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getStyleClass().add("dialog-btn-row");

        VBox card = new VBox(0, header, body, btnRow);
        card.getStyleClass().add(success ? "app-success-root" : "error-dialog-root");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("dialog-backdrop");

        StackPane popup = new StackPane(backdrop, card);
        StackPane.setAlignment(card, Pos.CENTER);

        Runnable dismiss = () -> root.getChildren().remove(popup);
        okBtn.setOnAction(ev -> dismiss.run());
        backdrop.setOnMouseClicked(ev -> dismiss.run());
        popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE || ev.getCode() == KeyCode.ENTER) {
                dismiss.run();
                ev.consume();
            }
        });
        root.getChildren().add(popup);
        popup.requestFocus();
    }

    /** Auto-dismissing toast — disappears after 2 seconds without user interaction. */
    private void showTimedNotification(String title, String message, boolean success) {
        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add("toast-message");
        msgLbl.setWrapText(false);

        VBox card = new VBox(msgLbl);
        card.getStyleClass().add("toast-card");
        card.setMaxWidth(260);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane.setAlignment(card, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(card, new Insets(0, 24, 32, 0));

        root.getChildren().add(card);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> root.getChildren().remove(card));
        pause.play();
    }

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
        if (currentGameView != null) {
            currentUiState = currentGameView.getUiState();
        }
        stopAutosaveTimer();
        if (autosaveEnabled) performAutosave();
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
        stopAutosaveTimer();
        if (autosaveEnabled) performAutosave();
        saveSettings(); // persist final window size
        if (homePageMusicController != null) homePageMusicController.stop();
        if (backgroundCanvas != null) backgroundCanvas.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

