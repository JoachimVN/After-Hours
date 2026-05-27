package edu.ntnu.idatt2003.g23;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.ntnu.idatt2003.g23.audio.HomePageMusicController;
import edu.ntnu.idatt2003.g23.audio.SfxController;
import edu.ntnu.idatt2003.g23.io.CsvEditorLoadAnalyzer;
import edu.ntnu.idatt2003.g23.io.CsvEditorLoadAnalyzer.LoadStats;
import edu.ntnu.idatt2003.g23.io.CsvParseResult;
import edu.ntnu.idatt2003.g23.io.CsvRow;
import edu.ntnu.idatt2003.g23.io.GameSaveExporter;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader;
import edu.ntnu.idatt2003.g23.io.GameSaveLoader.SaveMeta;
import edu.ntnu.idatt2003.g23.io.GameUiState;
import edu.ntnu.idatt2003.g23.io.GlobalSettingsManager;
import edu.ntnu.idatt2003.g23.io.StockCsvExporter;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;
import edu.ntnu.idatt2003.g23.model.transaction.TransactionFactory;
import edu.ntnu.idatt2003.g23.session.GameSessionService;
import edu.ntnu.idatt2003.g23.ui.BackgroundCanvas;
import edu.ntnu.idatt2003.g23.ui.navigation.NavigationCoordinator;
import edu.ntnu.idatt2003.g23.ui.overlay.AppOverlayService;
import edu.ntnu.idatt2003.g23.ui.overlay.SplashOverlayController;
import edu.ntnu.idatt2003.g23.ui.views.csveditor.CsvEditorView;
import edu.ntnu.idatt2003.g23.ui.views.customstocks.CustomStocksView;
import edu.ntnu.idatt2003.g23.ui.views.game.GameController;
import edu.ntnu.idatt2003.g23.ui.views.game.GameView;
import edu.ntnu.idatt2003.g23.ui.views.landingpage.LandingPageView;
import edu.ntnu.idatt2003.g23.ui.views.nogame.NoGameView;
import edu.ntnu.idatt2003.g23.ui.views.profile.ProfileController;
import edu.ntnu.idatt2003.g23.ui.views.profile.ProfileView;
import edu.ntnu.idatt2003.g23.ui.views.saveselect.SaveSelectController;
import edu.ntnu.idatt2003.g23.ui.views.saveselect.SaveSelectView;
import edu.ntnu.idatt2003.g23.ui.views.settings.SettingsController;
import edu.ntnu.idatt2003.g23.ui.views.settings.SettingsView;
import edu.ntnu.idatt2003.g23.ui.views.setup.SetupView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Launches the JavaFX application and manages top-level scene navigation.
 * <p>
 * Music rules:
 * - Home music plays on: Home, Setup, Custom Stock, Settings
 * - Ambience plays on: GameView only
 * - navigateKeepMusic() never touches music
 * - navigateToGame() fades current → ambience
 * - goHome() fades ambience → home music (from game context)
 * - goHomeKeepMusic() no music change (from non-game contexts)
 */
public class App extends Application {

  private static final double LANDING_THEME_CONTEXT_MULTIPLIER = 1.0;
  private static final double NON_LANDING_THEME_CONTEXT_MULTIPLIER = 0.35;
  private static final String NOTIF_TITLE_CSV_ERROR = "CSV Error";
  private static final String NOTIF_TITLE_SAVE_ERROR = "Save Error";
  private static final String NOTIF_TITLE_EDIT_BLOCKED = "Edit Blocked";
  private static final String EXCHANGE_NAME_CUSTOM_MARKET = "Custom Market";
  private static final String MSG_COULD_NOT_LOAD_MARKET_DATA = "Could not load market data:\n";
  private static final String MSG_COULD_NOT_READ_FILE = "Could not read file:\n";
  private static final String MSG_COULD_NOT_SAVE_CSV = "Could not save CSV:\n";

  private StackPane root;
  private Parent homePage;
  private HomePageMusicController homePageMusicController;
  private BackgroundCanvas backgroundCanvas;
  private AppOverlayService overlayService;
  private NavigationCoordinator navigationCoordinator;

  /**
   * Retained so back-navigation can return without recreating the form.
   */
  private Parent currentSetupPage;
  /**
   * Retained so Settings can return to the game without recreating it.
   */
  private Parent currentGamePage;
  /**
   * Save folder of the currently loaded save; null for a new game.
   */
  private java.nio.file.Path currentSavePath;
  /**
   * The active player and exchange (needed for in-game save).
   */
  private Player currentPlayer;
  private Exchange currentExchange;
  private boolean animationsEnabled = true;
  private boolean musicMuted = false;
  private boolean sfxMuted = false;
  private boolean devModeEnabled = false;
  private boolean autosaveEnabled = false;
  private boolean autosaveToast = true;
  private boolean showTutorialEnabled = GlobalSettingsManager.DEFAULT_SHOW_TUTORIAL;
  private boolean performanceModeEnabled = GlobalSettingsManager.DEFAULT_PERFORMANCE_MODE;
  private int maxHistoryWeeks = GlobalSettingsManager.DEFAULT_MAX_HISTORY_WEEKS;
  private String currentAutosaveId = null; // unique per game instance
  private boolean fullscreenEnabled = false;
  private double musicVolume = GlobalSettingsManager.DEFAULT_MUSIC_VOLUME;
  private double sfxVolume = GlobalSettingsManager.DEFAULT_SFX_VOLUME;
  /**
   * Non-maximised window dimensions (0 = not set, start maximised).
   */
  private int windowWidth = GlobalSettingsManager.DEFAULT_WINDOW_WIDTH;
  private int windowHeight = GlobalSettingsManager.DEFAULT_WINDOW_HEIGHT;
  private Stage primaryStage;
  private Timeline autosaveTimer;
  private SfxController sfxController;
  private GameView currentGameView;
  private GameController currentGameController;
  private GameUiState currentUiState;
  private String currentProfileAvatar = "bust-in-silhouette";
  private boolean currentFlagged = false;
  private boolean gameAudioContext = false;
  private final GameSessionService gameSessionService = new GameSessionService();

