package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.sun.prism.shader.DrawRoundRect_RadialGradient_REFLECT_AlphaTest_Loader;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.Purchase;
import edu.ntnu.idatt2003.g23.model.transaction.Sale;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import static edu.ntnu.idatt2003.g23.ui.util.LabelUtil.labelSmall;

import edu.ntnu.idatt2003.g23.ui.util.CurrencyFormatter;

public final class GameViewNew {
    private final GameController gameController;
    private final Label cashVal;
    private final Label portVal;
    private final Label nwVal;
    private final Label weekNumLbl;

    private final ObservableList<Stock> allStocks;
    private final FilteredList<Stock> filteredStocks;
    private final ObservableList<Share> portfolioItems;
    private final ObjectProperty<Stock> selectedStock;

    private final TextField searchField;
    private final VBox stockListBox;
    private final Set<String> favorites;
    private final Node[] selectedCardRef;
    private final Player player;
    private final Exchange exchange;
    private String stockFilter;
    private String stockSort;

    private final VBox detailArea;

    public GameViewNew(GameController controller, Player player, Exchange exchange) {
        StackPane overlayRef = null;
        Node rootRef = null;

        this.gameController = controller;
        this.cashVal = new Label();
        this.portVal = new Label();
        this.nwVal = new Label();
        this.weekNumLbl = new Label();

        this.player = player;
        this.exchange = exchange;

        this.allStocks = FXCollections.observableArrayList(exchange.getStocks());
        this.filteredStocks = new FilteredList<>(this.allStocks, s -> true);
        this.portfolioItems = FXCollections.observableArrayList(player.getPortfolio().getShares());
        this.selectedStock = new SimpleObjectProperty<>(this.allStocks.isEmpty() ? null : this.allStocks.get(0));

        this.favorites = new HashSet<>();
        this.stockFilter = "ALL";
        this.stockSort   = "NAME";
        this.selectedCardRef = new Node[]{null};

        // ── Stat pill labels ─────────────────────────────────────────────────
        cashVal.getStyleClass().add("stat-pill-value");
        portVal.getStyleClass().add("stat-pill-value");
        nwVal.getStyleClass().add("stat-pill-value");
        weekNumLbl.getStyleClass().add("week-number");

        // ── Detail panel (right) — rebuilt on stock selection ────────────────
        this.detailArea = new VBox();
        detailArea.getStyleClass().add("game-detail-area");
        VBox.setVgrow(detailArea, Priority.ALWAYS);

        // ── Stock list (left panel) ──────────────────────────────────────────
        this.stockListBox = new VBox(4);
        this.stockListBox.getStyleClass().add("game-stock-list");

        // ── Search field (declared early for closure access) ─────────────────
        this.searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D  Search stocks\u2026");
        searchField.getStyleClass().add("game-search-field");

        // Initial stock list population
        applyFilter();

        // Rebuild detail when selection changes
        selectedStock.addListener((obs, old, stock) -> {
            rebuildDetail();
        });

        // Show detail immediately for first stock
        if (selectedStock.get() != null) {
            rebuildDetail();
        }

        // ── Search field listener ──────────────────────────────────────────────
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());

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
                stockFilter = chipKeys[idx];
                for (Node n : filterChips.getChildren()) n.getStyleClass().remove("stock-filter-chip-active");
                chip.getStyleClass().add("stock-filter-chip-active");
                applyFilter();
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

    private void updateData() {
        weekNumLbl.setText(String.valueOf(exchange.getWeek()));
        cashVal.setText(CurrencyFormatter.format(player.getMoney()));
        portVal.setText(CurrencyFormatter.format(player.getPortfolio().getNetWorth()));
        nwVal.setText(CurrencyFormatter.format(player.getNetWorth()));
        portfolioItems.setAll(player.getPortfolio().getShares());
        applyFilter();
        rebuildDetail();
    }

