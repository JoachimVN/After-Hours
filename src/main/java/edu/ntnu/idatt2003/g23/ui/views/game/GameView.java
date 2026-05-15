package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.io.GameUiState;
import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.ui.util.AvatarUtil;
import edu.ntnu.idatt2003.g23.ui.util.CurrencyFormatter;

import static edu.ntnu.idatt2003.g23.ui.util.LabelUtil.labelSmall;

import edu.ntnu.idatt2003.g23.util.NumberParser;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;

public final class GameView implements GameViewInterface {
  private static final String WEEK_ADVANCE_SOUND = "/audio/sfx/Week_Advance.mp3";
  private static final String LEVEL_UP_SOUND = "/audio/sfx/Level_Up.mp3";
  private static final String SPIKE_UP_SOUND = "/audio/sfx/Spike_Up.mp3";
  private static final String SPIKE_DOWN_SOUND = "/audio/sfx/Spike_Down.mp3";
  private static final String SPIKE_BOTH_SOUND = "/audio/sfx/Spike_Both.mp3";
  private static final String ERROR_PAUSE_KEY = "errorPause";
  private static final String ERROR_FADE_KEY = "errorFade";
  private static final String ERROR_SIZE_KEY = "errorSize";
  private static final String ERROR_CONTAINER_KEY = "errorContainer";
  private static final String ERROR_CONTAINER_CLIP_KEY = "errorContainerClip";
  private static final Duration INLINE_ERROR_SIZE_ANIM = Duration.millis(220);
  private static final Duration INLINE_ERROR_VISIBLE = Duration.seconds(4.0);
  private static final Duration INLINE_ERROR_FADE = Duration.millis(600);
  private static final Duration POPUP_ERROR_VISIBLE = Duration.seconds(3.0);
  private static final Duration POPUP_ERROR_FADE = Duration.millis(300);
  private static final Duration SPIKE_POPUP_VISIBLE = Duration.seconds(8.0);
  private static final Duration SPIKE_POPUP_FADE = Duration.millis(220);
  private static final BigDecimal MIN_UPWARD_SPIKE_POPUP_PCT = new BigDecimal("20.00");
  private static final BigDecimal MIN_DOWNWARD_SPIKE_POPUP_PCT = new BigDecimal("-16.67");
  private static final int PROFILE_NAME_MAX_CHARS = 13;

  private final GameController gameController;
  private final Label statusVal;
  private final Label cashVal;
  private final Label portfolioVal;
  private final Label netWorthVal;
  private final Label weekNumLbl;
  private final Arc statusProgressArc;
  private final Tooltip statusTooltip;
  private final Button settingsBtn;
  private final Button profileBtn;
  private final HBox profileIdentityContent;
  private final StackPane profileNameBox;
  private final Text profileNameText;
  private final Runnable onProfile;
  private final Runnable onPanelOpen;
  private final Runnable onStockSelectionChanged;
  private final boolean showTutorialOnStart;
  private final Consumer<Boolean> onShowTutorialPreferenceChange;
  private final Runnable onStartFreshFromTutorial;

  private final ObservableList<Stock> allStocks;
  private final FilteredList<Stock> filteredStocks;
  private final ObservableList<Share> portfolioItems;
  private final ObjectProperty<Stock> selectedStock;
  private final TableView<Share> portfolioTable;

  private final TextField searchField;
  private final boolean performanceMode;
  private final VBox stockListBox;
  private final ScrollPane stockScroll;
  private final SortedList<Stock> sortedStocks;
  private final ListView<Stock> stockListView;
  private final Set<String> favorites;
  private final Set<String> activeFilters;
  private final List<String> filterChipOrder;
  private FlowPane filterChipsPane;
  private String stockSort;
  private TxRow highlightedTx = null;
  private AnimationTimer highlightFadeTimer = null;

  private final VBox detailArea;
  private final VBox spikePopupList;
  private final List<PauseTransition> spikePopupTimers;
  private Label currentTradeErrorLabel;
  private Label currentSellAllErrorLabel;

  private StackPane overlayRef = null;
  private Node rootRef = null;
  private SplitPane hSplitRef = null;
  private double portfolioDividerRatio = 1.0;
  private PlayerStatus lastKnownStatus = null;
  private AudioClip levelUpClip = null;
  private DoubleSupplier sfxVolumeSupplierField = null;
  private StackPane tutorialOverlay = null;
  private Pane tutorialShadeLayer = null;
  private Rectangle tutorialShadeTop = null;
  private Rectangle tutorialShadeLeft = null;
  private Rectangle tutorialShadeRight = null;
  private Rectangle tutorialShadeBottom = null;
  private Rectangle tutorialSpotlightRing = null;
  private final GaussianBlur tutorialBackdropBlur = new GaussianBlur(22);
  private VBox tutorialCard = null;
  private HBox tutorialNavRow = null;
  private HBox tutorialFinishRow = null;
  private Label tutorialTitleLbl = null;
  private Label tutorialBodyLbl = null;
  private Label tutorialStepLbl = null;
  private Button tutorialBackBtn = null;
  private Button tutorialNextBtn = null;
  private Button tutorialCloseBtn = null;
  private Button tutorialKeepProgressBtn = null;
  private Button tutorialStartFreshBtn = null;
  private CheckBox tutorialDontShowAgainToggle = null;
  private int tutorialStepIndex = 0;
  private boolean tutorialDismissedThisSession = false;
  private boolean tutorialDidSelectStock = false;
  private boolean tutorialDidTrade = false;
  private boolean tutorialDidAdvanceWeek = false;
  private boolean tutorialOpenedMovers = false;
  private boolean tutorialTouchedPortfolio = false;
  private int tutorialSuspendDepth = 0;
  private boolean tutorialWasVisibleBeforeSuspend = false;
  private int tutorialStartWeek = 0;
  private int tutorialStartTransactionCount = 0;
  private Node tutorialStockAreaTarget = null;
  private Node tutorialTradeAreaTarget = null;
  private Node tutorialPortfolioAreaTarget = null;
  private Region tutorialPortfolioResizeHandleTarget = null;
  private Node tutorialMoversCardTarget = null;
  private Node tutorialMoversTabBarTarget = null;
  private Button tutorialNextWeekBtnTarget = null;
  private Button tutorialMarketMoversBtnTarget = null;
  private boolean tutorialSpikeScheduled = false;
  private boolean tutorialUseMoversStep = true;
  private boolean tutorialUseTwoStockMoversCopy = false;
  private Runnable musicFilterOn = null;
  private Runnable musicFilterOff = null;

  public void setMusicFilterCallbacks(Runnable onFilter, Runnable offFilter) {
    this.musicFilterOn = onFilter;
    this.musicFilterOff = offFilter;
  }

  public GameView(GameController gameController, Runnable onBack, Runnable onProfile,
                    DoubleSupplier sfxVolumeSupplier) {
        this(gameController, onBack, onProfile, null, null, null, sfxVolumeSupplier, null,
            false, null, null);
  }

  public GameView(GameController gameController, Runnable onBack, Runnable onProfile,
                    DoubleSupplier sfxVolumeSupplier, GameUiState initialState) {
        this(gameController, onBack, onProfile, null, null, null,
      sfxVolumeSupplier, initialState, false, null, null);
  }