  @Override
  public void start(Stage stage) {
    homePageMusicController = new HomePageMusicController(getClass());
    homePageMusicController.preloadStartupAudio();
    sfxController = new SfxController(getClass());

    GlobalSettingsManager.Settings gs = GlobalSettingsManager.load();
    musicVolume = gs.musicVolume();
    sfxVolume = gs.sfxVolume();
    homePageMusicController.setVolume(musicVolume);
    sfxController.setVolume(gs.sfxMuted() ? 0.0 : sfxVolume);
    animationsEnabled = gs.animations();
    musicMuted = gs.musicMuted();
    sfxMuted = gs.sfxMuted();
    devModeEnabled = gs.devMode();
    autosaveEnabled = gs.autosave();
    autosaveToast = gs.autosaveToast();
    showTutorialEnabled = gs.showTutorial();
    performanceModeEnabled = gs.performanceMode();
    maxHistoryWeeks = gs.maxHistoryWeeks();
    fullscreenEnabled = gs.fullscreen();
    windowWidth = gs.windowWidth();
    windowHeight = gs.windowHeight();

    AppConfig.DEV_MODE.set(gs.devMode());
    AppConfig.DEV_MODE.addListener((obs, oldVal, newVal) -> {
      if (Boolean.compare(newVal, devModeEnabled) != 0) {
        devModeEnabled = newVal;
        saveSettings();
      }
    });
    AppConfig.PERFORMANCE_MODE.set(performanceModeEnabled);
    AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.set(maxHistoryWeeks);
    primaryStage = stage;
    homePage = LandingPageView.build(
        () -> {
          sfxController.play(SfxController.PLAY1, Math.min(sfxController.getVolume() * 1.5, 1.0));
          goToSaveSelect();
        },
        () -> {
          sfxController.play(SfxController.SETTINGS);
          Parent s = buildSettingsView(this::goHomeKeepMusic, null);
          navigateKeepMusic(s);
          fadeInPage(s);
        },
        Platform::exit);

    backgroundCanvas = new BackgroundCanvas();
    backgroundCanvas.setAnimationsEnabled(animationsEnabled);
    root = new StackPane(backgroundCanvas, homePage);
    navigationCoordinator = new NavigationCoordinator(root, backgroundCanvas);
    overlayService = new AppOverlayService(root);
    ColorAdjust globalFilter = new ColorAdjust();
    globalFilter.setBrightness(0.05);
    globalFilter.setContrast(0.025);
    root.setEffect(globalFilter);
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
        getClass().getResource("/css/profile.css").toExternalForm(),
        getClass().getResource("/css/scrollbar.css").toExternalForm(),
        getClass().getResource("/css/saveselect.css").toExternalForm());