    private void applyFilter() {
        String lower = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filteredStocks.setPredicate(s -> {
            boolean textMatch = lower.isEmpty()
                    || s.getSymbol().toLowerCase().contains(lower)
                    || s.getCompany().toLowerCase().contains(lower);
            boolean typeMatch = switch (stockFilter) {
                case "OWNED"     -> player.getPortfolio().getShareBySymbol(s.getSymbol())
                        .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(BigDecimal.ZERO) > 0;
                case "UP"        -> s.percentageChange().compareTo(BigDecimal.ZERO) > 0;
                case "DOWN"      -> s.percentageChange().compareTo(BigDecimal.ZERO) < 0;
                case "FAVORITES" -> favorites.contains(s.getSymbol());
                default          -> true;
            };
            return textMatch && typeMatch;
        });
        java.util.Comparator<Stock> sortCmp = switch (stockSort) {
            case "PRICE_ASC"  -> java.util.Comparator.comparing(Stock::getSalesPrice);
            case "PRICE_DESC" -> java.util.Comparator.comparing(Stock::getSalesPrice).reversed();
            case "CHG_ASC"    -> java.util.Comparator.comparing(Stock::percentageChange);
            case "CHG_DESC"   -> java.util.Comparator.comparing(Stock::percentageChange).reversed();
            case "NAME_DESC"  -> java.util.Comparator.comparing(Stock::getSymbol).reversed();
            default           -> java.util.Comparator.comparing(Stock::getSymbol);
        };
        rebuildStockList(stockListBox, filteredStocks, sortCmp, selectedStock, selectedCardRef, player, favorites, this::applyFilter);
    };