  public GameView(GameController gameController, Runnable onBack, Runnable onProfile,
                  Runnable onSettings,
        Runnable onPanelOpen,
        Runnable onStockSelectionChanged,
                  DoubleSupplier sfxVolumeSupplier,
                  GameUiState initialState,
                  boolean showTutorialOnStart,
                  Consumer<Boolean> onShowTutorialPreferenceChange,
                  Runnable onStartFreshFromTutorial) {
    this.gameController = gameController;
    this.gameController.setView(this);
    this.sfxVolumeSupplierField = sfxVolumeSupplier;
    this.levelUpClip = loadAudioClip(LEVEL_UP_SOUND);
    this.lastKnownStatus = gameController.getPlayerStatus();

    this.statusVal = new Label();
    this.cashVal = new Label();
    this.portfolioVal = new Label();
    this.netWorthVal = new Label();
    this.weekNumLbl = new Label();
    this.statusProgressArc = new Arc(0, 0, 11, 11, 90, 0);
    this.statusTooltip = new Tooltip();
    this.settingsBtn = new Button();
    this.profileBtn = new Button();
    this.profileNameText = new Text();
    this.profileNameText.getStyleClass().add("profile-identity-name");
    this.profileNameText.setBoundsType(TextBoundsType.VISUAL);
    this.profileNameBox = new StackPane(this.profileNameText);
    this.profileNameBox.getStyleClass().add("profile-identity-name-box");
    this.profileNameBox.setAlignment(Pos.CENTER);
    this.profileIdentityContent = new HBox(12, this.profileNameBox);
    this.profileIdentityContent.setAlignment(Pos.CENTER_LEFT);
    this.onProfile = onProfile;
    this.onPanelOpen = onPanelOpen;
    this.onStockSelectionChanged = onStockSelectionChanged;
    this.showTutorialOnStart = showTutorialOnStart;
    this.onShowTutorialPreferenceChange = onShowTutorialPreferenceChange;
    this.onStartFreshFromTutorial = onStartFreshFromTutorial;

    this.profileNameBox.minHeightProperty().bind(this.profileBtn.heightProperty().subtract(12));
    this.profileNameBox.prefHeightProperty().bind(this.profileBtn.heightProperty().subtract(12));
    this.profileNameBox.maxHeightProperty().bind(this.profileBtn.heightProperty().subtract(12));
    this.profileBtn.heightProperty().addListener((obs, oldV, newV) -> fitProfileNameGlyph());
    this.profileNameBox.heightProperty().addListener((obs, oldV, newV) -> fitProfileNameGlyph());
    this.profileNameText.textProperty().addListener((obs, oldV, newV) -> fitProfileNameGlyph());

    this.allStocks = FXCollections.observableArrayList(gameController.getStocks());
    this.filteredStocks = new FilteredList<>(this.allStocks, s -> true);
    this.sortedStocks = new SortedList<>(this.filteredStocks, java.util.Comparator.comparing(Stock::getSymbol));
    this.portfolioItems = FXCollections.observableArrayList(gameController.getPortfolioShares());
    this.selectedStock = new SimpleObjectProperty<>(null);
    this.performanceMode = AppConfig.PERFORMANCE_MODE.get();

    this.favorites = new HashSet<>();
    this.activeFilters = new HashSet<>();
    this.filterChipOrder = new ArrayList<>(List.of("FAVORITES", "OWNED", "UP", "DOWN"));
    this.filterChipsPane = new FlowPane(4, 4);
    this.stockSort = "NAME";

    // ── Stat pill labels ─────────────────────────────────────────────────
    statusVal.getStyleClass().addAll("stat-pill-value", "status-pill-value");
    cashVal.getStyleClass().add("money-segment-value");
    portfolioVal.getStyleClass().add("money-segment-value");
    netWorthVal.getStyleClass().add("money-segment-value");
    weekNumLbl.getStyleClass().add("week-number");

    statusProgressArc.setType(ArcType.OPEN);
    statusProgressArc.setFill(Color.TRANSPARENT);
    statusProgressArc.setStrokeLineCap(StrokeLineCap.ROUND);
    statusProgressArc.getStyleClass().add("status-pill-ring-progress");

    statusTooltip.setShowDelay(Duration.millis(120));
    statusTooltip.setShowDuration(Duration.INDEFINITE);
    statusTooltip.getStyleClass().add("status-pill-tooltip");

    // ── Detail panel (right) — rebuilt on stock selection ────────────────
    this.detailArea = new VBox();
    detailArea.getStyleClass().add("game-detail-area");
    VBox.setVgrow(detailArea, Priority.ALWAYS);
    Rectangle detailClip = new Rectangle();
    detailClip.widthProperty().bind(detailArea.widthProperty());
    detailClip.heightProperty().bind(detailArea.heightProperty());
    detailArea.setClip(detailClip);
    this.spikePopupList = new VBox(8);
    this.spikePopupList.getStyleClass().add("game-spike-popup-list");
    this.spikePopupList.setAlignment(Pos.TOP_RIGHT);
    this.spikePopupList.setPickOnBounds(false);
    this.spikePopupTimers = new ArrayList<>();

    // ── Stock list (left panel) — standard or virtualized (performance mode)
    this.stockListBox = new VBox(4);
    this.stockListBox.getStyleClass().add("game-stock-list");
    this.stockScroll = new ScrollPane(stockListBox);
    stockScroll.setFitToWidth(true);
    stockScroll.getStyleClass().add("game-scroll");
    stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    stockScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

    this.stockListView = new ListView<>(sortedStocks);
    this.stockListView.getStyleClass().addAll("game-stock-list-view", "game-scroll");
    this.stockListView.setFocusTraversable(false);
    this.stockListView.setCellFactory(lv -> {
      ListCell<Stock> cell = new ListCell<>() {
        private void syncSelectedStyle() {
          Node graphic = getGraphic();
          if (graphic instanceof HBox card) {
            card.getStyleClass().remove("stock-card-selected");
            if (isSelected()) {
              card.getStyleClass().add("stock-card-selected");
            }
          }
        }

        @Override
        protected void updateItem(Stock stock, boolean empty) {
          super.updateItem(stock, empty);
          if (empty || stock == null) {
            setGraphic(null);
            setText(null);
          } else {
            setGraphic(buildStockCard(stock));
            syncSelectedStyle();
          }
        }

        @Override
        public void updateSelected(boolean selected) {
          super.updateSelected(selected);
          syncSelectedStyle();
        }
      };
      cell.setOnMouseClicked(e -> {
        Stock stock = cell.getItem();
        if (stock == null || cell.isEmpty()) {
          return;
        }
        stockListView.getSelectionModel().select(stock);
      });
      return cell;
    });
    this.stockListView.getSelectionModel().selectedItemProperty().addListener((obs, old, stock) -> {
      if (stock == null) {
        return;
      }
      if (selectedStock.get() == null
          || !stock.getSymbol().equals(selectedStock.get().getSymbol())) {
        notifyStockSelectionChanged();
        selectedStock.set(stock);
      }
    });

    // ── Search field (declared early for closure access) ─────────────────
    this.searchField = new TextField();
    searchField.setPromptText("\uD83D\uDD0D  Search stocks\u2026");
    searchField.getStyleClass().add("game-search-field");

    // Apply initial UI state if provided
    if (initialState != null) {
      this.favorites.addAll(initialState.favorites());
      this.activeFilters.addAll(initialState.activeFilters());
      if (!initialState.filterChipOrder().isEmpty()) {
        this.filterChipOrder.clear();
        this.filterChipOrder.addAll(initialState.filterChipOrder());
      }
      if (initialState.stockSort() != null) {
        this.stockSort = initialState.stockSort();
      }
    }

    // Initial stock list population
    applyFilter();

    // Rebuild detail when selection changes; clear tx highlight when switching to a different stock
    selectedStock.addListener((obs, old, stock) -> {
      if (stock != null) {
        tutorialDidSelectStock = true;
        refreshTutorialProgress();
      }
      if (highlightedTx != null &&
          (stock == null || !highlightedTx.symbol().equals(stock.getSymbol()))) {
        highlightedTx = null;
        if (highlightFadeTimer != null) {
          highlightFadeTimer.stop();
          highlightFadeTimer = null;
        }
      }
      rebuildDetail();
      if (performanceMode) {
        Stock selectedInList = stockListView.getSelectionModel().getSelectedItem();
        if (stock == null) {
          stockListView.getSelectionModel().clearSelection();
        } else if (selectedInList == null
            || !stock.getSymbol().equals(selectedInList.getSymbol())) {
          stockListView.getSelectionModel().select(stock);
        }
      } else {
        refreshSelectedStockCardStyles();
      }
    });

    // Show detail immediately for first stock
    rebuildDetail();

    // ── Search field listener (debounced 150ms) ───────────────────────────
    PauseTransition searchDebounce = new PauseTransition(javafx.util.Duration.millis(150));
    searchDebounce.setOnFinished(ev -> applyFilter());
    searchField.textProperty().addListener((obs, old, val) -> searchDebounce.playFromStart());
    searchField.setOnAction(ev -> {
      notifyPanelOpen();
      applyFilter();
    });

    // ── Stock filter chips (multi-select, drag-reorderable) ───────────────
    rebuildFilterChips();
    Label filterLabel = new Label("FILTER");
    filterLabel.getStyleClass().add("stock-row-section-label");
    VBox filterRow = new VBox(3, filterLabel, filterChipsPane);
    filterRow.getStyleClass().add("stock-filter-row");

    // ── Sort row ──────────────────────────────────────────────────────────
    Button sortName = new Button("A\u2013Z");
    Button sortPrice = new Button("Price \u25bc");
    Button sortChg = new Button("Change \u25bc");
    sortName.getStyleClass().add("stock-sort-chip");
    sortPrice.getStyleClass().add("stock-sort-chip");
    sortChg.getStyleClass().add("stock-sort-chip");
    if (stockSort.startsWith("PRICE")) {
      sortPrice.getStyleClass().add("stock-sort-chip-active");
      sortPrice.setText("Price " + (stockSort.equals("PRICE_DESC") ? "\u25bc" : "\u25b2"));
    } else if (stockSort.startsWith("CHG")) {
      sortChg.getStyleClass().add("stock-sort-chip-active");
      sortChg.setText("Change " + (stockSort.equals("CHG_DESC") ? "\u25bc" : "\u25b2"));
    } else {
      sortName.getStyleClass().add("stock-sort-chip-active");
      if (stockSort.equals("NAME_DESC")) {
        sortName.setText("Z\u2013A");
      }
    }
    FlowPane sortChips = new FlowPane(4, 4);
    sortChips.getChildren().addAll(sortName, sortPrice, sortChg);

    sortName.setOnAction(ev -> {
      notifyPanelOpen();
      if (sortName.getStyleClass().contains("stock-sort-chip-active")) {
        stockSort = stockSort.equals("NAME_DESC") ? "NAME" : "NAME_DESC";
      } else {
        stockSort = "NAME";
        sortName.getStyleClass().add("stock-sort-chip-active");
        sortPrice.getStyleClass().remove("stock-sort-chip-active");
        sortChg.getStyleClass().remove("stock-sort-chip-active");
      }
      sortName.setText(stockSort.equals("NAME_DESC") ? "Z\u2013A" : "A\u2013Z");
      applyFilter();
    });
    sortPrice.setOnAction(ev -> {
      notifyPanelOpen();
      if (sortPrice.getStyleClass().contains("stock-sort-chip-active")) {
        // toggle direction
        stockSort = stockSort.equals("PRICE_DESC") ? "PRICE_ASC" : "PRICE_DESC";
      } else {
        stockSort = "PRICE_DESC";
        sortName.getStyleClass().remove("stock-sort-chip-active");
        sortChg.getStyleClass().remove("stock-sort-chip-active");
        sortPrice.getStyleClass().add("stock-sort-chip-active");
      }
      sortPrice.setText("Price " + (stockSort.equals("PRICE_DESC") ? "\u25bc" : "\u25b2"));
      applyFilter();
    });
    sortChg.setOnAction(ev -> {
      notifyPanelOpen();
      if (sortChg.getStyleClass().contains("stock-sort-chip-active")) {
        stockSort = stockSort.equals("CHG_DESC") ? "CHG_ASC" : "CHG_DESC";
      } else {
        stockSort = "CHG_DESC";
        sortName.getStyleClass().remove("stock-sort-chip-active");
        sortPrice.getStyleClass().remove("stock-sort-chip-active");
        sortChg.getStyleClass().add("stock-sort-chip-active");
      }
      sortChg.setText("Change " + (stockSort.equals("CHG_DESC") ? "\u25bc" : "\u25b2"));
      applyFilter();
    });

    Label sortLabel = new Label("SORT");
    sortLabel.getStyleClass().add("stock-row-section-label");
    VBox sortRow = new VBox(3, sortLabel, sortChips);
    sortRow.getStyleClass().add("stock-sort-row");

    Label marketTitle = new Label(gameController.getExchangeName());
    marketTitle.getStyleClass().add("game-panel-title");

    Node stockListNode = performanceMode ? stockListView : stockScroll;
    VBox.setVgrow(stockListNode, Priority.ALWAYS);

    VBox leftPanel = new VBox(8, marketTitle, searchField, filterRow, sortRow, stockListNode);
    leftPanel.getStyleClass().add("game-left-panel");
    leftPanel.setMinWidth(300);
    leftPanel.setMaxWidth(600);
    this.tutorialStockAreaTarget = leftPanel;

    // ── Portfolio table (bottom of right panel) ──────────────────────────
    Label portTitle = new Label("Portfolio");
    portTitle.getStyleClass().add("game-panel-title");
    Label portSummaryHint = new Label("View summary");
    portSummaryHint.getStyleClass().add("game-portfolio-summary-hint");
    Region portHeaderSpacer = new Region();
    HBox.setHgrow(portHeaderSpacer, Priority.ALWAYS);
    HBox portHeader = new HBox(8, portTitle, portHeaderSpacer, portSummaryHint);
    portHeader.getStyleClass().add("game-portfolio-header");
    portHeader.setAlignment(Pos.CENTER_LEFT);
    portHeader.setOnMouseClicked(e -> showPortfolioSummary());

    portfolioTable =
        buildPortfolioTable(portfolioItems, selectedShareStock -> { // TODO: Rewrite this shit
          if (selectedShareStock == null) {
            return;
          }

          String symbol = selectedShareStock.getSymbol();
          boolean existsInFiltered =
              filteredStocks.stream().anyMatch(s -> s.getSymbol().equals(symbol));
          if (!existsInFiltered) {
            searchField.setText("");
          }

          Stock target = allStocks.stream()
              .filter(s -> s.getSymbol().equals(symbol))
              .findFirst()
              .orElse(selectedShareStock);

          if (selectedStock.get() == null || !symbol.equals(selectedStock.get().getSymbol())) {
            notifyStockSelectionChanged();
          }
          selectedStock.set(target);
          focusStockCardInList(symbol);
        });
    portfolioTable.setMinHeight(400);
        portfolioTable.setPlaceholder(new Region());
        portfolioTable.visibleProperty().bind(Bindings.isNotEmpty(portfolioItems));
        portfolioTable.managedProperty().bind(Bindings.isNotEmpty(portfolioItems));
    VBox.setVgrow(portfolioTable, Priority.ALWAYS);

      Label emptyPortfolioLbl = new Label("No shares owned yet");
      emptyPortfolioLbl.getStyleClass().add("game-portfolio-empty");
      StackPane emptyPortfolioOverlay = new StackPane(emptyPortfolioLbl);
      emptyPortfolioOverlay.getStyleClass().add("game-portfolio-empty-overlay");
      emptyPortfolioOverlay.setMouseTransparent(true);
      emptyPortfolioOverlay.setPickOnBounds(false);
      emptyPortfolioOverlay.setAlignment(Pos.CENTER);
    emptyPortfolioOverlay.visibleProperty().bind(Bindings.isEmpty(portfolioItems));
    emptyPortfolioOverlay.managedProperty().bind(Bindings.isEmpty(portfolioItems));

      StackPane portfolioContent = new StackPane(portfolioTable, emptyPortfolioOverlay);
      StackPane.setAlignment(portfolioTable, Pos.CENTER);
      StackPane.setAlignment(emptyPortfolioOverlay, Pos.CENTER);
      VBox.setVgrow(portfolioContent, Priority.ALWAYS);
      portfolioContent.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
        tutorialTouchedPortfolio = true;
        refreshTutorialProgress();
      });

      VBox portfolioSection = new VBox(0, portHeader, portfolioContent);
    portfolioSection.getStyleClass().add("game-portfolio-pane");
    portfolioSection.setMinHeight(200);
    portfolioSection.setPrefHeight(200);
    portfolioSection.setMaxHeight(Region.USE_PREF_SIZE);

    // ── Right panel: explicit vertical layout with dedicated drag handle ──
    Region portfolioResizeHandle = new Region();
    portfolioResizeHandle.getStyleClass().add("game-portfolio-resize-handle");
    portfolioResizeHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
      tutorialTouchedPortfolio = true;
      refreshTutorialProgress();
    });
    this.tutorialPortfolioResizeHandleTarget = portfolioResizeHandle;
    this.tutorialPortfolioAreaTarget = portfolioResizeHandle;

    VBox rightPanel = new VBox(detailArea, portfolioResizeHandle, portfolioSection);
    rightPanel.getStyleClass().add("game-right-panel");
    HBox.setHgrow(rightPanel, Priority.ALWAYS);
    VBox.setVgrow(rightPanel, Priority.ALWAYS);
    double initPortDivider = (initialState != null && initialState.portfolioDivider() > 0)
      ? initialState.portfolioDivider() : 1.0;
    installPortfolioResize(rightPanel, portfolioSection, portfolioResizeHandle, initPortDivider);

    // ── Body: horizontal split (sidebar | right panel) ────────────────────
    SplitPane hSplit = new SplitPane(leftPanel, rightPanel);
    hSplit.setOrientation(Orientation.HORIZONTAL);
    hSplit.getStyleClass().add("game-body-split");
    VBox.setVgrow(hSplit, Priority.ALWAYS);
    double initSidebarDivider = (initialState != null && initialState.sidebarDivider() > 0)
      ? initialState.sidebarDivider() : 0.125;
    hSplit.setDividerPositions(initSidebarDivider);
    this.hSplitRef = hSplit;

    // ── Sub-bar: week + next-week ─────────────────────────────────────────
    VBox weekCard = new VBox(2,
        labelSmall("WEEK"),
        weekNumLbl);
    weekCard.getStyleClass().add("week-card");
    weekCard.setAlignment(Pos.CENTER);

    Button nextWeekBtn = new Button("\u25B6  Next Week");
    nextWeekBtn.getStyleClass().add("next-week-button");
    this.tutorialNextWeekBtnTarget = nextWeekBtn;

    Label calmDownLbl = new Label("\uD83D\uDE0C Calm down");
    calmDownLbl.getStyleClass().add("calm-down-label");
    calmDownLbl.setOpacity(0);
    calmDownLbl.setMouseTransparent(true);

    FadeTransition[] calmFade = {null};

    // Rate-limit: max 6 advances per second (sliding window)
    long[] advanceTimes = new long[6];
    int[] advanceHead = {0};

    AudioClip weekAdvanceClip = loadAudioClip(WEEK_ADVANCE_SOUND);
    boolean[] playedOnMousePress = {false};
    nextWeekBtn.setOnMousePressed(e -> {
      playedOnMousePress[0] = true;
      playAudioClip(weekAdvanceClip,
          () -> Math.min(sfxVolumeSupplier.getAsDouble() * 1.10, 1.0)); // 10% volume boost
    });

    nextWeekBtn.setOnAction(e -> {
      if (!playedOnMousePress[0]) {
        playAudioClip(weekAdvanceClip, () -> Math.min(sfxVolumeSupplier.getAsDouble() * 1.10, 1.0));
      }
      playedOnMousePress[0] = false;
      long now = System.currentTimeMillis();
      long oldest = advanceTimes[advanceHead[0]];
      if (now - oldest < 1000) {
        if (calmFade[0] != null) {
          calmFade[0].stop();
        }
        calmDownLbl.setOpacity(1);
        FadeTransition ft = new FadeTransition(Duration.millis(600), calmDownLbl);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setDelay(Duration.millis(700));
        calmFade[0] = ft;
        ft.play();
        return;
      }
      advanceTimes[advanceHead[0]] = now;
      advanceHead[0] = (advanceHead[0] + 1) % 6;
      gameController.handleNextWeek();
      tutorialDidAdvanceWeek = true;
      updateData();
      refreshTutorialProgress();
    });

    Button sellAllHoldingsBtn = new Button("\u2198  Sell All Holdings");
    sellAllHoldingsBtn.getStyleClass().addAll("next-week-button", "sell-all-holdings-button");
    sellAllHoldingsBtn.setOnAction(e -> {
      if (currentSellAllErrorLabel != null) {
        hideInlineError(currentSellAllErrorLabel, true);
      }
      gameController.handleSellAll(overlayRef);
    });
    Label sellAllErrorLbl = new Label();
    sellAllErrorLbl.getStyleClass().add("sell-all-error-label");
    sellAllErrorLbl.setWrapText(true);
    VBox sellAllErrorBox = createInlineErrorBox(sellAllErrorLbl);
    currentSellAllErrorLabel = sellAllErrorLbl;
    VBox sellAllStack = new VBox(2, sellAllErrorBox, sellAllHoldingsBtn);
    sellAllStack.setAlignment(Pos.BOTTOM_CENTER);

    Button marketMoversBtn = new Button("\uD83D\uDCC8  Market Movers");
    marketMoversBtn.getStyleClass().add("market-movers-button");
    this.tutorialMarketMoversBtnTarget = marketMoversBtn;
    marketMoversBtn.setOnAction(e -> {
      tutorialOpenedMovers = true;
      notifyPanelOpen();
      showMarketMovers();
      refreshTutorialProgress();
    });
    VBox nextWeekStack = new VBox(2, calmDownLbl, nextWeekBtn);
    nextWeekStack.setAlignment(Pos.BOTTOM_CENTER);
    Region subSpacer = new Region();
    HBox.setHgrow(subSpacer, Priority.ALWAYS);

    Button historyBtn = new Button("\uD83D\uDCCB  History");
    historyBtn.getStyleClass().add("market-movers-button");
    historyBtn.setOnAction(e -> {
      notifyPanelOpen();
      showTransactionHistory();
    });

    HBox marketActionRow = new HBox(10, historyBtn, marketMoversBtn);
    marketActionRow.setAlignment(Pos.CENTER_RIGHT);

    VBox marketActionBox = new VBox(8, marketActionRow);
    marketActionBox.getStyleClass().add("game-market-action-box");
    marketActionBox.setAlignment(Pos.TOP_RIGHT);

    HBox subBar = new HBox(16, weekCard, nextWeekStack, sellAllStack, subSpacer, marketActionBox);
    subBar.getStyleClass().add("game-sub-bar");
    subBar.setAlignment(Pos.BOTTOM_LEFT);

    // ── Top bar ──────────────────────────────────────────────────────────
    Button backBtn = new Button("\u2190");
    backBtn.getStyleClass().add("game-icon-button");
    backBtn.setOnAction(e -> onBack.run());

    ImageView appTitle;
    var logoUrl = GameView.class.getResource("/images/logos/After_Hours_Logo.png");
    if (logoUrl != null) {
      Image logoImg = new Image(logoUrl.toExternalForm());
      appTitle = new ImageView(logoImg);
      appTitle.setPreserveRatio(true);
      appTitle.setFitHeight(48);
    } else {
      appTitle = new ImageView();
    }

    Node statusPill = statusPill("Player Status", statusVal, statusProgressArc, statusTooltip);
    Node financePill = moneyPill(cashVal, portfolioVal, netWorthVal, "money-pill-overview");

    updateProfileIdentityButton();
    profileBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    profileBtn.getStyleClass().add("profile-identity-button");
    profileBtn.setOnAction(e -> this.onProfile.run());

    settingsBtn.setText("⚙");
    settingsBtn.getStyleClass().add("game-icon-button");
    settingsBtn.setDisable(onSettings == null);
    settingsBtn.setOnAction(e -> {
      if (onSettings != null) {
        onSettings.run();
      }
    });

    HBox leftGroup = new HBox(10, backBtn, appTitle);
    leftGroup.setAlignment(Pos.CENTER_LEFT);
    HBox rightGroup = new HBox(10, statusPill, profileBtn, settingsBtn);
    rightGroup.setAlignment(Pos.CENTER_RIGHT);

    Region edgeSpacer = new Region();
    HBox.setHgrow(edgeSpacer, Priority.ALWAYS);
    HBox edgesBar = new HBox(10, leftGroup, edgeSpacer, rightGroup);
    edgesBar.setAlignment(Pos.CENTER_LEFT);

    HBox pillCenter = new HBox(financePill);
    pillCenter.setAlignment(Pos.CENTER);
    pillCenter.setPickOnBounds(false);
    pillCenter.setMouseTransparent(true);

    StackPane topBar = new StackPane(edgesBar, pillCenter);
    topBar.getStyleClass().add("game-top-bar");

    // ── Root ─────────────────────────────────────────────────────────────
    BorderPane root = new BorderPane();
    rootRef = root;
    root.getStyleClass().add("home-page");

    VBox topSection = new VBox(0, topBar, subBar);
    root.setTop(topSection);
    root.setCenter(hSplit);

    // ── Dev panel ─────────────────────────────────────────────────────────
    StackPane devPanel = buildDevPanel();
    devPanel.visibleProperty().bind(AppConfig.DEV_MODE);
    devPanel.managedProperty().bind(AppConfig.DEV_MODE);
    StackPane.setAlignment(devPanel, Pos.BOTTOM_RIGHT);

    StackPane overlay = new StackPane(root, spikePopupList, devPanel);
    StackPane.setAlignment(spikePopupList, Pos.TOP_RIGHT);
    StackPane.setMargin(spikePopupList, new Insets(150, 12, 0, 0));
    overlayRef = overlay;

    // Restore selected stock from saved UI state
    if (initialState != null && initialState.selectedSymbol() != null) {
      String sym = initialState.selectedSymbol();
      allStocks.stream()
          .filter(s -> s.getSymbol().equals(sym))
          .findFirst()
          .ifPresent(selectedStock::set);
    }

    // ── Global keybindings ──────────────────────────────────────────────────
    // N / Space → Next Week  |  / → Focus search  |  M → Market Movers  |  H → History
    // S → Settings | P → Profile
    // Escape → clear search, then go back to landing page
    overlay.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      boolean inTextField = e.getTarget() instanceof TextInputControl;
      if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && isTutorialVisible()) {
        closeTutorial();
        e.consume();
        return;
      }
      // Base children: root + spikePopupList + devPanel. Any extra layer means a modal popup is open.
      boolean dialogOpen = overlay.getChildren().size() > 3;
      if (dialogOpen) {
        return;
      }
      switch (e.getCode()) {
        case N, SPACE -> {
          if (!inTextField) {
            nextWeekBtn.fire();
            e.consume();
          }
        }
        case SLASH -> {
          if (!inTextField) {
            searchField.requestFocus();
            e.consume();
          }
        }
        case F -> {
          if (e.isControlDown()) {
            searchField.requestFocus();
            e.consume();
          }
        }
        case M -> {
          if (!inTextField) {
            marketMoversBtn.fire();
            e.consume();
          }
        }
        case H -> {
          if (!inTextField) {
            historyBtn.fire();
            e.consume();
          }
        }
        case S -> {
          if (!inTextField && onSettings != null) {
            settingsBtn.fire();
            e.consume();
          }
        }
        case P -> {
          if (!inTextField) {
            profileBtn.fire();
            e.consume();
          }
        }
        case ENTER -> {
          if (!inTextField) {
            e.consume();
          }
        }
        case ESCAPE -> {
          if (inTextField) {
            overlay.requestFocus();
            e.consume();
          } else if (!searchField.getText().isEmpty()) {
            searchField.clear();
            searchField.getParent().requestFocus();
            e.consume();
          } else {
            onBack.run();
            e.consume();
          }
        }
        default -> {
        }
      }
    });

    updateData();
    maybeShowTutorial();
  }

  public StackPane getRoot() {
    return overlayRef;
  }

  private void maybeShowTutorial() {
    if (!showTutorialOnStart || tutorialDismissedThisSession) {
      return;
    }
    int stockCount = Math.max(0, gameController.getStocks().size());
    tutorialUseTwoStockMoversCopy = stockCount == 2;
    tutorialUseMoversStep = stockCount >= 2;
    tutorialDidSelectStock = false;
    tutorialDidTrade = false;
    tutorialDidAdvanceWeek = false;
    tutorialOpenedMovers = false;
    tutorialTouchedPortfolio = false;
    tutorialStartWeek = gameController.getCurrentWeek();
    tutorialStartTransactionCount = gameController.getTransactionCount();
    tutorialStepIndex = 0;
    openTutorial();
  }

  private void openTutorial() {
    if (overlayRef == null) {
      return;
    }
    if (tutorialOverlay == null) {
      buildTutorialOverlay();
    }
    if (!overlayRef.getChildren().contains(tutorialOverlay)) {
      overlayRef.getChildren().add(tutorialOverlay);
    }
    tutorialOverlay.toFront();
    Platform.runLater(this::updateTutorialStep);
  }

  private void closeTutorial() {
    tutorialDismissedThisSession = true;
    clearTutorialHighlight();
    if (overlayRef != null && tutorialOverlay != null) {
      overlayRef.getChildren().remove(tutorialOverlay);
    }
    setTutorialBackdropBlur(false);
  }

  private boolean isTutorialVisible() {
    return overlayRef != null && tutorialOverlay != null && overlayRef.getChildren().contains(tutorialOverlay);
  }

  private void buildTutorialOverlay() {
    tutorialOverlay = new StackPane();
    tutorialOverlay.getStyleClass().add("game-tutorial-overlay");
    tutorialOverlay.setPickOnBounds(false);
    tutorialOverlay.setMouseTransparent(false);

    tutorialShadeLayer = new Pane();
    tutorialShadeLayer.getStyleClass().add("game-tutorial-shade-layer");
    tutorialShadeLayer.setPickOnBounds(false);
    tutorialShadeLayer.setMouseTransparent(false);
    tutorialShadeLayer.prefWidthProperty().bind(tutorialOverlay.widthProperty());
    tutorialShadeLayer.prefHeightProperty().bind(tutorialOverlay.heightProperty());

    tutorialShadeTop = new Rectangle();
    tutorialShadeLeft = new Rectangle();
    tutorialShadeRight = new Rectangle();
    tutorialShadeBottom = new Rectangle();
    tutorialSpotlightRing = new Rectangle();
    tutorialShadeTop.getStyleClass().add("game-tutorial-shade");
    tutorialShadeLeft.getStyleClass().add("game-tutorial-shade");
    tutorialShadeRight.getStyleClass().add("game-tutorial-shade");
    tutorialShadeBottom.getStyleClass().add("game-tutorial-shade");
    tutorialSpotlightRing.getStyleClass().add("game-tutorial-spotlight-ring");
    tutorialShadeTop.setManaged(false);
    tutorialShadeLeft.setManaged(false);
    tutorialShadeRight.setManaged(false);
    tutorialShadeBottom.setManaged(false);
    tutorialSpotlightRing.setManaged(false);
    tutorialShadeTop.setFill(Color.rgb(2, 6, 18, 0.90));
    tutorialShadeLeft.setFill(Color.rgb(2, 6, 18, 0.90));
    tutorialShadeRight.setFill(Color.rgb(2, 6, 18, 0.90));
    tutorialShadeBottom.setFill(Color.rgb(2, 6, 18, 0.90));
    tutorialSpotlightRing.setFill(Color.TRANSPARENT);
    tutorialSpotlightRing.setStroke(Color.rgb(255, 201, 74, 1.0));
    tutorialSpotlightRing.setStrokeWidth(3);
    tutorialSpotlightRing.setArcWidth(32);
    tutorialSpotlightRing.setArcHeight(32);
    tutorialSpotlightRing.setMouseTransparent(true);
    for (Rectangle shade : new Rectangle[] {tutorialShadeTop, tutorialShadeLeft, tutorialShadeRight, tutorialShadeBottom}) {
      shade.setOnMousePressed(MouseEvent::consume);
      shade.setOnMouseDragged(MouseEvent::consume);
      shade.setOnMouseReleased(MouseEvent::consume);
      shade.setOnMouseClicked(MouseEvent::consume);
    }
    tutorialShadeLayer.getChildren().addAll(
        tutorialShadeTop,
        tutorialShadeLeft,
        tutorialShadeRight,
        tutorialShadeBottom,
        tutorialSpotlightRing);

    tutorialCard = new VBox(10);
    tutorialCard.getStyleClass().add("game-tutorial-card");
    tutorialCard.setMaxWidth(560);
    tutorialCard.setMaxHeight(Region.USE_PREF_SIZE);
    tutorialCard.setPrefWidth(520);
    tutorialCard.setMouseTransparent(false);

    tutorialStepLbl = new Label();
    tutorialStepLbl.getStyleClass().add("game-tutorial-step");
    Region tutorialHeaderSpacer = new Region();
    HBox.setHgrow(tutorialHeaderSpacer, Priority.ALWAYS);
    tutorialTitleLbl = new Label();
    tutorialTitleLbl.getStyleClass().add("game-tutorial-title");
    tutorialTitleLbl.setWrapText(true);
    tutorialBodyLbl = new Label();
    tutorialBodyLbl.getStyleClass().add("game-tutorial-body");
    tutorialBodyLbl.setWrapText(true);
    VBox tutorialBodyFrame = new VBox(tutorialBodyLbl);
    tutorialBodyFrame.getStyleClass().add("game-tutorial-body-frame");

    tutorialDontShowAgainToggle = new CheckBox("Don't show tutorial next time");
    tutorialDontShowAgainToggle.getStyleClass().add("game-tutorial-toggle");
    tutorialDontShowAgainToggle.setSelected(false);
    tutorialDontShowAgainToggle.selectedProperty().addListener((obs, oldV, selected) -> {
      if (onShowTutorialPreferenceChange != null) {
        onShowTutorialPreferenceChange.accept(!selected);
      }
    });

    tutorialBackBtn = new Button("Back");
    tutorialBackBtn.getStyleClass().add("game-tutorial-btn");
    tutorialBackBtn.setOnAction(e -> {
      if (tutorialStepIndex > 0) {
        tutorialStepIndex--;
        updateTutorialStep();
      }
    });

    tutorialNextBtn = new Button("Next");
    tutorialNextBtn.getStyleClass().addAll("game-tutorial-btn", "game-tutorial-btn-primary");
    tutorialNextBtn.setOnAction(e -> {
      if (!isTutorialStepComplete(tutorialStepIndex)) {
        return;
      }
      int last = tutorialStepCount() - 1;
      if (tutorialStepIndex >= last) {
        closeTutorial();
        return;
      }
      tutorialStepIndex++;
      updateTutorialStep();
    });

    tutorialCloseBtn = new Button("X");
    tutorialCloseBtn.getStyleClass().add("game-tutorial-close");
    tutorialCloseBtn.setOnAction(e -> closeTutorial());

    tutorialKeepProgressBtn = new Button("Continue This Run");
    tutorialKeepProgressBtn.getStyleClass().addAll("game-tutorial-btn", "game-tutorial-btn-primary");
    tutorialKeepProgressBtn.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(tutorialKeepProgressBtn, Priority.ALWAYS);
    tutorialKeepProgressBtn.setOnAction(e -> closeTutorial());

    tutorialStartFreshBtn = new Button("Reset Session");
    tutorialStartFreshBtn.getStyleClass().addAll("game-tutorial-btn", "game-tutorial-btn-danger", "game-tutorial-reset");
    tutorialStartFreshBtn.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(tutorialStartFreshBtn, Priority.ALWAYS);
    tutorialStartFreshBtn.setOnAction(e -> {
      closeTutorial();
      if (onStartFreshFromTutorial != null) {
        onStartFreshFromTutorial.run();
      }
    });

    HBox tutorialHeaderRow = new HBox(10, tutorialStepLbl, tutorialHeaderSpacer, tutorialCloseBtn);
    tutorialHeaderRow.setAlignment(Pos.CENTER_LEFT);
    tutorialHeaderRow.getStyleClass().add("game-tutorial-header-row");

    tutorialNavRow = new HBox(8, tutorialBackBtn, tutorialNextBtn);
    tutorialNavRow.setAlignment(Pos.CENTER_LEFT);
    tutorialNavRow.getStyleClass().add("game-tutorial-nav-row");
    HBox.setHgrow(tutorialBackBtn, Priority.NEVER);
    HBox.setHgrow(tutorialNextBtn, Priority.NEVER);

    tutorialFinishRow = new HBox(10, tutorialKeepProgressBtn, tutorialStartFreshBtn);
    tutorialFinishRow.setAlignment(Pos.CENTER_LEFT);
    tutorialFinishRow.getStyleClass().addAll("game-tutorial-finish-row", "game-tutorial-nav-row");

    tutorialCard.getChildren().addAll(
      tutorialHeaderRow,
        tutorialTitleLbl,
        tutorialBodyFrame,
        tutorialDontShowAgainToggle,
        tutorialNavRow,
        tutorialFinishRow);

    StackPane.setAlignment(tutorialCard, Pos.TOP_CENTER);
    StackPane.setMargin(tutorialCard, new Insets(96, 0, 0, 0));
    tutorialOverlay.getChildren().addAll(tutorialShadeLayer, tutorialCard);

    tutorialOverlay.widthProperty().addListener((obs, oldV, newV) -> {
      if (isTutorialVisible()) {
        updateTutorialStep();
      }
    });
    tutorialOverlay.heightProperty().addListener((obs, oldV, newV) -> {
      if (isTutorialVisible()) {
        updateTutorialStep();
      }
    });
  }

  private int tutorialStepCount() {
    return 7;
  }

  private void updateTutorialStep() {
    if (tutorialOverlay == null) {
      return;
    }
    refreshTutorialProgressFlags();
    int step = Math.max(0, Math.min(tutorialStepCount() - 1, tutorialStepIndex));
    tutorialStepIndex = step;
    int humanStep = step + 1;
    tutorialStepLbl.setText("Tutorial " + humanStep + " / " + tutorialStepCount());
    tutorialBackBtn.setDisable(step == 0);
    tutorialNextBtn.setText(step == tutorialStepCount() - 1 ? "Finish" : "Next");
    tutorialNextBtn.setDisable(!isTutorialStepComplete(step));

    boolean onLast = step == tutorialStepCount() - 1;
    tutorialNavRow.setVisible(!onLast);
    tutorialNavRow.setManaged(!onLast);
    tutorialFinishRow.setVisible(onLast);
    tutorialFinishRow.setManaged(onLast);
    tutorialKeepProgressBtn.setVisible(onLast);
    tutorialKeepProgressBtn.setManaged(onLast);
    tutorialStartFreshBtn.setVisible(onLast);
    tutorialStartFreshBtn.setManaged(onLast);

    clearTutorialHighlight();
    Node stepTarget = null;
    String completionHint = "";
    switch (step) {
      case 0 -> {
        tutorialTitleLbl.setText("Welcome to After Hours");
        tutorialBodyLbl.setText(
            "This guide is interactive and you can close it anytime. \nIf you don't want to see it again, just check the box below.");
      }
      case 1 -> {
        tutorialTitleLbl.setText("Find Stocks Quickly");
        tutorialBodyLbl.setText(
            "Use search, filters, and sorting in the left panel. Click a stock to inspect it and open trading details. \nSome of these features will not work until you start trading.");
        stepTarget = tutorialStockAreaTarget;
        completionHint = "Select a stock you want to invest in to continue.";
      }
      case 2 -> {
        tutorialTitleLbl.setText("Your First Trade");
        tutorialBodyLbl.setText(
          "Use the highlighted trade panel to buy your first share(s). Start with the buy controls.");
        stepTarget = tutorialTradeAreaTarget;
        completionHint = "Complete one transaction to continue.";
      }
      case 3 -> {
        tutorialTitleLbl.setText("Advance to Next Week");
        tutorialBodyLbl.setText(
            "Click Next Week to advance to the next week and simulate market movement.");
        stepTarget = tutorialNextWeekBtnTarget;
        completionHint = "Advance at least one week to continue.";
        if (!tutorialSpikeScheduled) {
          gameController.scheduleTutorialMomentumNudge();
          tutorialSpikeScheduled = true;
        }
      }
      case 4 -> {
        tutorialTitleLbl.setText("Portfolio");
        tutorialBodyLbl.setText(
            "Hold + Drag the selected bar below to open up your portfolio.\nThis is where you can track your holdings and see their performance.");
        stepTarget = tutorialPortfolioAreaTarget;
        completionHint = tutorialTouchedPortfolio
            ? "Inspect the portfolio, then continue."
            : "Touch the resize bar to continue.";
      }
      case 5 -> {
        if (tutorialUseMoversStep) {
          tutorialTitleLbl.setText("Market Movers");
          if (tutorialUseTwoStockMoversCopy) {
            tutorialBodyLbl.setText(
                "With two stocks, the movers list is short. Use it to see the top mover this week and compare direction.");
          } else {
            tutorialBodyLbl.setText(
                "Open Market Movers to inspect top gainers and losers. This helps you spot momentum and analyze the market before trading.");
          }
          stepTarget = tutorialMarketMoversBtnTarget;
          completionHint = "Open Market Movers to continue.";
        } else {
          tutorialTitleLbl.setText("Track Price Change");
          tutorialBodyLbl.setText(
              "With a single-stock market, focus on week-to-week price change and how it affects your holdings.");
          stepTarget = tutorialNextWeekBtnTarget;
          completionHint = "Advance one week to continue.";
        }
      }
      case 6 -> {
        tutorialTitleLbl.setText("You're Ready");
        tutorialBodyLbl.setText(
        "Continue this run to keep everything exactly as it is, or reset the session to start fresh.");
      }
      default -> {
      }
    }
    if (!completionHint.isEmpty() && !isTutorialStepComplete(step)) {
      tutorialBodyLbl.setText(tutorialBodyLbl.getText() + "\n\n" + completionHint);
    }
    setTutorialBackdropBlur(step == 0);
    if (step == 0) {
      showTutorialBackdropOnly();
      positionTutorialCard(null);
    } else if (step == 4 && tutorialTouchedPortfolio) {
      clearTutorialHighlight();
      positionTutorialCard(null);
    } else if (step == 2 || step == 4) {
      highlightTutorialNode(stepTarget);
      positionTutorialCard(null);
    } else {
      highlightTutorialNode(stepTarget);
      positionTutorialCard(stepTarget);
    }
    tutorialOverlay.toFront();
  }

  private void refreshTutorialProgressFlags() {
    tutorialDidSelectStock = tutorialDidSelectStock || selectedStock.get() != null;
    tutorialDidTrade = tutorialDidTrade
        || gameController.getTransactionCount() > tutorialStartTransactionCount;
    tutorialDidAdvanceWeek = tutorialDidAdvanceWeek
        || gameController.getCurrentWeek() > tutorialStartWeek;
  }

  private boolean isTutorialStepComplete(int step) {
    return switch (step) {
      case 0, 6 -> true;
      case 1 -> tutorialDidSelectStock;
      case 2 -> tutorialDidTrade;
      case 3 -> tutorialDidAdvanceWeek;
      case 4 -> tutorialTouchedPortfolio;
      case 5 -> tutorialUseMoversStep ? tutorialOpenedMovers : tutorialDidAdvanceWeek;
      default -> true;
    };
  }

  private void refreshTutorialProgress() {
    if (!isTutorialVisible() || tutorialSuspendDepth > 0) {
      return;
    }
    tutorialOverlay.toFront();
    refreshTutorialProgressFlags();
    maybeAdvanceTutorialAfterTrade();
    updateTutorialStep();
  }

  private void maybeAdvanceTutorialAfterTrade() {
    if (tutorialStepIndex == 2 && tutorialDidTrade) {
      tutorialStepIndex = 3;
    }
  }

  private void suspendTutorialOverlay() {
    if (tutorialOverlay == null || overlayRef == null) {
      return;
    }
    if (tutorialSuspendDepth == 0) {
      tutorialWasVisibleBeforeSuspend = isTutorialVisible();
      if (tutorialWasVisibleBeforeSuspend) {
        clearTutorialHighlight();
        overlayRef.getChildren().remove(tutorialOverlay);
      }
      setTutorialBackdropBlur(false);
    }
    tutorialSuspendDepth++;
  }

  private void resumeTutorialOverlay() {
    if (tutorialSuspendDepth <= 0) {
      return;
    }
    tutorialSuspendDepth--;
    if (tutorialSuspendDepth > 0 || !tutorialWasVisibleBeforeSuspend || overlayRef == null) {
      return;
    }
    tutorialWasVisibleBeforeSuspend = false;
    if (!overlayRef.getChildren().contains(tutorialOverlay)) {
      overlayRef.getChildren().add(tutorialOverlay);
    }
    tutorialOverlay.toFront();
    Platform.runLater(this::updateTutorialStep);
  }

  private void setTutorialBackdropBlur(boolean enabled) {
    if (rootRef == null) {
      return;
    }
    rootRef.setEffect(enabled ? tutorialBackdropBlur : null);
  }

  private void showTutorialBackdropOnly() {
    if (overlayRef == null) {
      return;
    }
    clearTutorialHighlight();
    double overlayW = overlayRef.getWidth();
    double overlayH = overlayRef.getHeight();
    if (overlayW <= 1 || overlayH <= 1) {
      return;
    }
    tutorialShadeTop.setVisible(true);
    tutorialShadeTop.setX(0);
    tutorialShadeTop.setY(0);
    tutorialShadeTop.setWidth(overlayW);
    tutorialShadeTop.setHeight(overlayH);
  }

  private Bounds getTutorialTargetBounds(Node target) {
    if (overlayRef == null || target == null) {
      return null;
    }
    if (tutorialStepIndex == 4) {
      Bounds portfolioBounds = getPortfolioTutorialBounds();
      if (portfolioBounds != null) {
        return portfolioBounds;
      }
    }
    return toOverlayBounds(target);
  }

  private Bounds getPortfolioTutorialBounds() {
    if (tutorialTouchedPortfolio) {
      return null;
    }

    Bounds handleBounds = toOverlayBounds(tutorialPortfolioResizeHandleTarget);
    if (handleBounds == null) {
      return null;
    }

    double x = handleBounds.getMinX() + 28;
    double width = Math.max(24, handleBounds.getWidth() - 56);
    double baseHighlightHeight = Math.max(8, Math.min(14, handleBounds.getHeight()));
    double highlightHeight = baseHighlightHeight * 1.5;
    double centerY = handleBounds.getMinY() + handleBounds.getHeight() * 0.5;
    double y = centerY - highlightHeight * 0.5;
    return new BoundingBox(x, y, width, highlightHeight);
  }

  private Bounds toOverlayBounds(Node target) {
    if (overlayRef == null || target == null) {
      return null;
    }
    Bounds sceneBounds = target.localToScene(target.getBoundsInLocal());
    return sceneBounds == null ? null : overlayRef.sceneToLocal(sceneBounds);
  }

  private void highlightTutorialNode(Node target) {
    clearTutorialHighlight();
    Bounds overlayBounds = getTutorialTargetBounds(target);
    if (overlayBounds == null) {
      return;
    }

    double overlayW = overlayRef.getWidth();
    double overlayH = overlayRef.getHeight();
    if (overlayW <= 1 || overlayH <= 1) {
      return;
    }

    double pad = tutorialStepIndex == 4 ? 2 : 10;
    double x = Math.max(0, overlayBounds.getMinX() - pad);
    double y = Math.max(0, overlayBounds.getMinY() - pad);
    double w = Math.min(overlayW - x, overlayBounds.getWidth() + pad * 2);
    double h = Math.min(overlayH - y, overlayBounds.getHeight() + pad * 2);

    double minW = tutorialStepIndex == 4 ? 24 : 120;
    double minH = tutorialStepIndex == 4 ? 10 : 56;
    if (w < minW) {
      double centerX = x + w * 0.5;
      x = Math.max(0, Math.min(overlayW - minW, centerX - minW * 0.5));
      w = Math.min(minW, overlayW - x);
    }
    if (h < minH) {
      double centerY = y + h * 0.5;
      y = Math.max(0, Math.min(overlayH - minH, centerY - minH * 0.5));
      h = Math.min(minH, overlayH - y);
    }

    tutorialShadeTop.setVisible(true);
    tutorialShadeLeft.setVisible(true);
    tutorialShadeRight.setVisible(true);
    tutorialShadeBottom.setVisible(true);
    tutorialSpotlightRing.setVisible(true);

    tutorialShadeTop.setX(0);
    tutorialShadeTop.setY(0);
    tutorialShadeTop.setWidth(overlayW);
    tutorialShadeTop.setHeight(y);

    tutorialShadeLeft.setX(0);
    tutorialShadeLeft.setY(y);
    tutorialShadeLeft.setWidth(x);
    tutorialShadeLeft.setHeight(h);

    tutorialShadeRight.setX(x + w);
    tutorialShadeRight.setY(y);
    tutorialShadeRight.setWidth(Math.max(0, overlayW - (x + w)));
    tutorialShadeRight.setHeight(h);

    tutorialShadeBottom.setX(0);
    tutorialShadeBottom.setY(y + h);
    tutorialShadeBottom.setWidth(overlayW);
    tutorialShadeBottom.setHeight(Math.max(0, overlayH - (y + h)));

    tutorialSpotlightRing.setX(x);
    tutorialSpotlightRing.setY(y);
    tutorialSpotlightRing.setWidth(w);
    tutorialSpotlightRing.setHeight(h);

  }

  private void positionTutorialCard(Node target) {
    if (tutorialCard == null || overlayRef == null) {
      return;
    }
    if (target == null) {
      StackPane.setAlignment(tutorialCard, Pos.TOP_CENTER);
      StackPane.setMargin(tutorialCard, new Insets(96, 0, 0, 0));
      return;
    }
    Bounds b = getTutorialTargetBounds(target);
    if (b == null) {
      return;
    }

    double overlayW = overlayRef.getWidth();
    double overlayH = overlayRef.getHeight();
    if (overlayW <= 1 || overlayH <= 1) {
      return;
    }

    double cardW = tutorialCard.prefWidth(-1);
    if (cardW <= 0 || Double.isNaN(cardW)) {
      cardW = 520;
    }
    if (target == tutorialMoversTabBarTarget || target == tutorialMoversCardTarget) {
      cardW = Math.min(cardW, 380);
    }
    double cardH = tutorialCard.prefHeight(cardW);
    if (cardH <= 0 || Double.isNaN(cardH)) {
      cardH = 260;
    }

    double centerX = b.getMinX() + (b.getWidth() - cardW) * 0.5;
    boolean wideTarget = b.getWidth() >= Math.min(360, overlayW * 0.34);
    boolean lowTarget = b.getMaxY() >= overlayH * 0.62;
    boolean highTarget = b.getMinY() <= overlayH * 0.25;
    boolean tradeStep = tutorialStepIndex == 2;
    boolean portfolioStep = tutorialStepIndex == 4;

    if (tradeStep) {
      double chosenX = clamp(centerX, 16, Math.max(16, overlayW - cardW - 16));
      double alignedY = clamp(b.getMinY() - cardH - 18, 20, Math.max(20, overlayH - cardH - 16));
      StackPane.setAlignment(tutorialCard, Pos.TOP_LEFT);
      StackPane.setMargin(tutorialCard, new Insets(alignedY, 0, 0, chosenX));
      return;
    }

    if (portfolioStep) {
      double chosenX = clamp(centerX, 16, Math.max(16, overlayW - cardW - 16));
      double alignedY = clamp(b.getMinY() - cardH - 14, 20, Math.max(20, overlayH - cardH - 16));
      StackPane.setAlignment(tutorialCard, Pos.TOP_LEFT);
      StackPane.setMargin(tutorialCard, new Insets(alignedY, 0, 0, chosenX));
      return;
    }

    double[][] candidates;
    if (target == tutorialMoversTabBarTarget || target == tutorialMoversCardTarget) {
      candidates = new double[][] {
          {b.getMaxX() + 22, b.getMinY() - 8},
          {b.getMinX() - cardW - 22, b.getMinY() - 8},
          {centerX, b.getMaxY() + 22},
          {centerX, b.getMinY() - cardH - 22}
      };
    } else if (wideTarget || lowTarget) {
      candidates = new double[][] {
          {centerX, b.getMinY() - cardH - 22},
          {centerX, b.getMaxY() + 22},
          {b.getMinX() - cardW - 22, b.getMinY()},
          {b.getMaxX() + 22, b.getMinY()}
      };
    } else if (highTarget) {
      candidates = new double[][] {
          {b.getMaxX() + 18, b.getMinY()},
          {b.getMinX() - cardW - 18, b.getMinY()},
          {centerX, b.getMaxY() + 18},
          {centerX, b.getMinY() - cardH - 18}
      };
    } else {
      candidates = new double[][] {
          {b.getMaxX() + 18, b.getMinY()},
          {b.getMinX() - cardW - 18, b.getMinY()},
          {centerX, b.getMinY() - cardH - 18},
          {centerX, b.getMaxY() + 18}
      };
    }

    double chosenX = Math.max(16, (overlayW - cardW) * 0.5);
    double chosenY = 96;
    boolean found = false;
    for (double[] candidate : candidates) {
      double cx = clamp(candidate[0], 16, Math.max(16, overlayW - cardW - 16));
      double cy = clamp(candidate[1], 20, Math.max(20, overlayH - cardH - 16));
      Bounds cardBounds = new BoundingBox(cx, cy, cardW, cardH);
      boolean overlapsTarget = cardBounds.intersects(
          b.getMinX() - 10,
          b.getMinY() - 10,
          b.getWidth() + 20,
          b.getHeight() + 20);
      if (!overlapsTarget) {
        chosenX = cx;
        chosenY = cy;
        found = true;
        break;
      }
    }
    if (!found) {
      chosenX = clamp(centerX, 16, Math.max(16, overlayW - cardW - 16));
      if (lowTarget) {
        chosenY = clamp(b.getMinY() - cardH - 18, 20, Math.max(20, overlayH - cardH - 16));
      } else {
        chosenY = clamp(b.getMaxY() + 16, 20, Math.max(20, overlayH - cardH - 16));
      }
    }

    if (tutorialStepIndex == 5) {
      chosenX = clamp(chosenX - 200, 16, Math.max(16, overlayW - cardW - 16));
    }

    StackPane.setAlignment(tutorialCard, Pos.TOP_LEFT);
    StackPane.setMargin(tutorialCard, new Insets(chosenY, 0, 0, chosenX));
  }

  private void clearTutorialHighlight() {
    if (tutorialShadeTop != null) {
      tutorialShadeTop.setVisible(false);
      tutorialShadeLeft.setVisible(false);
      tutorialShadeRight.setVisible(false);
      tutorialShadeBottom.setVisible(false);
      tutorialSpotlightRing.setVisible(false);
    }
  }

  public GameUiState getUiState() {
    String selSym = selectedStock.get() != null ? selectedStock.get().getSymbol() : null;
    double sidebarDiv = (hSplitRef != null && hSplitRef.getDividerPositions().length > 0)
      ? hSplitRef.getDividerPositions()[0] : 0.125;
    double portDiv = portfolioDividerRatio;
    return new GameUiState(
        List.copyOf(favorites),
        List.copyOf(activeFilters),
        List.copyOf(filterChipOrder),
        stockSort,
        selSym,
        sidebarDiv,
        portDiv);
  }

  public List<String> getFavoriteSymbolsSnapshot() {
    return List.copyOf(favorites);
  }

  public boolean isFavoriteSymbol(String symbol) {
    return symbol != null && favorites.contains(symbol);
  }

  public void toggleFavoriteSymbol(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      return;
    }
    if (favorites.contains(symbol)) {
      favorites.remove(symbol);
    } else {
      favorites.add(symbol);
    }
    applyFilter();
    rebuildDetail();
  }

  private void maybeShowSpikePopups() {
    List<String> spikedSymbols = gameController.consumeLastSpikeSymbols();
    if (spikedSymbols.isEmpty()) {
      return;
    }

    boolean hasUp = false;
    boolean hasDown = false;

    for (String symbol : spikedSymbols) {
      Stock stock = allStocks.stream()
          .filter(s -> s.getSymbol().equals(symbol))
          .findFirst()
          .orElse(null);
      if (stock == null) {
        continue;
      }
      boolean owned = gameController.isOwned(symbol);
      boolean watchlisted = favorites.contains(symbol);
      if (!owned && !watchlisted) {
        continue;
      }

      BigDecimal changePct = stock.percentageChange().setScale(2, RoundingMode.HALF_UP);
      boolean qualifiesUpwardSpike = changePct.compareTo(MIN_UPWARD_SPIKE_POPUP_PCT) >= 0;
      boolean qualifiesDownwardSpike = changePct.compareTo(MIN_DOWNWARD_SPIKE_POPUP_PCT) <= 0;
      if (!qualifiesUpwardSpike && !qualifiesDownwardSpike) {
        continue;
      }
      if (qualifiesUpwardSpike) hasUp = true;
      if (qualifiesDownwardSpike) hasDown = true;
      addSpikePopup(stock, owned, changePct);
    }

    String soundPath = (hasUp && hasDown) ? SPIKE_BOTH_SOUND
        : hasUp ? SPIKE_UP_SOUND
        : hasDown ? SPIKE_DOWN_SOUND
        : null;
    if (soundPath != null) {
      AudioClip spikeClip = loadAudioClip(soundPath);
      playAudioClip(spikeClip, sfxVolumeSupplierField);
    }
  }

  private void addSpikePopup(Stock stock, boolean owned, BigDecimal changePct) {
    Label symbolBadge = new Label(stock.getSymbol());
    symbolBadge.getStyleClass().add("profile-badge");

    Label ownedWatchlist = new Label(owned ? "Owned" : "Watchlist");
    ownedWatchlist.getStyleClass().add("profile-favorite-owned-chip");
    if (owned) {
      ownedWatchlist.getStyleClass().add("profile-favorite-owned-chip-owned");
    }

    Label company = new Label(stock.getCompany());
    company.getStyleClass().add("profile-position-sub");

    VBox identityBlock = new VBox(4, company, ownedWatchlist);
    identityBlock.setAlignment(Pos.CENTER_LEFT);

    boolean up = changePct.compareTo(BigDecimal.ZERO) >= 0;
    String pctText = (up ? "+" : "") + changePct.toPlainString() + "%";
    Label pctLabel = new Label(pctText);
    pctLabel.getStyleClass().addAll("profile-position-pnl", up ? "profile-value-up" : "profile-value-down");

    Label arrowLabel = new Label(up ? "↗" : "↘");
    arrowLabel.getStyleClass().addAll("game-spike-arrow", up ? "profile-value-up" : "profile-value-down");

    HBox trend = new HBox(5, pctLabel, arrowLabel);
    trend.setAlignment(Pos.CENTER_RIGHT);

    Button closeBtn = new Button("x");
    closeBtn.getStyleClass().add("game-spike-close-btn");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox content = new HBox(10, symbolBadge, identityBlock, spacer, trend);
    content.setAlignment(Pos.CENTER_LEFT);
    content.setMaxWidth(Double.MAX_VALUE);
    content.setPadding(new Insets(10, 42, 10, 11));

    StackPane popup = new StackPane(content, closeBtn);
    popup.getStyleClass().addAll("game-spike-popup", "game-spike-popup-clickable");
    popup.setMaxWidth(Double.MAX_VALUE);
    StackPane.setAlignment(content, Pos.CENTER_LEFT);
    StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
    StackPane.setMargin(closeBtn, new Insets(4, 6, 0, 0));

    PauseTransition ttl = new PauseTransition(SPIKE_POPUP_VISIBLE);
    ttl.setOnFinished(e -> fadeOutSpikePopup(popup, ttl));
    closeBtn.setOnAction(e -> {
      e.consume();
      fadeOutSpikePopup(popup, ttl);
    });
    popup.setOnMouseClicked(e -> {
      selectStockBySymbol(stock.getSymbol());
      fadeOutSpikePopup(popup, ttl);
    });

    spikePopupList.getChildren().add(popup);
    spikePopupTimers.add(ttl);
    ttl.play();
  }

  private void fadeOutSpikePopup(Node popup, PauseTransition ttl) {
    if (popup == null || !spikePopupList.getChildren().contains(popup)) {
      return;
    }
    if (Boolean.TRUE.equals(popup.getProperties().get("spikePopupFading"))) {
      return;
    }
    popup.getProperties().put("spikePopupFading", true);
    if (ttl != null) {
      ttl.stop();
      spikePopupTimers.remove(ttl);
    }

    FadeTransition fade = new FadeTransition(SPIKE_POPUP_FADE, popup);
    fade.setFromValue(popup.getOpacity());
    fade.setToValue(0.0);
    fade.setOnFinished(e -> {
      popup.getProperties().remove("spikePopupFading");
      popup.setOpacity(1.0);
      spikePopupList.getChildren().remove(popup);
    });
    fade.play();
  }

  private void showTestSpikePopup() {
    Stock target = selectedStock.get();
    if (target == null && !allStocks.isEmpty()) {
      target = allStocks.get(0);
    }
    if (target == null) {
      return;
    }
    boolean owned = gameController.isOwned(target.getSymbol());
    addSpikePopup(target, owned, new BigDecimal("42.00"));
  }

  private void installPortfolioResize(VBox rightPanel, VBox portfolioSection,
                                      Region portfolioResizeHandle, double initialRatio) {
    this.portfolioDividerRatio = initialRatio > 0 ? initialRatio : 1.0;

    final double[] dragStartY = {0};
    final double[] dragStartRatio = {portfolioDividerRatio};

    portfolioResizeHandle.setOnMousePressed(event -> {
      dragStartY[0] = event.getSceneY();
      dragStartRatio[0] = portfolioDividerRatio;
      event.consume();
    });

    portfolioResizeHandle.setOnMouseDragged(event -> {
      double usableHeight = rightPanel.getHeight() - portfolioResizeHandle.getHeight();
      if (usableHeight <= 0) {
        return;
      }
      double deltaY = event.getSceneY() - dragStartY[0];
      portfolioDividerRatio = dragStartRatio[0] + (deltaY / usableHeight);
      applyPortfolioResize(rightPanel, portfolioSection, portfolioResizeHandle);
      event.consume();
    });

    rightPanel.heightProperty().addListener((obs, oldHeight, newHeight) ->
        applyPortfolioResize(rightPanel, portfolioSection, portfolioResizeHandle));
    Platform.runLater(() -> applyPortfolioResize(rightPanel, portfolioSection, portfolioResizeHandle));
  }

  private void applyPortfolioResize(VBox rightPanel, VBox portfolioSection,
                                    Region portfolioResizeHandle) {
    double usableHeight = rightPanel.getHeight() - portfolioResizeHandle.getHeight();
    if (usableHeight <= 0) {
      return;
    }

    double minDetailHeight = Math.max(340, detailArea.minHeight(-1));
    double minPortfolioHeight = 96;
    double minRatio = usableHeight > 0 ? (minDetailHeight / usableHeight) : 0.5;
    double maxRatio = usableHeight > 0 ? ((usableHeight - minPortfolioHeight) / usableHeight) : 0.5;

    if (maxRatio < minRatio) {
      minRatio = 0.5;
      maxRatio = 0.5;
    }

    portfolioDividerRatio = clamp(portfolioDividerRatio, minRatio, maxRatio);
    double detailHeight = snapToPixel(Math.max(minDetailHeight, usableHeight * portfolioDividerRatio));
    double portfolioHeight = snapToPixel(Math.max(minPortfolioHeight, usableHeight - detailHeight));

    // Keep total fixed to the available height to avoid 1px oscillation during layout passes.
    double snappedUsable = snapToPixel(usableHeight);
    double totalHeight = detailHeight + portfolioHeight;
    if (totalHeight != snappedUsable) {
      portfolioHeight = Math.max(minPortfolioHeight, portfolioHeight + (snappedUsable - totalHeight));
    }

    detailArea.setMinHeight(minDetailHeight);
    detailArea.setPrefHeight(detailHeight);
    detailArea.setMaxHeight(detailHeight);
    portfolioSection.setMinHeight(minPortfolioHeight);
    portfolioSection.setPrefHeight(portfolioHeight);
    portfolioSection.setMaxHeight(portfolioHeight);

    if (isTutorialVisible() && tutorialStepIndex == 5) {
      updateTutorialStep();
    }
  }

  private static double snapToPixel(double value) {
    return Math.max(0, Math.rint(value));
  }

  public void updateData() {
    ShareSelectionKey selectedShareKey = capturePortfolioSelection();

    PlayerStatus status = gameController.getPlayerStatus();
    if (lastKnownStatus != null && status.ordinal() > lastKnownStatus.ordinal()) {
      showLevelUpPopup(status);
    }
    lastKnownStatus = status;
    BigDecimal overallProgress = gameController.getPlayerStatusProgress();
    BigDecimal weeksProgress = gameController.getPlayerWeeksProgress();
    BigDecimal growthProgress = gameController.getPlayerNetWorthProgress();
    int weeksTraded = gameController.getPlayerWeeksTraded();
    int targetWeeks = gameController.getPlayerWeeksTargetForNextStatus();
    BigDecimal growthRatio = gameController.getPlayerGrowthRatio();
    BigDecimal growthTarget = gameController.getPlayerGrowthTargetForNextStatus();

    statusVal.setText(formatStatus(status));
    statusProgressArc.setLength(-360 * clamp01(overallProgress).doubleValue());
    statusTooltip.setText(null);
    statusTooltip.setGraphic(buildStatusTooltipContent(status, overallProgress, weeksTraded,
      targetWeeks, weeksProgress, growthRatio, growthTarget, growthProgress));

    weekNumLbl.setText(String.valueOf(gameController.getCurrentWeek()));
    cashVal.setText(CurrencyFormatter.format(gameController.getPlayerCash()));
    portfolioVal.setText(CurrencyFormatter.format(gameController.getPortfolioNetWorth()));
    netWorthVal.setText(CurrencyFormatter.format(gameController.getPlayerNetWorth()));
    updateProfileIdentityButton();
    portfolioItems.setAll(gameController.getPortfolioShares());
    if (!portfolioTable.getSortOrder().isEmpty()) {
      portfolioTable.sort();
    }
    restorePortfolioSelection(selectedShareKey);
    portfolioTable.refresh();
    applyFilter();
    rebuildDetail();
    maybeShowSpikePopups();
    refreshTutorialProgress();
  }

  private ShareSelectionKey capturePortfolioSelection() {
    Share selected = portfolioTable.getSelectionModel().getSelectedItem();
    if (selected == null || selected.getStock() == null) {
      return null;
    }
    return new ShareSelectionKey(
        selected.getStock().getSymbol(),
        selected.getQuantity(),
        selected.getPurchasePrice());
  }

  private void restorePortfolioSelection(ShareSelectionKey selectedShareKey) {
    if (selectedShareKey == null) {
      return;
    }

    for (int i = 0; i < portfolioItems.size(); i++) {
      Share share = portfolioItems.get(i);
      if (share.getStock() == null) {
        continue;
      }
      boolean symbolMatch = selectedShareKey.symbol().equals(share.getStock().getSymbol());
      boolean quantityMatch = selectedShareKey.quantity().compareTo(share.getQuantity()) == 0;
      boolean priceMatch = selectedShareKey.purchasePrice().compareTo(share.getPurchasePrice()) == 0;
      if (symbolMatch && quantityMatch && priceMatch) {
        portfolioTable.getSelectionModel().select(i);
        return;
      }
    }
  }

  private record ShareSelectionKey(String symbol, BigDecimal quantity, BigDecimal purchasePrice) {
  }

  private void updateProfileIdentityButton() {
    Node avatar = AvatarUtil.createImageView(gameController.getPlayerAvatar(), 23.4);
    profileNameText.setText(truncateProfileName(gameController.getPlayerName()));
    profileIdentityContent.getChildren().setAll(avatar, profileNameBox);
    profileBtn.setGraphic(profileIdentityContent);
    profileBtn.setText(null);
    Platform.runLater(this::fitProfileNameGlyph);
  }

  private static String truncateProfileName(String name) {
    if (name == null || name.isBlank()) {
      return "";
    }
    String trimmed = name.trim();
    if (trimmed.length() <= PROFILE_NAME_MAX_CHARS) {
      return trimmed;
    }
    return trimmed.substring(0, PROFILE_NAME_MAX_CHARS - 3) + "...";
  }

  private void fitProfileNameGlyph() {
    if (profileNameText.getText() == null || profileNameText.getText().isBlank()) {
      return;
    }

    double availableHeight = profileNameBox.getHeight();
    if (availableHeight <= 0) {
      availableHeight = Math.max(10.0, profileBtn.getHeight() - 12.0);
    }
    availableHeight = Math.max(10.0, availableHeight - 3.0);
    double glyphHeight = profileNameText.getLayoutBounds().getHeight();
    if (glyphHeight <= 0) {
      return;
    }

    double scale = availableHeight / glyphHeight;
    scale = Math.max(0.86, Math.min(1.15, scale));
    profileNameText.setScaleX(scale);
    profileNameText.setScaleY(scale);
  }

  private void rebuildFilterChips() {
    filterChipsPane.getChildren().clear();

    // ── "All" chip always first (clears active filters) ──────────────────
    Button allChip = new Button("All");
    allChip.getStyleClass().add("stock-filter-chip");
    if (activeFilters.isEmpty()) {
      allChip.getStyleClass().add("stock-filter-chip-active");
    }
    allChip.setOnAction(ev -> {
      notifyPanelOpen();
      activeFilters.clear();
      rebuildFilterChips();
      applyFilter();
    });
    filterChipsPane.getChildren().add(allChip);

    // ── Reorderable filter chips ──────────────────────────────────────────
    for (String key : filterChipOrder) {
      String label = switch (key) {
        case "FAVORITES" -> "\u2605 Favorites";
        case "OWNED" -> "Owned";
        case "UP" -> "\u25B2 Up";
        case "DOWN" -> "\u25BC Down";
        default -> key;
      };
      Button chip = new Button(label);
      chip.getStyleClass().add("stock-filter-chip");
      if (activeFilters.contains(key)) {
        chip.getStyleClass().add("stock-filter-chip-active");
      }

      chip.setOnAction(ev -> {
        notifyPanelOpen();
        if (activeFilters.contains(key)) {
          activeFilters.remove(key);
        } else {
          activeFilters.add(key);
        }
        rebuildFilterChips();
        applyFilter();
      });

      // ── Drag to reorder ───────────────────────────────────────────────
      chip.setOnDragDetected(ev -> {
        Dragboard db = chip.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent cc = new ClipboardContent();
        cc.putString(key);
        db.setContent(cc);
        chip.getStyleClass().add("stock-filter-chip-dragging");
        ev.consume();
      });
      chip.setOnDragOver(ev -> {
        if (ev.getDragboard().hasString() && !ev.getDragboard().getString().equals(key)) {
          ev.acceptTransferModes(TransferMode.MOVE);
        }
        ev.consume();
      });
      chip.setOnDragDropped(ev -> {
        String dragged = ev.getDragboard().getString();
        int fromIdx = filterChipOrder.indexOf(dragged);
        int toIdx = filterChipOrder.indexOf(key);
        if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
          notifyPanelOpen();
          filterChipOrder.remove(fromIdx);
          filterChipOrder.add(toIdx, dragged);
          rebuildFilterChips();
          applyFilter();
        }
        ev.setDropCompleted(true);
        ev.consume();
      });
      chip.setOnDragDone(ev -> chip.getStyleClass().remove("stock-filter-chip-dragging"));

      filterChipsPane.getChildren().add(chip);
    }
  }

  private void applyFilter() {
    String lower = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

    // Active filters in chip order (so priority = left-to-right order the user set)
    List<String> orderedFilters = filterChipOrder.stream()
        .filter(activeFilters::contains).toList();

    filteredStocks.setPredicate(s -> {
      boolean textMatch = lower.isEmpty()
          || s.getSymbol().toLowerCase().contains(lower)
          || s.getCompany().toLowerCase().contains(lower);
      if (!textMatch) {
        return false;
      }
      // With no filters active show everything; otherwise show stocks that
      // match at least one active filter (ordering is handled in sort below)
      if (orderedFilters.isEmpty()) {
        return true;
      }
      return orderedFilters.stream().anyMatch(f -> matchesFilter(s, f));
    });

    java.util.Comparator<Stock> sortCmp = switch (stockSort) {
      case "PRICE_ASC" -> java.util.Comparator.comparing(Stock::getSalesPrice);
      case "PRICE_DESC" -> java.util.Comparator.comparing(Stock::getSalesPrice).reversed();
      case "CHG_ASC" -> java.util.Comparator.comparing(Stock::percentageChange);
      case "CHG_DESC" -> java.util.Comparator.comparing(Stock::percentageChange).reversed();
      case "NAME_DESC" -> java.util.Comparator.comparing(Stock::getSymbol).reversed();
      default -> java.util.Comparator.comparing(Stock::getSymbol);
    };

    if (orderedFilters.size() > 1) {
      // Primary sort by group:
      //   rank 0 = matches ALL active filters (the intersection)
      //   rank 1 = matches only orderedFilters[0]
      //   rank 2 = matches only orderedFilters[1]  … etc.
      // This respects the chip order the user has dragged them into.
      java.util.Comparator<Stock> filterPriority = java.util.Comparator.comparingInt((Stock s) -> {
        boolean matchesAll = orderedFilters.stream().allMatch(f -> matchesFilter(s, f));
        if (matchesAll) {
          return 0;
        }
        for (int i = 0; i < orderedFilters.size(); i++) {
          if (matchesFilter(s, orderedFilters.get(i))) {
            return i + 1;
          }
        }
        return orderedFilters.size() + 1; // shouldn't occur (predicate already excluded)
      });
      sortCmp = filterPriority.thenComparing(sortCmp);
    }

    rebuildStockList(sortCmp);
  }

  private boolean matchesFilter(Stock s, String filter) {
    return switch (filter) {
      case "OWNED" -> gameController.isOwned(s.getSymbol());
      case "UP" -> s.percentageChange().compareTo(BigDecimal.ZERO) > 0;
      case "DOWN" -> s.percentageChange().compareTo(BigDecimal.ZERO) < 0;
      case "FAVORITES" -> favorites.contains(s.getSymbol());
      default -> true;
    };
  }

  private void focusStockCardInList(String symbol) {
    Platform.runLater(() -> {
      if (performanceMode) {
        for (int i = 0; i < sortedStocks.size(); i++) {
          if (sortedStocks.get(i).getSymbol().equals(symbol)) {
            stockListView.getSelectionModel().select(i);
            stockListView.scrollTo(i);
            break;
          }
        }
        return;
      }

      int total = stockListBox.getChildren().size();
      for (int i = 0; i < total; i++) {
        Node n = stockListBox.getChildren().get(i);
        Object data = n.getUserData();
        if (data instanceof String cardSymbol && cardSymbol.equals(symbol)) {
          stockScroll.setVvalue(total <= 1 ? 0 : (double) i / (double) (total - 1));
          break;
        }
      }
    });
  }

  private void notifyStockSelectionChanged() {
    if (onStockSelectionChanged != null) {
      onStockSelectionChanged.run();
    }
  }

  private void notifyPanelOpen() {
    if (onPanelOpen != null) {
      onPanelOpen.run();
    }
  }

  public void selectStockBySymbol(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      return;
    }

    boolean existsInFiltered = filteredStocks.stream().anyMatch(s -> s.getSymbol().equals(symbol));
    if (!existsInFiltered) {
      searchField.setText("");
    }

    allStocks.stream()
        .filter(s -> s.getSymbol().equals(symbol))
        .findFirst()
        .ifPresent(target -> {
          if (selectedStock.get() == null || !symbol.equals(selectedStock.get().getSymbol())) {
            notifyStockSelectionChanged();
          }
          selectedStock.set(target);
          focusStockCardInList(symbol);
        });
  }

  private void rebuildDetail() {
    detailArea.getChildren().clear();
    Stock stock = selectedStock.get();
    if (stock == null) {
      Label hint = new Label("\u2190  Select a stock to view details");
      hint.getStyleClass().add("detail-no-selection-hint");
      StackPane placeholder = new StackPane(hint);
      placeholder.getStyleClass().add("detail-no-selection");
      VBox.setVgrow(placeholder, Priority.ALWAYS);
      detailArea.getChildren().add(placeholder);
      return;
    }

    Label sym = new Label(stock.getSymbol());
    sym.getStyleClass().add("detail-symbol");
    Label comp = new Label(stock.getCompany());
    comp.getStyleClass().add("detail-company");
    Label price = new Label(CurrencyFormatter.format(stock.getSalesPrice()));
    price.getStyleClass().add("detail-price");

    BigDecimal pct = stock.percentageChange();
    Label pctBadge;
    if (pct.compareTo(BigDecimal.ZERO) == 0) {
      pctBadge = new Label("\u2014");
      pctBadge.getStyleClass().add("detail-badge-neutral");
    } else {
      String sign = pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
      pctBadge = new Label(sign + pct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
      pctBadge.getStyleClass()
          .add(pct.compareTo(BigDecimal.ZERO) > 0 ? "detail-badge-up" : "detail-badge-down");
    }

    // ── Fav star button (right of symbol) ────────────────────────────────
    boolean isFavDetail = favorites.contains(stock.getSymbol());
    Button detailFavBtn = new Button(isFavDetail ? "\u2605" : "\u2606");
    detailFavBtn.getStyleClass().add("detail-fav-btn");
    if (isFavDetail) {
      detailFavBtn.getStyleClass().add("detail-fav-btn-active");
    }
    detailFavBtn.setOnAction(ev -> {
      if (favorites.contains(stock.getSymbol())) {
        favorites.remove(stock.getSymbol());
      } else {
        favorites.add(stock.getSymbol());
      }
      boolean nowFav = favorites.contains(stock.getSymbol());
      detailFavBtn.setText(nowFav ? "\u2605" : "\u2606");
      if (nowFav) {
        detailFavBtn.getStyleClass().add("detail-fav-btn-active");
      } else {
        detailFavBtn.getStyleClass().remove("detail-fav-btn-active");
      }
      applyFilter();
    });

    Region symSpacer = new Region();
    HBox.setHgrow(symSpacer, Priority.ALWAYS);
    HBox symRow = new HBox(8, sym, symSpacer, detailFavBtn);
    symRow.setAlignment(Pos.CENTER_LEFT);

    // ── Owned badge (right of price row) ─────────────────────────────────
    BigDecimal ownedQtyDetail = gameController.getOwnedQuantity(stock.getSymbol());
    BigDecimal capQtyDetail = gameController.getStockOwnershipCap(stock);
    HBox priceRow = new HBox(12, price, pctBadge);
    priceRow.setAlignment(Pos.BASELINE_LEFT);

    // H/L stats
    BigDecimal hi = stock.allTimeHigh();
    BigDecimal lo = stock.allTimeLow();

    VBox hiBox = new VBox(2, labelSmall("ALL TIME HIGH"), new Label(CurrencyFormatter.format(hi)) {{
      getStyleClass().add("stat-hl-value-up");
    }});
    VBox loBox = new VBox(2, labelSmall("ALL TIME LOW"), new Label(CurrencyFormatter.format(lo)) {{
      getStyleClass().add("stat-hl-value-down");
    }});

    // Always include ownedBox so hlRow height stays constant regardless of ownership
    Label ownedBadge = new Label("Owned: "
      + ownedQtyDetail.stripTrailingZeros().toPlainString()
      + " / "
      + capQtyDetail.stripTrailingZeros().toPlainString()
      + " max");
    ownedBadge.getStyleClass().add("detail-owned-badge");
    VBox ownedBox = new VBox(2, labelSmall("HOLDING"), ownedBadge);
    ownedBox.setAlignment(Pos.BOTTOM_RIGHT);
    Region hlSpacer = new Region();
    HBox.setHgrow(hlSpacer, Priority.ALWAYS);
    HBox hlRow = new HBox(24, hiBox, loBox, hlSpacer, ownedBox);

    // ── quantity stepper: [−] [field] [+] ─────────────────────────────────────
    Button decBtn = new Button("\u2212");
    decBtn.getStyleClass().add("trade-quantity-btn");
    TextField quantityField = new TextField("1");
    quantityField.getStyleClass().add("trade-quantity-field");
    Button incBtn = new Button("+");
    incBtn.getStyleClass().add("trade-quantity-btn");

    TextField amountField = new TextField();
    amountField.setPromptText("Enter amount ($)");
    amountField.getStyleClass().add("trade-amount-field");
    Button maxBuyBtn = new Button("\u25b2  Buy Max");
    maxBuyBtn.getStyleClass().add("trade-max-buy-button");
    Button maxSellBtn = new Button("\u25bc  Sell Max");
    maxSellBtn.getStyleClass().add("trade-max-sell-button");

    final boolean[] syncingFields = {false};
    final boolean[] amountTracksSell = {false};

    final Runnable normalizequantityAndAmount = () -> {
      BigDecimal unitCostWithFee = gameController.unitCostWithFee(stock);

      int quantity;
      try {
        quantity = NumberParser.parse(quantityField.getText()).intValue();
      } catch (NumberFormatException ex) {
        quantity = 1;
      }
      if (quantity < 0) {
        quantity = 0;
      }

      syncingFields[0] = true;
      quantityField.setText(String.valueOf(quantity));
      amountField.setText(
          unitCostWithFee.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP)
              .toPlainString());
      syncingFields[0] = false;
    };

    decBtn.setOnAction(e -> {
      amountTracksSell[0] = false;
      try {
        int v = Math.max(0, NumberParser.parse(quantityField.getText()).intValue() - 1);
        quantityField.setText(String.valueOf(v));
      } catch (NumberFormatException ignored) {
        quantityField.setText("1");
      }
      normalizequantityAndAmount.run();
    });
    incBtn.setOnAction(e -> {
      amountTracksSell[0] = false;
      try {
        quantityField.setText(
            String.valueOf(NumberParser.parse(quantityField.getText()).intValue() + 1));
      } catch (NumberFormatException ignored) {
        quantityField.setText("1");
      }
      normalizequantityAndAmount.run();
    });

    HBox stepper = new HBox(0, decBtn, quantityField, incBtn);
    stepper.getStyleClass().add("trade-quantity-stepper");
    stepper.setAlignment(Pos.CENTER);

    // ── Live cost / proceeds labels ────────────────────────────────────────
    Label buyAmountLbl = new Label("\u2014");
    buyAmountLbl.getStyleClass().add("trade-button-amount");
    Label sellAmountLbl = new Label("\u2014");
    sellAmountLbl.getStyleClass().add("trade-button-amount");

    // Both labels track the quantity field
    final Runnable updateBuyAmount = () -> {
      int quantity;
      try {
        quantity = NumberParser.parse(quantityField.getText()).intValue();
      } catch (NumberFormatException ex) {
        buyAmountLbl.setText("\u2014");
        return;
      }
      if (quantity < 1) {
        buyAmountLbl.setText("\u2014");
        return;
      }
      buyAmountLbl.setText(CurrencyFormatter.format(
          gameController.unitCostWithFee(stock).multiply(BigDecimal.valueOf(quantity))));
    };
    final Runnable updateSellAmount = () -> {
      int quantity;
      try {
        quantity = NumberParser.parse(quantityField.getText()).intValue();
      } catch (NumberFormatException ex) {
        sellAmountLbl.setText("\u2014");
        return;
      }
      if (quantity < 1) {
        sellAmountLbl.setText("\u2014");
        return;
      }
      List<BigDecimal> p = gameController.previewSell(stock, BigDecimal.valueOf(quantity));
      sellAmountLbl.setText(p == null ? "\u2014" : CurrencyFormatter.format(p.get(3)));
    };
    final Runnable applyMaxForBuy = () -> {
      BigDecimal unitCostWithFee = gameController.unitCostWithFee(stock);
      int maxBuyquantity = gameController.maxBuyQuantity(stock);
      maxBuyquantity = Math.max(0, maxBuyquantity);
      amountTracksSell[0] = false;
      syncingFields[0] = true;
      quantityField.setText(String.valueOf(maxBuyquantity));
      amountField.setText(unitCostWithFee.multiply(BigDecimal.valueOf(maxBuyquantity))
          .setScale(2, RoundingMode.HALF_UP).toPlainString());
      syncingFields[0] = false;
      updateBuyAmount.run();
      updateSellAmount.run();
    };
    final Runnable applyMaxForSell = () -> {
      int maxSellQuantity = gameController.maxSellQuantity(stock);
      maxSellQuantity = Math.max(0, maxSellQuantity);
      amountTracksSell[0] = true;
      syncingFields[0] = true;
      quantityField.setText(String.valueOf(maxSellQuantity));
      List<BigDecimal> sellPreview =
          gameController.previewSell(stock, BigDecimal.valueOf(maxSellQuantity));
      amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview.get(3)).setScale(2,
          RoundingMode.HALF_UP).toPlainString());
      syncingFields[0] = false;
      updateBuyAmount.run();
      updateSellAmount.run();
    };

    maxBuyBtn.setOnAction(e -> {
      applyMaxForBuy.run();
      if (rootRef != null) {
        rootRef.requestFocus();
      }
    });
    maxSellBtn.setOnAction(e -> {
      applyMaxForSell.run();
      if (rootRef != null) {
        rootRef.requestFocus();
      }
    });

    Label tradeErrorLbl = new Label();
    tradeErrorLbl.getStyleClass().add("trade-error-label");
    tradeErrorLbl.setMaxWidth(Double.MAX_VALUE);
    tradeErrorLbl.setAlignment(Pos.CENTER);
    tradeErrorLbl.setWrapText(true);
    VBox tradeErrorBox = createInlineErrorBox(tradeErrorLbl);
    currentTradeErrorLabel = tradeErrorLbl;

    final Runnable clearTradeError = () -> {
      hideInlineError(tradeErrorLbl, true);
    };

    updateBuyAmount.run();
    updateSellAmount.run();
    quantityField.textProperty().addListener((obs, old, val) -> {
      if (syncingFields[0]) {
        return;
      }
      clearTradeError.run();
      // Allow the field to be empty while the user is typing
      if (val == null || val.isBlank()) {
        updateBuyAmount.run();
        updateSellAmount.run();
        return;
      }
      int quantity;
      try {
        quantity = NumberParser.parse(val).intValue();
      } catch (NumberFormatException ex) {
        // Don't overwrite while user is mid-type
        return;
      }
      quantity = Math.max(0, quantity);
      syncingFields[0] = true;
      quantityField.setText(String.valueOf(quantity));
      if (amountTracksSell[0]) {
        List<BigDecimal> sellPreview =
            gameController.previewSell(stock, BigDecimal.valueOf(quantity));
        amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview.get(3))
            .setScale(2, RoundingMode.HALF_UP).toPlainString());
      } else {
        amountField.setText(
            gameController.unitCostWithFee(stock).multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
      }
      syncingFields[0] = false;
      updateBuyAmount.run();
      updateSellAmount.run();
    });
    amountField.textProperty().addListener((obs, old, val) -> {
      if (syncingFields[0]) {
        return;
      }
      amountTracksSell[0] = false;
      BigDecimal amount;
      try {
        amount = NumberParser.parse(val);
      } catch (NumberFormatException ex) {
        return;
      }
      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        syncingFields[0] = true;
        quantityField.setText("0");
        syncingFields[0] = false;
        updateBuyAmount.run();
        updateSellAmount.run();
        return;
      }
      BigDecimal unitCostWithFee = gameController.unitCostWithFee(stock);
      int quantity = amount.divide(unitCostWithFee, 0, RoundingMode.DOWN).intValue();
      quantity = Math.max(0, quantity);
      syncingFields[0] = true;
      quantityField.setText(String.valueOf(quantity));
      syncingFields[0] = false;
      updateBuyAmount.run();
      updateSellAmount.run();
    });

    // ── BUY button (primary — flex) ────────────────────────────────────────
    Label buyTopLbl = new Label("\u2197  BUY");
    buyTopLbl.getStyleClass().add("trade-btn-label");
    VBox buyGraphic = new VBox(3, buyTopLbl, buyAmountLbl);
    buyGraphic.setAlignment(Pos.CENTER);

    Button buyBtn = new Button();
    buyBtn.setGraphic(buyGraphic);
    buyBtn.getStyleClass().add("trade-buy-button");
    buyBtn.setOnAction(e -> {
      int parsedquantity;
      String qText = quantityField.getText();
      if (qText == null || qText.isBlank()) {
        showTradeError("Enter at least 1 share to buy.");
        return;
      }
      try {
        parsedquantity = NumberParser.parse(qText).intValue();
      } catch (NumberFormatException ex) {
        showTradeError("That doesn't look like a valid number.");
        return;
      }

      if (parsedquantity < 1) {
        showTradeError("Enter at least 1 share to buy.");
        return;
      }

      clearTradeError.run();
      BigDecimal quantity = BigDecimal.valueOf(parsedquantity);
      gameController.handleBuy(stock, quantity);
      amountField.clear();
      if (rootRef != null) {
        rootRef.requestFocus();
      }
    });

    // ── SELL button (secondary — fixed width) ──────────────────────────────
    Label sellTopLbl = new Label("\u2198  SELL");
    sellTopLbl.getStyleClass().add("trade-btn-label-sell");
    VBox sellGraphic = new VBox(3, sellTopLbl, sellAmountLbl);
    sellGraphic.setAlignment(Pos.CENTER);

    Button sellBtn = new Button();
    sellBtn.setGraphic(sellGraphic);
    sellBtn.getStyleClass().add("trade-sell-button");
    sellBtn.setOnAction(e -> {
      int parsedquantity;
      String qText = quantityField.getText();
      if (qText == null || qText.isBlank()) {
        showTradeError("Enter at least 1 share to sell.");
        return;
      }
      try {
        parsedquantity = NumberParser.parse(qText).intValue();
      } catch (NumberFormatException ex) {
        showTradeError("That doesn't look like a valid number.");
        return;
      }

      if (parsedquantity < 1) {
        showTradeError("Enter at least 1 share to sell.");
        return;
      }

      BigDecimal sellquantity = BigDecimal.valueOf(parsedquantity);
      BigDecimal totalOwned = gameController.getOwnedQuantity(stock.getSymbol());
      if (totalOwned.compareTo(BigDecimal.ZERO) == 0) {
        showTradeError("You don't own any " + stock.getSymbol() + " shares to sell.");
        return;
      }
      if (sellquantity.compareTo(totalOwned) > 0) {
        showTradeError("Too many - you only own "
          + totalOwned.stripTrailingZeros().toPlainString() + " " + stock.getSymbol() + " shares.");
        return;
      }
      clearTradeError.run();
      List<BigDecimal> preview = gameController.previewSell(stock, sellquantity);
      showTradeConfirm("SELL", stock, sellquantity, preview.get(0), preview.get(1), preview.get(2),
          preview.get(3));
      amountField.clear();
      if (rootRef != null) {
        rootRef.requestFocus();
      }
    });

    VBox selectorColumn = new VBox(4, stepper, amountField, tradeErrorBox);
    selectorColumn.getStyleClass().add("trade-selector-column");
    selectorColumn.setAlignment(Pos.CENTER);

    VBox buyColumn = new VBox(6, buyBtn, maxBuyBtn);
    buyColumn.getStyleClass().add("trade-action-column");
    buyColumn.getStyleClass().add("trade-buy-column");
    buyColumn.setAlignment(Pos.CENTER_RIGHT);

    VBox sellColumn = new VBox(6, sellBtn, maxSellBtn);
    sellColumn.getStyleClass().add("trade-action-column");
    sellColumn.getStyleClass().add("trade-sell-column");
    sellColumn.setAlignment(Pos.CENTER_LEFT);

    HBox tradeRow = new HBox(8, buyColumn, selectorColumn, sellColumn);
    tradeRow.setAlignment(Pos.CENTER);
    VBox tradePanel = new VBox(0, tradeRow);
    tradePanel.getStyleClass().add("trade-panel");
    tradePanel.setAlignment(Pos.CENTER);
    this.tutorialTradeAreaTarget = tradePanel;

    // ── Graph + header ────────────────────────────────────────────────────
    Pane graphPlaceholder = buildPriceChart(stock);
    VBox.setVgrow(graphPlaceholder, Priority.ALWAYS);

    VBox header = new VBox(4, symRow, comp, priceRow, hlRow, graphPlaceholder, tradePanel);
    header.getStyleClass().add("game-detail-header");

    detailArea.getChildren().add(header);
    VBox.setVgrow(header, Priority.ALWAYS);
  }

  private static TableView<Share> buildPortfolioTable(ObservableList<Share> items,
                                                      java.util.function.Consumer<Stock> onSelectStock) {
    TableView<Share> table = new TableView<>(items);
    table.getStyleClass().add("game-table");
    table.setPlaceholder(new Label("No shares owned yet"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setRowFactory(tv -> {
      TableRow<Share> row = new TableRow<>();
      row.setOnMouseClicked(e -> {
        if (!row.isEmpty() && row.getItem() != null) {
          onSelectStock.accept(row.getItem().getStock());
        }
      });
      return row;
    });

    TableColumn<Share, String> symCol = col("Symbol", c ->
        c.getStock().getSymbol(), 70, 85);
    TableColumn<Share, BigDecimal> quantityCol = numericCol("Quantity", Share::getQuantity, 45, 65,
        value -> value.stripTrailingZeros().toPlainString());
    TableColumn<Share, BigDecimal> boughtCol = numericCol("Bought",
        c -> c.getPurchasePrice().multiply(c.getQuantity()), 85, 110, CurrencyFormatter::format);
    TableColumn<Share, BigDecimal> nowCol = numericCol("Now",
        c -> c.getStock().getSalesPrice().multiply(c.getQuantity()), 85, 110, CurrencyFormatter::format);

    TableColumn<Share, BigDecimal> plCol = new TableColumn<>("P&L");
    plCol.setMinWidth(90);
    plCol.setMaxWidth(120);
    plCol.setCellValueFactory(c -> new SimpleObjectProperty<>(
        c.getValue().getStock().getSalesPrice().subtract(c.getValue().getPurchasePrice())
            .multiply(c.getValue().getQuantity())));
    plCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setStyle("");
          return;
        }
        if (item.compareTo(BigDecimal.ZERO) == 0) {
          setText("\u2014");
          setStyle("-fx-text-fill: #4a6899;");
        } else {
          boolean up = item.compareTo(BigDecimal.ZERO) > 0;
          setText((up ? "+" : "") + CurrencyFormatter.format(item));
          setStyle(up ? "-fx-text-fill: #4ecb71;" : "-fx-text-fill: #e05a5a;");
        }
      }
    });

    TableColumn<Share, BigDecimal> pctCol = new TableColumn<>("%");
    pctCol.setMinWidth(72);
    pctCol.setMaxWidth(90);
    pctCol.setCellValueFactory(c -> {
      Share sh = c.getValue();
      BigDecimal cost = sh.getPurchasePrice();
      if (cost.compareTo(BigDecimal.ZERO) == 0) {
        return new SimpleObjectProperty<>(BigDecimal.ZERO);
      }
      return new SimpleObjectProperty<>(sh.getStock().getSalesPrice().subtract(cost)
          .divide(cost, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100))
          .setScale(2, RoundingMode.HALF_UP));
    });
    pctCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setStyle("");
          return;
        }
        if (item.compareTo(BigDecimal.ZERO) == 0) {
          setText("\u2014");
          setStyle("-fx-text-fill: #4a6899;");
        } else {
          boolean up = item.compareTo(BigDecimal.ZERO) > 0;
          setText((up ? "+" : "") + item.toPlainString() + "%");
          setStyle(up ? "-fx-text-fill: #4ecb71;" : "-fx-text-fill: #e05a5a;");
        }
      }
    });

    table.getColumns().add(symCol);
    table.getColumns().add(quantityCol);
    table.getColumns().add(boughtCol);
    table.getColumns().add(nowCol);
    table.getColumns().add(plCol);
    table.getColumns().add(pctCol);
    return table;
  }

  public void showError(String message) {
    if (overlayRef == null) {
      return;
    }

    Label iconLbl = new Label("\u26A0");
    iconLbl.getStyleClass().add("error-dialog-icon");
    Label titleLbl = new Label("ERROR");
    titleLbl.getStyleClass().add("error-dialog-title");
    HBox header = new HBox(10, iconLbl, titleLbl);
    header.getStyleClass().add("error-dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    Label msgLbl = new Label(message != null ? message : "An unexpected error occurred.");
    msgLbl.getStyleClass().add("error-dialog-message");
    msgLbl.setWrapText(true);
    msgLbl.setMaxWidth(300);
    VBox msgBox = new VBox(msgLbl);
    msgBox.getStyleClass().add("error-dialog-body");

    Button okBtn = new Button("OK");
    okBtn.getStyleClass().add("dialog-confirm-sell-btn");
    HBox btnRow = new HBox(okBtn);
    btnRow.setAlignment(Pos.CENTER_RIGHT);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, msgBox, btnRow);
    card.getStyleClass().add("error-dialog-root");
    card.setMaxWidth(360);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("error-dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> {
      stopErrorTransitions(popup);
      overlayRef.getChildren().remove(popup);
    };
    okBtn.setOnAction(ev -> dismiss.run());
    backdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    overlayRef.getChildren().add(popup);
    popup.setOpacity(1.0);
    scheduleFadeOutAndRemove(popup, POPUP_ERROR_VISIBLE, POPUP_ERROR_FADE,
        () -> overlayRef.getChildren().remove(popup));
    popup.requestFocus();
  }

  public void showTradeError(String message) {
    if (currentTradeErrorLabel == null) {
      return;
    }
    showFadingInlineError(currentTradeErrorLabel, message);
  }

  public void showSellAllError(String message) {
    if (currentSellAllErrorLabel == null) {
      return;
    }
    showFadingInlineError(currentSellAllErrorLabel, message);
  }

  private void showFadingInlineError(Label label, String message) {
    label.setText(message != null ? message : "An error occurred.");
    animateInlineErrorVisibility(label, true, null);

    stopErrorTransitions(label);

    PauseTransition pause = new PauseTransition(INLINE_ERROR_VISIBLE);
    FadeTransition fade = new FadeTransition(INLINE_ERROR_FADE, resolveInlineErrorContainer(label));
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    fade.setOnFinished(ev -> {
      animateInlineErrorVisibility(label, false, () -> label.setText(""));
      label.getProperties().remove(ERROR_PAUSE_KEY);
      label.getProperties().remove(ERROR_FADE_KEY);
    });
    pause.setOnFinished(ev -> fade.playFromStart());

    label.getProperties().put(ERROR_PAUSE_KEY, pause);
    label.getProperties().put(ERROR_FADE_KEY, fade);
    pause.playFromStart();
  }

  private VBox createInlineErrorBox(Label label) {
    VBox box = new VBox(label);
    box.setAlignment(Pos.CENTER);
    box.setMinHeight(0);
    box.setMaxHeight(0);
    box.setOpacity(0.0);

    Rectangle clip = new Rectangle();
    clip.widthProperty().bind(box.widthProperty());
    clip.setHeight(0);
    box.setClip(clip);
    box.managedProperty().bind(box.maxHeightProperty().greaterThan(0));

    label.getProperties().put(ERROR_CONTAINER_KEY, box);
    label.getProperties().put(ERROR_CONTAINER_CLIP_KEY, clip);
    return box;
  }

  private void hideInlineError(Label label, boolean clearText) {
    if (label == null) {
      return;
    }
    stopErrorTransitions(label);
    animateInlineErrorVisibility(label, false, clearText ? () -> label.setText("") : null);
  }

  private void animateInlineErrorVisibility(Label label, boolean visible, Runnable onFinished) {
    VBox box = resolveInlineErrorContainer(label);
    Rectangle clip = resolveInlineErrorClip(label);

    Object existing = label.getProperties().remove(ERROR_SIZE_KEY);
    if (existing instanceof Timeline oldTimeline) {
      oldTimeline.stop();
    }

    double from = Math.max(box.getHeight(), box.getMaxHeight());
    double to = visible ? Math.max(18.0, label.prefHeight(Math.max(0, label.getWidth())) + 4.0) : 0.0;

    if (visible) {
      box.setOpacity(1.0);
    }

    Timeline timeline = new Timeline(
        new KeyFrame(Duration.ZERO,
            new KeyValue(box.maxHeightProperty(), from, Interpolator.EASE_BOTH),
            new KeyValue(clip.heightProperty(), from, Interpolator.EASE_BOTH),
            new KeyValue(box.opacityProperty(), box.getOpacity(), Interpolator.EASE_BOTH)),
        new KeyFrame(INLINE_ERROR_SIZE_ANIM,
            new KeyValue(box.maxHeightProperty(), to, Interpolator.EASE_BOTH),
            new KeyValue(clip.heightProperty(), to, Interpolator.EASE_BOTH),
            new KeyValue(box.opacityProperty(), visible ? 1.0 : 0.0, Interpolator.EASE_BOTH))
    );
    timeline.setOnFinished(ev -> {
      if (!visible) {
        box.setMaxHeight(0);
        clip.setHeight(0);
        box.setOpacity(0.0);
      }
      label.getProperties().remove(ERROR_SIZE_KEY);
      if (onFinished != null) {
        onFinished.run();
      }
    });

    label.getProperties().put(ERROR_SIZE_KEY, timeline);
    timeline.playFromStart();
  }

  private VBox resolveInlineErrorContainer(Label label) {
    Object container = label.getProperties().get(ERROR_CONTAINER_KEY);
    if (container instanceof VBox box) {
      return box;
    }
    return new VBox(label);
  }

  private Rectangle resolveInlineErrorClip(Label label) {
    Object clip = label.getProperties().get(ERROR_CONTAINER_CLIP_KEY);
    if (clip instanceof Rectangle rectangle) {
      return rectangle;
    }
    Rectangle fallback = new Rectangle();
    fallback.setHeight(0);
    return fallback;
  }

  private void scheduleFadeOutAndRemove(Node node, Duration visibleDuration,
                                        Duration fadeDuration, Runnable onRemove) {
    stopErrorTransitions(node);

    PauseTransition pause = new PauseTransition(visibleDuration);
    FadeTransition fade = new FadeTransition(fadeDuration, node);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    fade.setOnFinished(ev -> {
      onRemove.run();
      node.setOpacity(1.0);
      node.getProperties().remove(ERROR_PAUSE_KEY);
      node.getProperties().remove(ERROR_FADE_KEY);
    });
    pause.setOnFinished(ev -> fade.playFromStart());

    node.getProperties().put(ERROR_PAUSE_KEY, pause);
    node.getProperties().put(ERROR_FADE_KEY, fade);
    pause.playFromStart();
  }

  private void stopErrorTransitions(Node node) {
    Object pause = node.getProperties().remove(ERROR_PAUSE_KEY);
    if (pause instanceof javafx.animation.Animation p) {
      p.stop();
    }
    Object fade = node.getProperties().remove(ERROR_FADE_KEY);
    if (fade instanceof javafx.animation.Animation f) {
      f.stop();
    }
  }

  public void showBulkTradeConfirm(String action, BigDecimal quantity, BigDecimal gross,
                                   BigDecimal fee, BigDecimal tax, BigDecimal total) {
    suspendTutorialOverlay();
    Label iconLbl = new Label("\u2198");
    iconLbl.getStyleClass().add("dialog-action-icon-sell");
    Label titleLbl = new Label("ORDER SUMMARY");
    titleLbl.getStyleClass().add("dialog-title");
    HBox header = new HBox(10, iconLbl, titleLbl);
    header.getStyleClass().add("dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    VBox rows = new VBox(0,
        dialogRow("Action", action, "dialog-val-sell"),
        dialogRow("Symbols", "ALL", null),
        dialogRow("Quantity", quantity.stripTrailingZeros().toPlainString(), null),
        dialogRow("Subtotal", CurrencyFormatter.format(gross), null),
        dialogRow("Fee (1%)", CurrencyFormatter.format(fee), "dialog-val-fee"),
        dialogRow("Tax", CurrencyFormatter.format(tax), "dialog-val-fee")
    );
    rows.getStyleClass().add("dialog-rows");

    Label totalKey = new Label("YOU RECEIVE");
    totalKey.getStyleClass().add("dialog-total-key");
    Label totalVal = new Label(CurrencyFormatter.format(total));
    totalVal.getStyleClass().add("dialog-total-val-sell");
    VBox totalSection = new VBox(4, totalKey, totalVal);
    totalSection.getStyleClass().add("dialog-total-section");

    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");
    Button confirmBtn = new Button("Confirm Sell All");
    confirmBtn.getStyleClass().add("dialog-confirm-sell-btn");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox btnRow = new HBox(10, cancelBtn, spacer, confirmBtn);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, rows, totalSection, btnRow);
    card.getStyleClass().add("trade-dialog-root");
    card.setMaxWidth(360);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> {
      overlayRef.getChildren().remove(popup);
      resumeTutorialOverlay();
      refreshTutorialProgressFlags();
      maybeAdvanceTutorialAfterTrade();
      if (isTutorialVisible()) {
        updateTutorialStep();
      }
    };
    cancelBtn.setOnAction(ev -> dismiss.run());
    confirmBtn.setOnAction(ev -> {
      dismiss.run();
      gameController.executeSellAll();
    });
    backdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    overlayRef.getChildren().add(popup);
    popup.requestFocus();
  }

  public void showBulkReceipt(String action, BigDecimal quantity, BigDecimal total, BigDecimal fee,
                              BigDecimal tax, BigDecimal newCash) {
    suspendTutorialOverlay();
    Label checkLbl = new Label("\u2713");
    checkLbl.getStyleClass().add("receipt-check");
    Label titleLbl = new Label("ORDER COMPLETE");
    titleLbl.getStyleClass().add("dialog-title");
    HBox header = new HBox(10, checkLbl, titleLbl);
    header.getStyleClass().add("dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    VBox rows = new VBox(0,
        dialogRow("Action", action, "dialog-val-sell"),
        dialogRow("Symbols", "ALL", null),
        dialogRow("Quantity", quantity.stripTrailingZeros().toPlainString(), null),
        dialogRow("Fee (1%)", CurrencyFormatter.format(fee), "dialog-val-fee"),
        dialogRow("Tax", CurrencyFormatter.format(tax), "dialog-val-fee"),
        dialogRow("Received", CurrencyFormatter.format(total), null),
        dialogRow("New Balance", CurrencyFormatter.format(newCash), "dialog-val-cash")
    );
    rows.getStyleClass().add("dialog-rows");

    Button doneBtn = new Button("Done");
    doneBtn.getStyleClass().add("dialog-confirm-buy-btn");
    HBox btnRow = new HBox(doneBtn);
    btnRow.setAlignment(Pos.CENTER_RIGHT);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, rows, btnRow);
    card.getStyleClass().add("trade-dialog-root");
    card.setMaxWidth(340);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> {
      overlayRef.getChildren().remove(popup);
      resumeTutorialOverlay();
      if (isTutorialVisible() && tutorialStepIndex == 2 && isTutorialStepComplete(2)) {
        tutorialStepIndex = 3;
        updateTutorialStep();
      }
    };
    doneBtn.setOnAction(ev -> dismiss.run());
    backdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    overlayRef.getChildren().add(popup);
    popup.requestFocus();
  }

  private static HBox dialogRow(String key, String val, String valStyle) {
    Label kLbl = new Label(key);
    kLbl.getStyleClass().add("dialog-row-key");
    Label vLbl = new Label(val);
    vLbl.getStyleClass().add("dialog-row-val");
    if (valStyle != null) {
      vLbl.getStyleClass().add(valStyle);
    }
    Region sp = new Region();
    HBox.setHgrow(sp, Priority.ALWAYS);
    HBox row = new HBox(8, kLbl, sp, vLbl);
    row.getStyleClass().add("dialog-row");
    return row;
  }

  public void showTradeConfirm(String action, Stock stock, BigDecimal quantity,
                               BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total) {
    suspendTutorialOverlay();
    boolean isBuy = action != null && action.startsWith("BUY");

    Label iconLbl = new Label(isBuy ? "\u2197" : "\u2198");
    iconLbl.getStyleClass().add(isBuy ? "dialog-action-icon-buy" : "dialog-action-icon-sell");
    Label titleLbl = new Label("ORDER SUMMARY");
    titleLbl.getStyleClass().add("dialog-title");
    HBox header = new HBox(10, iconLbl, titleLbl);
    header.getStyleClass().add("dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    VBox rows = new VBox(0,
        dialogRow("Action", action, isBuy ? "dialog-val-buy" : "dialog-val-sell"),
        dialogRow("Symbol", stock.getSymbol(), null),
        dialogRow("Company", stock.getCompany(), null),
        dialogRow("Quantity", quantity.stripTrailingZeros().toPlainString(), null),
        dialogRow("Price", CurrencyFormatter.format(stock.getSalesPrice()), null),
        dialogRow("Subtotal", CurrencyFormatter.format(gross), null),
        dialogRow(isBuy ? "Fee (0.5%)" : "Fee (1%)", CurrencyFormatter.format(fee),
            "dialog-val-fee"),
        dialogRow("Tax", CurrencyFormatter.format(tax), "dialog-val-fee")
    );
    rows.getStyleClass().add("dialog-rows");

    Label totalKey = new Label(isBuy ? "TOTAL COST" : "YOU RECEIVE");
    totalKey.getStyleClass().add("dialog-total-key");
    Label totalVal = new Label(CurrencyFormatter.format(total));
    totalVal.getStyleClass().add(isBuy ? "dialog-total-val-buy" : "dialog-total-val-sell");
    VBox totalSection = new VBox(4, totalKey, totalVal);
    totalSection.getStyleClass().add("dialog-total-section");

    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");
    Button confirmBtn = new Button(isBuy ? "Confirm Buy" : "Confirm Sell");
    confirmBtn.getStyleClass().add(isBuy ? "dialog-confirm-buy-btn" : "dialog-confirm-sell-btn");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox btnRow = new HBox(10, cancelBtn, spacer, confirmBtn);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, rows, totalSection, btnRow);
    card.getStyleClass().add("trade-dialog-root");
    card.setMaxWidth(360);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> {
      overlayRef.getChildren().remove(popup);
      resumeTutorialOverlay();
      refreshTutorialProgressFlags();
      maybeAdvanceTutorialAfterTrade();
      if (isTutorialVisible()) {
        updateTutorialStep();
      }
    };
    cancelBtn.setOnAction(ev -> dismiss.run());
    confirmBtn.setOnAction(ev -> {
      dismiss.run();
      if (isBuy) {
        gameController.executeBuy(stock, quantity, total, fee);
      } else {
        gameController.executeSell(stock, quantity);
      }
    });
    backdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    overlayRef.getChildren().add(popup);
  }

  private void showLevelUpPopup(PlayerStatus newStatus) {
    if (overlayRef == null) {
      return;
    }

    // Play Level_Up.mp3 immediately — audio has built-in fade-in then strong hit at ~1.5s
    if (levelUpClip != null && sfxVolumeSupplierField != null) {
      double vol = Math.min(sfxVolumeSupplierField.getAsDouble() * 1.3, 1.0);
      levelUpClip.play(vol);
    }

    GaussianBlur blur = new GaussianBlur(0);
    rootRef.setEffect(blur);

    Region dimBackdrop = new Region();
    dimBackdrop.getStyleClass().add("level-up-backdrop");
    dimBackdrop.setOpacity(0);

    // ── Title & flavor text ─────────────────────────────────────────────
    String statusName = switch (newStatus) {
      case INVESTOR -> "INVESTOR";
      case SPECULATOR -> "SPECULATOR";
      default -> newStatus.name();
    };
    String tagline = switch (newStatus) {
      case INVESTOR -> "How late did you stay up to get here?.";
      case SPECULATOR -> "It's officially past your bedtime.\nWell done.";
      default -> "Don't stay up too late!.";
    };

    Label titleLbl = new Label("LEVEL UP");
    titleLbl.getStyleClass().add("level-up-title");

    Label statusNameLbl = new Label(statusName);
    statusNameLbl.getStyleClass().add("level-up-status-name");

    Label taglineLbl = new Label(tagline);
    taglineLbl.getStyleClass().add("level-up-tagline");

    VBox titleBlock = new VBox(6, titleLbl, statusNameLbl, taglineLbl);
    titleBlock.setAlignment(Pos.CENTER);

    Region divider = new Region();
    divider.getStyleClass().add("level-up-divider");

    // ── Benefits list ───────────────────────────────────────────────────
    List<String[]> benefits = switch (newStatus) {
      case INVESTOR -> List.of(
          new String[]{"\uD83D\uDCB0", "Tax Rate", "25%  (was 30%)"},
          new String[]{"\uD83D\uDCC8", "Ownership Cap", "Hold \u00D71.5 max shares per stock"},
          new String[]{"\uD83C\uDFA7", "New Avatars", "2 unlocked"}
      );
      case SPECULATOR -> List.of(
          new String[]{"\uD83D\uDCB0", "Tax Rate", "20%  (was 25%)"},
          new String[]{"\uD83D\uDCC8", "Ownership Cap", "Hold \u00D72.0 max shares per stock"},
          new String[]{"\uD83C\uDFA7", "New Avatars", "2 unlocked"}
      );
      default -> List.of();
    };

    final List<HBox> newAvatarsRows = new ArrayList<>();
    VBox benefitsBox = new VBox(8);
    benefitsBox.getStyleClass().add("level-up-benefits-box");
    for (String[] b : benefits) {
      Label emojLbl = new Label(b[0]);
      emojLbl.getStyleClass().add("level-up-benefit-icon");
      Label nameLbl = new Label(b[1]);
      nameLbl.getStyleClass().add("level-up-benefit-name");
      Label valLbl = new Label(b[2]);
      valLbl.getStyleClass().add("level-up-benefit-value");
      Region rowSpacer = new Region();
      HBox.setHgrow(rowSpacer, Priority.ALWAYS);
      HBox row = new HBox(10, emojLbl, nameLbl, rowSpacer, valLbl);
      row.getStyleClass().add("level-up-benefit-row");
      if ("New Avatars".equals(b[1])) {
        row.getStyleClass().add("level-up-benefit-row-clickable");
        newAvatarsRows.add(row);
      }
      row.setAlignment(Pos.CENTER_LEFT);
      benefitsBox.getChildren().add(row);
    }

    // ── Unlocked avatars ────────────────────────────────────────────────
    List<String> unlockedAvatars = switch (newStatus) {
      case INVESTOR -> List.of("man-office-worker", "woman-office-worker");
      case SPECULATOR -> List.of("man-in-tuxedo", "woman-in-tuxedo");
      default -> List.of();
    };

    HBox avatarRow = new HBox(14);
    avatarRow.setAlignment(Pos.CENTER);
    avatarRow.getStyleClass().add("level-up-avatar-row");
    String previousAvatar = gameController.getSelectedPlayerAvatar();
    final String[] selectedPopupAvatar = { null };
    for (String stem : unlockedAvatars) {
      ImageView iv = AvatarUtil.createImageView(stem, 48);
      StackPane avatarFrame = new StackPane(iv);
      avatarFrame.getStyleClass().addAll("level-up-avatar-frame", "level-up-avatar-frame-clickable");
      avatarFrame.setOnMouseClicked(e -> {
        if (stem.equals(selectedPopupAvatar[0])) {
          selectedPopupAvatar[0] = null;
          gameController.setPlayerAvatar(previousAvatar);
          avatarRow.getChildren().forEach(node -> node.getStyleClass().remove("level-up-avatar-frame-selected"));
          updateProfileIdentityButton();
          return;
        }
        selectedPopupAvatar[0] = stem;
        gameController.setPlayerAvatar(stem);
        avatarRow.getChildren().forEach(node -> node.getStyleClass().remove("level-up-avatar-frame-selected"));
        avatarFrame.getStyleClass().add("level-up-avatar-frame-selected");
        updateProfileIdentityButton();
      });
      avatarRow.getChildren().add(avatarFrame);
    }

    Button continueBtn = new Button("Continue");
    continueBtn.getStyleClass().add("level-up-continue-btn");

    VBox headerArea = new VBox(0, titleBlock);
    headerArea.getStyleClass().add("level-up-header-area");
    headerArea.setAlignment(Pos.CENTER);

    VBox bodyArea = new VBox(14, benefitsBox, avatarRow);
    bodyArea.getStyleClass().add("level-up-body-area");
    bodyArea.setAlignment(Pos.CENTER);

    VBox footerArea = new VBox(continueBtn);
    footerArea.getStyleClass().add("level-up-footer-area");
    footerArea.setAlignment(Pos.CENTER);

    VBox card = new VBox(0, headerArea, divider, bodyArea, footerArea);
    card.getStyleClass().add("level-up-card");
    card.setMaxWidth(440);
    card.setMaxHeight(Region.USE_PREF_SIZE);
    card.setAlignment(Pos.CENTER);
    card.setOpacity(0);
    card.setScaleX(0.88);
    card.setScaleY(0.88);

    StackPane popup = new StackPane(dimBackdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);
    overlayRef.getChildren().add(popup);

    // ── Backdrop fades in immediately ───────────────────────────────────
    Timeline blurIn = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
        new KeyFrame(Duration.millis(600),
            new KeyValue(blur.radiusProperty(), 8, Interpolator.EASE_OUT))
    );
    FadeTransition dimIn = new FadeTransition(Duration.millis(500), dimBackdrop);
    dimIn.setFromValue(0);
    dimIn.setToValue(1);
    blurIn.play();
    dimIn.play();
    if (musicFilterOn != null) musicFilterOn.run();
    PauseTransition cardDelay = new PauseTransition(Duration.millis(1500));
    cardDelay.setOnFinished(ev -> {
      FadeTransition cardIn = new FadeTransition(Duration.millis(340), card);
      cardIn.setFromValue(0);
      cardIn.setToValue(1);
      Timeline scaleIn = new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(card.scaleXProperty(), 0.88, Interpolator.EASE_OUT),
              new KeyValue(card.scaleYProperty(), 0.88, Interpolator.EASE_OUT)),
          new KeyFrame(Duration.millis(380),
              new KeyValue(card.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
              new KeyValue(card.scaleYProperty(), 1.0, Interpolator.EASE_OUT))
      );
      cardIn.play();
      scaleIn.play();

      // ── Grow pulse 2.4s after card appears ────────────────────────────
      PauseTransition pulseDelay = new PauseTransition(Duration.millis(900));
      pulseDelay.setOnFinished(pev -> {
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.scaleXProperty(), 1.0),
                new KeyValue(card.scaleYProperty(), 1.0)),
            new KeyFrame(Duration.millis(150),
                new KeyValue(card.scaleXProperty(), 1.06, Interpolator.EASE_OUT),
                new KeyValue(card.scaleYProperty(), 1.06, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(320),
                new KeyValue(card.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                new KeyValue(card.scaleYProperty(), 1.0, Interpolator.EASE_IN))
        );
        pulse.play();
      });
      pulseDelay.play();
    });
    cardDelay.play();

    // ── Dismiss ─────────────────────────────────────────────────────────
    Runnable dismiss = () -> {
      cardDelay.stop();
      Timeline blurOut = new Timeline(
          new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), blur.getRadius())),
          new KeyFrame(Duration.millis(250),
              new KeyValue(blur.radiusProperty(), 0, Interpolator.EASE_IN))
      );
      FadeTransition dimOut = new FadeTransition(Duration.millis(220), dimBackdrop);
      dimOut.setFromValue(dimBackdrop.getOpacity());
      dimOut.setToValue(0);
      FadeTransition cardOut = new FadeTransition(Duration.millis(180), card);
      cardOut.setFromValue(card.getOpacity());
      cardOut.setToValue(0);
      blurOut.play();
      dimOut.play();
      cardOut.play();
      blurOut.setOnFinished(fev -> {
        overlayRef.getChildren().remove(popup);
        rootRef.setEffect(null);
        if (musicFilterOff != null) musicFilterOff.run();
      });
    };

    for (HBox newAvatarsRow : newAvatarsRows) {
      newAvatarsRow.setOnMouseClicked(ev -> {
        dismiss.run();
        this.onProfile.run();
      });
    }

    continueBtn.setOnAction(ev -> dismiss.run());
    dimBackdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE
          || ev.getCode() == javafx.scene.input.KeyCode.ENTER
          || ev.getCode() == javafx.scene.input.KeyCode.SPACE) {
        dismiss.run();
        ev.consume();
      }
    });
    popup.setFocusTraversable(true);
    Platform.runLater(popup::requestFocus);
  }

  private void showMarketMovers() {
    suspendTutorialOverlay();
    GaussianBlur blur = new GaussianBlur(0);
    rootRef.setEffect(blur);

    Region dimBackdrop = new Region();
    dimBackdrop.getStyleClass().add("market-movers-backdrop");
    dimBackdrop.setOpacity(0);

    Label titleLbl = new Label("\uD83D\uDCC8  Market Movers");
    titleLbl.getStyleClass().add("market-movers-title");
    Button closeBtn = new Button("\u2715");
    closeBtn.getStyleClass().add("market-movers-close-btn");
    Region titleSpacer = new Region();
    HBox.setHgrow(titleSpacer, Priority.ALWAYS);
    HBox titleRow = new HBox(12, titleLbl, titleSpacer, closeBtn);
    titleRow.getStyleClass().add("market-movers-header");
    titleRow.setAlignment(Pos.CENTER_LEFT);

    // ── Tab bar ───────────────────────────────────────────────────────────
    String[] tabRef = {"1W"};
    Button tab1w = new Button("1W");
    Button tab4w = new Button("4W");
    Button tab10w = new Button("10W");
    Button tabAll = new Button("All");
    for (Button t : new Button[] {tab1w, tab4w, tab10w, tabAll}) {
      t.getStyleClass().add("movers-tab");
    }
    tab1w.getStyleClass().add("movers-tab-active");
    HBox tabBar = new HBox(4, tab1w, tab4w, tab10w, tabAll);
    tabBar.getStyleClass().add("movers-tab-bar");

    Runnable[] dismissRef = {null};
    HBox columns = new HBox(0);
    columns.getStyleClass().add("market-movers-columns");

    Runnable[] rebuildRef = {null};
    rebuildRef[0] = () -> {
      int weeks = switch (tabRef[0]) {
        case "4W" -> 4;
        case "10W" -> 10;
        case "All" -> -1;
        default -> 1;
      };
      List<Stock> all = gameController.getStocks();
      List<Stock> gainers = all.stream()
          .filter(s -> s.percentageChangeOverWeeks(weeks).compareTo(BigDecimal.ZERO) > 0)
          .sorted((a, b) -> b.percentageChangeOverWeeks(weeks)
              .compareTo(a.percentageChangeOverWeeks(weeks))).limit(10).toList();
      List<Stock> losers = all.stream()
          .filter(s -> s.percentageChangeOverWeeks(weeks).compareTo(BigDecimal.ZERO) < 0)
          .sorted((a, b) -> a.percentageChangeOverWeeks(weeks)
              .compareTo(b.percentageChangeOverWeeks(weeks))).limit(10).toList();
      VBox gainersCol = buildMoversColumn("\u25B2  TOP GAINERS", gainers, true, weeks, dismissRef);
      VBox losersCol = buildMoversColumn("\u25BC  TOP LOSERS", losers, false, weeks, dismissRef);
      HBox.setHgrow(gainersCol, Priority.ALWAYS);
      HBox.setHgrow(losersCol, Priority.ALWAYS);
      columns.getChildren().setAll(gainersCol, losersCol);
    };
    rebuildRef[0].run();

    tab1w.setOnAction(ev -> {
      notifyPanelOpen();
      tabRef[0] = "1W";
      tab1w.getStyleClass().add("movers-tab-active");
      tab4w.getStyleClass().remove("movers-tab-active");
      tabAll.getStyleClass().remove("movers-tab-active");
      rebuildRef[0].run();
      refreshTutorialProgress();
    });
    tab4w.setOnAction(ev -> {
      notifyPanelOpen();
      tabRef[0] = "4W";
      tab4w.getStyleClass().add("movers-tab-active");
      tab1w.getStyleClass().remove("movers-tab-active");
      tab10w.getStyleClass().remove("movers-tab-active");
      tabAll.getStyleClass().remove("movers-tab-active");
      rebuildRef[0].run();
      refreshTutorialProgress();
    });
    tab10w.setOnAction(ev -> {
      notifyPanelOpen();
      tabRef[0] = "10W";
      tab10w.getStyleClass().add("movers-tab-active");
      tab1w.getStyleClass().remove("movers-tab-active");
      tab4w.getStyleClass().remove("movers-tab-active");
      tabAll.getStyleClass().remove("movers-tab-active");
      rebuildRef[0].run();
      refreshTutorialProgress();
    });
    tabAll.setOnAction(ev -> {
      notifyPanelOpen();
      tabRef[0] = "All";
      tabAll.getStyleClass().add("movers-tab-active");
      tab1w.getStyleClass().remove("movers-tab-active");
      tab4w.getStyleClass().remove("movers-tab-active");
      tab10w.getStyleClass().remove("movers-tab-active");
      rebuildRef[0].run();
      refreshTutorialProgress();
    });

    VBox card = new VBox(0, titleRow, tabBar, columns);
    card.getStyleClass().add("market-movers-card");
    card.setMaxWidth(720);
    card.setMaxHeight(Region.USE_PREF_SIZE);
    card.setOpacity(0);

    StackPane popup = new StackPane(dimBackdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);
    overlayRef.getChildren().add(popup);
    tutorialMoversCardTarget = card;
    tutorialMoversTabBarTarget = tabBar;
    refreshTutorialProgress();

    Timeline blurIn = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
        new KeyFrame(Duration.millis(300),
            new KeyValue(blur.radiusProperty(), 8, Interpolator.EASE_OUT))
    );
    FadeTransition dimIn = new FadeTransition(Duration.millis(300), dimBackdrop);
    dimIn.setFromValue(0);
    dimIn.setToValue(1);
    FadeTransition cardIn = new FadeTransition(Duration.millis(220), card);
    cardIn.setFromValue(0);
    cardIn.setToValue(1);
    cardIn.setDelay(Duration.millis(80));
    blurIn.play();
    dimIn.play();
    cardIn.play();

    Runnable dismiss = () -> {
      Timeline blurOut = new Timeline(
          new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 8)),
          new KeyFrame(Duration.millis(250),
              new KeyValue(blur.radiusProperty(), 0, Interpolator.EASE_IN))
      );
      FadeTransition dimOut = new FadeTransition(Duration.millis(250), dimBackdrop);
      dimOut.setFromValue(1);
      dimOut.setToValue(0);
      FadeTransition cardOut = new FadeTransition(Duration.millis(180), card);
      cardOut.setFromValue(1);
      cardOut.setToValue(0);
      blurOut.play();
      dimOut.play();
      cardOut.play();
      blurOut.setOnFinished(ev -> {
        overlayRef.getChildren().remove(popup);
        rootRef.setEffect(null);
        tutorialMoversCardTarget = null;
        tutorialMoversTabBarTarget = null;
        if (tutorialUseMoversStep && tutorialStepIndex == 5) {
          tutorialStepIndex = 6;
        }
        resumeTutorialOverlay();
        refreshTutorialProgress();
      });
    };
    dismissRef[0] = dismiss;

    closeBtn.setOnAction(ev -> dismiss.run());
    dimBackdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });
    popup.requestFocus();
  }

  private VBox buildMoversColumn(String title, List<Stock> stocks, boolean isGainers, int weeks,
                                 Runnable[] dismissRef) {
    Label colTitle = new Label(title);
    colTitle.getStyleClass().add("market-movers-col-title");
    colTitle.getStyleClass()
        .add(isGainers ? "market-movers-col-title-gainers" : "market-movers-col-title-losers");

    VBox rows = new VBox(0);
    if (stocks.isEmpty()) {
      Label none = new Label(isGainers ? "No gainers in this period." : "No losers in this period.");
      none.getStyleClass().add("movers-company");
      rows.getChildren().add(none);
    }
    for (int i = 0; i < stocks.size(); i++) {
      Stock s = stocks.get(i);
      BigDecimal pct = s.percentageChangeOverWeeks(weeks);
      String sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

      Label rankLbl = new Label("#" + (i + 1));
      rankLbl.getStyleClass().add("movers-rank");
      if (i == 0) {
        rankLbl.getStyleClass().add("movers-rank-gold");
      } else if (i == 1) {
        rankLbl.getStyleClass().add("movers-rank-silver");
      } else if (i == 2) {
        rankLbl.getStyleClass().add("movers-rank-bronze");
      }

      Label symLbl = new Label(s.getSymbol());
      symLbl.getStyleClass().add("movers-symbol");
      Label compLbl = new Label(s.getCompany());
      compLbl.getStyleClass().add("movers-company");

      // Fav star inline next to symbol (only if favorited)
      HBox symRow;
      if (favorites.contains(s.getSymbol())) {
        Label moversStarLbl = new Label("\u2605");
        moversStarLbl.getStyleClass().add("movers-fav-star");
        symRow = new HBox(4, symLbl, moversStarLbl);
      } else {
        symRow = new HBox(symLbl);
      }
      symRow.setAlignment(Pos.CENTER_LEFT);

      VBox textBox = new VBox(1, symRow, compLbl);

      // Show owned quantity only if player owns shares
      BigDecimal moversOwnedQuantity = gameController.getOwnedQuantity(s.getSymbol());
      if (moversOwnedQuantity.compareTo(BigDecimal.ZERO) > 0) {
        Label moversOwnedLbl =
            new Label(moversOwnedQuantity.stripTrailingZeros().toPlainString() + " owned");
        moversOwnedLbl.getStyleClass().add("movers-owned-label");
        textBox.getChildren().add(moversOwnedLbl);
      }

      Label priceLbl = new Label(CurrencyFormatter.format(s.getSalesPrice()));
      priceLbl.getStyleClass().add("movers-price");
      Label pctLbl = new Label(sign + pct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
      pctLbl.getStyleClass().add(isGainers ? "movers-pct-up" : "movers-pct-down");
      VBox rightBox = new VBox(2, priceLbl, pctLbl);
      rightBox.setAlignment(Pos.CENTER_RIGHT);

      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      HBox row = new HBox(8, rankLbl, textBox, spacer, rightBox);
      row.getStyleClass().add("movers-row");
      row.getStyleClass().add(isGainers ? "movers-row-up" : "movers-row-down");
      row.setAlignment(Pos.CENTER_LEFT);

      row.setOnMouseClicked(ev -> {
        if (dismissRef[0] != null) {
          dismissRef[0].run();
        }
        String sym = s.getSymbol();
        if (filteredStocks.stream().noneMatch(st -> st.getSymbol().equals(sym))) {
          searchField.setText("");
        }
        Stock target = allStocks.stream()
            .filter(st -> st.getSymbol().equals(sym))
            .findFirst().orElse(s);
        if (selectedStock.get() == null || !sym.equals(selectedStock.get().getSymbol())) {
          notifyStockSelectionChanged();
        }
        selectedStock.set(target);
        focusStockCardInList(sym);
      });

      rows.getChildren().add(row);
    }

    VBox col = new VBox(8, colTitle, rows);
    col.getStyleClass().add("market-movers-col");
    col.getStyleClass().add(isGainers ? "market-movers-col-gainers" : "market-movers-col-losers");
    return col;
  }

  private void showTransactionHistory() {
    List<TxRow> allTx = gameController.getTransactionHistory();

    ObservableList<TxRow> txItems = FXCollections.observableArrayList(allTx);
    FilteredList<TxRow> filteredTx = new FilteredList<>(txItems, t -> true);

    String[] txFilterRef = {"ALL"};
    Runnable[] applyTxFilter = {null};

    TextField txSearch = new TextField();
    txSearch.setPromptText("\uD83D\uDD0D  Search by symbol\u2026");
    txSearch.getStyleClass().add("game-search-field");

    // ── Type filter chips ─────────────────────────────────────────────────
    String[] txChipKeys = {"ALL", "BUY", "SELL"};
    String[] txChipLabels = {"All", "Buy", "Sell"};
    HBox txFilterRow = new HBox(6);
    txFilterRow.getStyleClass().addAll("stock-filter-row", "history-filter-row");
    txFilterRow.setAlignment(Pos.CENTER_LEFT);

    applyTxFilter[0] = () -> {
      String lower = txSearch.getText() == null ? "" : txSearch.getText().trim().toLowerCase();
      filteredTx.setPredicate(t -> {
        boolean textMatch = lower.isEmpty()
            || t.symbol().toLowerCase().contains(lower)
            || t.company().toLowerCase().contains(lower);
        boolean typeMatch = switch (txFilterRef[0]) {
          case "BUY" -> t.isBuy();
          case "SELL" -> !t.isBuy();
          default -> true;
        };
        return textMatch && typeMatch;
      });
    };
    txSearch.textProperty().addListener((obs, old, val) -> applyTxFilter[0].run());
    txSearch.setOnAction(ev -> {
      notifyPanelOpen();
      applyTxFilter[0].run();
    });

    for (int i = 0; i < txChipLabels.length; i++) {
      final int idx = i;
      Button chip = new Button(txChipLabels[i]);
      chip.getStyleClass().add("stock-filter-chip");
      if (i == 0) {
        chip.getStyleClass().add("stock-filter-chip-active");
      }
      chip.setOnAction(ev -> {
        notifyPanelOpen();
        txFilterRef[0] = txChipKeys[idx];
        for (Node n : txFilterRow.getChildren()) {
          n.getStyleClass().remove("stock-filter-chip-active");
        }
        chip.getStyleClass().add("stock-filter-chip-active");
        applyTxFilter[0].run();
      });
      txFilterRow.getChildren().add(chip);
    }

    // ── Table ─────────────────────────────────────────────────────────────
    SortedList<TxRow> sortedTx = new SortedList<>(filteredTx);
    TableView<TxRow> table = new TableView<>(sortedTx);
    sortedTx.comparatorProperty().bind(table.comparatorProperty());
    table.getStyleClass().add("history-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<TxRow, String> weekCol = new TableColumn<>("Week");
    weekCol.setCellValueFactory(
        cd -> new SimpleStringProperty(String.valueOf(cd.getValue().week())));
    weekCol.setComparator(java.util.Comparator.comparingInt(Integer::parseInt));
    weekCol.setMinWidth(50);
    weekCol.setPrefWidth(50);

    TableColumn<TxRow, String> typeCol = new TableColumn<>("Type");
    typeCol.setCellValueFactory(
        cd -> new SimpleStringProperty(cd.getValue().isBuy() ? "BUY" : "SELL"));
    typeCol.setMinWidth(56);
    typeCol.setPrefWidth(56);
    typeCol.setComparator(String::compareTo);
    typeCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          return;
        }
        setText(item);
        getStyleClass().removeAll("tx-type-buy", "tx-type-sell");
        getStyleClass().add("BUY".equals(item) ? "tx-type-buy" : "tx-type-sell");
      }
    });

    TableColumn<TxRow, String> symCol = new TableColumn<>("Symbol");
    symCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().symbol()));
    symCol.setMinWidth(76);
    symCol.setPrefWidth(86);

    TableColumn<TxRow, String> compCol = new TableColumn<>("Company");
    compCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().company()));
    compCol.setMinWidth(140);
    compCol.setPrefWidth(180);

    TableColumn<TxRow, BigDecimal> quantityCol = new TableColumn<>("Quantity");
    quantityCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().quantity()));
    quantityCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item.stripTrailingZeros().toPlainString());
      }
    });
    quantityCol.setMinWidth(62);
    quantityCol.setPrefWidth(72);

    TableColumn<TxRow, BigDecimal> priceCol = new TableColumn<>("Price per share");
    priceCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().pricePerShare()));
    priceCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : CurrencyFormatter.format(item));
      }
    });
    priceCol.setMinWidth(90);
    priceCol.setPrefWidth(100);

    TableColumn<TxRow, BigDecimal> feeCol = new TableColumn<>("Fee");
    feeCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().fee()));
    feeCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : CurrencyFormatter.format(item));
      }
    });
    feeCol.setMinWidth(72);
    feeCol.setPrefWidth(82);

    TableColumn<TxRow, BigDecimal> taxCol = new TableColumn<>("Tax");
    taxCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().tax()));
    taxCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          return;
        }
        setText(item.compareTo(BigDecimal.ZERO) == 0 ? "\u2014" : CurrencyFormatter.format(item));
      }
    });
    taxCol.setMinWidth(72);
    taxCol.setPrefWidth(82);

    TableColumn<TxRow, BigDecimal> totalCol = new TableColumn<>("Total");
    totalCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().total()));
    totalCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : CurrencyFormatter.format(item));
      }
    });
    totalCol.setMinWidth(96);
    totalCol.setPrefWidth(108);

    table.getColumns().add(weekCol);
    table.getColumns().add(typeCol);
    table.getColumns().add(symCol);
    table.getColumns().add(compCol);
    table.getColumns().add(quantityCol);
    table.getColumns().add(priceCol);
    table.getColumns().add(feeCol);
    table.getColumns().add(taxCol);
    table.getColumns().add(totalCol);
    weekCol.setSortType(TableColumn.SortType.DESCENDING);
    table.getSortOrder().add(weekCol);
    table.sort();
    // Let the table consume the available card space before showing scrollbars.
    table.setMinHeight(58); // one row + header baseline
    table.setMaxHeight(Double.MAX_VALUE);
    Label emptyLbl = new Label("No transactions yet.");
    emptyLbl.getStyleClass().add("market-movers-col-title");
    table.setPlaceholder(emptyLbl);

    // ── Row click: select stock and highlight its dot on the graph ────────
    Runnable[] dismissRef = {null};
    table.setRowFactory(tv -> {
      TableRow<TxRow> row = new TableRow<>() {
        @Override
        protected void updateItem(TxRow item, boolean empty) {
          super.updateItem(item, empty);
          getStyleClass().removeAll("tx-row-buy", "tx-row-sell");
          if (!empty && item != null) {
            getStyleClass().add(item.isBuy() ? "tx-row-buy" : "tx-row-sell");
          }
        }
      };
      row.setOnMouseClicked(ev -> {
        if (!row.isEmpty() && ev.getClickCount() == 1) {
          TxRow tx = row.getItem();
          if (dismissRef[0] != null) {
            dismissRef[0].run();
          }
          if (highlightFadeTimer != null) {
            highlightFadeTimer.stop();
            highlightFadeTimer = null;
          }
          highlightedTx = tx;
          String sym = tx.symbol();
          if (filteredStocks.stream().noneMatch(s -> s.getSymbol().equals(sym))) {
            searchField.setText("");
          }
          allStocks.stream()
              .filter(s -> s.getSymbol().equals(sym))
              .findFirst()
              .ifPresent(target -> {
                if (target == selectedStock.get()) {
                  // Same stock already selected — listener won't fire, force chart rebuild
                  rebuildDetail();
                } else {
                  notifyStockSelectionChanged();
                  selectedStock.set(target);
                }
                focusStockCardInList(sym);
              });
        }
      });
      return row;
    });

    // ── Layout ────────────────────────────────────────────────────────────
    Label titleLbl = new Label("\uD83D\uDCCB  Transaction History");
    titleLbl.getStyleClass().add("market-movers-title");
    Button closeBtn = new Button("\u2715");
    closeBtn.getStyleClass().add("market-movers-close-btn");
    Region titleSpacer = new Region();
    HBox.setHgrow(titleSpacer, Priority.ALWAYS);
    HBox titleRow = new HBox(12, titleLbl, titleSpacer, closeBtn);
    titleRow.getStyleClass().add("market-movers-header");
    titleRow.setAlignment(Pos.CENTER_LEFT);

    Label txSortLabel = new Label("Sort by:");
    txSortLabel.getStyleClass().addAll("stock-row-section-label", "history-sort-label");

    HBox controlsRow = new HBox(10, txSearch, txSortLabel, txFilterRow);
    controlsRow.getStyleClass().add("history-controls-row");
    controlsRow.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(txSearch, Priority.ALWAYS);

    VBox card = new VBox(0, titleRow, controlsRow, table);
    card.getStyleClass().add("history-card");
    card.setMaxWidth(920);
    card.setPrefHeight(580);
    card.setMaxHeight(580);
    card.setOpacity(0);
    VBox.setVgrow(table, Priority.ALWAYS);

    GaussianBlur blur = new GaussianBlur(0);
    rootRef.setEffect(blur);

    Region dimBackdrop = new Region();
    dimBackdrop.getStyleClass().add("market-movers-backdrop");
    dimBackdrop.setOpacity(0);

    StackPane popup = new StackPane(dimBackdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);
    overlayRef.getChildren().add(popup);

    Timeline blurIn = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
        new KeyFrame(Duration.millis(300),
            new KeyValue(blur.radiusProperty(), 8, Interpolator.EASE_OUT))
    );
    FadeTransition dimIn = new FadeTransition(Duration.millis(300), dimBackdrop);
    dimIn.setFromValue(0);
    dimIn.setToValue(1);
    FadeTransition cardIn = new FadeTransition(Duration.millis(220), card);
    cardIn.setFromValue(0);
    cardIn.setToValue(1);
    cardIn.setDelay(Duration.millis(80));
    blurIn.play();
    dimIn.play();
    cardIn.play();

    Runnable dismiss = () -> {
      Timeline blurOut = new Timeline(
          new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 8)),
          new KeyFrame(Duration.millis(250),
              new KeyValue(blur.radiusProperty(), 0, Interpolator.EASE_IN))
      );
      FadeTransition dimOut = new FadeTransition(Duration.millis(250), dimBackdrop);
      dimOut.setFromValue(1);
      dimOut.setToValue(0);
      FadeTransition cardOut = new FadeTransition(Duration.millis(180), card);
      cardOut.setFromValue(1);
      cardOut.setToValue(0);
      blurOut.play();
      dimOut.play();
      cardOut.play();
      blurOut.setOnFinished(ev -> {
        overlayRef.getChildren().remove(popup);
        rootRef.setEffect(null);
      });
    };
    dismissRef[0] = dismiss;
    closeBtn.setOnAction(ev -> dismiss.run());
    dimBackdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      switch (ev.getCode()) {
        case ESCAPE -> {
          dismiss.run();
          ev.consume();
        }
        case SLASH -> {
          if (!(ev.getTarget() instanceof TextField)) {
            txSearch.requestFocus();
            ev.consume();
          }
        }
        case F -> {
          if (ev.isControlDown()) {
            txSearch.requestFocus();
            ev.consume();
          }
        }
        default -> {
        }
      }
    });
    popup.requestFocus();
  }

  private void showPortfolioSummary() {
    Runnable[] dismissRef = {null};

    // ── Summary stats ─────────────────────────────────────────────────────
    BigDecimal totalInvested = portfolioItems.stream()
        .map(s -> s.getPurchasePrice().multiply(s.getQuantity()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalValue = gameController.getPortfolioNetWorth();
    BigDecimal totalPnl = totalValue.subtract(totalInvested);
    int holdingsCount = portfolioItems.size();

    Label holdingsVal = new Label(holdingsCount + (holdingsCount == 1 ? " stock" : " stocks"));
    holdingsVal.getStyleClass().add("portfolio-summary-stat-value");
    Label holdingsLbl = new Label("Holdings");
    holdingsLbl.getStyleClass().add("portfolio-summary-stat-key");
    VBox holdingsBox = new VBox(2, holdingsLbl, holdingsVal);
    holdingsBox.setAlignment(Pos.CENTER_LEFT);

    Label investedVal = new Label(CurrencyFormatter.format(totalInvested));
    investedVal.getStyleClass().add("portfolio-summary-stat-value");
    Label investedLbl = new Label("Invested");
    investedLbl.getStyleClass().add("portfolio-summary-stat-key");
    VBox investedBox = new VBox(2, investedLbl, investedVal);
    investedBox.setAlignment(Pos.CENTER_LEFT);

    Label valueVal = new Label(CurrencyFormatter.format(totalValue));
    valueVal.getStyleClass().add("portfolio-summary-stat-value");
    Label valueLbl = new Label("Current Value");
    valueLbl.getStyleClass().add("portfolio-summary-stat-key");
    VBox valueBox = new VBox(2, valueLbl, valueVal);
    valueBox.setAlignment(Pos.CENTER_LEFT);

    boolean pnlZero = totalPnl.compareTo(BigDecimal.ZERO) == 0;
    boolean pnlUp = totalPnl.compareTo(BigDecimal.ZERO) > 0;
    BigDecimal pnlPct = totalInvested.compareTo(BigDecimal.ZERO) == 0
        ? BigDecimal.ZERO
        : totalPnl.divide(totalInvested, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    String pnlText = pnlZero
      ? "\u2014"
      : (pnlUp ? "+" : "") + CurrencyFormatter.format(totalPnl)
        + " (" + (pnlUp ? "+" : "") + pnlPct.toPlainString() + "%)";
    Label pnlVal = new Label(pnlText);
    if (pnlZero) {
      pnlVal.getStyleClass().add("portfolio-summary-stat-value");
    } else {
      pnlVal.getStyleClass().addAll("portfolio-summary-stat-value",
        pnlUp ? "portfolio-summary-pnl-up" : "portfolio-summary-pnl-down");
    }
    Label pnlLbl = new Label("Total P&L");
    pnlLbl.getStyleClass().add("portfolio-summary-stat-key");
    VBox pnlBox = new VBox(2, pnlLbl, pnlVal);
    pnlBox.setAlignment(Pos.CENTER_LEFT);

    Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
    Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
    Region sp3 = new Region(); HBox.setHgrow(sp3, Priority.ALWAYS);
    HBox statsStrip = new HBox(0, holdingsBox, sp1, investedBox, sp2, valueBox, sp3, pnlBox);
    statsStrip.getStyleClass().add("portfolio-summary-stats-strip");
    statsStrip.setAlignment(Pos.CENTER_LEFT);

    // ── Portfolio table ───────────────────────────────────────────────────
    TableView<Share> table = buildPortfolioTable(portfolioItems, stock -> {
      if (dismissRef[0] != null) dismissRef[0].run();
      allStocks.stream().filter(s -> s.getSymbol().equals(stock.getSymbol()))
          .findFirst().ifPresent(target -> {
            if (target == selectedStock.get()) {
              rebuildDetail();
            } else {
              notifyStockSelectionChanged();
              selectedStock.set(target);
            }
            focusStockCardInList(stock.getSymbol());
          });
    });
    table.getStyleClass().add("history-table");
    table.setMinHeight(58);
    table.setMaxHeight(Double.MAX_VALUE);

    // ── Layout ────────────────────────────────────────────────────────────
    Label titleLbl = new Label("\uD83D\uDCC8  Portfolio Summary");
    titleLbl.getStyleClass().add("market-movers-title");
    Button closeBtn = new Button("\u2715");
    closeBtn.getStyleClass().add("market-movers-close-btn");
    Region titleSpacer = new Region();
    HBox.setHgrow(titleSpacer, Priority.ALWAYS);
    HBox titleRow = new HBox(12, titleLbl, titleSpacer, closeBtn);
    titleRow.getStyleClass().add("market-movers-header");
    titleRow.setAlignment(Pos.CENTER_LEFT);

    VBox card = new VBox(0, titleRow, statsStrip, table);
    card.getStyleClass().add("history-card");
    card.setMaxWidth(880);
    card.setPrefHeight(520);
    card.setMaxHeight(520);
    card.setOpacity(0);
    VBox.setVgrow(table, Priority.ALWAYS);

    GaussianBlur blur = new GaussianBlur(0);
    rootRef.setEffect(blur);

    Region dimBackdrop = new Region();
    dimBackdrop.getStyleClass().add("market-movers-backdrop");
    dimBackdrop.setOpacity(0);

    StackPane popup = new StackPane(dimBackdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);
    overlayRef.getChildren().add(popup);

    Timeline blurIn = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
        new KeyFrame(Duration.millis(300),
            new KeyValue(blur.radiusProperty(), 8, Interpolator.EASE_OUT)));
    FadeTransition dimIn = new FadeTransition(Duration.millis(300), dimBackdrop);
    dimIn.setFromValue(0); dimIn.setToValue(1);
    FadeTransition cardIn = new FadeTransition(Duration.millis(220), card);
    cardIn.setFromValue(0); cardIn.setToValue(1);
    cardIn.setDelay(Duration.millis(80));
    blurIn.play(); dimIn.play(); cardIn.play();

    Runnable dismiss = () -> {
      Timeline blurOut = new Timeline(
          new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 8)),
          new KeyFrame(Duration.millis(250),
              new KeyValue(blur.radiusProperty(), 0, Interpolator.EASE_IN)));
      FadeTransition dimOut = new FadeTransition(Duration.millis(250), dimBackdrop);
      dimOut.setFromValue(1); dimOut.setToValue(0);
      FadeTransition cardOut = new FadeTransition(Duration.millis(180), card);
      cardOut.setFromValue(1); cardOut.setToValue(0);
      blurOut.play(); dimOut.play(); cardOut.play();
      blurOut.setOnFinished(ev -> {
        overlayRef.getChildren().remove(popup);
        rootRef.setEffect(null);
      });
    };
    dismissRef[0] = dismiss;
    closeBtn.setOnAction(ev -> dismiss.run());
    dimBackdrop.setOnMouseClicked(ev -> dismiss.run());
    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });
    popup.requestFocus();
  }

  private static Node moneyPill(Label cashLabel, Label portfolioLabel,
                                Label netWorthLabel, String toneClass) {
    Node cashSegment = moneySegment("Cash", cashLabel, "money-segment-cash");
    Node portfolioSegment = moneySegment("Portfolio", portfolioLabel, "money-segment-portfolio");
    Node netWorthSegment = moneySegment("Net Worth", netWorthLabel, "money-segment-networth");

    HBox box = new HBox(6, cashSegment, portfolioSegment, netWorthSegment);
    box.getStyleClass().addAll("stat-pill", "money-pill", toneClass);
    box.setFillHeight(false);
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
  }

  private static Node moneySegment(String key, Label valueLabel, String styleClass) {
    Label keyLabel = new Label(key);
    keyLabel.getStyleClass().add("money-segment-key");

    VBox textBox = new VBox(0, keyLabel, valueLabel);
    textBox.setAlignment(Pos.CENTER_LEFT);

    Region line = new Region();
    line.getStyleClass().add("money-segment-line");
    line.prefHeightProperty().bind(textBox.heightProperty());
    line.minHeightProperty().bind(textBox.heightProperty());
    line.maxHeightProperty().bind(textBox.heightProperty());

    HBox segment = new HBox(8, line, textBox);
    segment.getStyleClass().addAll("money-segment", styleClass);
    segment.setAlignment(Pos.CENTER_LEFT);
    return segment;
  }

  private static Node statusPill(String key, Label valueLabel, Arc progressArc, Tooltip tooltip) {
    Label keyLbl = new Label(key);
    keyLbl.getStyleClass().add("stat-pill-key");

    VBox textBox = new VBox(1, keyLbl, valueLabel);
    textBox.setAlignment(Pos.CENTER_LEFT);

    Circle track = new Circle(12, 12, 11);
    track.getStyleClass().add("status-pill-ring-track");

    Circle center = new Circle(12, 12, 8);
    center.getStyleClass().add("status-pill-ring-core");

    progressArc.setCenterX(12);
    progressArc.setCenterY(12);

    Pane ring = new Pane(track, progressArc, center);
    ring.getStyleClass().add("status-pill-ring");
    ring.setMinSize(24, 24);
    ring.setPrefSize(24, 24);
    ring.setMaxSize(24, 24);

    HBox box = new HBox(10, textBox, ring);
    box.getStyleClass().addAll("stat-pill", "status-pill");
    box.setAlignment(Pos.CENTER_LEFT);

    Tooltip.install(box, tooltip);
    return box;
  }

  private static BigDecimal clamp01(BigDecimal value) {
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO;
    }
    if (value.compareTo(BigDecimal.ONE) > 0) {
      return BigDecimal.ONE;
    }
    return value;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private static String formatStatus(PlayerStatus status) {
    String lower = status.name().toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  private static String formatPercent(BigDecimal ratio) {
    return clamp01(ratio)
        .multiply(BigDecimal.valueOf(100))
        .setScale(1, RoundingMode.HALF_UP)
        .toPlainString() + "%";
  }

  private static String formatGrowth(BigDecimal growthRatio) {
    return growthRatio.setScale(2, RoundingMode.HALF_UP).toPlainString() + "x";
  }

  private static VBox buildStatusTooltipContent(
      PlayerStatus status,
      BigDecimal overallProgress,
      int weeksTraded,
      int targetWeeks,
      BigDecimal weeksProgress,
      BigDecimal growthRatio,
      BigDecimal growthTarget,
      BigDecimal growthProgress) {
    Label title = new Label("Player Status: " + formatStatus(status));
    title.getStyleClass().add("chart-tooltip-week");

    String subtitle = status == PlayerStatus.SPECULATOR
        ? "Top tier reached. Keep compounding."
        : "Level up by filling both tracks.";
    Label subtitleLbl = new Label(subtitle);
    subtitleLbl.getStyleClass().add("chart-tooltip-row");

    VBox overall = tooltipProgressRow("Overall", formatPercent(overallProgress), overallProgress);
    VBox weeks = tooltipProgressRow("Weeks", weeksTraded + " / " + targetWeeks, weeksProgress);
    VBox growth = tooltipProgressRow(
        "Growth",
        formatGrowth(growthRatio) + " / " + formatGrowth(growthTarget),
        growthProgress);

    VBox root = new VBox(6, title, subtitleLbl, overall, weeks, growth);
    root.setFillWidth(true);
    return root;
  }

  private static VBox tooltipProgressRow(String label, String value, BigDecimal progress) {
    Label rowLabel = new Label(label + "  " + value);
    rowLabel.getStyleClass().add("chart-tooltip-row");

    double width = 108;
    double pct = clamp01(progress).doubleValue();

    Rectangle track = new Rectangle(width, 6);
    track.setArcWidth(6);
    track.setArcHeight(6);
    track.setFill(Color.rgb(30, 64, 128, 0.50));

    Rectangle fill = new Rectangle(width * pct, 6);
    fill.setArcWidth(6);
    fill.setArcHeight(6);
    fill.setFill(Color.web("#f5a201"));

    StackPane bar = new StackPane(track, fill);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.setMinWidth(width);
    bar.setPrefWidth(width);
    bar.setMaxWidth(width);

    VBox row = new VBox(3, rowLabel, bar);
    row.setFillWidth(true);
    return row;
  }

  private StackPane buildDevPanel() {
    Label title = new Label("🛠  DEV MODE");
    title.getStyleClass().add("dev-panel-title");

    // Week counter
    Label weekDisplay = new Label("Week: " + gameController.getCurrentWeek());
    weekDisplay.getStyleClass().add("dev-panel-stat");

    // Net worth snapshot (declared early so all handlers below can reference it)
    Label nwLabel = new Label();
    nwLabel.getStyleClass().add("dev-panel-stat");
    Runnable updateNw = () -> {
      nwLabel.setText("NW: $" + gameController.getPlayerNetWorth()
          .setScale(0, RoundingMode.HALF_UP).toPlainString());
    };
    updateNw.run();

    // Advance week buttons (bypass rate limiter)
    TextField advInput = new TextField();
    advInput.setPromptText("Weeks");
    advInput.getStyleClass().add("dev-panel-input");
    advInput.setPrefWidth(70);
    Button advCustom = devBtn("Advance");
    HBox advRow = new HBox(4, advInput, advCustom);
    advRow.setAlignment(Pos.CENTER_LEFT);

    Runnable doAdvance = () -> {
      weekDisplay.setText("Week: " + gameController.getCurrentWeek());
      updateNw.run();
      updateData();
    };
    advCustom.setOnAction(e -> {
      try {
        int n = NumberParser.parse(advInput.getText()).intValue();
        if (n > 0) {
          gameController.advanceWeeks(n);
          doAdvance.run();
          advInput.clear();
        }
      } catch (NumberFormatException ignored) {
        advInput.selectAll();
      }
    });

    // Cash buttons
    Button cash1k = devBtn("+$1K");
    Button cash10k = devBtn("+$10K");
    Button cash100k = devBtn("+$100K");
    cash1k.setOnAction(e -> {
      gameController.addCash(BigDecimal.valueOf(1_000));
      updateNw.run();
      updateData();
    });
    cash10k.setOnAction(e -> {
      gameController.addCash(BigDecimal.valueOf(10_000));
      updateNw.run();
      updateData();
    });
    cash100k.setOnAction(e -> {
      gameController.addCash(BigDecimal.valueOf(100_000));
      updateNw.run();
      updateData();
    });
    HBox cashRow = new HBox(4, cash1k, cash10k, cash100k);
    cashRow.setAlignment(Pos.CENTER_LEFT);

    // Custom cash input
    TextField cashInput = new TextField();
    cashInput.setPromptText("Amount");
    cashInput.getStyleClass().add("dev-panel-input");
    cashInput.setPrefWidth(90);
    Button setCashBtn = devBtn("Set Cash");
    setCashBtn.setOnAction(e -> {
      try {
        BigDecimal amount = NumberParser.parse(cashInput.getText());
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
          gameController.setCash(amount);
          cashInput.clear();
          updateNw.run();
          updateData();
        }
      } catch (NumberFormatException ignored) {
        cashInput.selectAll();
      }
    });
    HBox setCashRow = new HBox(4, cashInput, setCashBtn);
    setCashRow.setAlignment(Pos.CENTER_LEFT);

    // Freeze prices toggle
    boolean[] frozen = {false};
    Button freezeBtn = devBtn("Freeze Prices");
    freezeBtn.setOnAction(e -> {
      frozen[0] = !frozen[0];
      gameController.setFrozen(frozen[0]);
      freezeBtn.setText(frozen[0] ? "Unfreeze Prices" : "Freeze Prices");
      if (frozen[0]) {
        freezeBtn.getStyleClass().add("dev-btn-active");
      } else {
        freezeBtn.getStyleClass().remove("dev-btn-active");
      }
    });

    // Status override controls
    Label statusDisplay = new Label("Status: " + formatStatus(gameController.getPlayerStatus()));
    statusDisplay.getStyleClass().add("dev-panel-stat");

    Button statusNoviceBtn = devBtn("NOVICE");
    Button statusInvestorBtn = devBtn("INVESTOR");
    Button statusSpeculatorBtn = devBtn("SPECULATOR");
    Button statusAutoBtn = devBtn("Auto Status");
    Button testSpikeBtn = devBtn("Test Spike Notif");

    Runnable refreshStatusDisplay = () ->
        statusDisplay.setText("Status: " + formatStatus(gameController.getPlayerStatus()));

    statusNoviceBtn.setOnAction(e -> {
      gameController.setPlayerStatusOverride(PlayerStatus.NOVICE);
      refreshStatusDisplay.run();
      updateData();
    });
    statusInvestorBtn.setOnAction(e -> {
      gameController.setPlayerStatusOverride(PlayerStatus.INVESTOR);
      refreshStatusDisplay.run();
      updateData();
    });
    statusSpeculatorBtn.setOnAction(e -> {
      gameController.setPlayerStatusOverride(PlayerStatus.SPECULATOR);
      refreshStatusDisplay.run();
      updateData();
    });
    statusAutoBtn.setOnAction(e -> {
      gameController.clearPlayerStatusOverride();
      refreshStatusDisplay.run();
      updateData();
    });
    testSpikeBtn.setOnAction(e -> showTestSpikePopup());
    HBox statusRow = new HBox(4, statusNoviceBtn, statusInvestorBtn, statusSpeculatorBtn);
    statusRow.setAlignment(Pos.CENTER_LEFT);


    VBox panel = new VBox(6,
        title,
        weekDisplay,
        nwLabel,
        new Label("Advance:") {{
          getStyleClass().add("dev-panel-section");
        }},
        advRow,
        new Label("Cash:") {{
          getStyleClass().add("dev-panel-section");
        }},
        cashRow,
        setCashRow,
        new Label("Status:") {{
          getStyleClass().add("dev-panel-section");
        }},
        statusDisplay,
        statusRow,
        statusAutoBtn,
        testSpikeBtn,
        freezeBtn
    );
    panel.getStyleClass().add("dev-panel");
    panel.setMaxWidth(220);
    panel.setMaxHeight(Region.USE_PREF_SIZE);

    Button closeBtn = new Button("x");
    closeBtn.getStyleClass().add("game-spike-close-btn");
    closeBtn.setOnAction(e -> {
      AppConfig.DEV_MODE.unbind();
      AppConfig.DEV_MODE.set(false);
    });
    StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);

    StackPane wrapper = new StackPane(panel, closeBtn);
    wrapper.setMaxWidth(220);
    wrapper.setMaxHeight(Region.USE_PREF_SIZE);
    wrapper.setPickOnBounds(false);

    // Drag the panel from most non-interactive areas.
    final double[] dragStartSceneX = {0};
    final double[] dragStartSceneY = {0};
    final double[] dragStartTranslateX = {0};
    final double[] dragStartTranslateY = {0};
    final boolean[] isDraggingPanel = {false};
    java.util.function.Predicate<Node> dragBlockedTarget =
        n -> n instanceof Button || n instanceof TextInputControl;

    java.util.function.Consumer<javafx.scene.input.MouseEvent> startDrag = e -> {
      if (e.getTarget() instanceof Node target && dragBlockedTarget.test(target)) {
        return;
      }
      dragStartSceneX[0] = e.getSceneX();
      dragStartSceneY[0] = e.getSceneY();
      dragStartTranslateX[0] = wrapper.getTranslateX();
      dragStartTranslateY[0] = wrapper.getTranslateY();
      isDraggingPanel[0] = true;
      wrapper.setCursor(Cursor.CLOSED_HAND);
      e.consume();
    };

    java.util.function.Consumer<javafx.scene.input.MouseEvent> dragPanel = e -> {
      if (e.getTarget() instanceof Node target && dragBlockedTarget.test(target)) {
        return;
      }
      double dx = e.getSceneX() - dragStartSceneX[0];
      double dy = e.getSceneY() - dragStartSceneY[0];
      wrapper.setTranslateX(dragStartTranslateX[0] + dx);
      wrapper.setTranslateY(dragStartTranslateY[0] + dy);
      e.consume();
    };

    java.util.function.Consumer<javafx.scene.input.MouseEvent> endDrag = e -> {
      if (!isDraggingPanel[0]) {
        return;
      }
      isDraggingPanel[0] = false;
      if (e.getTarget() instanceof Node target && !dragBlockedTarget.test(target)) {
        wrapper.setCursor(Cursor.OPEN_HAND);
      } else {
        wrapper.setCursor(Cursor.DEFAULT);
      }
      e.consume();
    };

    panel.setOnMouseMoved(e -> {
      if (isDraggingPanel[0]) {
        return;
      }
      if (e.getTarget() instanceof Node target && !dragBlockedTarget.test(target)) {
        wrapper.setCursor(Cursor.OPEN_HAND);
      } else {
        wrapper.setCursor(Cursor.DEFAULT);
      }
    });
    panel.setOnMouseExited(e -> {
      if (!isDraggingPanel[0]) {
        wrapper.setCursor(Cursor.DEFAULT);
      }
    });

    panel.setOnMousePressed(e -> startDrag.accept(e));
    panel.setOnMouseDragged(e -> dragPanel.accept(e));
    panel.setOnMouseReleased(e -> endDrag.accept(e));
    return wrapper;
  }

  private static Button devBtn(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("dev-btn");
    return b;
  }

  private void rebuildStockList(java.util.Comparator<Stock> sortCmp) {
    if (performanceMode) {
      sortedStocks.setComparator(sortCmp);
      return;
    }

    stockListBox.getChildren().clear();
    filteredStocks.stream().sorted(sortCmp).forEach(stock ->
        stockListBox.getChildren().add(buildStockCard(stock)));
  }

  private void refreshSelectedStockCardStyles() {
    String selectedSymbol = selectedStock.get() == null ? null : selectedStock.get().getSymbol();
    for (Node node : stockListBox.getChildren()) {
      if (!(node instanceof HBox card)) {
        continue;
      }
      card.getStyleClass().remove("stock-card-selected");
      Object data = card.getUserData();
      if (selectedSymbol != null && data instanceof String symbol && selectedSymbol.equals(symbol)) {
        card.getStyleClass().add("stock-card-selected");
      }
    }
  }

  private Node buildStockCard(Stock stock) {
    Label symLbl = new Label(stock.getSymbol());
    symLbl.getStyleClass().add("stock-card-symbol");

    Label compLbl = new Label(stock.getCompany());
    compLbl.getStyleClass().add("stock-card-company");

    Label priceLbl = new Label(CurrencyFormatter.format(stock.getSalesPrice()));
    priceLbl.getStyleClass().add("stock-card-price");

    BigDecimal pct = stock.percentageChange();
    Label pctLbl;
    if (pct.compareTo(BigDecimal.ZERO) == 0) {
      pctLbl = new Label("\u2014");
      pctLbl.getStyleClass().add("stock-pct-neutral");
    } else {
      String sign = pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
      pctLbl = new Label(sign + pct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
      pctLbl.getStyleClass()
          .add(pct.compareTo(BigDecimal.ZERO) > 0 ? "stock-pct-up" : "stock-pct-down");
    }

    BigDecimal ownedQuantity = gameController.getOwnedQuantity(stock.getSymbol());
    BigDecimal capQuantity = gameController.getStockOwnershipCap(stock);
    Label ownedMaxLbl = null;
    if (ownedQuantity.compareTo(BigDecimal.ZERO) > 0) {
      String ownedText = ownedQuantity.compareTo(capQuantity) >= 0
          ? "Owned: MAX"
          : "Owned: " + ownedQuantity.stripTrailingZeros().toPlainString()
            + "/" + capQuantity.stripTrailingZeros().toPlainString();
      ownedMaxLbl = new Label(ownedText);
      ownedMaxLbl.getStyleClass().add("stock-owned-label");
    }

    // ── Favorite star button ─────────────────────────────────────────────
    boolean isFav = favorites.contains(stock.getSymbol());
    Button favBtn = new Button(isFav ? "\u2605" : "\u2606");
    favBtn.getStyleClass().add("stock-fav-btn");
    if (isFav) {
      favBtn.getStyleClass().add("stock-fav-btn-active");
    }
    favBtn.setOnAction(ev -> {
      if (favorites.contains(stock.getSymbol())) {
        favorites.remove(stock.getSymbol());
      } else {
        favorites.add(stock.getSymbol());
      }
      applyFilter();
    });
    favBtn.setOnMouseClicked(e -> e.consume());

    VBox left;
    if (ownedMaxLbl != null) {
      left = new VBox(2, symLbl, compLbl, pctLbl, ownedMaxLbl);
    } else {
      left = new VBox(2, symLbl, compLbl, pctLbl);
    }
    VBox right = new VBox(4);
    right.setAlignment(Pos.TOP_RIGHT);
    right.getChildren().addAll(favBtn, priceLbl);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox card = new HBox(8, left, spacer, right);
    card.getStyleClass().add("stock-card");
    if (isFav) {
      card.getStyleClass().add("stock-card-favorited");
    }
    card.setAlignment(Pos.TOP_LEFT);
    card.setPadding(new Insets(12, 14, 12, 14));
    card.setUserData(stock.getSymbol());

    if (!performanceMode && stock.equals(selectedStock.get())) {
      card.getStyleClass().add("stock-card-selected");
    }

    if (!performanceMode) {
      card.setOnMouseClicked(e -> {
        if (selectedStock.get() == null
            || !stock.getSymbol().equals(selectedStock.get().getSymbol())) {
          notifyStockSelectionChanged();
        }
        selectedStock.set(stock);
      });
    }
    return card;
  }

  public void showReceipt(String action, Stock stock, BigDecimal quantity,
                          BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash) {
    suspendTutorialOverlay();
    boolean isBuy = action != null && action.startsWith("BUY");

    Label checkLbl = new Label("\u2713");
    checkLbl.getStyleClass().add("receipt-check");
    Label titleLbl = new Label("ORDER COMPLETE");
    titleLbl.getStyleClass().add("dialog-title");
    HBox header = new HBox(10, checkLbl, titleLbl);
    header.getStyleClass().add("dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);

    VBox rows = new VBox(0,
        dialogRow("Action", action, isBuy ? "dialog-val-buy" : "dialog-val-sell"),
        dialogRow("Symbol", stock.getSymbol(), null),
        dialogRow("Quantity", quantity.stripTrailingZeros().toPlainString(), null),
        dialogRow("Price", CurrencyFormatter.format(stock.getSalesPrice()), null),
        dialogRow(isBuy ? "Fee (0.5%)" : "Fee (1%)", CurrencyFormatter.format(fee),
            "dialog-val-fee"),
        dialogRow("Tax", CurrencyFormatter.format(tax), "dialog-val-fee"),
        dialogRow(isBuy ? "Total Paid" : "Received", CurrencyFormatter.format(total), null),
        dialogRow("New Balance", CurrencyFormatter.format(newCash), "dialog-val-cash")
    );
    rows.getStyleClass().add("dialog-rows");

    Button doneBtn = new Button("Done");
    doneBtn.getStyleClass().add("dialog-confirm-buy-btn");
    HBox btnRow = new HBox(doneBtn);
    btnRow.setAlignment(Pos.CENTER_RIGHT);
    btnRow.getStyleClass().add("dialog-btn-row");

    VBox card = new VBox(0, header, rows, btnRow);
    card.getStyleClass().add("trade-dialog-root");
    card.setMaxWidth(340);
    card.setMaxHeight(Region.USE_PREF_SIZE);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> {
      overlayRef.getChildren().remove(popup);
      resumeTutorialOverlay();
      refreshTutorialProgressFlags();
      maybeAdvanceTutorialAfterTrade();
      if (isTutorialVisible()) {
        updateTutorialStep();
      }
    };
    doneBtn.setOnAction(ev -> dismiss.run());
    backdrop.setOnMouseClicked(ev -> dismiss.run());

    overlayRef.getChildren().add(popup);
  }

  private Pane buildPriceChart(Stock stock) {
    Canvas canvas = new Canvas();
    Pane pane = new Pane(canvas);
    pane.getStyleClass().add("price-chart-placeholder");

    canvas.widthProperty().bind(pane.widthProperty());
    canvas.heightProperty().bind(pane.heightProperty());

    List<BigDecimal> prices = stock.getHistoricalPrices();

    record TradeDot(int week, BigDecimal quantity, BigDecimal price, boolean isSell) {
    }

    String sym = stock.getSymbol();
    List<TradeDot> tradeDots = gameController.getTradePointsForStock(sym).stream()
        .map(tp -> new TradeDot(tp.week(), tp.quantity(), tp.price(), tp.isSell()))
        .collect(java.util.stream.Collectors.toList());

    // Fade state for tx highlight (1.0 = fully visible, 0.0 = gone)
    double[] highlightFade =
        {highlightedTx != null && highlightedTx.symbol().equals(sym) ? 1.0 : 0.0};

    // State for hover crosshair — rebuilt on each draw
    List<double[]> drawnDots = new ArrayList<>();
    double[][] xsRef = {new double[0]};
    double[][] ysRef = {new double[0]};
    int[] hoverIdx = {-1};
    boolean[] onDot = {false};

    // Trade-dot tooltip
    VBox tooltip = new VBox(3);
    tooltip.getStyleClass().add("chart-tooltip");
    tooltip.setVisible(false);
    tooltip.setMouseTransparent(true);
    pane.getChildren().add(tooltip);

    Runnable[] drawRef = {null};
    drawRef[0] = () -> {
      double w = canvas.getWidth();
      double h = canvas.getHeight();
      if (w <= 0 || h <= 0) {
        return;
      }

      GraphicsContext gc = canvas.getGraphicsContext2D();
      gc.clearRect(0, 0, w, h);
      drawnDots.clear();

      if (prices == null || prices.size() < 2) {
        gc.setFill(Color.web("#4a6899", 0.55));
        gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText("No price history yet", w / 2 - 60, h / 2);
        xsRef[0] = new double[0];
        ysRef[0] = new double[0];
        return;
      }

      BigDecimal minVal = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
      BigDecimal maxVal = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
      BigDecimal range = maxVal.subtract(minVal);
      if (range.compareTo(BigDecimal.ZERO) == 0) {
        range = BigDecimal.ONE;
      }

      final double padL = 10, padR = 10, padT = 14, padB = 10;
      double cW = w - padL - padR;
      double cH = h - padT - padB;

      boolean up = prices.get(prices.size() - 1).compareTo(prices.get(0)) >= 0;
      String lineHex = up ? "#4ecb71" : "#e05a5a";

      // Horizontal grid lines
      gc.setStroke(Color.web("#1e4080", 0.22));
      gc.setLineWidth(1);
      for (int g = 1; g < 4; g++) {
        double y = padT + (cH * g / 4.0);
        gc.strokeLine(padL, y, padL + cW, y);
      }

      int n = prices.size();
      int firstHistoryWeek = Math.max(1, gameController.getCurrentWeek() - n + 1);
      double[] xs = new double[n];
      double[] ys = new double[n];
      double innerH = cH * 0.82;
      double innerOff = cH * 0.09;
      for (int i = 0; i < n; i++) {
        xs[i] = padL + (n == 1 ? 0 : (i / (double) (n - 1)) * cW);
        double norm =
            prices.get(i).subtract(minVal).divide(range, 6, RoundingMode.HALF_UP).doubleValue();
        ys[i] = padT + innerH + innerOff - (norm * innerH);
      }
      xsRef[0] = xs;
      ysRef[0] = ys;

      // Gradient fill under the line
      LinearGradient fillGrad =
          new LinearGradient(0, padT, 0, padT + cH, false, CycleMethod.NO_CYCLE,
              new Stop(0, Color.web(lineHex, 0.28)),
              new Stop(1, Color.web(lineHex, 0.03)));
      gc.setFill(fillGrad);
      gc.beginPath();
      gc.moveTo(xs[0], padT + cH);
      gc.lineTo(xs[0], ys[0]);
      for (int i = 1; i < n; i++) {
        gc.lineTo(xs[i], ys[i]);
      }
      gc.lineTo(xs[n - 1], padT + cH);
      gc.closePath();
      gc.fill();

      // Price line
      gc.setStroke(Color.web(lineHex, 0.90));
      gc.setLineWidth(2);
      gc.beginPath();
      gc.moveTo(xs[0], ys[0]);
      for (int i = 1; i < n; i++) {
        gc.lineTo(xs[i], ys[i]);
      }
      gc.stroke();

      // Crosshair (drawn before trade dots so dots appear on top)
      int hi = hoverIdx[0];
      if (hi >= 0 && hi < n && !onDot[0]) {
        double cx = xs[hi], cy = ys[hi];
        // Vertical dashed rule
        gc.setStroke(Color.web("#ffffff", 0.16));
        gc.setLineWidth(1);
        gc.setLineDashes(4, 4);
        gc.strokeLine(cx, padT, cx, padT + cH);
        gc.setLineDashes((double[]) null);
        // Crosshair dot
        gc.setFill(Color.web(lineHex, 0.95));
        gc.fillOval(cx - 3.5, cy - 3.5, 7, 7);
        gc.setStroke(Color.web("#ffffff", 0.55));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - 3.5, cy - 3.5, 7, 7);
        // Price chip near top of chart
        String weeklyChangeText = "";
        Color weeklyChangeColor = Color.web("#8aa2c8", 0.90);
        if (hi > 0 && prices.get(hi - 1).compareTo(BigDecimal.ZERO) != 0) {
          BigDecimal weeklyPct = prices.get(hi).subtract(prices.get(hi - 1))
              .divide(prices.get(hi - 1), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
          if (weeklyPct.compareTo(BigDecimal.ZERO) != 0) {
            String pctSign = weeklyPct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
            weeklyChangeText = pctSign + weeklyPct.toPlainString() + "%";
            weeklyChangeColor = weeklyPct.compareTo(BigDecimal.ZERO) > 0
                ? Color.web("#4ecb71", 0.95)
                : Color.web("#e05a5a", 0.95);
          }
        }
        String chipPrefix = "Week " + (firstHistoryWeek + hi) + "  "
          + CurrencyFormatter.format(prices.get(hi));
        String chipSuffix = "";
        javafx.scene.text.Font chipFont =
            javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10);
        gc.setFont(chipFont);
        Text measureText = new Text();
        measureText.setFont(chipFont);
        measureText.setBoundsType(TextBoundsType.VISUAL);
        measureText.setText(chipPrefix);
        double prefixWidth = measureText.getLayoutBounds().getWidth();
        measureText.setText(weeklyChangeText);
        double pctWidth = measureText.getLayoutBounds().getWidth();
        measureText.setText(chipSuffix);
        double suffixWidth = measureText.getLayoutBounds().getWidth();
        double pctGap = weeklyChangeText.isEmpty() ? 0.0 : 8.0;
        double tw = prefixWidth + pctGap + pctWidth + suffixWidth;
        double chipX = Math.min(cx + 8, w - tw - 12);
        double chipY = padT + 2;
        gc.setFill(Color.web("#e8d8b0", 0.90));
        gc.fillText(chipPrefix, chipX, chipY + 11);
        gc.setFill(weeklyChangeColor);
        gc.fillText(weeklyChangeText, chipX + prefixWidth + pctGap, chipY + 11);
        gc.setFill(Color.web("#e8d8b0", 0.90));
        gc.fillText(chipSuffix, chipX + prefixWidth + pctGap + pctWidth, chipY + 11);
      }

      // Pre-compute which weeks have buys, sells, or both
      java.util.Set<Integer> weeksWithBuys = new java.util.HashSet<>();
      java.util.Set<Integer> weeksWithSells = new java.util.HashSet<>();
      for (TradeDot d : tradeDots) {
        if (d.isSell()) {
          weeksWithSells.add(d.week());
        } else {
          weeksWithBuys.add(d.week());
        }
      }
      java.util.Set<Integer> weeksWithBoth = new java.util.HashSet<>(weeksWithBuys);
      weeksWithBoth.retainAll(weeksWithSells);

      // Trade dots — one dot per week (merged if both buy+sell that week)
      java.util.Set<Integer> drawnWeeks = new java.util.HashSet<>();
      // pass 0 = buy-only, pass 1 = sell-only, pass 2 = mixed (drawn last / on top)
      for (int pass = 0; pass < 3; pass++) {
        for (TradeDot dot : tradeDots) {
          int week = dot.week();
          int idx = week - firstHistoryWeek;
          if (idx < 0 || idx >= n) {
            continue;
          }
          boolean isMixed = weeksWithBoth.contains(week);
          if (pass == 0 && (isMixed || dot.isSell())) {
            continue;  // buy-only pass
          }
          if (pass == 1 && (isMixed || !dot.isSell())) {
            continue; // sell-only pass
          }
          if (pass == 2 && !isMixed) {
            continue;                   // mixed pass
          }
          if (!drawnWeeks.add(week)) {
            continue; // already drew this week
          }
          double dotX = xs[idx], dotY = ys[idx];
          if (isMixed) {
            // Blended: green outer halo, split inner (left=buy green, right=sell)
            gc.setFill(Color.web("#1e7a40", 0.28));
            gc.fillOval(dotX - 7, dotY - 7, 14, 14);
            // Left half — buy green
            gc.save();
            gc.beginPath();
            gc.rect(dotX - 10, dotY - 10, 10, 20);
            gc.clip();
            gc.setFill(Color.web("#4ecb71"));
            gc.fillOval(dotX - 4, dotY - 4, 8, 8);
            gc.restore();
            // Right half — sell red
            gc.save();
            gc.beginPath();
            gc.rect(dotX, dotY - 10, 10, 20);
            gc.clip();
            gc.setFill(Color.web("#e05a5a"));
            gc.fillOval(dotX - 4, dotY - 4, 8, 8);
            gc.restore();
            // Thin dividing line
            gc.setStroke(Color.web("#060d20", 0.55));
            gc.setLineWidth(1);
            gc.strokeLine(dotX, dotY - 4, dotX, dotY + 4);
            // 2 = mixed sentinel for hit detection
            drawnDots.add(new double[] {dotX, dotY, week, 0, 0, 2});
          } else if (dot.isSell()) {
            gc.setFill(Color.web("#e05a5a", 0.28));
            gc.fillOval(dotX - 7, dotY - 7, 14, 14);
            gc.setFill(Color.web("#e05a5a"));
            gc.fillOval(dotX - 4, dotY - 4, 8, 8);
            drawnDots.add(new double[] {dotX, dotY, week, dot.quantity().doubleValue(),
                dot.price().doubleValue(), 1});
          } else {
            gc.setFill(Color.web("#4ecb71", 0.30));
            gc.fillOval(dotX - 7, dotY - 7, 14, 14);
            gc.setFill(Color.web("#4ecb71"));
            gc.fillOval(dotX - 4, dotY - 4, 8, 8);
            drawnDots.add(new double[] {dotX, dotY, week, dot.quantity().doubleValue(),
                dot.price().doubleValue(), 0});
          }
          // Highlight when selected from transaction history
          boolean isHighlighted = highlightedTx != null
              && highlightedTx.symbol().equals(sym)
              && highlightedTx.week() == week
              && (isMixed || highlightedTx.isBuy() != dot.isSell());
          if (isHighlighted && highlightFade[0] > 0) {
            double fa = highlightFade[0];
            String dotColor = isMixed ? "#1e7a40" : (dot.isSell() ? "#e05a5a" : "#4ecb71");
            gc.setStroke(Color.web(dotColor, 0.22 * fa));
            gc.setLineWidth(1);
            gc.setLineDashes(3, 4);
            gc.strokeLine(dotX, padT + 18, dotX, dotY - 9);
            gc.setLineDashes((double[]) null);
            gc.setStroke(Color.web(dotColor, 0.12 * fa));
            gc.setLineWidth(7);
            gc.strokeOval(dotX - 13, dotY - 13, 26, 26);
            gc.setStroke(Color.web(dotColor, 0.30 * fa));
            gc.setLineWidth(2);
            gc.strokeOval(dotX - 9, dotY - 9, 18, 18);
            gc.setStroke(Color.web("#ffffff", 0.40 * fa));
            gc.setLineWidth(1);
            gc.strokeOval(dotX - 6, dotY - 6, 12, 12);
            String txLabel =
                (isMixed ? "BUY/SELL" : (dot.isSell() ? "SELL" : "BUY")) + "  Week " + week;
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 9));
            double tw = txLabel.length() * 5.3;
            double chipX = Math.min(dotX + 5, w - tw - 12);
            double chipY = padT + 2;
            gc.setFill(Color.web("#060d20", 0.75 * fa));
            gc.fillRoundRect(chipX - 4, chipY - 2, tw + 8, 13, 5, 5);
            gc.setFill(Color.web(dotColor, 0.85 * fa));
            gc.fillText(txLabel, chipX, chipY + 9);
          }
        }
      }

      // Last price dot + label (suppressed when crosshair is active)
      double lx = xs[n - 1], ly = ys[n - 1];
      gc.setFill(Color.web(lineHex));
      gc.fillOval(lx - 3.5, ly - 3.5, 7, 7);
      if (hi < 0 || onDot[0]) {
        String lastTxt = CurrencyFormatter.format(prices.get(n - 1));
        double lblX = Math.min(lx + 8, w - lastTxt.length() * 6.5);
        gc.setFill(Color.web("#e8d8b0", 0.85));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        gc.fillText(lastTxt, lblX, ly + 4);
      }
    };

    // Hover: crosshair on price line, or trade-dot tooltip
    pane.setOnMouseMoved(e -> {
      double mx = e.getX(), my = e.getY();
      double[] hit = null;
      for (double[] dot : drawnDots) {
        double dx = mx - dot[0], dy = my - dot[1];
        if (dx * dx + dy * dy <= 64) {
          hit = dot;
          break;
        }
      }
      if (hit != null) {
        onDot[0] = true;
        hoverIdx[0] = -1;
        int hitWeek = (int) hit[2];
        List<TradeDot> weekBuys =
            tradeDots.stream().filter(d -> d.week() == hitWeek && !d.isSell()).toList();
        List<TradeDot> weekSells =
            tradeDots.stream().filter(d -> d.week() == hitWeek && d.isSell()).toList();
        tooltip.getChildren().clear();
        // ── Buys section ──────────────────────────────────────────
        if (!weekBuys.isEmpty()) {
          BigDecimal totalQty = weekBuys.stream().map(TradeDot::quantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal avgPrice = weekBuys.stream().map(d -> d.price().multiply(d.quantity()))
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .divide(totalQty, 4, RoundingMode.HALF_UP);
          Label weekLbl = new Label(
              "Bought · Week " + hitWeek + (weekBuys.size() > 1 ? "  ×" + weekBuys.size() : ""));
          weekLbl.getStyleClass().add("chart-tooltip-week");
          Label quantityLbl =
              new Label("Quantity: " + totalQty.stripTrailingZeros().toPlainString());
          quantityLbl.getStyleClass().add("chart-tooltip-row");
          Label priceLbl = new Label((weekBuys.size() > 1 ? "Average price: " : "Price: ") +
              CurrencyFormatter.format(avgPrice));
          priceLbl.getStyleClass().add("chart-tooltip-row");
          BigDecimal gain = stock.getSalesPrice().subtract(avgPrice).multiply(totalQty);
          String sign = gain.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
          Label gainLbl = new Label("P&L: " + sign + CurrencyFormatter.format(gain));
          gainLbl.getStyleClass().add(
              gain.compareTo(BigDecimal.ZERO) >= 0 ? "chart-tooltip-gain" : "chart-tooltip-loss");
          tooltip.getChildren().addAll(weekLbl, quantityLbl, priceLbl, gainLbl);
        }
        // ── Divider when both types present ───────────────────────
        if (!weekBuys.isEmpty() && !weekSells.isEmpty()) {
          Region divider = new Region();
          divider.setMaxWidth(Double.MAX_VALUE);
          divider.setMinHeight(3);
          divider.setMaxHeight(3);
          divider.setStyle("-fx-background-color:rgba(92, 116, 255, 0.25);");
          tooltip.getChildren().add(divider);
        }
        // ── Sells section ─────────────────────────────────────────
        if (!weekSells.isEmpty()) {
          BigDecimal totalQty = weekSells.stream().map(TradeDot::quantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal avgPrice = weekSells.stream().map(d -> d.price().multiply(d.quantity()))
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .divide(totalQty, 4, RoundingMode.HALF_UP);
          Label weekLbl = new Label(
              "Sold · Week " + hitWeek + (weekSells.size() > 1 ? "  ×" + weekSells.size() : ""));
          weekLbl.getStyleClass().add("chart-tooltip-sell-week");
          Label quantityLbl =
              new Label("Quantity: " + totalQty.stripTrailingZeros().toPlainString());
          quantityLbl.getStyleClass().add("chart-tooltip-row");
          Label priceLbl = new Label((weekSells.size() > 1 ? "Average price: " : "Price: ") +
              CurrencyFormatter.format(avgPrice));
          priceLbl.getStyleClass().add("chart-tooltip-row");
          tooltip.getChildren().addAll(weekLbl, quantityLbl, priceLbl);
        }
        double tx = hit[0] + 12, ty = hit[1] - 70;
        if (ty < 4) {
          ty = hit[1] + 14;
        }
        if (tx + 150 > pane.getWidth()) {
          tx = hit[0] - 155;
        }
        tooltip.setLayoutX(tx);
        tooltip.setLayoutY(ty);
        tooltip.setVisible(true);
        drawRef[0].run();
      } else {
        onDot[0] = false;
        tooltip.setVisible(false);
        double[] xs = xsRef[0];
        if (xs.length >= 2) {
          final double padL = 10, padR = 10;
          double cW = canvas.getWidth() - padL - padR;
          int n = xs.length;
          int idx = (int) Math.round((mx - padL) / cW * (n - 1));
          hoverIdx[0] = Math.max(0, Math.min(n - 1, idx));
        } else {
          hoverIdx[0] = -1;
        }
        drawRef[0].run();
      }
    });
    pane.setOnMouseExited(e -> {
      tooltip.setVisible(false);
      onDot[0] = false;
      hoverIdx[0] = -1;
      drawRef[0].run();
    });

    canvas.widthProperty().addListener((obs, o, nv) -> drawRef[0].run());
    canvas.heightProperty().addListener((obs, o, nv) -> drawRef[0].run());

    // Start fade timer if this chart has a highlight
    if (highlightedTx != null && highlightedTx.symbol().equals(sym)) {
      if (highlightFadeTimer != null) {
        highlightFadeTimer.stop();
      }
      long[] startNano = {-1};
      AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
          if (startNano[0] < 0) {
            startNano[0] = now;
          }
          long elapsed = now - startNano[0];
          long WAIT_NS = 5_000_000_000L; //  5 s
          long FADE_NS = 800_000_000L; //  0.8 s
          if (elapsed >= WAIT_NS + FADE_NS) {
            highlightFade[0] = 0.0;
            highlightedTx = null;
            highlightFadeTimer = null;
            stop();
            drawRef[0].run();
          } else if (elapsed >= WAIT_NS) {
            double t = (elapsed - WAIT_NS) / (double) FADE_NS;
            // ease-in curve so fade feels natural
            highlightFade[0] = 1.0 - (t * t);
            drawRef[0].run();
          }
          // still in wait window — no redraw needed
        }
      };
      highlightFadeTimer = timer;
      timer.start();
    }

    Platform.runLater(drawRef[0]);
    return pane;
  }

  private static TableColumn<Share, String> col(String title,
                                                java.util.function.Function<Share, String> fn,
                                                double min, double max) {
    TableColumn<Share, String> c = new TableColumn<>(title);
    c.setCellValueFactory(cell -> new SimpleStringProperty(fn.apply(cell.getValue())));
    c.setMinWidth(min);
    c.setMaxWidth(max);
    return c;
  }

  private static TableColumn<Share, BigDecimal> numericCol(String title,
                                                            java.util.function.Function<Share, BigDecimal> fn,
                                                            double min,
                                                            double max,
                                                            java.util.function.Function<BigDecimal, String> formatter) {
    TableColumn<Share, BigDecimal> c = new TableColumn<>(title);
    c.setCellValueFactory(cell -> new SimpleObjectProperty<>(fn.apply(cell.getValue())));
    c.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : formatter.apply(item));
      }
    });
    c.setMinWidth(min);
    c.setMaxWidth(max);
    return c;
  }

  private static AudioClip loadAudioClip(String resourcePath) {
    var resource = GameView.class.getResource(resourcePath);
    if (resource == null) {
      return null;
    }
    return new AudioClip(resource.toExternalForm());
  }

  private static void playAudioClip(AudioClip clip, DoubleSupplier sfxVolumeSupplier) {
    if (clip == null) {
      return;
    }
    double volume = sfxVolumeSupplier == null ? 1.0 : sfxVolumeSupplier.getAsDouble();
    clip.play(Math.clamp(volume, 0.0, 1.0));
  }
}