    configureStage(stage, scene);
    // Respect fullscreen setting; otherwise keep forced maximized startup.
    if (fullscreenEnabled) {
      stage.setFullScreen(true);
    }
    stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.F11) {
        toggleFullscreen();
        event.consume();
      }
    });
    scene.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
      if (isTextInputTarget(event.getTarget())) {
        event.consume();
      }
    });

    // Track window size changes so we can persist them
    stage.widthProperty().addListener((obs, o, w) -> {
      if (!stage.isFullScreen() && !stage.isMaximized()) {
        windowWidth = w.intValue();
      }
    });
    stage.heightProperty().addListener((obs, o, h) -> {
      if (!stage.isFullScreen() && !stage.isMaximized()) {
        windowHeight = h.intValue();
      }
    });

    stage.show();

    if (musicMuted) {
      splashOverlayController.fadeAfterStartup();
    } else {
      splashOverlayController.fadeAfterStartup();
      homePageMusicController.play(
          null,
          splashOverlayController::fadeAfterFailure);
    }
  }

  // ── Navigation targets ────────────────────────────────────────────────────

  private Runnable withBack(Runnable r) {
    return () -> {
      sfxController.play(SfxController.BACK, Math.min(sfxController.getVolume() * 1.5, 1.0));
      r.run();
    };
  }

  private void goToSaveSelect() {
    final Parent[] saveSelectPageRef = new Parent[1];
    SaveSelectController ctrl = new SaveSelectController(
        () -> {
          sfxController.play(SfxController.PLAY2, Math.min(sfxController.getVolume() * 1.5, 1.0));
          goToSetup();
        },
        withBack(this::goHomeKeepMusic),
        this::loadFromSave,
        meta -> {
          if (saveSelectPageRef[0] != null) {
            openCsvEditorFromSaveMetaForContinue(meta,
                () -> navigateKeepMusic(saveSelectPageRef[0]));
          }
        },
        currentPlayer,
        currentExchange,
        currentSavePath,
        currentUiState,
        currentFlagged,
        devModeEnabled);
    Parent saveSelectPage = new SaveSelectView(ctrl).getRoot();
    saveSelectPageRef[0] = saveSelectPage;
    navigateKeepMusic(saveSelectPage);
    fadeInPage(saveSelectPage);
  }

  private void loadFromSave(Object[] data) {
    Player player = (Player) data[0];
    Exchange exchange = (Exchange) data[1];
    java.nio.file.Path savePath = (java.nio.file.Path) data[2];
    GameUiState uiState = data.length > 3 ? (GameUiState) data[3] : null;
    currentFlagged = data.length > 4 && Boolean.TRUE.equals(data[4]);
    gameSessionService.setFlagged(currentFlagged);
    buildAndStartGameFromSave(player, exchange, savePath, uiState, false);
  }

  private void goToSetup() {
    // Starting a new game — clear the in-memory session
    gameSessionService.clearForNewGame();
    currentPlayer = null;
    currentExchange = null;
    currentSavePath = null;
    currentGameController = null;
    currentUiState = null;
    currentFlagged = false;
    currentProfileAvatar = gameSessionService.getProfileAvatar();
    currentSetupPage = new SetupView(
        withBack(this::goToSaveSelect),
        this::startGame,
        (name, cash) -> {
          sfxController.play(SfxController.PLAY3, Math.min(sfxController.getVolume() * 1.5, 1.0));
          goToCustomStocks(name, cash);
        },
        currentProfileAvatar,
        () -> sfxController.play(SfxController.SELECT)).getRoot();
    navigateKeepMusic(currentSetupPage);
  }

  private void goToCustomStocks(String name, double cash) {
    goToCustomStocks(name, cash, null);
  }

  private void goToCustomStocks(String name, double cash, File selectedFile) {
    Parent importPage = CustomStocksView.build(
        withBack(() -> navigateKeepMusic(currentSetupPage)),
        () -> openCsvEditorFromImport(new CsvParseResult(List.of()), name, cash, selectedFile),
        file -> openCsvEditorFromImport(file, name, cash),
        file -> startGameWithCsv(name, cash, file),
        csvResource -> openCsvEditorFromBuiltInMarket(csvResource, name, cash),
        meta -> openCsvEditorFromSaveMeta(meta, name, cash),
        currentSavePath,
      selectedFile,
      () -> sfxController.play(SfxController.SELECT));
    navigateKeepMusic(importPage);
    fadeInPage(importPage);
  }

  private void startGame(String name, double cash, String csvResource) {
    runWithLoadingOverlay(
        () -> StockCsvLoader.loadFromResourceWithErrors(csvResource),
        result -> {
          if (result.hasErrors()) {
            openCsvEditor(result, name, cash);
          } else {
            List<Stock> stocks = StockCsvLoader.toStocks(result.getRows());
            String exchangeName = AppConfig.marketNameFor(csvResource);
            buildAndStartGame(name, cash, stocks, false, exchangeName);
          }
        },
        error -> overlayService.showNotification(NOTIF_TITLE_CSV_ERROR, MSG_COULD_NOT_LOAD_MARKET_DATA + error.getMessage(), false));
  }

  private void startGameWithCsv(String name, double cash, File csvFile) {
    runWithLoadingOverlay(
        () -> {
          try {
            return StockCsvLoader.parseWithErrors(new FileReader(csvFile, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> {
          if (result.hasErrors()) {
            openCsvEditorFromImport(result, name, cash, csvFile);
          } else {
            buildAndStartGame(name, cash, StockCsvLoader.toStocks(result.getRows()), false, EXCHANGE_NAME_CUSTOM_MARKET);
          }
        },
        error -> overlayService.showNotification(NOTIF_TITLE_CSV_ERROR, MSG_COULD_NOT_READ_FILE + error.getMessage(), false));
  }

  private void openCsvEditor(CsvParseResult result, String name, double cash) {
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.build(
          result,
          withBack(this::goHomeKeepMusic),
          rows -> buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, EXCHANGE_NAME_CUSTOM_MARKET),
          (rows, file) -> saveCsvRowsAndStartGame(name, cash, rows, file),
          () -> openCsvEditor(result, name, cash));
      navigateKeepMusic(editorPage);
      fadeInPage(editorPage);
    };
    if (CsvEditorLoadAnalyzer.shouldWarn(stats)) {
      overlayService.showLargeFileWarning(stats, doOpen);
    } else {
      doOpen.run();
    }
  }

  private void openCsvEditorFromImport(File csvFile, String name, double cash) {
    runWithLoadingOverlay(
        () -> {
          try {
            return StockCsvLoader.parseWithErrors(new FileReader(csvFile, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> openCsvEditorFromImport(result, name, cash, csvFile),
        error -> overlayService.showNotification(NOTIF_TITLE_CSV_ERROR, MSG_COULD_NOT_READ_FILE + error.getMessage(), false));
  }

  private void openCsvEditorFromImport(CsvParseResult result, String name, double cash,
      File selectedFile) {
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.build(
          result,
          withBack(() -> goToCustomStocks(name, cash, selectedFile)),
          rows -> buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, EXCHANGE_NAME_CUSTOM_MARKET),
          (rows, file) -> saveCsvRowsAndStartGame(name, cash, rows, file),
          () -> openCsvEditorFromImport(result, name, cash, selectedFile));
      navigateKeepMusic(editorPage);
      fadeInPage(editorPage);
    };
    if (CsvEditorLoadAnalyzer.shouldWarn(stats)) {
      overlayService.showLargeFileWarning(stats, doOpen);
    } else {
      doOpen.run();
    }
  }

  private void openCsvEditorFromBuiltInMarket(String csvResource, String name, double cash) {
    runWithLoadingOverlay(
        () -> {
          try (InputStream is = getClass().getClassLoader().getResourceAsStream(csvResource)) {
            if (is == null) {
              throw new IllegalStateException("Built-in market resource not found: " + csvResource);
            }
            return StockCsvLoader.parseWithErrors(new InputStreamReader(is, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> {
          LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
          Runnable doOpen = () -> {
            Parent editorPage = CsvEditorView.build(
                result,
                withBack(() -> goToCustomStocks(name, cash, null)),
                rows -> buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, AppConfig.marketNameFor(csvResource)),
                (rows, file) -> saveCsvRowsAndStartGame(name, cash, rows, file),
                () -> openCsvEditorFromBuiltInMarket(csvResource, name, cash));
            navigateKeepMusic(editorPage);
            fadeInPage(editorPage);
          };
          if (CsvEditorLoadAnalyzer.shouldWarn(stats)) {
            overlayService.showLargeFileWarning(stats, doOpen);
          } else {
            doOpen.run();
          }
        },
        error -> overlayService.showNotification(NOTIF_TITLE_CSV_ERROR, MSG_COULD_NOT_LOAD_MARKET_DATA + error.getMessage(), false));
  }

  private void openCsvEditorFromSaveMeta(SaveMeta selectedSave, String name, double cash) {
    File saveStocksFile = resolveSaveStocksFile(selectedSave);
    if (saveStocksFile == null) {
      overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, "Could not locate stocks.csv for that save.", false);
      return;
    }
    openCsvEditorFromImport(saveStocksFile, name, cash);
  }

  private void saveCsvRowsAndStartGame(String name, double cash,
      List<edu.ntnu.idatt2003.g23.io.CsvRow> rows, File file) {
    try {
      edu.ntnu.idatt2003.g23.io.StockCsvExporter.writeCsvRows(file.toPath(), rows);
    } catch (IOException e) {
      overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, MSG_COULD_NOT_SAVE_CSV + e.getMessage(), false);
      return;
    }
    buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, EXCHANGE_NAME_CUSTOM_MARKET);
  }

  private void openBlankCsvEditorStandalone(Runnable onBack) {
    openCsvEditorStandalone(new CsvParseResult(List.of()), onBack,
        () -> openBlankCsvEditorStandalone(onBack));
  }

  private void openCsvEditorStandalone(CsvParseResult result, Runnable onBack,
      Runnable onReset) {
    openCsvEditorStandalone(result, onBack, onReset, null);
  }

  private void openCsvEditorStandalone(CsvParseResult result, Runnable onBack,
      Runnable onReset, Runnable onSuccessfulSave) {
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.buildStandalone(
          result,
          withBack(onBack),
          (editedRows, file) -> {
            try {
              StockCsvExporter.writeCsvRows(file.toPath(), editedRows);
              if (onSuccessfulSave != null) {
                onSuccessfulSave.run();
              }
              overlayService.showNotification("Saved", "Stock data exported to:\n" + file.getName(), true);
              onBack.run();
            } catch (IOException e) {
              overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, MSG_COULD_NOT_SAVE_CSV + e.getMessage(), false);
            }
          },
          onReset);
      navigateKeepMusic(editorPage);
      fadeInPage(editorPage);
    };
    if (CsvEditorLoadAnalyzer.shouldWarn(stats)) {
      overlayService.showLargeFileWarning(stats, doOpen);
    } else {
      doOpen.run();
    }
  }

  private void openCsvEditorFromImportStandalone(File csvFile, Runnable onBack) {
    openCsvEditorFromImportStandalone(csvFile, onBack, null);
  }

  private void openCsvEditorFromImportStandalone(File csvFile, Runnable onBack,
      Runnable onSuccessfulSave) {
    runWithLoadingOverlay(
        () -> {
          try {
            return StockCsvLoader.parseWithErrors(new FileReader(csvFile, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> openCsvEditorStandalone(result, onBack,
            () -> openCsvEditorFromImportStandalone(csvFile, onBack, onSuccessfulSave),
            onSuccessfulSave),
        error -> overlayService.showNotification(NOTIF_TITLE_CSV_ERROR, MSG_COULD_NOT_READ_FILE + error.getMessage(), false));
  }

  private void openCsvEditorFromBuiltInMarketStandalone(String csvResource, Runnable onBack) {
    runWithLoadingOverlay(
        () -> {
          try (InputStream is = getClass().getClassLoader().getResourceAsStream(csvResource)) {
            if (is == null) {
              throw new IllegalStateException("Built-in market resource not found: " + csvResource);
            }
            return StockCsvLoader.parseWithErrors(new InputStreamReader(is, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> openCsvEditorStandalone(result, onBack,
            () -> openCsvEditorFromBuiltInMarketStandalone(csvResource, onBack)),
        error -> overlayService.showNotification(NOTIF_TITLE_CSV_ERROR, MSG_COULD_NOT_LOAD_MARKET_DATA + error.getMessage(), false));
  }

  private void openCsvEditorFromSaveMetaStandalone(SaveMeta selectedSave, Runnable onBack) {
    File saveStocksFile = resolveSaveStocksFile(selectedSave);
    if (saveStocksFile == null) {
      overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, "Could not locate stocks.csv for that save.", false);
      return;
    }
    openCsvEditorFromImportStandalone(saveStocksFile, onBack, () -> {
      try {
        GameSaveExporter.markSaveAsFlagged(selectedSave.saveDir());
      } catch (IOException e) {
        overlayService.showNotification("Save Flag Warning",
            "CSV was saved, but flag metadata could not be updated:\n" + e.getMessage(), false);
      }
    });
  }

  private void openCsvEditorFromSaveMetaForContinue(SaveMeta selectedSave, Runnable onBack) {
    if (selectedSave == null || selectedSave.saveDir() == null) {
      overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, "Could not locate that save.", false);
      return;
    }

    Object[] loaded;
    try {
      loaded = GameSaveLoader.load(selectedSave.saveDir());
    } catch (IOException | IllegalStateException e) {
      overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, "Could not load save:\n" + e.getMessage(), false);
      return;
    }

    Player loadedPlayer = (Player) loaded[0];
    Exchange loadedExchange = (Exchange) loaded[1];
    GameUiState loadedUiState = loaded.length > 2 ? (GameUiState) loaded[2] : null;
    CsvParseResult result = exchangeAsParseResult(loadedExchange);
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.build(
          result,
          withBack(onBack),
          rows -> {
            Exchange updatedExchange = new Exchange(loadedExchange.getName(), StockCsvLoader.toStocks(rows));
            updatedExchange.setWeek(loadedExchange.getWeek());
            Player updatedPlayer;
            try {
              updatedPlayer = rebuildPlayerForEditedExchange(loadedPlayer, updatedExchange);
            } catch (IllegalStateException e) {
              overlayService.showNotification(NOTIF_TITLE_EDIT_BLOCKED, e.getMessage(), false);
              return;
            }
            try {
              GameSaveExporter.markSaveAsFlagged(selectedSave.saveDir());
            } catch (IOException e) {
              overlayService.showNotification("Save Flag Warning",
                  "CSV was edited, but flag metadata could not be updated:\n" + e.getMessage(), false);
            }
            currentFlagged = true;
            gameSessionService.setFlagged(true);
            buildAndStartGameFromSave(updatedPlayer, updatedExchange, selectedSave.saveDir(),
                loadedUiState, false);
          },
          (rows, file) -> {
            try {
              StockCsvExporter.writeCsvRows(file.toPath(), rows);
              GameSaveExporter.markSaveAsFlagged(selectedSave.saveDir());
            } catch (IOException e) {
              overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, MSG_COULD_NOT_SAVE_CSV + e.getMessage(), false);
              return;
            }
            Exchange updatedExchange = new Exchange(loadedExchange.getName(), StockCsvLoader.toStocks(rows));
            updatedExchange.setWeek(loadedExchange.getWeek());
            Player updatedPlayer;
            try {
              updatedPlayer = rebuildPlayerForEditedExchange(loadedPlayer, updatedExchange);
            } catch (IllegalStateException e) {
              overlayService.showNotification(NOTIF_TITLE_EDIT_BLOCKED, e.getMessage(), false);
              return;
            }
            currentFlagged = true;
            gameSessionService.setFlagged(true);
            buildAndStartGameFromSave(updatedPlayer, updatedExchange, selectedSave.saveDir(),
                loadedUiState, false);
          },
          () -> openCsvEditorFromSaveMetaForContinue(selectedSave, onBack));
      navigateKeepMusic(editorPage);
      fadeInPage(editorPage);
    };
    if (CsvEditorLoadAnalyzer.shouldWarn(stats)) {
      overlayService.showLargeFileWarning(stats, doOpen);
    } else {
      doOpen.run();
    }
  }

  private void openCurrentMarketCsvEditorForGame(Runnable onBack) {
    if (currentExchange == null || currentPlayer == null || currentGamePage == null) {
      overlayService.showNotification("Error", "No active game session", false);
      return;
    }

    CsvParseResult result = currentExchangeAsParseResult();
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.build(
          result,
          withBack(onBack),
          rows -> applyEditedMarketToCurrentGame(rows, null),
          this::applyEditedMarketToCurrentGame,
          () -> openCurrentMarketCsvEditorForGame(onBack));
      navigateKeepMusic(editorPage);
      fadeInPage(editorPage);
    };
    if (CsvEditorLoadAnalyzer.shouldWarn(stats)) {
      overlayService.showLargeFileWarning(stats, doOpen);
    } else {
      doOpen.run();
    }
  }

  /**
   * Formats a stock's price history as a semicolon-delimited CSV string.
   * Keeps the full in-memory values so the editor sees the exact current history.
   */
  private String formatStockPricesForCsv(Stock stock) {
    List<BigDecimal> prices = stock.getHistoricalPrices();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < prices.size(); i++) {
      if (i > 0) {
        sb.append(";");
      }
      sb.append(prices.get(i).stripTrailingZeros().toPlainString());
    }
    return sb.toString();
  }

  private CsvParseResult currentExchangeAsParseResult() {
    return exchangeAsParseResult(currentExchange);
  }

  private CsvParseResult exchangeAsParseResult(Exchange exchange) {
    List<CsvRow> rows = new ArrayList<>();
    int lineNum = 1;
    for (Stock stock : exchange.getStocks()) {
      rows.add(new CsvRow(lineNum++, stock.getSymbol(), stock.getCompany(),
          formatStockPricesForCsv(stock), ""));
    }
    return new CsvParseResult(rows);
  }

  private File resolveSaveStocksFile(SaveMeta selectedSave) {
    if (selectedSave == null || selectedSave.saveDir() == null) {
      return null;
    }
    File stocksFile = selectedSave.saveDir().resolve("stocks.csv").toFile();
    return stocksFile.isFile() ? stocksFile : null;
  }

  private void openStockDataToolsFromSettings(Parent settingsPage) {
    final Parent[] stockToolsPageRef = new Parent[1];
    Runnable returnToStockTools = () -> {
      if (stockToolsPageRef[0] != null) {
        navigateKeepMusic(stockToolsPageRef[0]);
      }
    };

    Parent stockToolsPage = CustomStocksView.build(
        withBack(() -> navigateKeepMusic(settingsPage)),
        () -> openBlankCsvEditorStandalone(returnToStockTools),
        file -> openCsvEditorFromImportStandalone(file, returnToStockTools),
        null,
        csvResource -> openCsvEditorFromBuiltInMarketStandalone(csvResource, returnToStockTools),
        meta -> openCsvEditorFromSaveMetaStandalone(meta, returnToStockTools),
        currentSavePath,
      null,
      () -> sfxController.play(SfxController.SELECT));
    stockToolsPageRef[0] = stockToolsPage;
    navigateKeepMusic(stockToolsPage);
    fadeInPage(stockToolsPage);
  }

  private void applyEditedMarketToCurrentGame(List<CsvRow> rows, File exportFile) {
    if (currentPlayer == null || currentExchange == null) {
      overlayService.showNotification("Error", "No active game session", false);
      return;
    }
    if (exportFile != null) {
      try {
        StockCsvExporter.writeCsvRows(exportFile.toPath(), rows);
      } catch (IOException e) {
        overlayService.showNotification(NOTIF_TITLE_SAVE_ERROR, MSG_COULD_NOT_SAVE_CSV + e.getMessage(), false);
        return;
      }
    }

    Exchange updatedExchange = new Exchange(currentExchange.getName(), StockCsvLoader.toStocks(rows));
    updatedExchange.setWeek(currentExchange.getWeek());

    Player updatedPlayer;
    try {
      updatedPlayer = rebuildPlayerForEditedExchange(currentPlayer, updatedExchange);
    } catch (IllegalStateException e) {
      overlayService.showNotification(NOTIF_TITLE_EDIT_BLOCKED, e.getMessage(), false);
      return;
    }

    GameUiState preservedUiState = currentGameView != null ? currentGameView.getUiState() : currentUiState;
    buildAndStartGameFromSave(updatedPlayer, updatedExchange, currentSavePath, preservedUiState,
      false);
    currentFlagged = true;
    gameSessionService.setFlagged(true);
    if (exportFile != null) {
      overlayService.showNotification("Saved", "Stock data exported to:\n" + exportFile.getName(), true);
    }
  }

  private Player rebuildPlayerForEditedExchange(Player sourcePlayer, Exchange updatedExchange) {
    Set<String> availableSymbols = new LinkedHashSet<>();
    for (Stock stock : updatedExchange.getStocks()) {
      availableSymbols.add(stock.getSymbol());
    }

    Set<String> requiredSymbols = new LinkedHashSet<>();
    for (Share share : sourcePlayer.getPortfolio().getShares()) {
      requiredSymbols.add(share.getStock().getSymbol());
    }
    for (Transaction tx : sourcePlayer.getTransactionArchive().getAll()) {
      requiredSymbols.add(tx.getShare().getStock().getSymbol());
    }

    List<String> missingSymbols = requiredSymbols.stream()
        .filter(symbol -> !availableSymbols.contains(symbol))
        .toList();
    if (!missingSymbols.isEmpty()) {
      throw new IllegalStateException(
          "You can't remove or rename symbols that exist in this save: " + String.join(", ", missingSymbols));
    }

    Player rebuiltPlayer = new Player(sourcePlayer.getName(), sourcePlayer.getStartingMoney());
    rebuiltPlayer.setMoney(sourcePlayer.getMoney());
    rebuiltPlayer.setStatus(sourcePlayer.getStatus());
    rebuiltPlayer.setProfileAvatar(sourcePlayer.getProfileAvatar());
    rebuiltPlayer.setWeeksUsingChickAvatar(sourcePlayer.getWeeksUsingChickAvatar());
    rebuiltPlayer.setWeeklySnapshots(sourcePlayer.getWeeklySnapshots());

    for (Share share : sourcePlayer.getPortfolio().getShares()) {
      rebuiltPlayer.getPortfolio().addShare(remapShare(share, updatedExchange));
    }
    for (Transaction tx : sourcePlayer.getTransactionArchive().getAll()) {
      Share remappedShare = remapShare(tx.getShare(), updatedExchange);
      Transaction rebuiltTx = tx instanceof Purchase
          ? TransactionFactory.createPurchase(remappedShare, tx.getWeek())
          : TransactionFactory.createSale(remappedShare, tx.getWeek());
      rebuiltPlayer.getTransactionArchive().add(rebuiltTx);
    }

    return rebuiltPlayer;
  }

  private Share remapShare(Share sourceShare, Exchange updatedExchange) {
    Stock updatedStock = updatedExchange.getStock(sourceShare.getStock().getSymbol());
    return new Share(updatedStock, sourceShare.getQuantity(), sourceShare.getPurchasePrice());
  }

  private void buildAndStartGame(String name, double cash, List<Stock> stocks, boolean fromEditor,
      String exchangeName) {
    if (cash == 0) {
      showNoGamePage();
      return;
    }
    if (stocks.isEmpty()) {
      showNoGamePage(fromEditor);
      return;
    }
    Player player = new Player(
        name == null || name.isBlank() ? "Player" : name,
        BigDecimal.valueOf(cash));
    player.setProfileAvatar(currentProfileAvatar);
    Exchange exchange = new Exchange(exchangeName, stocks);
    buildAndStartGameFromSave(player, exchange, null, null, true);
  }

  private void buildAndStartGameFromSave(Player player, Exchange exchange,
      java.nio.file.Path savePath, GameUiState uiState, boolean fromFreshGameFlow) {
    if (exchange.getStocks().isEmpty()) {
      showNoGamePage(false);
      return;
    }
    Player previousPlayer = currentPlayer;
    Exchange previousExchange = currentExchange;
    String previousAutosaveId = currentAutosaveId;

    gameSessionService.startSession(
        previousPlayer,
        previousExchange,
        previousAutosaveId,
        player,
        exchange,
        savePath);
    currentPlayer = gameSessionService.getPlayer();
    currentExchange = gameSessionService.getExchange();
    currentSavePath = gameSessionService.getSavePath();
    currentProfileAvatar = gameSessionService.getProfileAvatar();
    currentAutosaveId = gameSessionService.getAutosaveId();
    currentFlagged = gameSessionService.isFlagged();
    GameController gameController = new GameController(player, exchange);
    final Runnable[] onGameProfileRef = new Runnable[1];
    final Runnable[] onGameSettingsRef = new Runnable[1];

    onGameProfileRef[0] = () -> {
      sfxController.play(SfxController.PROFILE);
      currentProfileAvatar = currentGameController.getSelectedPlayerAvatar();
      gameSessionService.setProfileAvatar(currentProfileAvatar);
      navigateKeepMusic(buildProfileView(
          () -> {
            sfxController.play(SfxController.BACK,
                Math.min(sfxController.getVolume() * 1.5, 1.0));
            if (currentGameView != null) {
              currentGameView.updateData();
            }
            navigateKeepMusic(currentGamePage);
          },
          this::performSave));
    };
    onGameSettingsRef[0] = () -> {
      sfxController.play(SfxController.SETTINGS);
      boolean perfModeAtOpen = performanceModeEnabled;
      int maxHistoryAtOpen = maxHistoryWeeks;
      navigateKeepMusic(buildSettingsView(
          () -> {
            boolean perfChanged = perfModeAtOpen != performanceModeEnabled
                || maxHistoryAtOpen != maxHistoryWeeks;
            if (perfChanged) {
              rebuildCurrentGameViewForPerformance();
            }
            navigateKeepMusic(currentGamePage);
          },
          this::performSave));
    };

    GameView gameview = (uiState == null)
        ? new GameView(
            gameController,
            withBack(this::goHome),
            onGameProfileRef[0],
            onGameSettingsRef[0],
            () -> sfxController.play(SfxController.SELECT),
            () -> sfxController.play(SfxController.SELECT),
            sfxController::getVolume,
            null,
            fromFreshGameFlow && showTutorialEnabled,
            enabled -> {
              showTutorialEnabled = enabled;
              saveSettings();
            },
            this::restartCurrentGameInPlace)
        : new GameView(
            gameController,
            withBack(this::goHome),
            onGameProfileRef[0],
            onGameSettingsRef[0],
            () -> sfxController.play(SfxController.SELECT),
            () -> sfxController.play(SfxController.SELECT),
            sfxController::getVolume,
            uiState,
            fromFreshGameFlow && showTutorialEnabled,
            enabled -> {
              showTutorialEnabled = enabled;
              saveSettings();
            },
            this::restartCurrentGameInPlace);
    currentGameController = gameController;
    currentGameView = gameview;
    gameview.setMusicFilterCallbacks(
        homePageMusicController::applyLowPassFilter,
        homePageMusicController::removeFilter);
    currentGamePage = gameview.getRoot();
    playGameEntryAudio();
    navigateToGame(currentGamePage);
    if (autosaveEnabled) {
      startAutosaveTimer();
    }
  }

  private void restartCurrentGameInPlace() {
    if (currentPlayer == null || currentExchange == null) {
      return;
    }

    List<Stock> resetStocks = new ArrayList<>();
    for (Stock stock : currentExchange.getStocks()) {
      List<BigDecimal> history = stock.getHistoricalPrices();
      BigDecimal openingPrice = history.isEmpty() ? stock.getSalesPrice() : history.get(0);
      Stock resetStock = new Stock(
          stock.getSymbol(),
          stock.getCompany(),
          new ArrayList<>(List.of(openingPrice)));
      resetStock.setVolatility(stock.getVolatility());
      resetStocks.add(resetStock);
    }

    Player resetPlayer = new Player(currentPlayer.getName(), currentPlayer.getStartingMoney());
    resetPlayer.setProfileAvatar(currentPlayer.getProfileAvatar());
    resetPlayer.setWeeksUsingChickAvatar(currentPlayer.getWeeksUsingChickAvatar());

    Exchange resetExchange = new Exchange(currentExchange.getName(), resetStocks);
    buildAndStartGameFromSave(resetPlayer, resetExchange, null, null, false);
  }

  private void rebuildCurrentGameViewForPerformance() {
    if (currentGameController == null) {
      return;
    }

    currentGameController.refreshReplaySeriesForSettingsChange();

    GameUiState preservedUiState = currentGameView != null ? currentGameView.getUiState() : null;
    Runnable onGameProfile = () -> {
      sfxController.play(SfxController.PROFILE);
      currentProfileAvatar = currentGameController.getSelectedPlayerAvatar();
      gameSessionService.setProfileAvatar(currentProfileAvatar);
      navigateKeepMusic(buildProfileView(
          () -> {
            sfxController.play(SfxController.BACK,
                Math.min(sfxController.getVolume() * 1.5, 1.0));
            if (currentGameView != null) {
              currentGameView.updateData();
            }
            navigateKeepMusic(currentGamePage);
          },
          this::performSave));
    };
    Runnable onGameSettings = () -> navigateKeepMusic(buildSettingsView(
        () -> navigateKeepMusic(currentGamePage),
        this::performSave));

    GameView refreshed = new GameView(
        currentGameController,
        withBack(this::goHome),
        onGameProfile,
        onGameSettings,
        () -> sfxController.play(SfxController.SELECT),
        () -> sfxController.play(SfxController.SELECT),
        sfxController::getVolume,
        preservedUiState,
        false,
        enabled -> {
          showTutorialEnabled = enabled;
          saveSettings();
        },
        this::restartCurrentGameInPlace);
    currentGameView = refreshed;
    refreshed.setMusicFilterCallbacks(
        homePageMusicController::applyLowPassFilter,
        homePageMusicController::removeFilter);
    currentGamePage = refreshed.getRoot();
  }

  private void performSave() {
    if (currentPlayer == null || currentExchange == null) {
      return;
    }
    GameUiState uiState = currentGameView != null ? currentGameView.getUiState() : null;
    boolean flagged = currentFlagged
        || (currentGameController != null && currentGameController.hasDevModeMutationsUsed());
    try {
      if (currentSavePath != null) {
        GameSaveExporter.overwrite(currentSavePath, currentPlayer, currentExchange, uiState,
            flagged);
      } else {
        currentSavePath = GameSaveExporter.save(currentPlayer, currentExchange, uiState,
            flagged);
        gameSessionService.setSavePath(currentSavePath);
      }
      currentFlagged = flagged;
      gameSessionService.setFlagged(flagged);
      overlayService.showNotification("Game Saved", "Your progress has been saved.", true);
    } catch (IOException e) {
      overlayService.showNotification("Save Failed", "Could not save the game:\n" + e.getMessage(), false);
    }
  }

  private void performAutosave() {
    if (currentPlayer == null || currentExchange == null) {
      return;
    }
    GameUiState uiState = currentGameView != null ? currentGameView.getUiState() : null;
    boolean flagged = currentFlagged
        || (currentGameController != null && currentGameController.hasDevModeMutationsUsed());
    try {
      GameSaveExporter.autosave(currentPlayer, currentExchange, uiState, currentAutosaveId,
          flagged);
      currentFlagged = flagged;
      gameSessionService.setFlagged(flagged);
      if (autosaveToast) {
        overlayService.showTimedNotification("Autosaved", "Progress autosaved.", true);
      }
    } catch (IOException e) {
      overlayService.showNotification("Autosave Failed", "Could not autosave:\n" + e.getMessage(), false);
    }
  }

  private void startAutosaveTimer() {
    stopAutosaveTimer();
    autosaveTimer = new Timeline(new KeyFrame(Duration.minutes(1), e -> performAutosave()));
    autosaveTimer.setCycleCount(Animation.INDEFINITE);
    autosaveTimer.play();
  }

  private void stopAutosaveTimer() {
    if (autosaveTimer != null) {
      autosaveTimer.stop();
      autosaveTimer = null;
    }
  }

  private void showNoGamePage() {
    Parent page = NoGameView.build(NoGameView.noCashMonologue(), NoGameView.MSG_NO_CASH, true,
        withBack(this::goHome));
    navigateToGame(page);
    Platform.runLater(() -> {
      if (musicMuted) {
        homePageMusicController.stop();
      } else {
        homePageMusicController.fadeOutThenPlayAmbienceStartingWith(
            "/audio/music/ambience/Ambience3.mp3");
      }
    });
  }

  private void showNoGamePage(boolean fromEditor) {
    String ctx = fromEditor ? NoGameView.MSG_SKIPPED : NoGameView.MSG_EMPTY;
    Parent page = NoGameView.build(NoGameView.noStocksMonologue(), ctx, true, withBack(this::goHome));
    navigateToGame(page);
    Platform.runLater(() -> {
      if (musicMuted) {
        homePageMusicController.stop();
      } else {
        homePageMusicController.fadeOutThenPlayAmbienceStartingWith(
            "/audio/music/ambience/Ambience3.mp3");
      }
    });
  }

  private Parent buildSettingsView(Runnable onBack, Runnable onSave) {
    final Parent[] settingsPageRef = new Parent[1];
    SettingsController ctrl = new SettingsController(
        () -> {
          sfxController.play(SfxController.BACK, Math.min(sfxController.getVolume() * 1.5, 1.0));
          onBack.run();
        },
        primaryStage);

    // ── Audio ────────────────────────────────────────────────────────────
    ctrl.musicVolume = musicVolume;
    ctrl.musicMuted = musicMuted;
    ctrl.onMusicVolumeChange = vol -> {
      musicVolume = vol;
      homePageMusicController.setVolume(musicMuted ? 0.0 : vol);
      saveSettings();
    };
    ctrl.onMusicMutedChange = muted -> {
      boolean mutedValue = Boolean.TRUE.equals(muted);
      playSettingsToggleSfx(!mutedValue);
      musicMuted = mutedValue;
      if (mutedValue) homePageMusicController.stop();
      else resumeMusicForContext();
      saveSettings();
    };
    ctrl.sfxVolume = sfxVolume;
    ctrl.sfxMuted = sfxMuted;
    ctrl.onSfxVolumeChange = vol -> {
      sfxVolume = vol;
      sfxController.setVolume(sfxMuted ? 0.0 : vol);
      saveSettings();
    };
    ctrl.onSfxMutedChange = muted -> {
      boolean mutedValue = Boolean.TRUE.equals(muted);
      playSettingsToggleSfx(!mutedValue);
      sfxMuted = mutedValue;
      sfxController.setVolume(mutedValue ? 0.0 : sfxVolume);
      saveSettings();
    };

    // ── Visual ───────────────────────────────────────────────────────────
    ctrl.animationsEnabled = animationsEnabled;
    ctrl.onAnimationsChange = enabled -> {
      playSettingsToggleSfx(enabled);
      animationsEnabled = enabled;
      backgroundCanvas.setAnimationsEnabled(enabled);
      saveSettings();
    };
    ctrl.fullscreenEnabled = fullscreenEnabled;
    ctrl.onFullscreenChange = enabled -> {
      playSettingsToggleSfx(enabled);
      fullscreenEnabled = enabled;
      primaryStage.setFullScreen(enabled);
      saveSettings();
    };

    // ── Gameplay / performance ────────────────────────────────────────────
    ctrl.devModeEnabled = devModeEnabled;
    ctrl.onDevModeChange = enabled -> {
      playSettingsToggleSfx(enabled);
      devModeEnabled = enabled;
      AppConfig.DEV_MODE.set(enabled);
      saveSettings();
    };
    ctrl.autosaveEnabled = autosaveEnabled;
    ctrl.onAutosaveChange = enabled -> {
      boolean enabledValue = Boolean.TRUE.equals(enabled);
      playSettingsToggleSfx(enabledValue);
      autosaveEnabled = enabledValue;
      if (enabledValue) startAutosaveTimer();
      else stopAutosaveTimer();
      saveSettings();
    };
    ctrl.autosaveToast = autosaveToast;
    ctrl.onAutosaveToastChange = enabled -> {
      playSettingsToggleSfx(enabled);
      autosaveToast = enabled;
      saveSettings();
    };
    ctrl.showTutorial = showTutorialEnabled;
    ctrl.onShowTutorialChange = enabled -> {
      playSettingsToggleSfx(enabled);
      showTutorialEnabled = enabled;
      saveSettings();
    };
    ctrl.performanceModeEnabled = performanceModeEnabled;
    ctrl.onPerformanceModeChange = enabled -> {
      playSettingsToggleSfx(enabled);
      performanceModeEnabled = enabled;
      AppConfig.PERFORMANCE_MODE.set(enabled);
      rebuildCurrentGameViewForPerformance();
      saveSettings();
    };
    ctrl.maxHistoryWeeks = maxHistoryWeeks;
    ctrl.onMaxHistoryWeeksChange = weeks -> {
      maxHistoryWeeks = Math.max(50, weeks);
      AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.set(maxHistoryWeeks);
      rebuildCurrentGameViewForPerformance();
      saveSettings();
    };

    // Export from Settings should work in both home/setup and in-game contexts.
    ctrl.onExportJsonCsv = this::exportSaveDataFromSettings;
    ctrl.onExportCsvOnly = this::exportSaveCsvFromSettings;

    // ── Reset all ────────────────────────────────────────────────────────
    ctrl.onResetAll = () -> {
      musicVolume = GlobalSettingsManager.DEFAULT_MUSIC_VOLUME;
      sfxVolume = GlobalSettingsManager.DEFAULT_SFX_VOLUME;
      animationsEnabled = GlobalSettingsManager.DEFAULT_ANIMATIONS;
      musicMuted = GlobalSettingsManager.DEFAULT_MUSIC_MUTED;
      sfxMuted = GlobalSettingsManager.DEFAULT_SFX_MUTED;
      devModeEnabled = GlobalSettingsManager.DEFAULT_DEV_MODE;
      autosaveEnabled = GlobalSettingsManager.DEFAULT_AUTOSAVE;
      autosaveToast = GlobalSettingsManager.DEFAULT_AUTOSAVE_TOAST;
      showTutorialEnabled = GlobalSettingsManager.DEFAULT_SHOW_TUTORIAL;
      performanceModeEnabled = GlobalSettingsManager.DEFAULT_PERFORMANCE_MODE;
      maxHistoryWeeks = GlobalSettingsManager.DEFAULT_MAX_HISTORY_WEEKS;
      fullscreenEnabled = GlobalSettingsManager.DEFAULT_FULLSCREEN;
      windowWidth = GlobalSettingsManager.DEFAULT_WINDOW_WIDTH;
      windowHeight = GlobalSettingsManager.DEFAULT_WINDOW_HEIGHT;
      AppConfig.PERFORMANCE_MODE.set(performanceModeEnabled);
      AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.set(maxHistoryWeeks);
      AppConfig.DEV_MODE.set(false);
      if (musicMuted) homePageMusicController.stop();
      else resumeMusicForContext();
      sfxController.setVolume(sfxVolume);
      backgroundCanvas.setAnimationsEnabled(animationsEnabled);
      primaryStage.setFullScreen(false);
      primaryStage.setMaximized(true);
      if (autosaveEnabled) startAutosaveTimer();
      else stopAutosaveTimer();
      saveSettings();
      navigateKeepMusic(buildSettingsView(onBack, onSave));
    };

    // Resolution controls should work in all settings contexts.
    ctrl.onResolutionChange = dims -> {
      primaryStage.setFullScreen(false);
      primaryStage.setMaximized(false);
      primaryStage.setWidth(dims[0]);
      primaryStage.setHeight(dims[1]);
    };
    ctrl.onMaximize = () -> {
      primaryStage.setFullScreen(false);
      primaryStage.setMaximized(true);
    };

    // ── In-game only ──────────────────────────────────────────────────────
    if (onSave != null) {
      ctrl.currentSavePath = currentSavePath;
      ctrl.onSave = () -> {
        onSave.run();
        navigateKeepMusic(buildSettingsView(onBack, onSave));
      };
      ctrl.currentPlayerName = currentGameController != null
          ? currentGameController.getPlayerName() : null;
      ctrl.onNameChanged = currentGameController != null
          ? name -> currentGameController.setPlayerName(name) : null;
      ctrl.onOpenCsvTools = () -> {
        if (settingsPageRef[0] != null) {
          openStockDataToolsFromSettings(settingsPageRef[0]);
        }
      };
      ctrl.onEditCurrentMarketData = () -> {
        if (settingsPageRef[0] != null) {
          openCurrentMarketCsvEditorForGame(() -> navigateKeepMusic(settingsPageRef[0]));
        }
      };
    } else {
      ctrl.onOpenCsvTools = () -> {
        if (settingsPageRef[0] != null) {
          openStockDataToolsFromSettings(settingsPageRef[0]);
        }
      };
    }

    Parent settingsPage = SettingsView.build(ctrl);
    settingsPageRef[0] = settingsPage;
    return settingsPage;
  }

  private String exportSaveDataFromSettings(SaveMeta saveMeta, boolean latestOnly) {
    if (saveMeta == null) {
      return null;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Save Data (JSON + CSV)");
    chooser.setInitialFileName(
        saveMeta.displayName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_save_export");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
    File destination = chooser.showSaveDialog(primaryStage);
    if (destination == null) {
      return null;
    }
    try {
      GameSaveExporter.exportSaveDataFiles(saveMeta.saveDir(), destination.toPath(), latestOnly);
      return destination.getName();
    } catch (IOException ex) {
      overlayService.showNotification("Export Failed", "Export failed: " + ex.getMessage(), false);
      throw new UncheckedIOException(ex);
    }
  }

  private String exportSaveCsvFromSettings(SaveMeta saveMeta, boolean latestOnly) {
    if (saveMeta == null) {
      return null;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Market CSV");
    chooser.setInitialFileName(
        saveMeta.displayName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_market_data");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
    File destination = chooser.showSaveDialog(primaryStage);
    if (destination == null) {
      return null;
    }
    try {
      GameSaveExporter.exportSaveCsvFile(saveMeta.saveDir(), destination.toPath(), latestOnly);
      return destination.getName();
    } catch (IOException ex) {
      overlayService.showNotification("Export Failed", "Export failed: " + ex.getMessage(), false);
      throw new UncheckedIOException(ex);
    }
  }

  private Parent buildProfileView(Runnable onBackToGame, Runnable onSave) {
    if (currentGameController == null) {
      return buildSettingsView(onBackToGame, onSave);
    }
    ProfileController profileController = new ProfileController(
        currentGameController,
        () -> currentGameView == null ? List.of() : currentGameView.getFavoriteSymbolsSnapshot(),
        symbol -> {
          if (currentGameView != null) {
            currentGameView.toggleFavoriteSymbol(symbol);
            onSave.run();
          }
        },
        symbol -> currentGameView != null && currentGameView.isFavoriteSymbol(symbol));
    Runnable openSettingsFromProfile = () -> {
      sfxController.play(SfxController.SETTINGS);
      navigateKeepMusic(buildSettingsView(
          () -> {
            currentGameController.refreshReplaySeriesForSettingsChange();
            navigateKeepMusic(buildProfileView(onBackToGame, onSave));
          },
          onSave));
    };
    return ProfileView.build(
        profileController,
        onBackToGame,
        openSettingsFromProfile,
        () -> sfxController.play(SfxController.SELECT),
        currentProfileAvatar,
        avatar -> {
          currentProfileAvatar = avatar;
          gameSessionService.setProfileAvatar(avatar);
          currentGameController.setPlayerAvatar(avatar);
          if (currentGameView != null) {
            currentGameView.updateData();
          }
        },
        name -> {
          currentGameController.setPlayerName(name);
          if (currentGameView != null) {
            currentGameView.updateData();
          }
        },
        symbol -> {
          navigateKeepMusic(currentGamePage);
          if (currentGameView != null) {
            currentGameView.selectStockBySymbol(symbol);
          }
        });
  }

  private void toggleFullscreen() {
    fullscreenEnabled = !primaryStage.isFullScreen();
    primaryStage.setFullScreen(fullscreenEnabled);
    saveSettings();
  }

  private void saveSettings() {
    // Only persist a custom size when we actually have one (non-fullscreen,
    // non-maximised)
    int savedW = (primaryStage.isFullScreen() || primaryStage.isMaximized()) ? windowWidth
        : (int) primaryStage.getWidth();
    int savedH = (primaryStage.isFullScreen() || primaryStage.isMaximized()) ? windowHeight
        : (int) primaryStage.getHeight();
    GlobalSettingsManager.save(new GlobalSettingsManager.Settings(
        musicVolume,
        sfxVolume,
        animationsEnabled,
        musicMuted,
        sfxMuted,
        autosaveEnabled,
        autosaveToast,
        showTutorialEnabled,
        fullscreenEnabled,
        devModeEnabled,
          performanceModeEnabled,
          maxHistoryWeeks,
        savedW,
        savedH));
  }

  private void playSettingsToggleSfx(boolean nowOn) {
    if (nowOn) {
      sfxController.play(SfxController.SETTINGS_ON, sfxController.getVolume() * 0.75);
    } else {
      sfxController.play(SfxController.SETTINGS_OFF);
    }
  }

  // ── Music-aware navigation primitives ────────────────────────────────────

  private <T> void runWithLoadingOverlay(
      Supplier<T> work,
      Consumer<T> onSuccess,
      Consumer<Throwable> onError) {
    Task<T> task = new Task<>() {
      @Override
      protected T call() {
        return work.get();
      }
    };

    task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
    task.setOnFailed(e -> onError.accept(task.getException()));

    Thread t = new Thread(task, "ui-background-loader");
    t.setDaemon(true);
    t.start();
  }

  private void navigateToGame(Parent page) {
    gameAudioContext = true;
    navigationCoordinator.navigate(page);
  }

  /**
   * Swap page without touching music (home, setup, CSV, settings contexts).
   */
  private void navigateKeepMusic(Parent page) {
    navigationCoordinator.navigate(page);
    if (!gameAudioContext) {
      updateHomeThemeContextLoudness(page, true);
    }
  }

  /**
   * Fade a page in from opacity 0 — use only when coming from the home page.
   */
  private void fadeInPage(Parent page) {
    navigationCoordinator.fadeIn(page);
  }

  /**
   * Fade the current page out, then run the navigation action.
   */
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

  /**
   * Return home from game: fade ambience out, restart home music.
   */
  private void goHome() {
    if (currentGameView != null) {
      currentUiState = currentGameView.getUiState();
    }
    stopAutosaveTimer();
    if (autosaveEnabled) {
      performAutosave();
    }
    gameAudioContext = false;
    homePageMusicController.setContextVolumeMultiplier(LANDING_THEME_CONTEXT_MULTIPLIER, false);
    if (musicMuted) {
      homePageMusicController.stop();
    } else {
      homePageMusicController.fadeOutThenPlay(null, null);
    }
    fadeOutThenNavigate(() -> root.getChildren().setAll(backgroundCanvas, homePage));
  }

  /**
   * Return home from non-game pages: no music change.
   */
  private void goHomeKeepMusic() {
    gameAudioContext = false;
    fadeOutThenNavigate(() -> {
      root.getChildren().setAll(backgroundCanvas, homePage);
      updateHomeThemeContextLoudness(homePage, true);
    });
  }

  private void playGameEntryAudio() {
    if (musicMuted && sfxMuted) {
      homePageMusicController.stop();
      return;
    }

    double startSfxVolume = sfxMuted ? 0.0 : sfxController.getVolume();
    homePageMusicController.playGameStartThenAmbience(startSfxVolume, !musicMuted);
  }

  private void resumeMusicForContext() {
    homePageMusicController.setVolume(musicVolume);
    if (gameAudioContext) {
      homePageMusicController.playAmbience();
    } else {
      Parent currentPage = getCurrentPage();
      updateHomeThemeContextLoudness(currentPage == null ? homePage : currentPage, false);
      homePageMusicController.play(null, null);
    }
  }

  private Parent getCurrentPage() {
    if (root == null || root.getChildren().size() < 2) {
      return null;
    }
    return (Parent) root.getChildren().get(root.getChildren().size() - 1);
  }

  private void updateHomeThemeContextLoudness(Parent page, boolean smooth) {
    double contextMultiplier = page == homePage
        ? LANDING_THEME_CONTEXT_MULTIPLIER
        : NON_LANDING_THEME_CONTEXT_MULTIPLIER;
    homePageMusicController.setContextVolumeMultiplier(contextMultiplier, smooth);
  }

  private static boolean isTextInputTarget(Object target) {
    if (!(target instanceof Node node)) {
      return false;
    }
    for (Node current = node; current != null; current = current.getParent()) {
      if (current instanceof TextInputControl) {
        return true;
      }
    }
    return false;
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
    if (autosaveEnabled) {
      performAutosave();
    }
    saveSettings(); // persist final window size
    if (homePageMusicController != null) {
      homePageMusicController.stop();
    }
    if (backgroundCanvas != null) {
      backgroundCanvas.stop();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
