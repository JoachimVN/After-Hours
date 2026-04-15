package edu.ntnu.idatt2003.g23.ui.views;

import java.math.BigDecimal;
import java.math.RoundingMode;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.model.transaction.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class GameView {

    public static BorderPane build(Runnable onBack, Runnable onSettings,
                                   Player player, Exchange exchange) {

        // ── Observable data ──────────────────────────────────────────────────
        ObservableList<Stock> allStocks        = FXCollections.observableArrayList(exchange.getStocks());
        FilteredList<Stock>   filteredStocks   = new FilteredList<>(allStocks, s -> true);
        ObservableList<Share> portfolioItems   = FXCollections.observableArrayList(player.getPortfolio().getShares());
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

        // ── Refresh closure ──────────────────────────────────────────────────
        Runnable[] refreshRef = {null};
        refreshRef[0] = () -> {
            weekNumLbl.setText(String.valueOf(exchange.getWeek()));
            cashVal.setText(fmt(player.getMoney()));
            portVal.setText(fmt(player.getPortfolio().getNetWorth()));
            nwVal.setText(fmt(player.getNetWorth()));
            portfolioItems.setAll(player.getPortfolio().getShares());
            rebuildStockList(stockListBox, filteredStocks, selectedStock, refreshRef);
            rebuildDetail(detailArea, selectedStock.get(), player, exchange, portfolioItems, refreshRef);
        };

        // Initial stock list population
        rebuildStockList(stockListBox, filteredStocks, selectedStock, refreshRef);

        // Rebuild detail when selection changes
        selectedStock.addListener((obs, old, stock) ->
                rebuildDetail(detailArea, stock, player, exchange, portfolioItems, refreshRef));

        // Show detail immediately for first stock
        if (selectedStock.get() != null) {
            rebuildDetail(detailArea, selectedStock.get(), player, exchange, portfolioItems, refreshRef);
        }

        // ── Search field ─────────────────────────────────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D  Search stocks\u2026");
        searchField.getStyleClass().add("game-search-field");
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredStocks.setPredicate(s ->
                    lower.isEmpty()
                    || s.getSymbol().toLowerCase().contains(lower)
                    || s.getCompany().toLowerCase().contains(lower));
            rebuildStockList(stockListBox, filteredStocks, selectedStock, refreshRef);
        });

        Label marketTitle = new Label("Market Stocks");
        marketTitle.getStyleClass().add("game-panel-title");

        ScrollPane stockScroll = new ScrollPane(stockListBox);
        stockScroll.setFitToWidth(true);
        stockScroll.getStyleClass().add("game-scroll");
        stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(stockScroll, Priority.ALWAYS);

        VBox leftPanel = new VBox(12, marketTitle, searchField, stockScroll);
        leftPanel.getStyleClass().add("game-left-panel");
        leftPanel.setPrefWidth(300);
        leftPanel.setMinWidth(220);
        leftPanel.setMaxWidth(340);

        // ── Portfolio table (bottom of right panel) ──────────────────────────
        Label portTitle = new Label("Portfolio");
        portTitle.getStyleClass().add("game-panel-title");

        TableView<Share> portfolioTable = buildPortfolioTable(portfolioItems, player, exchange, refreshRef);
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
        nextWeekBtn.setOnAction(e -> {
            exchange.advance();
            refreshRef[0].run();
        });

        Region subSpacer = new Region(); HBox.setHgrow(subSpacer, Priority.ALWAYS);
        HBox subBar = new HBox(16, weekCard, nextWeekBtn, subSpacer);
        subBar.getStyleClass().add("game-sub-bar");
        subBar.setAlignment(Pos.CENTER_LEFT);

        // ── Top bar ──────────────────────────────────────────────────────────
        Button backBtn = new Button("\u2190");
        backBtn.getStyleClass().add("game-icon-button");
        backBtn.setOnAction(e -> onBack.run());

        Label appTitle = new Label("After Hours");
        appTitle.getStyleClass().add("game-app-title");

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
        root.getStyleClass().add("home-page");

        VBox topSection = new VBox(0, topBar, subBar);
        root.setTop(topSection);
        root.setCenter(body);
        return root;
    }

    // ── Stock list builder ────────────────────────────────────────────────────

    private static void rebuildStockList(VBox box, FilteredList<Stock> stocks,
                                         ObjectProperty<Stock> selectedStock,
                                         Runnable[] refreshRef) {
        box.getChildren().clear();
        for (Stock stock : stocks) {
            box.getChildren().add(buildStockCard(stock, selectedStock, refreshRef));
        }
    }

    private static Node buildStockCard(Stock stock, ObjectProperty<Stock> selectedStock,
                                        Runnable[] refreshRef) {
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

        VBox left  = new VBox(2, symLbl, compLbl, pctLbl);
        VBox right = new VBox();
        right.setAlignment(Pos.TOP_RIGHT);
        right.getChildren().add(priceLbl);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox card = new HBox(8, left, spacer, right);
        card.getStyleClass().add("stock-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));

        if (stock.equals(selectedStock.get())) {
            card.getStyleClass().add("stock-card-selected");
        }

        card.setOnMouseClicked(e -> selectedStock.set(stock));
        return card;
    }

    // ── Detail panel builder ──────────────────────────────────────────────────

    private static void rebuildDetail(VBox area, Stock stock, Player player, Exchange exchange,
                                       ObservableList<Share> portfolioItems, Runnable[] refreshRef) {
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

        // Trade panel
        Label tradeTitle = new Label("Trade " + stock.getSymbol());
        tradeTitle.getStyleClass().add("trade-title");

        TextField qtyField = new TextField("1");
        qtyField.getStyleClass().add("setup-text-field");
        qtyField.setMaxWidth(100);

        // Live amount labels shown on the buttons
        Label buyAmountLbl  = new Label("—");
        buyAmountLbl.getStyleClass().add("trade-button-amount");
        Label sellAmountLbl = new Label("—");
        sellAmountLbl.getStyleClass().add("trade-button-amount");

        // Recompute displayed amounts whenever qty changes
        final Runnable updateAmounts = () -> {
            BigDecimal qty;
            try { qty = new BigDecimal(qtyField.getText().trim()); }
            catch (NumberFormatException ex) {
                buyAmountLbl.setText("—"); sellAmountLbl.setText("—"); return;
            }
            // Buy total: price × qty × 1.005  (0.5% commission, no buy tax)
            buyAmountLbl.setText(fmt(stock.getSalesPrice().multiply(qty)
                    .multiply(new BigDecimal("1.005"))));
            // Sell total: mirrors SaleCalculator for the first owned position
            player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().findFirst().ifPresentOrElse(sh -> {
                BigDecimal gross      = stock.getSalesPrice().multiply(sh.getQuantity());
                BigDecimal commission = gross.multiply(new BigDecimal("0.01"));
                BigDecimal rawProfit  = gross.subtract(commission)
                        .subtract(sh.getPurchasePrice().multiply(sh.getQuantity()));
                BigDecimal tax = rawProfit.multiply(new BigDecimal("0.3"));
                sellAmountLbl.setText(fmt(gross.subtract(commission).subtract(tax)));
            }, () -> sellAmountLbl.setText("—"));
        };
        updateAmounts.run();
        qtyField.textProperty().addListener((obs, old, val) -> updateAmounts.run());

        // BUY button — large green with ↗ icon and live cost
        Label buyIcon = new Label("\u2197");
        buyIcon.getStyleClass().add("trade-btn-icon");
        Label buyLbl = new Label("BUY");
        buyLbl.getStyleClass().add("trade-btn-label");
        HBox buyTop = new HBox(6, buyIcon, buyLbl);
        buyTop.setAlignment(Pos.CENTER_LEFT);
        VBox buyContent = new VBox(1, buyTop, buyAmountLbl);
        buyContent.setAlignment(Pos.CENTER_LEFT);

        Button buyBtn = new Button();
        buyBtn.setGraphic(buyContent);
        buyBtn.getStyleClass().add("trade-buy-button");
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buyBtn, Priority.ALWAYS);
        buyBtn.setOnAction(e -> {
            try {
                BigDecimal qty = new BigDecimal(qtyField.getText().trim());
                Transaction tx = exchange.buy(stock.getSymbol(), qty, player);
                tx.commit(player);
                refreshRef[0].run();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });

        // SELL button — large red with ↘ icon and live proceeds
        Label sellIcon = new Label("\u2198");
        sellIcon.getStyleClass().add("trade-btn-icon");
        Label sellLbl = new Label("SELL");
        sellLbl.getStyleClass().add("trade-btn-label");
        HBox sellTop = new HBox(6, sellIcon, sellLbl);
        sellTop.setAlignment(Pos.CENTER_LEFT);
        VBox sellContent = new VBox(1, sellTop, sellAmountLbl);
        sellContent.setAlignment(Pos.CENTER_LEFT);

        Button sellBtn = new Button();
        sellBtn.setGraphic(sellContent);
        sellBtn.getStyleClass().add("trade-sell-button");
        sellBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sellBtn, Priority.ALWAYS);
        sellBtn.setOnAction(e -> {
            player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().findFirst().ifPresentOrElse(sh -> {
                try {
                    Transaction tx = exchange.sell(sh, player);
                    tx.commit(player);
                    refreshRef[0].run();
                } catch (Exception ex) { showError(ex.getMessage()); }
            }, () -> showError("You don't own any shares of " + stock.getSymbol()));
        });

        Label qtyLabel = new Label("Quantity");
        qtyLabel.getStyleClass().add("setup-field-label");
        HBox qtyRow = new HBox(8, qtyLabel, qtyField);
        qtyRow.setAlignment(Pos.CENTER_LEFT);

        HBox tradeButtons = new HBox(10, buyBtn, sellBtn);
        HBox.setHgrow(buyBtn, Priority.ALWAYS);
        HBox.setHgrow(sellBtn, Priority.ALWAYS);

        VBox tradePanel = new VBox(10, tradeTitle, qtyRow, tradeButtons);
        tradePanel.getStyleClass().add("trade-panel");

        VBox header = new VBox(4, sym, comp, priceRow, hlRow, tradePanel);
        header.getStyleClass().add("game-detail-header");

        area.getChildren().add(header);
        VBox.setVgrow(header, Priority.ALWAYS);
    }

    // ── Portfolio table ───────────────────────────────────────────────────────

    private static TableView<Share> buildPortfolioTable(ObservableList<Share> items, Player player,
                                                         Exchange exchange, Runnable[] refreshRef) {
        TableView<Share> table = new TableView<>(items);
        table.getStyleClass().add("game-table");
        table.setPlaceholder(new Label("No shares owned yet"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Share, String> symCol = col("Symbol", c ->
                c.getStock().getSymbol(), 70, 85);
        TableColumn<Share, String> qtyCol = col("Qty", c ->
                c.getQuantity().stripTrailingZeros().toPlainString(), 45, 65);
        TableColumn<Share, String> boughtCol = col("Bought @", c ->
                fmt(c.getPurchasePrice()), 85, 110);
        TableColumn<Share, String> nowCol = col("Now @", c ->
                fmt(c.getStock().getSalesPrice()), 85, 110);

        TableColumn<Share, String> plCol = new TableColumn<>("P&L");
        plCol.setMinWidth(90);
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

        TableColumn<Share, Void> sellCol = new TableColumn<>("");
        sellCol.setMinWidth(52); sellCol.setMaxWidth(52); sellCol.setSortable(false);
        sellCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Sell");
            { btn.getStyleClass().add("sell-button");
              btn.setOnAction(e -> {
                  Share sh = getTableRow().getItem();
                  if (sh == null) return;
                  try {
                      Transaction tx = exchange.sell(sh, player);
                      tx.commit(player);
                      refreshRef[0].run();
                  } catch (Exception ex) { showError(ex.getMessage()); }
              }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(symCol, qtyCol, boughtCol, nowCol, plCol, sellCol);
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

    private static String fmt(BigDecimal value) {
        return String.format("$%,.2f", value.doubleValue());
    }

    private static void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "An unexpected error occurred.");
        alert.showAndWait();
    }
}

