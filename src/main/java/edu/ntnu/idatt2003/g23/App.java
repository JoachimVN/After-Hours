package edu.ntnu.idatt2003.g23;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import edu.ntnu.idatt2003.g23.ui.BackgroundCanvas;
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
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.text.Font;
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

  private StackPane root;
  private Parent homePage;
  private HomePageMusicController homePageMusicController;
  private BackgroundCanvas backgroundCanvas;
  private AppOverlayService overlayService;

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
  private boolean gameAudioContext = false;

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
    performanceModeEnabled = gs.performanceMode();
    maxHistoryWeeks = gs.maxHistoryWeeks();
    fullscreenEnabled = gs.fullscreen();
    windowWidth = gs.windowWidth();
    windowHeight = gs.windowHeight();

    AppConfig.DEV_MODE.set(gs.devMode());
    AppConfig.PERFORMANCE_MODE.set(performanceModeEnabled);
    AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.set(maxHistoryWeeks);
    primaryStage = stage;
    homePage = LandingPageView.build(
        () -> {
          sfxController.play(SfxController.PLAY, Math.min(sfxController.getVolume() * 1.5, 1.0));
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
        getClass().getResource("/css/scrollbar.css").toExternalForm());

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
      homePageMusicController.play(
          splashOverlayController::fadeAfterStartup,
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
    SaveSelectController ctrl = new SaveSelectController(
        () -> {
          sfxController.play(SfxController.PLAY2, Math.min(sfxController.getVolume() * 1.5, 1.0));
          goToSetup();
        },
        withBack(this::goHomeKeepMusic),
        this::loadFromSave,
        currentPlayer,
        currentExchange,
        currentSavePath,
        currentUiState);
    Parent saveSelectPage = new SaveSelectView(ctrl).getRoot();
    navigateKeepMusic(saveSelectPage);
    fadeInPage(saveSelectPage);
  }

  private void loadFromSave(Object[] data) {
    Player player = (Player) data[0];
    Exchange exchange = (Exchange) data[1];
    java.nio.file.Path savePath = (java.nio.file.Path) data[2];
    GameUiState uiState = data.length > 3 ? (GameUiState) data[3] : null;
    buildAndStartGameFromSave(player, exchange, savePath, uiState);
  }

  private void goToSetup() {
    // Starting a new game — clear the in-memory session
    currentPlayer = null;
    currentExchange = null;
    currentSavePath = null;
    currentGameController = null;
    currentUiState = null;
    currentProfileAvatar = "bust-in-silhouette";
    currentSetupPage = new SetupView(
        withBack(this::goToSaveSelect),
        (name, cash, csvResource) -> startGame(name, cash, csvResource),
        (name, cash) -> {
          sfxController.play(SfxController.PLAY3, Math.min(sfxController.getVolume() * 1.5, 1.0));
          goToCustomStocks(name, cash);
        },
        currentProfileAvatar).getRoot();
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
        selectedFile);
    navigateKeepMusic(importPage);
    fadeInPage(importPage);
  }

  private void startGame(String name, double cash, String csvResource) {
    runWithLoadingOverlay(
        "Loading Market",
        "Parsing stock data...",
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
        error -> overlayService.showNotification("CSV Error", "Could not load market data:\n" + error.getMessage(), false));
  }

  private void startGameWithCsv(String name, double cash, File csvFile) {
    runWithLoadingOverlay(
        "Importing CSV",
        "Reading and validating file...",
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
            buildAndStartGame(name, cash, StockCsvLoader.toStocks(result.getRows()), false, "Custom Market");
          }
        },
        error -> overlayService.showNotification("CSV Error", "Could not read file:\n" + error.getMessage(), false));
  }

  private void openCsvEditor(CsvParseResult result, String name, double cash) {
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.build(
          result,
          withBack(this::goHomeKeepMusic),
          rows -> buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, "Custom Market"),
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
        "Opening CSV Editor",
        "Parsing CSV data...",
        () -> {
          try {
            return StockCsvLoader.parseWithErrors(new FileReader(csvFile, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> openCsvEditorFromImport(result, name, cash, csvFile),
        error -> overlayService.showNotification("CSV Error", "Could not read file:\n" + error.getMessage(), false));
  }

  private void openCsvEditorFromImport(CsvParseResult result, String name, double cash,
      File selectedFile) {
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.build(
          result,
          withBack(() -> goToCustomStocks(name, cash, selectedFile)),
          rows -> buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, "Custom Market"),
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
        "Loading Market",
        "Parsing stock data...",
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
        error -> overlayService.showNotification("CSV Error", "Could not load market data:\n" + error.getMessage(), false));
  }

  private void openCsvEditorFromSaveMeta(SaveMeta selectedSave, String name, double cash) {
    File saveStocksFile = resolveSaveStocksFile(selectedSave);
    if (saveStocksFile == null) {
      overlayService.showNotification("Save Error", "Could not locate stocks.csv for that save.", false);
      return;
    }
    openCsvEditorFromImport(saveStocksFile, name, cash);
  }

  private void saveCsvRowsAndStartGame(String name, double cash,
      List<edu.ntnu.idatt2003.g23.io.CsvRow> rows, File file) {
    try {
      edu.ntnu.idatt2003.g23.io.StockCsvExporter.writeCsvRows(file.toPath(), rows);
    } catch (IOException e) {
      overlayService.showNotification("Save Error", "Could not save CSV:\n" + e.getMessage(), false);
      return;
    }
    buildAndStartGame(name, cash, StockCsvLoader.toStocks(rows), true, "Custom Market");
  }

  private void openBlankCsvEditorStandalone(Runnable onBack) {
    openCsvEditorStandalone(new CsvParseResult(List.of()), onBack,
        () -> openBlankCsvEditorStandalone(onBack));
  }

  private void openCsvEditorStandalone(CsvParseResult result, Runnable onBack,
      Runnable onReset) {
    LoadStats stats = CsvEditorLoadAnalyzer.analyze(result);
    Runnable doOpen = () -> {
      Parent editorPage = CsvEditorView.buildStandalone(
          result,
          withBack(onBack),
          (editedRows, file) -> {
            try {
              StockCsvExporter.writeCsvRows(file.toPath(), editedRows);
              overlayService.showNotification("Saved", "Stock data exported to:\n" + file.getName(), true);
              onBack.run();
            } catch (IOException e) {
              overlayService.showNotification("Save Error", "Could not save CSV:\n" + e.getMessage(), false);
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
    runWithLoadingOverlay(
        "Opening CSV Editor",
        "Parsing CSV data...",
        () -> {
          try {
            return StockCsvLoader.parseWithErrors(new FileReader(csvFile, StandardCharsets.UTF_8));
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
          }
        },
        result -> openCsvEditorStandalone(result, onBack,
            () -> openCsvEditorFromImportStandalone(csvFile, onBack)),
        error -> overlayService.showNotification("CSV Error", "Could not read file:\n" + error.getMessage(), false));
  }

  private void openCsvEditorFromBuiltInMarketStandalone(String csvResource, Runnable onBack) {
    runWithLoadingOverlay(
        "Loading Market",
        "Parsing stock data...",
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
        error -> overlayService.showNotification("CSV Error", "Could not load market data:\n" + error.getMessage(), false));
  }

  private void openCsvEditorFromSaveMetaStandalone(SaveMeta selectedSave, Runnable onBack) {
    File saveStocksFile = resolveSaveStocksFile(selectedSave);
    if (saveStocksFile == null) {
      overlayService.showNotification("Save Error", "Could not locate stocks.csv for that save.", false);
      return;
    }
    openCsvEditorFromImportStandalone(saveStocksFile, onBack);
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
          (rows, file) -> applyEditedMarketToCurrentGame(rows, file),
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
    List<CsvRow> rows = new ArrayList<>();
    int lineNum = 1;
    for (Stock stock : currentExchange.getStocks()) {
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
        null);
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
        overlayService.showNotification("Save Error", "Could not save CSV:\n" + e.getMessage(), false);
        return;
      }
    }

    Exchange updatedExchange = new Exchange(currentExchange.getName(), StockCsvLoader.toStocks(rows));
    updatedExchange.setWeek(currentExchange.getWeek());

    Player updatedPlayer;
    try {
      updatedPlayer = rebuildPlayerForEditedExchange(currentPlayer, updatedExchange);
    } catch (IllegalStateException e) {
      overlayService.showNotification("Edit Blocked", e.getMessage(), false);
      return;
    }

    GameUiState preservedUiState = currentGameView != null ? currentGameView.getUiState() : currentUiState;
    buildAndStartGameFromSave(updatedPlayer, updatedExchange, currentSavePath, preservedUiState);
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
    buildAndStartGameFromSave(player, exchange, null, null);
  }

  private void buildAndStartGameFromSave(Player player, Exchange exchange,
      java.nio.file.Path savePath, GameUiState uiState) {
    if (exchange.getStocks().isEmpty()) {
      showNoGamePage(false);
      return;
    }
    Player previousPlayer = currentPlayer;
    Exchange previousExchange = currentExchange;
    String previousAutosaveId = currentAutosaveId;

    currentPlayer = player;
    currentExchange = exchange;
    currentSavePath = savePath;
    currentProfileAvatar = player.getProfileAvatar();
    // Each game instance gets a distinct autosave slot:
    // • loaded saves → use the existing save folder name
    // • new games → use playerName + start timestamp
    if (savePath != null) {
      currentAutosaveId = normalizeAutosaveSlotId(savePath.getFileName().toString());
    } else {
      boolean resumingSameInMemorySession = previousAutosaveId != null
          && previousPlayer == player
          && previousExchange == exchange;
      if (resumingSameInMemorySession) {
        currentAutosaveId = previousAutosaveId;
      } else {
        String safeName = player.getName().replaceAll("[^A-Za-z0-9_\\-]", "_");
        currentAutosaveId = safeName + "_"
            + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
      }
    }
    GameController gameController = new GameController(player, exchange);
    final Runnable[] onGameProfileRef = new Runnable[1];
    final Runnable[] onGameSettingsRef = new Runnable[1];

    onGameProfileRef[0] = () -> {
      sfxController.play(SfxController.PROFILE);
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
            null)
        : new GameView(
            gameController,
            withBack(this::goHome),
            onGameProfileRef[0],
            onGameSettingsRef[0],
            () -> sfxController.play(SfxController.SELECT),
            () -> sfxController.play(SfxController.SELECT),
            sfxController::getVolume,
            uiState);
    currentGameController = gameController;
    currentGameView = gameview;
    currentGamePage = gameview.getRoot();
    playGameEntryAudio();
    navigateToGame(currentGamePage);
    if (autosaveEnabled) {
      startAutosaveTimer();
    }
  }

  private void rebuildCurrentGameViewForPerformance() {
    if (currentGameController == null) {
      return;
    }

    currentGameController.refreshReplaySeriesForSettingsChange();

    GameUiState preservedUiState = currentGameView != null ? currentGameView.getUiState() : null;
    Runnable onGameProfile = () -> {
      sfxController.play(SfxController.PROFILE);
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
        preservedUiState);
    currentGameView = refreshed;
    currentGamePage = refreshed.getRoot();
  }

  private void performSave() {
    if (currentPlayer == null || currentExchange == null) {
      return;
    }
    GameUiState uiState = currentGameView != null ? currentGameView.getUiState() : null;
    try {
      if (currentSavePath != null) {
        GameSaveExporter.overwrite(currentSavePath, currentPlayer, currentExchange, uiState);
      } else {
        currentSavePath = GameSaveExporter.save(currentPlayer, currentExchange, uiState);
      }
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
    try {
      GameSaveExporter.autosave(currentPlayer, currentExchange, uiState, currentAutosaveId);
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
    autosaveTimer.setCycleCount(Timeline.INDEFINITE);
    autosaveTimer.play();
  }

  private void stopAutosaveTimer() {
    if (autosaveTimer != null) {
      autosaveTimer.stop();
      autosaveTimer = null;
    }
  }

  private void showNoGamePage() {
    Parent page = NoGameView.build(NoGameView.NO_CASH_MONOLOGUE, NoGameView.MSG_NO_CASH, true,
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
    Parent page = NoGameView.build(NoGameView.NO_STOCKS_MONOLOGUE, ctx, true, withBack(this::goHome));
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
      playSettingsToggleSfx(!muted);
      musicMuted = muted;
      if (muted) homePageMusicController.stop();
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
      playSettingsToggleSfx(!muted);
      sfxMuted = muted;
      sfxController.setVolume(muted ? 0.0 : sfxVolume);
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
      playSettingsToggleSfx(enabled);
      autosaveEnabled = enabled;
      if (enabled) startAutosaveTimer();
      else stopAutosaveTimer();
      saveSettings();
    };
    ctrl.autosaveToast = autosaveToast;
    ctrl.onAutosaveToastChange = enabled -> {
      playSettingsToggleSfx(enabled);
      autosaveToast = enabled;
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

    // ── In-game only ──────────────────────────────────────────────────────
    if (onSave != null) {
      ctrl.currentSavePath = currentSavePath;
      ctrl.onSave = () -> {
        onSave.run();
        navigateKeepMusic(buildSettingsView(onBack, onSave));
      };
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
      ctrl.onExport = file -> { /* export already completed in view */ };
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

  private String normalizeAutosaveSlotId(String slotId) {
    String safe = slotId == null ? "" : slotId.replaceAll("[^A-Za-z0-9_\\-]", "_");
    while (safe.startsWith("autosave_")) {
      safe = safe.substring("autosave_".length());
    }
    if (safe.isBlank()) {
      return "slot";
    }
    return safe;
  }

  // ── Music-aware navigation primitives ────────────────────────────────────

  private <T> void runWithLoadingOverlay(
      String title,
      String message,
      Supplier<T> work,
      Consumer<T> onSuccess,
      Consumer<Throwable> onError) {
    Task<T> task = new Task<>() {
      @Override
      protected T call() {
        return work.get();
      }
    };

    task.setOnSucceeded(e -> {
      onSuccess.accept(task.getValue());
    });
    task.setOnFailed(e -> {
      onError.accept(task.getException());
    });

    Thread t = new Thread(task, "ui-background-loader");
    t.setDaemon(true);
    t.start();
  }

  private void navigateToGame(Parent page) {
    gameAudioContext = true;
    root.getChildren().setAll(backgroundCanvas, page);
  }

  /**
   * Swap page without touching music (home, setup, CSV, settings contexts).
   */
  private void navigateKeepMusic(Parent page) {
    root.getChildren().setAll(backgroundCanvas, page);
    if (!gameAudioContext) {
      updateHomeThemeContextLoudness(page, true);
    }
  }

  /**
   * Fade a page in from opacity 0 — use only when coming from the home page.
   */
  private void fadeInPage(Parent page) {
    page.setOpacity(0);
    FadeTransition ft = new FadeTransition(Duration.millis(500), page);
    ft.setFromValue(0);
    ft.setToValue(1);
    ft.setInterpolator(Interpolator.EASE_BOTH);
    ft.play();
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
