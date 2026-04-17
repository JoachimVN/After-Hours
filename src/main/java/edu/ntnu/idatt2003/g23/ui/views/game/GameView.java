package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import java.util.function.Consumer;
import edu.ntnu.idatt2003.g23.AppConfig;

public final class GameView {

    public static StackPane build(Runnable onBack, Runnable onSettings,
                                   Player player, Exchange exchange) {
    GameController gameController = new GameController(player);

        StackPane[] overlayRef = {null};
        Node[] rootRef = {null};

        // ── Observable data ──────────────────────────────────────────────────
        ObservableList<Stock> allStocks        = FXCollections.observableArrayList(exchange.getStocks());
        FilteredList<Stock>   filteredStocks   = new FilteredList<>(allStocks, s -> true);
        ObservableList<Share> portfolioItems   = FXCollections.observableArrayList(gameController.getOwnedShares());
        ObjectProperty<Stock> selectedStock    = new SimpleObjectProperty<>(allStocks.isEmpty() ? null : allStocks.get(0));

        // ── Stat pill labels ─────────────────────────────────────────────────
        Label cashVal    = new Label(fmt(player.getMoney()));
        Label portVal    = new Label(fmt(player.getPortfolio().getNetWorth()));
        Label nwVal      = new Label(fmt(player.getNetWorth()));
        Label weekNumLbl = new Label(String.valueOf(exchange.getWeek()));
        cashVal.getStyleClass().add("stat-pill-value");
        portVal.getStyleClass().add("stat-pill-value");
        nwVal.getStyleClass().add("stat-pill-value");
        weekNumLbl.getStyleClass().add("week-number");

        // ── Detail panel (right) — rebuilt on stock selection ────────────────
        VBox detailArea = new VBox();
        detailArea.getStyleClass().add("game-detail-area");
        VBox.setVgrow(detailArea, Priority.ALWAYS);

        // ── Stock list (left panel) ──────────────────────────────────────────
        VBox stockListBox = new VBox(4);
        stockListBox.getStyleClass().add("game-stock-list");
        Node[] selectedCardRef = {null};

        // ── Favorites + filter + sort state ─────────────────────────────────
        Set<String> favorites = new HashSet<>();
        String[] stockFilterRef = {"ALL"};
        String[] stockSortRef   = {"NAME"};
        Runnable[] applyFilterRef = {null};

        // ── Search field (declared early for closure access) ─────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D  Search stocks\u2026");
        searchField.getStyleClass().add("game-search-field");

        // ── Refresh closure ──────────────────────────────────────────────────
        Runnable[] refreshRef = {null};
        refreshRef[0] = () -> {
            weekNumLbl.setText(String.valueOf(exchange.getWeek()));
            cashVal.setText(fmt(player.getMoney()));
            portVal.setText(fmt(player.getPortfolio().getNetWorth()));
            nwVal.setText(fmt(player.getNetWorth()));
            portfolioItems.setAll(gameController.getOwnedShares());
            if (applyFilterRef[0] != null) applyFilterRef[0].run();
            rebuildDetail(detailArea, selectedStock.get(), player, exchange, refreshRef, overlayRef);
        };

        // ── Apply-filter closure + initial population ────────────────────────
        applyFilterRef[0] = () -> {
            String lower = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            filteredStocks.setPredicate(s -> {
                boolean textMatch = lower.isEmpty()
                        || s.getSymbol().toLowerCase().contains(lower)
                        || s.getCompany().toLowerCase().contains(lower);
                boolean typeMatch = switch (stockFilterRef[0]) {
                    case "OWNED"  -> player.getPortfolio().getShareBySymbol(s.getSymbol())
                            .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
                            .compareTo(BigDecimal.ZERO) > 0;
                    case "UP"     -> pctChange(s).compareTo(BigDecimal.ZERO) > 0;
                    case "DOWN"   -> pctChange(s).compareTo(BigDecimal.ZERO) < 0;
                    case "FAVORITES" -> favorites.contains(s.getSymbol());
                    default       -> true;
                };
                return textMatch && typeMatch;
            });
            java.util.Comparator<Stock> sortCmp = switch (stockSortRef[0]) {
                case "PRICE_ASC"  -> java.util.Comparator.comparing(Stock::getSalesPrice);
                case "PRICE_DESC" -> java.util.Comparator.comparing(Stock::getSalesPrice).reversed();
                case "CHG_ASC"    -> java.util.Comparator.comparing(s -> pctChange((Stock) s));
                case "CHG_DESC"   -> java.util.Comparator.comparing((Stock s) -> pctChange(s)).reversed();
                case "NAME_DESC"  -> java.util.Comparator.comparing(Stock::getSymbol).reversed();
                default           -> java.util.Comparator.comparing(Stock::getSymbol);
            };
            rebuildStockList(stockListBox, filteredStocks, sortCmp, selectedStock, selectedCardRef, player, favorites, applyFilterRef[0]);
        };

        // Initial stock list population
        applyFilterRef[0].run();

        // Rebuild detail when selection changes
        selectedStock.addListener((obs, old, stock) -> {
            rebuildDetail(detailArea, stock, player, exchange, refreshRef, overlayRef);
        });

        // Show detail immediately for first stock
        if (selectedStock.get() != null) {
            rebuildDetail(detailArea, selectedStock.get(), player, exchange, refreshRef, overlayRef);
        }

        // ── Search field listener ──────────────────────────────────────────────
        searchField.textProperty().addListener((obs, old, val) -> applyFilterRef[0].run());

        // ── Stock filter chips ────────────────────────────────────────────────
        String[] chipKeys   = {"ALL", "FAVORITES", "OWNED", "UP", "DOWN"};
        String[] chipLabels = {"All",  "\u2605 Favorites", "Owned", "\u25B2 Up", "\u25BC Down"};
        FlowPane filterChips = new FlowPane(4, 4);
        for (int i = 0; i < chipLabels.length; i++) {
            final int idx = i;
            Button chip = new Button(chipLabels[i]);
            chip.getStyleClass().add("stock-filter-chip");
            if (i == 0) chip.getStyleClass().add("stock-filter-chip-active");
            chip.setOnAction(ev -> {
                stockFilterRef[0] = chipKeys[idx];
                for (Node n : filterChips.getChildren()) n.getStyleClass().remove("stock-filter-chip-active");
                chip.getStyleClass().add("stock-filter-chip-active");
                applyFilterRef[0].run();
            });
            filterChips.getChildren().add(chip);
        }
        Label filterLabel = new Label("FILTER");
        filterLabel.getStyleClass().add("stock-row-section-label");
        VBox filterRow = new VBox(3, filterLabel, filterChips);
        filterRow.getStyleClass().add("stock-filter-row");

        // ── Sort row ──────────────────────────────────────────────────────────
        Button sortName  = new Button("A\u2013Z");
        Button sortPrice = new Button("Price \u25bc");
        Button sortChg   = new Button("Change \u25bc");
        sortName.getStyleClass().addAll("stock-sort-chip", "stock-sort-chip-active");
        sortPrice.getStyleClass().add("stock-sort-chip");
        sortChg.getStyleClass().add("stock-sort-chip");
        FlowPane sortChips = new FlowPane(4, 4);
        sortChips.getChildren().addAll(sortName, sortPrice, sortChg);

        sortName.setOnAction(ev -> {
            if (sortName.getStyleClass().contains("stock-sort-chip-active")) {
                stockSortRef[0] = stockSortRef[0].equals("NAME_DESC") ? "NAME" : "NAME_DESC";
            } else {
                stockSortRef[0] = "NAME";
                sortName.getStyleClass().add("stock-sort-chip-active");
                sortPrice.getStyleClass().remove("stock-sort-chip-active");
                sortChg.getStyleClass().remove("stock-sort-chip-active");
            }
            sortName.setText(stockSortRef[0].equals("NAME_DESC") ? "Z\u2013A" : "A\u2013Z");
            applyFilterRef[0].run();
        });
        sortPrice.setOnAction(ev -> {
            if (sortPrice.getStyleClass().contains("stock-sort-chip-active")) {
                // toggle direction
                stockSortRef[0] = stockSortRef[0].equals("PRICE_DESC") ? "PRICE_ASC" : "PRICE_DESC";
            } else {
                stockSortRef[0] = "PRICE_DESC";
                sortName.getStyleClass().remove("stock-sort-chip-active");
                sortChg.getStyleClass().remove("stock-sort-chip-active");
                sortPrice.getStyleClass().add("stock-sort-chip-active");
            }
            sortPrice.setText("Price " + (stockSortRef[0].equals("PRICE_DESC") ? "\u25bc" : "\u25b2"));
            applyFilterRef[0].run();
        });
        sortChg.setOnAction(ev -> {
            if (sortChg.getStyleClass().contains("stock-sort-chip-active")) {
                stockSortRef[0] = stockSortRef[0].equals("CHG_DESC") ? "CHG_ASC" : "CHG_DESC";
            } else {
                stockSortRef[0] = "CHG_DESC";
                sortName.getStyleClass().remove("stock-sort-chip-active");
                sortPrice.getStyleClass().remove("stock-sort-chip-active");
                sortChg.getStyleClass().add("stock-sort-chip-active");
            }
            sortChg.setText("Change " + (stockSortRef[0].equals("CHG_DESC") ? "\u25bc" : "\u25b2"));
            applyFilterRef[0].run();
        });

        Label sortLabel = new Label("SORT");
        sortLabel.getStyleClass().add("stock-row-section-label");
        VBox sortRow = new VBox(3, sortLabel, sortChips);
        sortRow.getStyleClass().add("stock-sort-row");

        Label marketTitle = new Label("Market Stocks");
        marketTitle.getStyleClass().add("game-panel-title");

        ScrollPane stockScroll = new ScrollPane(stockListBox);
        stockScroll.setFitToWidth(true);
        stockScroll.getStyleClass().add("game-scroll");
        stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        stockScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        VBox.setVgrow(stockScroll, Priority.ALWAYS);

        VBox leftPanel = new VBox(8, marketTitle, searchField, filterRow, sortRow, stockScroll);
        leftPanel.getStyleClass().add("game-left-panel");
        leftPanel.setPrefWidth(300);
        leftPanel.setMinWidth(Region.USE_PREF_SIZE);
        leftPanel.setMaxWidth(Region.USE_PREF_SIZE);

        // ── Portfolio table (bottom of right panel) ──────────────────────────
        Label portTitle = new Label("Portfolio");
        portTitle.getStyleClass().add("game-panel-title");

        TableView<Share> portfolioTable = buildPortfolioTable(portfolioItems, selectedShareStock -> {
            if (selectedShareStock == null) {
                return;
            }

            String symbol = selectedShareStock.getSymbol();
            boolean existsInFiltered = filteredStocks.stream().anyMatch(s -> s.getSymbol().equals(symbol));
            if (!existsInFiltered) {
                searchField.setText("");
            }

            Stock target = allStocks.stream()
                    .filter(s -> s.getSymbol().equals(symbol))
                    .findFirst()
                    .orElse(selectedShareStock);

            selectedStock.set(target);
            focusStockCardInList(symbol, stockListBox, selectedCardRef, stockScroll);
        });
        portfolioTable.setPrefHeight(180);
        portfolioTable.setMaxHeight(220);

        VBox rightPanel = new VBox(0, detailArea, portTitle, portfolioTable);
        rightPanel.getStyleClass().add("game-right-panel");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // ── Body ─────────────────────────────────────────────────────────────
        HBox body = new HBox(0, leftPanel, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);

        // ── Sub-bar: week + next-week ─────────────────────────────────────────
        VBox weekCard = new VBox(2,
                labelSmall("WEEK"),
                weekNumLbl);
        weekCard.getStyleClass().add("week-card");
        weekCard.setAlignment(Pos.CENTER);

        Button nextWeekBtn = new Button("\u25B6  Next Week");
        nextWeekBtn.getStyleClass().add("next-week-button");

        Label calmDownLbl = new Label("\uD83D\uDE0C Calm down");
        calmDownLbl.getStyleClass().add("calm-down-label");
        calmDownLbl.setOpacity(0);
        calmDownLbl.setMouseTransparent(true);

        FadeTransition[] calmFade = {null};

        // Rate-limit: max 6 advances per second (sliding window)
        long[] advanceTimes = new long[6];
        int[] advanceHead = {0};

        nextWeekBtn.setOnAction(e -> {
            long now = System.currentTimeMillis();
            long oldest = advanceTimes[advanceHead[0]];
            if (now - oldest < 1000) {
                if (calmFade[0] != null) calmFade[0].stop();
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
            exchange.advance();
            refreshRef[0].run();
        });

        Button sellAllHoldingsBtn = new Button("\u2198  Sell All Holdings");
        sellAllHoldingsBtn.getStyleClass().addAll("next-week-button", "sell-all-holdings-button");
        sellAllHoldingsBtn.setOnAction(e -> {
            BigDecimal totalOwnedQty = player.getPortfolio().getShares().stream()
                    .map(Share::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalOwnedQty.compareTo(BigDecimal.ZERO) <= 0) {
                showError(overlayRef[0], "You don't own any shares to sell.");
                return;
            }

            BigDecimal[] preview = previewSellAll(player);
            showBulkTradeConfirm(overlayRef[0],
                    "SELL ALL HOLDINGS",
                    preview[4],
                    preview[0],
                    preview[1],
                    preview[2],
                    preview[3],
                    () -> {
                        try {
                            BigDecimal[] result = executeSellAll(player, exchange);
                            showBulkReceipt(overlayRef[0],
                                    "SELL ALL HOLDINGS",
                                    result[4],
                                    result[3],
                                    result[1],
                                    result[2],
                                    player.getMoney());
                            refreshRef[0].run();
                        } catch (Exception ex) {
                            showError(overlayRef[0], ex.getMessage());
                        }
                    });
        });

        Button marketMoversBtn = new Button("\uD83D\uDCC8  Market Movers");
        marketMoversBtn.getStyleClass().add("market-movers-button");
        marketMoversBtn.setOnAction(e -> {
            Consumer<Stock> selectStock = s -> {
                String sym = s.getSymbol();
                if (filteredStocks.stream().noneMatch(st -> st.getSymbol().equals(sym))) {
                    searchField.setText("");
                }
                Stock target = allStocks.stream()
                        .filter(st -> st.getSymbol().equals(sym))
                        .findFirst().orElse(s);
                selectedStock.set(target);
                focusStockCardInList(sym, stockListBox, selectedCardRef, stockScroll);
            };
            showMarketMovers(overlayRef[0], exchange, rootRef[0], selectStock);
        });
        VBox nextWeekStack = new VBox(2, calmDownLbl, nextWeekBtn);
        nextWeekStack.setAlignment(Pos.BOTTOM_CENTER);
        Region subSpacer = new Region(); HBox.setHgrow(subSpacer, Priority.ALWAYS);

        Button historyBtn = new Button("\uD83D\uDCCB  History");
        historyBtn.getStyleClass().add("market-movers-button");
        historyBtn.setOnAction(e -> showTransactionHistory(overlayRef[0], player, rootRef[0]));

        HBox subBar = new HBox(16, weekCard, nextWeekStack, sellAllHoldingsBtn, subSpacer, historyBtn, marketMoversBtn);
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

        Node cashPill  = statPill("Available Cash",   cashVal);
        Node portPill  = statPill("Portfolio Value",  portVal);
        Node nwPill    = statPill("Total Net Worth",  nwVal);

        Button settingsBtn = new Button("\u2699");
        settingsBtn.getStyleClass().add("game-icon-button");
        settingsBtn.setOnAction(e -> onSettings.run());

        Region tl = new Region(); HBox.setHgrow(tl, Priority.ALWAYS);
        HBox topBar = new HBox(10, backBtn, appTitle, tl, cashPill, portPill, nwPill, settingsBtn);
        topBar.getStyleClass().add("game-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // ── Root ─────────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        rootRef[0] = root;
        root.getStyleClass().add("home-page");

        VBox topSection = new VBox(0, topBar, subBar);
        root.setTop(topSection);
        root.setCenter(body);

        // ── Dev panel ─────────────────────────────────────────────────────────
        VBox devPanel = buildDevPanel(player, exchange, refreshRef);
        devPanel.visibleProperty().bind(AppConfig.DEV_MODE);
        devPanel.managedProperty().bind(AppConfig.DEV_MODE);
        StackPane.setAlignment(devPanel, Pos.BOTTOM_RIGHT);

        StackPane overlay = new StackPane(root, devPanel);
        overlayRef[0] = overlay;
        return overlay;
    }

    private static VBox buildDevPanel(Player player, Exchange exchange, Runnable[] refreshRef) {
        Label title = new Label("🛠  DEV MODE");
        title.getStyleClass().add("dev-panel-title");

        // Week counter
        Label weekDisplay = new Label("Week: " + exchange.getWeek());
        weekDisplay.getStyleClass().add("dev-panel-stat");

        // Advance week buttons (bypass rate limiter)
        TextField advInput = new TextField();
        advInput.setPromptText("Weeks");
        advInput.getStyleClass().add("dev-panel-input");
        advInput.setPrefWidth(70);
        Button advCustom = devBtn("Advance");
        HBox advRow = new HBox(4, advInput, advCustom);
        advRow.setAlignment(Pos.CENTER_LEFT);

        Runnable doAdvance = () -> {
            weekDisplay.setText("Week: " + exchange.getWeek());
            refreshRef[0].run();
        };
        advCustom.setOnAction(e -> {
            try {
                int n = Integer.parseInt(advInput.getText().trim());
                if (n > 0) { for (int i=0;i<n;i++) exchange.advance(); doAdvance.run(); advInput.clear(); }
            } catch (NumberFormatException ignored) { advInput.selectAll(); }
        });

        // Cash buttons
        Button cash1k   = devBtn("+$1K");
        Button cash10k  = devBtn("+$10K");
        Button cash100k = devBtn("+$100K");
        cash1k  .setOnAction(e -> { player.addMoney(BigDecimal.valueOf(1_000));    refreshRef[0].run(); });
        cash10k .setOnAction(e -> { player.addMoney(BigDecimal.valueOf(10_000));   refreshRef[0].run(); });
        cash100k.setOnAction(e -> { player.addMoney(BigDecimal.valueOf(100_000));  refreshRef[0].run(); });
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
                BigDecimal amount = new BigDecimal(cashInput.getText().trim().replace(",", ""));
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal current = player.getMoney();
                    if (amount.compareTo(current) > 0) {
                        player.addMoney(amount.subtract(current));
                    } else {
                        player.withdrawMoney(current.subtract(amount));
                    }
                    cashInput.clear();
                    refreshRef[0].run();
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
            exchange.setFrozen(frozen[0]);
            freezeBtn.setText(frozen[0] ? "Unfreeze Prices" : "Freeze Prices");
            if (frozen[0]) freezeBtn.getStyleClass().add("dev-btn-active");
            else           freezeBtn.getStyleClass().remove("dev-btn-active");
        });

        // Net worth snapshot
        Label nwLabel = new Label();
        nwLabel.getStyleClass().add("dev-panel-stat");
        Runnable updateNw = () -> {
            BigDecimal portVal = player.getPortfolio().getShares().stream()
                    .map(sh -> sh.getStock().getSalesPrice().multiply(sh.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            nwLabel.setText("NW: $" + player.getMoney().add(portVal)
                    .setScale(0, RoundingMode.HALF_UP).toPlainString());
        };
        updateNw.run();
        // update after every advance
        Runnable origRefresh = refreshRef[0];
        refreshRef[0] = () -> { origRefresh.run(); updateNw.run(); weekDisplay.setText("Week: " + exchange.getWeek()); };

        VBox panel = new VBox(6,
                title,
                weekDisplay,
                nwLabel,
                new Label("Advance:") {{ getStyleClass().add("dev-panel-section"); }},
                advRow,
                new Label("Cash:") {{ getStyleClass().add("dev-panel-section"); }},
                cashRow,
                setCashRow,
                freezeBtn
        );
        panel.getStyleClass().add("dev-panel");
        panel.setMaxWidth(220);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        return panel;
    }

    private static Button devBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("dev-btn");
        return b;
    }

    // ── Stock list builder ────────────────────────────────────────────────────

    private static void rebuildStockList(VBox box, FilteredList<Stock> stocks,
                                         java.util.Comparator<Stock> sortCmp,
                                         ObjectProperty<Stock> selectedStock,
                                         Node[] selectedCardRef,
                                         Player player,
                                         Set<String> favorites,
                                         Runnable onFavChanged) {
        box.getChildren().clear();
        selectedCardRef[0] = null;
        stocks.stream().sorted(sortCmp).forEach(stock ->
                box.getChildren().add(buildStockCard(stock, selectedStock, selectedCardRef, player, favorites, onFavChanged)));
    }

    private static Node buildStockCard(Stock stock, ObjectProperty<Stock> selectedStock,
                                        Node[] selectedCardRef,
                                        Player player,
                                        Set<String> favorites,
                                        Runnable onFavChanged) {
        Label symLbl    = new Label(stock.getSymbol());
        symLbl.getStyleClass().add("stock-card-symbol");

        Label compLbl   = new Label(stock.getCompany());
        compLbl.getStyleClass().add("stock-card-company");

        Label priceLbl  = new Label(fmt(stock.getSalesPrice()));
        priceLbl.getStyleClass().add("stock-card-price");

        BigDecimal pct  = pctChange(stock);
        String     sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        Label pctLbl    = new Label(sign + pct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
        pctLbl.getStyleClass().add(pct.compareTo(BigDecimal.ZERO) >= 0 ? "stock-pct-up" : "stock-pct-down");

        BigDecimal ownedQty = player.getPortfolio().getShareBySymbol(stock.getSymbol())
                .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        Label ownedLbl = null;
        if (ownedQty.compareTo(BigDecimal.ZERO) > 0) {
            ownedLbl = new Label("Owned: " + ownedQty.stripTrailingZeros().toPlainString());
            ownedLbl.getStyleClass().add("stock-owned-label");
        }

        // ── Favorite star button ─────────────────────────────────────────────
        boolean isFav = favorites.contains(stock.getSymbol());
        Button favBtn = new Button(isFav ? "\u2605" : "\u2606");
        favBtn.getStyleClass().add("stock-fav-btn");
        if (isFav) favBtn.getStyleClass().add("stock-fav-btn-active");
        favBtn.setOnAction(ev -> {
            if (favorites.contains(stock.getSymbol())) {
                favorites.remove(stock.getSymbol());
            } else {
                favorites.add(stock.getSymbol());
            }
            onFavChanged.run();
        });
        favBtn.setOnMouseClicked(e -> e.consume());

        VBox left;
        if (ownedLbl != null) {
            left = new VBox(2, symLbl, compLbl, pctLbl, ownedLbl);
        } else {
            left = new VBox(2, symLbl, compLbl, pctLbl);
        }
        VBox right = new VBox(4);
        right.setAlignment(Pos.TOP_RIGHT);
        right.getChildren().addAll(favBtn, priceLbl);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox card = new HBox(8, left, spacer, right);
        card.getStyleClass().add("stock-card");
        if (isFav) card.getStyleClass().add("stock-card-favorited");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setUserData(stock.getSymbol());

        if (stock.equals(selectedStock.get())) {
            card.getStyleClass().add("stock-card-selected");
            selectedCardRef[0] = card;
        }

        card.setOnMouseClicked(e -> {
            if (selectedCardRef[0] != null) {
                selectedCardRef[0].getStyleClass().remove("stock-card-selected");
            }
            if (!card.getStyleClass().contains("stock-card-selected")) {
                card.getStyleClass().add("stock-card-selected");
            }
            selectedCardRef[0] = card;
            selectedStock.set(stock);
        });
        return card;
    }

    private static void focusStockCardInList(String symbol, VBox stockListBox, Node[] selectedCardRef, ScrollPane stockScroll) {
        Platform.runLater(() -> {
            int total = stockListBox.getChildren().size();
            for (int i = 0; i < total; i++) {
                Node n = stockListBox.getChildren().get(i);
                Object data = n.getUserData();
                if (data instanceof String cardSymbol && cardSymbol.equals(symbol)) {
                    if (selectedCardRef[0] != null) {
                        selectedCardRef[0].getStyleClass().remove("stock-card-selected");
                    }
                    if (!n.getStyleClass().contains("stock-card-selected")) {
                        n.getStyleClass().add("stock-card-selected");
                    }
                    selectedCardRef[0] = n;
                    stockScroll.setVvalue(total <= 1 ? 0 : (double) i / (double) (total - 1));
                    break;
                }
            }
        });
    }

    // ── Detail panel builder ──────────────────────────────────────────────────

    private static void rebuildDetail(VBox area, Stock stock, Player player, Exchange exchange,
                                       Runnable[] refreshRef,
                                       StackPane[] overlayRef) {
        area.getChildren().clear();
        if (stock == null) return;

        Label sym   = new Label(stock.getSymbol());  sym.getStyleClass().add("detail-symbol");
        Label comp  = new Label(stock.getCompany()); comp.getStyleClass().add("detail-company");
        Label price = new Label(fmt(stock.getSalesPrice())); price.getStyleClass().add("detail-price");

        BigDecimal pct  = pctChange(stock);
        String     sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        Label pctBadge  = new Label(sign + pct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
        pctBadge.getStyleClass().add(pct.compareTo(BigDecimal.ZERO) >= 0 ? "detail-badge-up" : "detail-badge-down");

        HBox priceRow = new HBox(12, price, pctBadge);
        priceRow.setAlignment(Pos.BASELINE_LEFT);

        // H/L stats
        BigDecimal hi = stock.getHistoricalPrices().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lo = stock.getHistoricalPrices().stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        VBox hiBox = new VBox(2, labelSmall("ALL TIME HIGH"), new Label(fmt(hi)) {{ getStyleClass().add("stat-hl-value-up"); }});
        VBox loBox = new VBox(2, labelSmall("ALL TIME LOW"),  new Label(fmt(lo)) {{ getStyleClass().add("stat-hl-value-down"); }});
        Region hlSpacer = new Region(); HBox.setHgrow(hlSpacer, Priority.ALWAYS);
        HBox hlRow = new HBox(24, hiBox, loBox);

        // ── Qty stepper: [−] [field] [+] ─────────────────────────────────────
        Button decBtn = new Button("\u2212");
        decBtn.getStyleClass().add("trade-qty-btn");
        TextField qtyField = new TextField("1");
        qtyField.getStyleClass().add("trade-qty-field");
        Button incBtn = new Button("+");
        incBtn.getStyleClass().add("trade-qty-btn");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount ($)");
        amountField.getStyleClass().add("trade-amount-field");
        Button maxBuyBtn = new Button("\u25b2  Buy Max");
        maxBuyBtn.getStyleClass().add("trade-max-buy-button");
        Button maxSellBtn = new Button("\u25bc  Sell Max");
        maxSellBtn.getStyleClass().add("trade-max-sell-button");

        final boolean[] syncingFields = {false};
        final boolean[] amountTracksSell = {false};

        final Runnable normalizeQtyAndAmount = () -> {
            BigDecimal unitCostWithFee = stock.getSalesPrice().multiply(new BigDecimal("1.005"));

            int qty;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
            } catch (NumberFormatException ex) {
                qty = 1;
            }
            if (qty < 0) {
                qty = 0;
            }

            syncingFields[0] = true;
            qtyField.setText(String.valueOf(qty));
            amountField.setText(unitCostWithFee.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP).toPlainString());
            syncingFields[0] = false;
        };

        decBtn.setOnAction(e -> {
            amountTracksSell[0] = false;
            try {
                int v = Math.max(0, Integer.parseInt(qtyField.getText().trim()) - 1);
                qtyField.setText(String.valueOf(v));
            } catch (NumberFormatException ignored) {
                qtyField.setText("1");
            }
            normalizeQtyAndAmount.run();
        });
        incBtn.setOnAction(e -> {
            amountTracksSell[0] = false;
            try {
                qtyField.setText(String.valueOf(Integer.parseInt(qtyField.getText().trim()) + 1));
            } catch (NumberFormatException ignored) {
                qtyField.setText("1");
            }
            normalizeQtyAndAmount.run();
        });

        HBox stepper = new HBox(0, decBtn, qtyField, incBtn);
        stepper.getStyleClass().add("trade-qty-stepper");
        stepper.setAlignment(Pos.CENTER);

        // ── Live cost / proceeds labels ────────────────────────────────────────
        Label buyAmountLbl  = new Label("\u2014");
        buyAmountLbl.getStyleClass().add("trade-button-amount");
        Label sellAmountLbl = new Label("\u2014");
        sellAmountLbl.getStyleClass().add("trade-button-amount");

        // Both labels track the qty field
        final Runnable updateBuyAmount = () -> {
            int qty;
            try { qty = Integer.parseInt(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { buyAmountLbl.setText("\u2014"); return; }
            if (qty < 1) {
                buyAmountLbl.setText("\u2014");
                return;
            }
            buyAmountLbl.setText(fmt(stock.getSalesPrice().multiply(BigDecimal.valueOf(qty))
                    .multiply(new BigDecimal("1.005"))));
        };
        final Runnable updateSellAmount = () -> {
            int qty;
            try { qty = Integer.parseInt(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { sellAmountLbl.setText("\u2014"); return; }
            if (qty < 1) {
                sellAmountLbl.setText("\u2014");
                return;
            }
            BigDecimal[] p = previewSell(player, stock, BigDecimal.valueOf(qty));
            sellAmountLbl.setText(p == null ? "\u2014" : fmt(p[3]));
        };
        final Runnable applyMaxForBuy = () -> {
            BigDecimal unitCostWithFee = stock.getSalesPrice().multiply(new BigDecimal("1.005"));
            int maxBuyQty = player.getMoney().divide(unitCostWithFee, 0, RoundingMode.DOWN).intValue();
            maxBuyQty = Math.max(0, maxBuyQty);
            amountTracksSell[0] = false;
            syncingFields[0] = true;
                qtyField.setText(String.valueOf(maxBuyQty));
                amountField.setText(unitCostWithFee.multiply(BigDecimal.valueOf(maxBuyQty))
                    .setScale(2, RoundingMode.HALF_UP).toPlainString());
            syncingFields[0] = false;
            updateBuyAmount.run();
            updateSellAmount.run();
        };
        final Runnable applyMaxForSell = () -> {
            BigDecimal totalOwned = player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            int maxSellQty = totalOwned.setScale(0, RoundingMode.DOWN).intValue();
            maxSellQty = Math.max(0, maxSellQty);
            amountTracksSell[0] = true;
            syncingFields[0] = true;
                qtyField.setText(String.valueOf(maxSellQty));
                BigDecimal[] sellPreview = previewSell(player, stock, BigDecimal.valueOf(maxSellQty));
                amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview[3])
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
            syncingFields[0] = false;
            updateBuyAmount.run();
            updateSellAmount.run();
        };

        maxBuyBtn.setOnAction(e -> applyMaxForBuy.run());
        maxSellBtn.setOnAction(e -> applyMaxForSell.run());

        updateBuyAmount.run();
        updateSellAmount.run();
        qtyField.textProperty().addListener((obs, old, val) -> {
            if (syncingFields[0]) {
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(val.trim());
            } catch (NumberFormatException ex) {
                qty = 1;
            }
            qty = Math.max(0, qty);
            syncingFields[0] = true;
            qtyField.setText(String.valueOf(qty));
            if (amountTracksSell[0]) {
                BigDecimal[] sellPreview = previewSell(player, stock, BigDecimal.valueOf(qty));
                amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview[3])
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
            } else {
                amountField.setText(stock.getSalesPrice().multiply(new BigDecimal("1.005")).multiply(BigDecimal.valueOf(qty))
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
                amount = new BigDecimal(val.trim());
            } catch (NumberFormatException ex) {
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                syncingFields[0] = true;
                qtyField.setText("0");
                syncingFields[0] = false;
                updateBuyAmount.run();
                updateSellAmount.run();
                return;
            }
            BigDecimal unitCostWithFee = stock.getSalesPrice().multiply(new BigDecimal("1.005"));
            int qty = amount.divide(unitCostWithFee, 0, RoundingMode.DOWN).intValue();
            qty = Math.max(0, qty);
            syncingFields[0] = true;
            qtyField.setText(String.valueOf(qty));
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
            int parsedQty;
            try { parsedQty = Integer.parseInt(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { showError(overlayRef[0], "Enter a valid quantity."); return; }

            if (parsedQty < 1) {
                showError(overlayRef[0], "Quantity must be at least 1 whole share.");
                return;
            }

            BigDecimal qty   = BigDecimal.valueOf(parsedQty);
            BigDecimal gross = stock.getSalesPrice().multiply(qty);
            BigDecimal fee   = gross.multiply(new BigDecimal("0.005"));
            BigDecimal total = gross.add(fee);
            showTradeConfirm(overlayRef[0], "BUY", stock, qty, gross, fee, BigDecimal.ZERO, total, () -> {
                try {
                    Transaction tx = exchange.buy(stock.getSymbol(), qty, player);
                    tx.commit(player);
                    showReceipt(overlayRef[0], "BUY", stock, qty, total, fee, BigDecimal.ZERO, player.getMoney());
                    refreshRef[0].run();
                } catch (Exception ex) { showError(overlayRef[0], ex.getMessage()); }
            });
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
            int parsedQty;
            try { parsedQty = Integer.parseInt(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { showError(overlayRef[0], "Enter a valid quantity."); return; }

            if (parsedQty < 1) {
                showError(overlayRef[0], "Quantity must be at least 1 whole share.");
                return;
            }

            BigDecimal sellQty = BigDecimal.valueOf(parsedQty);
            BigDecimal totalOwned = player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalOwned.compareTo(BigDecimal.ZERO) == 0) {
                showError(overlayRef[0], "You don't own any shares of " + stock.getSymbol()); return;
            }
            if (sellQty.compareTo(totalOwned) > 0) {
                showError(overlayRef[0], "You only own " + totalOwned.stripTrailingZeros().toPlainString()
                        + " shares of " + stock.getSymbol()); return;
            }
            BigDecimal[] preview = previewSell(player, stock, sellQty);
            showTradeConfirm(overlayRef[0], "SELL", stock, sellQty,
                    preview[0], preview[1], preview[2], preview[3], () -> {
                try {
                    BigDecimal[] result = executeSell(player, exchange, stock, sellQty);
                    showReceipt(overlayRef[0], "SELL", stock, sellQty,
                            result[3], result[1], result[2], player.getMoney());
                    refreshRef[0].run();
                } catch (Exception ex) { showError(overlayRef[0], ex.getMessage()); }
            });
        });

        VBox selectorColumn = new VBox(6, stepper, amountField);
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

        Pane graphPlaceholder = buildPriceChart(stock, player);
        VBox.setVgrow(graphPlaceholder, Priority.ALWAYS);

        VBox header = new VBox(4, sym, comp, priceRow, hlRow, graphPlaceholder, tradePanel);
        header.getStyleClass().add("game-detail-header");

        area.getChildren().add(header);
        VBox.setVgrow(header, Priority.ALWAYS);
    }

    // ── Portfolio table ───────────────────────────────────────────────────────

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
        TableColumn<Share, String> qtyCol = col("Qty", c ->
                c.getQuantity().stripTrailingZeros().toPlainString(), 45, 65);
        TableColumn<Share, String> boughtCol = col("Bought", c ->
            fmt(c.getPurchasePrice().multiply(c.getQuantity())), 85, 110);
        TableColumn<Share, String> nowCol = col("Now", c ->
            fmt(c.getStock().getSalesPrice().multiply(c.getQuantity())), 85, 110);

        TableColumn<Share, String> plCol = new TableColumn<>("P&L");
        plCol.setMinWidth(90);
        plCol.setMaxWidth(120);
        plCol.setCellValueFactory(c -> {
            Share sh = c.getValue();
            BigDecimal pl = sh.getStock().getSalesPrice().subtract(sh.getPurchasePrice())
                    .multiply(sh.getQuantity());
            String sign = pl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return new SimpleStringProperty(sign + fmt(pl));
        });
        plCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                boolean up = item.startsWith("+");
                setStyle(up ? "-fx-text-fill: #4ecb71;" : "-fx-text-fill: #e05a5a;");
            }
        });

        table.getColumns().add(symCol);
        table.getColumns().add(qtyCol);
        table.getColumns().add(boughtCol);
        table.getColumns().add(nowCol);
        table.getColumns().add(plCol);
        return table;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static TableColumn<Share, String> col(String title,
            java.util.function.Function<Share, String> fn, double min, double max) {
        TableColumn<Share, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new SimpleStringProperty(fn.apply(cell.getValue())));
        c.setMinWidth(min); c.setMaxWidth(max);
        return c;
    }

    private static Node statPill(String key, Label valueLabel) {
        Label keyLbl = new Label(key);
        keyLbl.getStyleClass().add("stat-pill-key");
        VBox box = new VBox(1, keyLbl, valueLabel);
        box.getStyleClass().add("stat-pill");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Label labelSmall(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("game-stat-key");
        return l;
    }

    private static BigDecimal pctChange(Stock stock) {
        java.util.List<BigDecimal> prices = stock.getHistoricalPrices();
        if (prices.size() < 2) return BigDecimal.ZERO;
        BigDecimal prev    = prices.get(prices.size() - 2);
        BigDecimal current = prices.get(prices.size() - 1);
        if (prev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }

    /** Compound return from {@code weeks} price points ago to now (−1 = all-time). */
    private static BigDecimal compoundReturn(Stock stock, int weeks) {
        java.util.List<BigDecimal> prices = stock.getHistoricalPrices();
        if (prices.size() < 2) return BigDecimal.ZERO;
        int fromIdx = (weeks < 0) ? 0 : Math.max(0, prices.size() - 1 - weeks);
        BigDecimal from = prices.get(fromIdx);
        BigDecimal to   = prices.get(prices.size() - 1);
        if (from.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return to.subtract(from).divide(from, 6, RoundingMode.HALF_UP)
                 .multiply(BigDecimal.valueOf(100));
    }

    private static String fmt(BigDecimal value) {
        return String.format("$%,.2f", value.doubleValue());
    }

    /** Walk lots without mutating state; returns null if qty > owned. [gross, fee, tax, proceeds] */
    private static BigDecimal[] previewSell(Player player, Stock stock, BigDecimal qtyToSell) {
        BigDecimal rem = qtyToSell;
        BigDecimal tGross = BigDecimal.ZERO, tFee = BigDecimal.ZERO, tTax = BigDecimal.ZERO;
        for (Share lot : player.getPortfolio().getShareBySymbol(stock.getSymbol())) {
            if (rem.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal sq = rem.min(lot.getQuantity());
            BigDecimal g  = stock.getSalesPrice().multiply(sq);
            BigDecimal f  = g.multiply(new BigDecimal("0.01"));
            BigDecimal p  = g.subtract(f).subtract(lot.getPurchasePrice().multiply(sq));
            BigDecimal t  = p.max(BigDecimal.ZERO).multiply(new BigDecimal("0.3"));
            tGross = tGross.add(g); tFee = tFee.add(f); tTax = tTax.add(t);
            rem = rem.subtract(sq);
        }
        if (rem.compareTo(BigDecimal.ZERO) > 0) return null;
        return new BigDecimal[]{tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax)};
    }

    /** Sell `qtyToSell` shares lot-by-lot (partial lots are split). Returns [gross, fee, tax, proceeds]. */
    private static BigDecimal[] executeSell(Player player, Exchange exchange, Stock stock, BigDecimal qtyToSell) {
        BigDecimal rem = qtyToSell;
        BigDecimal tGross = BigDecimal.ZERO, tFee = BigDecimal.ZERO, tTax = BigDecimal.ZERO;
        for (Share lot : new ArrayList<>(player.getPortfolio().getShareBySymbol(stock.getSymbol()))) {
            if (rem.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal sq = rem.min(lot.getQuantity());
            Share sellShare;
            if (sq.compareTo(lot.getQuantity()) < 0) {
                // Partial lot: split into sell portion + leftover
                BigDecimal leftover = lot.getQuantity().subtract(sq);
                player.getPortfolio().removeShare(lot);
                sellShare = new Share(stock, sq, lot.getPurchasePrice());
                player.getPortfolio().addShare(sellShare);
                player.getPortfolio().addShare(new Share(stock, leftover, lot.getPurchasePrice()));
            } else {
                sellShare = lot;
            }
            Transaction tx = exchange.sell(sellShare, player);
            tx.commit(player);
            BigDecimal g = stock.getSalesPrice().multiply(sq);
            BigDecimal f = g.multiply(new BigDecimal("0.01"));
            BigDecimal p = g.subtract(f).subtract(lot.getPurchasePrice().multiply(sq));
            BigDecimal t = p.max(BigDecimal.ZERO).multiply(new BigDecimal("0.3"));
            tGross = tGross.add(g); tFee = tFee.add(f); tTax = tTax.add(t);
            rem = rem.subtract(sq);
        }
        return new BigDecimal[]{tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax)};
    }

    private static void showTradeConfirm(StackPane overlay, String action, Stock stock, BigDecimal qty,
            BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total, Runnable onConfirm) {
        boolean isBuy = action != null && action.startsWith("BUY");

        Label iconLbl  = new Label(isBuy ? "\u2197" : "\u2198");
        iconLbl.getStyleClass().add(isBuy ? "dialog-action-icon-buy" : "dialog-action-icon-sell");
        Label titleLbl = new Label("ORDER SUMMARY");
        titleLbl.getStyleClass().add("dialog-title");
        HBox header = new HBox(10, iconLbl, titleLbl);
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox(0,
            dialogRow("Action",   action,                                   isBuy ? "dialog-val-buy" : "dialog-val-sell"),
            dialogRow("Symbol",   stock.getSymbol(),                        null),
            dialogRow("Company",  stock.getCompany(),                       null),
            dialogRow("Qty",      qty.stripTrailingZeros().toPlainString(), null),
            dialogRow("Price",    fmt(stock.getSalesPrice()),               null),
            dialogRow("Subtotal", fmt(gross),                              null),
            dialogRow(isBuy ? "Fee (0.5%)" : "Fee (1%)", fmt(fee),        "dialog-val-fee"),
            dialogRow("Tax",      fmt(tax),                                "dialog-val-fee")
        );
        rows.getStyleClass().add("dialog-rows");

        Label totalKey = new Label(isBuy ? "TOTAL COST" : "YOU RECEIVE");
        totalKey.getStyleClass().add("dialog-total-key");
        Label totalVal = new Label(fmt(total));
        totalVal.getStyleClass().add(isBuy ? "dialog-total-val-buy" : "dialog-total-val-sell");
        VBox totalSection = new VBox(4, totalKey, totalVal);
        totalSection.getStyleClass().add("dialog-total-section");

        Button cancelBtn  = new Button("Cancel");
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

        Runnable dismiss = () -> overlay.getChildren().remove(popup);
        cancelBtn.setOnAction(ev  -> dismiss.run());
        confirmBtn.setOnAction(ev -> { dismiss.run(); onConfirm.run(); });
        backdrop.setOnMouseClicked(ev -> dismiss.run());

        overlay.getChildren().add(popup);
    }

    private static void showReceipt(StackPane overlay, String action, Stock stock, BigDecimal qty,
            BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash) {
        boolean isBuy = action != null && action.startsWith("BUY");

        Label checkLbl = new Label("\u2713");
        checkLbl.getStyleClass().add("receipt-check");
        Label titleLbl = new Label("ORDER COMPLETE");
        titleLbl.getStyleClass().add("dialog-title");
        HBox header = new HBox(10, checkLbl, titleLbl);
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox(0,
            dialogRow("Action",      action,                                   isBuy ? "dialog-val-buy" : "dialog-val-sell"),
            dialogRow("Symbol",      stock.getSymbol(),                        null),
            dialogRow("Qty",         qty.stripTrailingZeros().toPlainString(), null),
            dialogRow("Price",       fmt(stock.getSalesPrice()),               null),
            dialogRow(isBuy ? "Fee (0.5%)" : "Fee (1%)", fmt(fee),           "dialog-val-fee"),
            dialogRow("Tax",         fmt(tax),                                "dialog-val-fee"),
            dialogRow(isBuy ? "Total Paid" : "Received", fmt(total),         null),
            dialogRow("New Balance", fmt(newCash),                            "dialog-val-cash")
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

        Runnable dismiss = () -> overlay.getChildren().remove(popup);
        doneBtn.setOnAction(ev -> dismiss.run());
        backdrop.setOnMouseClicked(ev -> dismiss.run());

        overlay.getChildren().add(popup);
    }

    private static HBox dialogRow(String key, String val, String valStyle) {
        Label kLbl = new Label(key);
        kLbl.getStyleClass().add("dialog-row-key");
        Label vLbl = new Label(val);
        vLbl.getStyleClass().add("dialog-row-val");
        if (valStyle != null) vLbl.getStyleClass().add(valStyle);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(8, kLbl, sp, vLbl);
        row.getStyleClass().add("dialog-row");
        return row;
    }

    private static BigDecimal[] previewSellAll(Player player) {
        BigDecimal tGross = BigDecimal.ZERO;
        BigDecimal tFee = BigDecimal.ZERO;
        BigDecimal tTax = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (Share lot : player.getPortfolio().getShares()) {
            BigDecimal qty = lot.getQuantity();
            BigDecimal gross = lot.getStock().getSalesPrice().multiply(qty);
            BigDecimal fee = gross.multiply(new BigDecimal("0.01"));
            BigDecimal profit = gross.subtract(fee).subtract(lot.getPurchasePrice().multiply(qty));
            BigDecimal tax = profit.max(BigDecimal.ZERO).multiply(new BigDecimal("0.3"));

            tGross = tGross.add(gross);
            tFee = tFee.add(fee);
            tTax = tTax.add(tax);
            totalQty = totalQty.add(qty);
        }

        return new BigDecimal[]{tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax), totalQty};
    }

    private static BigDecimal[] executeSellAll(Player player, Exchange exchange) {
        Map<String, BigDecimal> qtyBySymbol = new LinkedHashMap<>();
        Map<String, Stock> stockBySymbol = new LinkedHashMap<>();

        for (Share share : new ArrayList<>(player.getPortfolio().getShares())) {
            String symbol = share.getStock().getSymbol();
            qtyBySymbol.merge(symbol, share.getQuantity(), BigDecimal::add);
            stockBySymbol.putIfAbsent(symbol, share.getStock());
        }

        BigDecimal tGross = BigDecimal.ZERO;
        BigDecimal tFee = BigDecimal.ZERO;
        BigDecimal tTax = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : qtyBySymbol.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal qty = entry.getValue();
            Stock stock = stockBySymbol.get(symbol);
            BigDecimal[] result = executeSell(player, exchange, stock, qty);
            tGross = tGross.add(result[0]);
            tFee = tFee.add(result[1]);
            tTax = tTax.add(result[2]);
            totalQty = totalQty.add(qty);
        }

        return new BigDecimal[]{tGross, tFee, tTax, tGross.subtract(tFee).subtract(tTax), totalQty};
    }

    private static void showBulkTradeConfirm(StackPane overlay, String action, BigDecimal qty,
            BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total, Runnable onConfirm) {
        Label iconLbl  = new Label("\u2198");
        iconLbl.getStyleClass().add("dialog-action-icon-sell");
        Label titleLbl = new Label("ORDER SUMMARY");
        titleLbl.getStyleClass().add("dialog-title");
        HBox header = new HBox(10, iconLbl, titleLbl);
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox(0,
            dialogRow("Action", action, "dialog-val-sell"),
            dialogRow("Symbols", "ALL", null),
            dialogRow("Qty", qty.stripTrailingZeros().toPlainString(), null),
            dialogRow("Subtotal", fmt(gross), null),
            dialogRow("Fee (1%)", fmt(fee), "dialog-val-fee"),
            dialogRow("Tax", fmt(tax), "dialog-val-fee")
        );
        rows.getStyleClass().add("dialog-rows");

        Label totalKey = new Label("YOU RECEIVE");
        totalKey.getStyleClass().add("dialog-total-key");
        Label totalVal = new Label(fmt(total));
        totalVal.getStyleClass().add("dialog-total-val-sell");
        VBox totalSection = new VBox(4, totalKey, totalVal);
        totalSection.getStyleClass().add("dialog-total-section");

        Button cancelBtn  = new Button("Cancel");
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

        Runnable dismiss = () -> overlay.getChildren().remove(popup);
        cancelBtn.setOnAction(ev  -> dismiss.run());
        confirmBtn.setOnAction(ev -> { dismiss.run(); onConfirm.run(); });
        backdrop.setOnMouseClicked(ev -> dismiss.run());

        overlay.getChildren().add(popup);
    }

    private static void showBulkReceipt(StackPane overlay, String action, BigDecimal qty,
            BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash) {
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
            dialogRow("Qty", qty.stripTrailingZeros().toPlainString(), null),
            dialogRow("Fee (1%)", fmt(fee), "dialog-val-fee"),
            dialogRow("Tax", fmt(tax), "dialog-val-fee"),
            dialogRow("Received", fmt(total), null),
            dialogRow("New Balance", fmt(newCash), "dialog-val-cash")
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

        Runnable dismiss = () -> overlay.getChildren().remove(popup);
        doneBtn.setOnAction(ev -> dismiss.run());
        backdrop.setOnMouseClicked(ev -> dismiss.run());

        overlay.getChildren().add(popup);
    }

    private static void showTransactionHistory(StackPane overlay, Player player, Node background) {
        record TxRow(int week, boolean isBuy, String symbol, String company,
                     BigDecimal qty, BigDecimal pricePerShare,
                     BigDecimal fee, BigDecimal tax, BigDecimal total) {}

        List<TxRow> allTx = new ArrayList<>();
        for (Purchase p : player.getTransactionArchive().getAllPurchases()) {
            allTx.add(new TxRow(
                    p.getWeek(), true,
                    p.getShare().getStock().getSymbol(),
                    p.getShare().getStock().getCompany(),
                    p.getShare().getQuantity(),
                    p.getShare().getPurchasePrice(),
                    p.getCalculator().calculateCommission(),
                    BigDecimal.ZERO,
                    p.getCalculator().calculateTotal()
            ));
        }
        for (Sale s : player.getTransactionArchive().getAllSales()) {
            BigDecimal qty   = s.getShare().getQuantity();
            BigDecimal gross = s.getCalculator().calculateGross();
            BigDecimal pricePerShare = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : gross.divide(qty, 4, RoundingMode.HALF_UP);
            allTx.add(new TxRow(
                    s.getWeek(), false,
                    s.getShare().getStock().getSymbol(),
                    s.getShare().getStock().getCompany(),
                    qty,
                    pricePerShare,
                    s.getCalculator().calculateCommission(),
                    s.getCalculator().calculateTax(),
                    s.getCalculator().calculateTotal()
            ));
        }
        allTx.sort((a, b) -> b.week() - a.week());

        ObservableList<TxRow> txItems = FXCollections.observableArrayList(allTx);
        FilteredList<TxRow>   filteredTx = new FilteredList<>(txItems, t -> true);

        String[]    txFilterRef  = {"ALL"};
        Runnable[]  applyTxFilter = {null};

        TextField txSearch = new TextField();
        txSearch.setPromptText("\uD83D\uDD0D  Search by symbol\u2026");
        txSearch.getStyleClass().add("game-search-field");

        // ── Type filter chips ─────────────────────────────────────────────────
        String[] txChipKeys   = {"ALL", "BUY", "SELL"};
        String[] txChipLabels = {"All",  "Buy",  "Sell"};
        HBox txFilterRow = new HBox(6);
        txFilterRow.getStyleClass().add("stock-filter-row");

        applyTxFilter[0] = () -> {
            String lower = txSearch.getText() == null ? "" : txSearch.getText().trim().toLowerCase();
            filteredTx.setPredicate(t -> {
                boolean textMatch = lower.isEmpty()
                        || t.symbol().toLowerCase().contains(lower)
                        || t.company().toLowerCase().contains(lower);
                boolean typeMatch = switch (txFilterRef[0]) {
                    case "BUY"  -> t.isBuy();
                    case "SELL" -> !t.isBuy();
                    default     -> true;
                };
                return textMatch && typeMatch;
            });
        };
        txSearch.textProperty().addListener((obs, old, val) -> applyTxFilter[0].run());

        for (int i = 0; i < txChipLabels.length; i++) {
            final int idx = i;
            Button chip = new Button(txChipLabels[i]);
            chip.getStyleClass().add("stock-filter-chip");
            if (i == 0) chip.getStyleClass().add("stock-filter-chip-active");
            chip.setOnAction(ev -> {
                txFilterRef[0] = txChipKeys[idx];
                for (Node n : txFilterRow.getChildren()) n.getStyleClass().remove("stock-filter-chip-active");
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

        TableColumn<TxRow, String> weekCol = new TableColumn<>("Wk");
        weekCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().week())));
        weekCol.setComparator(java.util.Comparator.comparingInt(Integer::parseInt));
        weekCol.setMinWidth(34); weekCol.setPrefWidth(34);

        TableColumn<TxRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().isBuy() ? "BUY" : "SELL"));
        typeCol.setMinWidth(46); typeCol.setPrefWidth(46);
        typeCol.setComparator(String::compareTo);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().removeAll("tx-type-buy", "tx-type-sell");
                getStyleClass().add("BUY".equals(item) ? "tx-type-buy" : "tx-type-sell");
            }
        });

        TableColumn<TxRow, String> symCol = new TableColumn<>("Symbol");
        symCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().symbol()));
        symCol.setMinWidth(64); symCol.setPrefWidth(72);

        TableColumn<TxRow, String> compCol = new TableColumn<>("Company");
        compCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().company()));
        compCol.setMinWidth(120); compCol.setPrefWidth(160);

        TableColumn<TxRow, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().qty().stripTrailingZeros().toPlainString()));
        qtyCol.setMinWidth(50); qtyCol.setPrefWidth(60);

        TableColumn<TxRow, String> priceCol = new TableColumn<>("Price/sh");
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(fmt(cd.getValue().pricePerShare())));
        priceCol.setMinWidth(70); priceCol.setPrefWidth(80);

        TableColumn<TxRow, String> feeCol = new TableColumn<>("Fee");
        feeCol.setCellValueFactory(cd -> new SimpleStringProperty(fmt(cd.getValue().fee())));
        feeCol.setMinWidth(60); feeCol.setPrefWidth(70);

        TableColumn<TxRow, String> taxCol = new TableColumn<>("Tax");
        taxCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().tax().compareTo(BigDecimal.ZERO) == 0 ? "\u2014" : fmt(cd.getValue().tax())));
        taxCol.setMinWidth(60); taxCol.setPrefWidth(70);

        TableColumn<TxRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cd -> new SimpleStringProperty(fmt(cd.getValue().total())));
        totalCol.setMinWidth(80); totalCol.setPrefWidth(90);

        table.getColumns().addAll(weekCol, typeCol, symCol, compCol, qtyCol, priceCol, feeCol, taxCol, totalCol);
        // Size table to fit its rows (28px per row + 30px header), capped at 12 rows
        double rowH = 28;
        double headerH = 30;
        double tableH = headerH + Math.min(filteredTx.size(), 12) * rowH;
        table.setPrefHeight(tableH);
        table.setMinHeight(headerH + rowH);  // at least one row visible
        // Grow the table when filter makes more rows visible
        filteredTx.addListener((javafx.collections.ListChangeListener<TxRow>) c -> {
            double h = headerH + Math.min(filteredTx.size(), 12) * rowH;
            table.setPrefHeight(h);
        });
        Label emptyLbl = new Label("No transactions yet.");
        emptyLbl.getStyleClass().add("market-movers-col-title");
        table.setPlaceholder(emptyLbl);

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

        HBox controlsRow = new HBox(10, txSearch, txFilterRow);
        controlsRow.getStyleClass().add("history-controls-row");
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txSearch, Priority.ALWAYS);

        VBox card = new VBox(0, titleRow, controlsRow, table);
        card.getStyleClass().add("history-card");
        card.setMaxWidth(920);
        card.setMaxHeight(580);
        card.setOpacity(0);

        GaussianBlur blur = new GaussianBlur(0);
        background.setEffect(blur);

        Region dimBackdrop = new Region();
        dimBackdrop.getStyleClass().add("market-movers-backdrop");
        dimBackdrop.setOpacity(0);

        StackPane popup = new StackPane(dimBackdrop, card);
        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(popup);

        Timeline blurIn = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(blur.radiusProperty(), 0)),
            new KeyFrame(Duration.millis(300),  new KeyValue(blur.radiusProperty(), 8, Interpolator.EASE_OUT))
        );
        FadeTransition dimIn  = new FadeTransition(Duration.millis(300), dimBackdrop);
        dimIn.setFromValue(0); dimIn.setToValue(1);
        FadeTransition cardIn = new FadeTransition(Duration.millis(220), card);
        cardIn.setFromValue(0); cardIn.setToValue(1);
        cardIn.setDelay(Duration.millis(80));
        blurIn.play(); dimIn.play(); cardIn.play();

        Runnable dismiss = () -> {
            Timeline blurOut = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(blur.radiusProperty(), 8)),
                new KeyFrame(Duration.millis(250),  new KeyValue(blur.radiusProperty(), 0, Interpolator.EASE_IN))
            );
            FadeTransition dimOut  = new FadeTransition(Duration.millis(250), dimBackdrop);
            dimOut.setFromValue(1); dimOut.setToValue(0);
            FadeTransition cardOut = new FadeTransition(Duration.millis(180), card);
            cardOut.setFromValue(1); cardOut.setToValue(0);
            blurOut.play(); dimOut.play(); cardOut.play();
            blurOut.setOnFinished(ev -> {
                overlay.getChildren().remove(popup);
                background.setEffect(null);
            });
        };
        closeBtn.setOnAction(ev -> dismiss.run());
        dimBackdrop.setOnMouseClicked(ev -> dismiss.run());
    }

    private static void showMarketMovers(StackPane overlay, Exchange exchange, Node background, Consumer<Stock> onSelectStock) {
        GaussianBlur blur = new GaussianBlur(0);
        background.setEffect(blur);

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
        Button tab1w  = new Button("1W");
        Button tab4w  = new Button("4W");
        Button tabAll = new Button("All");
        for (Button t : new Button[]{tab1w, tab4w, tabAll}) t.getStyleClass().add("movers-tab");
        tab1w.getStyleClass().add("movers-tab-active");
        HBox tabBar = new HBox(4, tab1w, tab4w, tabAll);
        tabBar.getStyleClass().add("movers-tab-bar");

        Runnable[] dismissRef = {null};
        HBox columns = new HBox(0);
        columns.getStyleClass().add("market-movers-columns");

        Runnable[] rebuildRef = {null};
        rebuildRef[0] = () -> {
            int weeks = switch (tabRef[0]) {
                case "4W"  -> 4;
                case "All" -> -1;
                default    -> 1;
            };
            List<Stock> all = exchange.getStocks();
            List<Stock> gainers = all.stream()
                    .sorted((a, b) -> compoundReturn(b, weeks).compareTo(compoundReturn(a, weeks)))
                    .limit(10).toList();
            List<Stock> losers = all.stream()
                    .sorted((a, b) -> compoundReturn(a, weeks).compareTo(compoundReturn(b, weeks)))
                    .limit(10).toList();
            VBox gainersCol = buildMoversColumn("\u25B2  TOP GAINERS", gainers, true, weeks, dismissRef, onSelectStock);
            VBox losersCol  = buildMoversColumn("\u25BC  TOP LOSERS",  losers,  false, weeks, dismissRef, onSelectStock);
            HBox.setHgrow(gainersCol, Priority.ALWAYS);
            HBox.setHgrow(losersCol,  Priority.ALWAYS);
            columns.getChildren().setAll(gainersCol, losersCol);
        };
        rebuildRef[0].run();

        tab1w.setOnAction(ev -> {
            tabRef[0] = "1W";
            tab1w.getStyleClass().add("movers-tab-active");
            tab4w.getStyleClass().remove("movers-tab-active");
            tabAll.getStyleClass().remove("movers-tab-active");
            rebuildRef[0].run();
        });
        tab4w.setOnAction(ev -> {
            tabRef[0] = "4W";
            tab4w.getStyleClass().add("movers-tab-active");
            tab1w.getStyleClass().remove("movers-tab-active");
            tabAll.getStyleClass().remove("movers-tab-active");
            rebuildRef[0].run();
        });
        tabAll.setOnAction(ev -> {
            tabRef[0] = "All";
            tabAll.getStyleClass().add("movers-tab-active");
            tab1w.getStyleClass().remove("movers-tab-active");
            tab4w.getStyleClass().remove("movers-tab-active");
            rebuildRef[0].run();
        });

        VBox card = new VBox(0, titleRow, tabBar, columns);
        card.getStyleClass().add("market-movers-card");
        card.setMaxWidth(720);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setOpacity(0);

        StackPane popup = new StackPane(dimBackdrop, card);
        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(popup);

        Timeline blurIn = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(blur.radiusProperty(), 0)),
            new KeyFrame(Duration.millis(300),  new KeyValue(blur.radiusProperty(), 8, Interpolator.EASE_OUT))
        );
        FadeTransition dimIn  = new FadeTransition(Duration.millis(300), dimBackdrop);
        dimIn.setFromValue(0); dimIn.setToValue(1);
        FadeTransition cardIn = new FadeTransition(Duration.millis(220), card);
        cardIn.setFromValue(0); cardIn.setToValue(1);
        cardIn.setDelay(Duration.millis(80));
        blurIn.play(); dimIn.play(); cardIn.play();

        Runnable dismiss = () -> {
            Timeline blurOut = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(blur.radiusProperty(), 8)),
                new KeyFrame(Duration.millis(250),  new KeyValue(blur.radiusProperty(), 0, Interpolator.EASE_IN))
            );
            FadeTransition dimOut  = new FadeTransition(Duration.millis(250), dimBackdrop);
            dimOut.setFromValue(1); dimOut.setToValue(0);
            FadeTransition cardOut = new FadeTransition(Duration.millis(180), card);
            cardOut.setFromValue(1); cardOut.setToValue(0);
            blurOut.play(); dimOut.play(); cardOut.play();
            blurOut.setOnFinished(ev -> {
                overlay.getChildren().remove(popup);
                background.setEffect(null);
            });
        };
        dismissRef[0] = dismiss;

        closeBtn.setOnAction(ev -> dismiss.run());
        dimBackdrop.setOnMouseClicked(ev -> dismiss.run());
    }

    private static VBox buildMoversColumn(String title, List<Stock> stocks, boolean isGainers,
                                           int weeks, Runnable[] dismissRef, Consumer<Stock> onSelectStock) {
        Label colTitle = new Label(title);
        colTitle.getStyleClass().add("market-movers-col-title");
        colTitle.getStyleClass().add(isGainers ? "market-movers-col-title-gainers" : "market-movers-col-title-losers");

        VBox rows = new VBox(0);
        for (int i = 0; i < stocks.size(); i++) {
            Stock s = stocks.get(i);
            BigDecimal pct = compoundReturn(s, weeks);
            String sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

            Label rankLbl = new Label("#" + (i + 1));
            rankLbl.getStyleClass().add("movers-rank");
            if (i == 0)      rankLbl.getStyleClass().add("movers-rank-gold");
            else if (i == 1) rankLbl.getStyleClass().add("movers-rank-silver");
            else if (i == 2) rankLbl.getStyleClass().add("movers-rank-bronze");

            Label symLbl = new Label(s.getSymbol());
            symLbl.getStyleClass().add("movers-symbol");
            Label compLbl = new Label(s.getCompany());
            compLbl.getStyleClass().add("movers-company");
            VBox textBox  = new VBox(1, symLbl, compLbl);

            Label priceLbl = new Label(fmt(s.getSalesPrice()));
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

            final Stock stockRef = s;
            row.setOnMouseClicked(ev -> {
                if (dismissRef[0] != null) dismissRef[0].run();
                onSelectStock.accept(stockRef);
            });

            rows.getChildren().add(row);
        }

        VBox col = new VBox(8, colTitle, rows);
        col.getStyleClass().add("market-movers-col");
        col.getStyleClass().add(isGainers ? "market-movers-col-gainers" : "market-movers-col-losers");
        return col;
    }

    private static Pane buildPriceChart(Stock stock, Player player) {
        Canvas canvas = new Canvas();
        Pane pane = new Pane(canvas);
        pane.getStyleClass().add("price-chart-placeholder");

        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());

        List<BigDecimal> prices = stock.getHistoricalPrices();

        record TradeDot(int week, BigDecimal qty, BigDecimal price, boolean isSell) {}

        String sym = stock.getSymbol();
        List<TradeDot> tradeDots = new ArrayList<>();
        player.getTransactionArchive().getAllPurchases().stream()
                .filter(p -> p.getShare().getStock().getSymbol().equals(sym))
                .map(p -> new TradeDot(p.getWeek(), p.getShare().getQuantity(), p.getShare().getPurchasePrice(), false))
                .forEach(tradeDots::add);
        player.getTransactionArchive().getAllSales().stream()
                .filter(s -> s.getShare().getStock().getSymbol().equals(sym))
                .map(s -> new TradeDot(s.getWeek(), s.getShare().getQuantity(), s.getShare().getPurchasePrice(), true))
                .forEach(tradeDots::add);

        // State for hover crosshair — rebuilt on each draw
        List<double[]> drawnDots = new ArrayList<>();
        double[][] xsRef   = {new double[0]};
        double[][] ysRef   = {new double[0]};
        int[]     hoverIdx = {-1};
        boolean[] onDot    = {false};

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
            if (w <= 0 || h <= 0) return;

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
            BigDecimal range  = maxVal.subtract(minVal);
            if (range.compareTo(BigDecimal.ZERO) == 0) range = BigDecimal.ONE;

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
            double[] xs = new double[n];
            double[] ys = new double[n];
            double innerH   = cH * 0.82;
            double innerOff = cH * 0.09;
            for (int i = 0; i < n; i++) {
                xs[i] = padL + (n == 1 ? 0 : (i / (double)(n - 1)) * cW);
                double norm = prices.get(i).subtract(minVal).divide(range, 6, RoundingMode.HALF_UP).doubleValue();
                ys[i] = padT + innerH + innerOff - (norm * innerH);
            }
            xsRef[0] = xs;
            ysRef[0] = ys;

            // Gradient fill under the line
            LinearGradient fillGrad = new LinearGradient(0, padT, 0, padT + cH, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(lineHex, 0.28)),
                    new Stop(1, Color.web(lineHex, 0.03)));
            gc.setFill(fillGrad);
            gc.beginPath();
            gc.moveTo(xs[0], padT + cH);
            gc.lineTo(xs[0], ys[0]);
            for (int i = 1; i < n; i++) gc.lineTo(xs[i], ys[i]);
            gc.lineTo(xs[n - 1], padT + cH);
            gc.closePath();
            gc.fill();

            // Price line
            gc.setStroke(Color.web(lineHex, 0.90));
            gc.setLineWidth(2);
            gc.beginPath();
            gc.moveTo(xs[0], ys[0]);
            for (int i = 1; i < n; i++) gc.lineTo(xs[i], ys[i]);
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
                String chipTxt = "Wk " + (hi + 1) + "  " + fmt(prices.get(hi));
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
                double tw    = chipTxt.length() * 6.0;
                double chipX = Math.min(cx + 8, w - tw - 12);
                double chipY = padT + 2;
                gc.setFill(Color.web("#060d20", 0.88));
                gc.fillRoundRect(chipX - 5, chipY - 3, tw + 10, 16, 6, 6);
                gc.setStroke(Color.web("#ffffff", 0.09));
                gc.setLineWidth(0.5);
                gc.strokeRoundRect(chipX - 5, chipY - 3, tw + 10, 16, 6, 6);
                gc.setFill(Color.web("#e8d8b0", 0.90));
                gc.fillText(chipTxt, chipX, chipY + 11);
            }

            // Trade dots — buys first (behind), then sells (on top)
            for (int pass = 0; pass < 2; pass++) {
                boolean drawingSells = (pass == 1);
                for (TradeDot dot : tradeDots) {
                    if (dot.isSell() != drawingSells) continue;
                    int idx = dot.week() - 1;
                    if (idx < 0 || idx >= n) continue;
                    double dotX = xs[idx], dotY = ys[idx];
                    if (dot.isSell()) {
                        gc.setFill(Color.web("#e05a5a", 0.28));
                        gc.fillOval(dotX - 7, dotY - 7, 14, 14);
                        gc.setFill(Color.web("#e05a5a"));
                        gc.fillOval(dotX - 4, dotY - 4, 8, 8);
                    } else {
                        gc.setFill(Color.web("#f5a201", 0.30));
                        gc.fillOval(dotX - 7, dotY - 7, 14, 14);
                        gc.setFill(Color.web("#f5a201"));
                        gc.fillOval(dotX - 4, dotY - 4, 8, 8);
                    }
                    drawnDots.add(new double[]{dotX, dotY, dot.week(), dot.qty().doubleValue(), dot.price().doubleValue(), dot.isSell() ? 1 : 0});
                }
            }

            // Last price dot + label (suppressed when crosshair is active)
            double lx = xs[n - 1], ly = ys[n - 1];
            gc.setFill(Color.web(lineHex));
            gc.fillOval(lx - 3.5, ly - 3.5, 7, 7);
            if (hi < 0 || onDot[0]) {
                String lastTxt = fmt(prices.get(n - 1));
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
                if (dx * dx + dy * dy <= 64) { hit = dot; break; }
            }
            if (hit != null) {
                onDot[0]    = true;
                hoverIdx[0] = -1;
                final double[] h = hit;
                boolean isSell = h[5] == 1;
                tooltip.getChildren().clear();
                Label weekLbl = new Label((isSell ? "Sold" : "Bought") + " · Week " + (int) h[2]);
                weekLbl.getStyleClass().add(isSell ? "chart-tooltip-sell-week" : "chart-tooltip-week");
                Label qtyLbl = new Label("Qty: " + BigDecimal.valueOf(h[3]).stripTrailingZeros().toPlainString());
                qtyLbl.getStyleClass().add("chart-tooltip-row");
                Label priceLbl = new Label("Price: " + fmt(BigDecimal.valueOf(h[4])));
                priceLbl.getStyleClass().add("chart-tooltip-row");
                tooltip.getChildren().addAll(weekLbl, qtyLbl, priceLbl);
                if (!isSell) {
                    BigDecimal gain = stock.getSalesPrice().subtract(BigDecimal.valueOf(h[4]))
                            .multiply(BigDecimal.valueOf(h[3]));
                    String sign = gain.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                    Label gainLbl = new Label("P&L: " + sign + fmt(gain));
                    gainLbl.getStyleClass().add(gain.compareTo(BigDecimal.ZERO) >= 0 ? "chart-tooltip-gain" : "chart-tooltip-loss");
                    tooltip.getChildren().add(gainLbl);
                }
                double tx = h[0] + 12, ty = h[1] - 70;
                if (ty < 4)                    ty = h[1] + 14;
                if (tx + 150 > pane.getWidth()) tx = h[0] - 155;
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
            onDot[0]    = false;
            hoverIdx[0] = -1;
            drawRef[0].run();
        });

        canvas.widthProperty().addListener((obs, o, nv) -> drawRef[0].run());
        canvas.heightProperty().addListener((obs, o, nv) -> drawRef[0].run());
        Platform.runLater(drawRef[0]);
        return pane;
    }
    private static void showError(StackPane overlay, String message) {
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

        Runnable dismiss = () -> overlay.getChildren().remove(popup);
        okBtn.setOnAction(ev -> dismiss.run());
        backdrop.setOnMouseClicked(ev -> dismiss.run());

        overlay.getChildren().add(popup);
    }
}