    private void rebuildDetail() {
        detailArea.getChildren().clear();
        Stock stock = selectedStock.get();
        if (stock == null) return;

        Label sym   = new Label(stock.getSymbol());  sym.getStyleClass().add("detail-symbol");
        Label comp  = new Label(stock.getCompany()); comp.getStyleClass().add("detail-company");
        Label price = new Label(CurrencyFormatter.format(stock.getSalesPrice())); price.getStyleClass().add("detail-price");

        BigDecimal pct  = stock.percentageChange();
        String     sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        Label pctBadge  = new Label(sign + pct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");
        pctBadge.getStyleClass().add(pct.compareTo(BigDecimal.ZERO) >= 0 ? "detail-badge-up" : "detail-badge-down");

        HBox priceRow = new HBox(12, price, pctBadge);
        priceRow.setAlignment(Pos.BASELINE_LEFT);

        // H/L stats
        BigDecimal hi = stock.allTimeHigh();
        BigDecimal lo = stock.allTimeLow();

        VBox hiBox = new VBox(2, labelSmall("ALL TIME HIGH"), new Label(CurrencyFormatter.format(hi)) {{ getStyleClass().add("stat-hl-value-up"); }});
        VBox loBox = new VBox(2, labelSmall("ALL TIME LOW"),  new Label(CurrencyFormatter.format(lo)) {{ getStyleClass().add("stat-hl-value-down"); }});
        Region hlSpacer = new Region(); HBox.setHgrow(hlSpacer, Priority.ALWAYS);
        HBox hlRow = new HBox(24, hiBox, loBox);

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
                quantity = Integer.parseInt(quantityField.getText().trim());
            } catch (NumberFormatException ex) {
                quantity = 1;
            }
            if (quantity < 0) {
                quantity = 0;
            }

            syncingFields[0] = true;
            quantityField.setText(String.valueOf(quantity));
            amountField.setText(unitCostWithFee.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP).toPlainString());
            syncingFields[0] = false;
        };

        decBtn.setOnAction(e -> {
            amountTracksSell[0] = false;
            try {
                int v = Math.max(0, Integer.parseInt(quantityField.getText().trim()) - 1);
                quantityField.setText(String.valueOf(v));
            } catch (NumberFormatException ignored) {
                quantityField.setText("1");
            }
            normalizequantityAndAmount.run();
        });
        incBtn.setOnAction(e -> {
            amountTracksSell[0] = false;
            try {
                quantityField.setText(String.valueOf(Integer.parseInt(quantityField.getText().trim()) + 1));
            } catch (NumberFormatException ignored) {
                quantityField.setText("1");
            }
            normalizequantityAndAmount.run();
        });

        HBox stepper = new HBox(0, decBtn, quantityField, incBtn);
        stepper.getStyleClass().add("trade-quantity-stepper");
        stepper.setAlignment(Pos.CENTER);

        // ── Live cost / proceeds labels ────────────────────────────────────────
        Label buyAmountLbl  = new Label("\u2014");
        buyAmountLbl.getStyleClass().add("trade-button-amount");
        Label sellAmountLbl = new Label("\u2014");
        sellAmountLbl.getStyleClass().add("trade-button-amount");

        // Both labels track the quantity field
        final Runnable updateBuyAmount = () -> {
            int quantity;
            try { quantity = Integer.parseInt(quantityField.getText().trim()); }
            catch (NumberFormatException ex) { buyAmountLbl.setText("\u2014"); return; }
            if (quantity < 1) {
                buyAmountLbl.setText("\u2014");
                return;
            }
            buyAmountLbl.setText(CurrencyFormatter.format(gameController.unitCostWithFee(stock).multiply(BigDecimal.valueOf(quantity))));
        };
        final Runnable updateSellAmount = () -> {
            int quantity;
            try { quantity = Integer.parseInt(quantityField.getText().trim()); }
            catch (NumberFormatException ex) { sellAmountLbl.setText("\u2014"); return; }
            if (quantity < 1) {
                sellAmountLbl.setText("\u2014");
                return;
            }
            List<BigDecimal> p = gameController.previewSell(stock, BigDecimal.valueOf(quantity));
            sellAmountLbl.setText(p == null ? "\u2014" : CurrencyFormatter.format(p.get(3)));
        };
        final Runnable applyMaxForBuy = () -> {
            BigDecimal unitCostWithFee = gameController.unitCostWithFee(stock);
            int maxBuyquantity = player.getMoney().divide(unitCostWithFee, 0, RoundingMode.DOWN).intValue();
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
                List<BigDecimal> sellPreview = gameController.previewSell(stock, BigDecimal.valueOf(maxSellQuantity));
                amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview.get(3)).setScale(2, RoundingMode.HALF_UP).toPlainString());
            syncingFields[0] = false;
            updateBuyAmount.run();
            updateSellAmount.run();
        };

        maxBuyBtn.setOnAction(e -> applyMaxForBuy.run());
        maxSellBtn.setOnAction(e -> applyMaxForSell.run());

        updateBuyAmount.run();
        updateSellAmount.run();
        quantityField.textProperty().addListener((obs, old, val) -> {
            if (syncingFields[0]) {
                return;
            }
            int quantity;
            try {
                quantity = Integer.parseInt(val.trim());
            } catch (NumberFormatException ex) {
                quantity = 1;
            }
            quantity = Math.max(0, quantity);
            syncingFields[0] = true;
            quantityField.setText(String.valueOf(quantity));
            if (amountTracksSell[0]) {
                List<BigDecimal> sellPreview = gameController.previewSell(stock, BigDecimal.valueOf(quantity));
                amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview.get(3))
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
            } else {
                amountField.setText(gameController.unitCostWithFee(stock).multiply(BigDecimal.valueOf(quantity))
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
            try { parsedquantity = Integer.parseInt(quantityField.getText().trim()); }
            catch (NumberFormatException ex) { showError(overlayRef[0], "Enter a valid quantity."); return; }

            if (parsedquantity < 1) {
                showError(overlayRef[0], "Quantity must be at least 1 whole share.");
                return;
            }

            BigDecimal quantity   = BigDecimal.valueOf(parsedquantity);
            BigDecimal gross = stock.getSalesPrice().multiply(quantity);
            BigDecimal fee   = gross.multiply(new BigDecimal("0.005"));
            BigDecimal total = gross.add(fee);
            showTradeConfirm(overlayRef[0], "BUY", stock, quantity, gross, fee, BigDecimal.ZERO, total, () -> {
                try {
                    Transaction tx = exchange.buy(stock.getSymbol(), quantity, player);
                    tx.commit(player);
                    showReceipt(overlayRef[0], "BUY", stock, quantity, total, fee, BigDecimal.ZERO, player.getMoney());
                    updateData();
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
            int parsedquantity;
            try { parsedquantity = Integer.parseInt(quantityField.getText().trim()); }
            catch (NumberFormatException ex) { showError(overlayRef[0], "Enter a valid quantity."); return; }

            if (parsedquantity < 1) {
                showError(overlayRef[0], "Quantity must be at least 1 whole share.");
                return;
            }

            BigDecimal sellquantity = BigDecimal.valueOf(parsedquantity);
            BigDecimal totalOwned = player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalOwned.compareTo(BigDecimal.ZERO) == 0) {
                showError(overlayRef[0], "You don't own any shares of " + stock.getSymbol()); return;
            }
            if (sellquantity.compareTo(totalOwned) > 0) {
                showError(overlayRef[0], "You only own " + totalOwned.stripTrailingZeros().toPlainString()
                        + " shares of " + stock.getSymbol()); return;
            }
            List<BigDecimal> preview = gameController.previewSell(stock, sellquantity);
            showTradeConfirm(overlayRef[0], "SELL", stock, sellquantity,
                    preview.get(0), preview.get(1), preview.get(2), preview.get(3), () -> {
                try {
                    BigDecimal[] result = executeSell(player, exchange, stock, sellquantity);
                    showReceipt(overlayRef[0], "SELL", stock, sellquantity,
                            result[3], result[1], result[2], player.getMoney());
                    updateData();
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

    
}