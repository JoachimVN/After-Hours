package edu.ntnu.idatt2003.g23.ui.views;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class GameView {

    public static StackPane build(Runnable onBack, Runnable onSettings,
                                   Player player, Exchange exchange) {

        StackPane[] overlayRef = {null};

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
            rebuildDetail(detailArea, selectedStock.get(), player, exchange, portfolioItems, refreshRef, overlayRef);
        };

        // Initial stock list population
        rebuildStockList(stockListBox, filteredStocks, selectedStock, refreshRef);

        // Rebuild detail when selection changes
        selectedStock.addListener((obs, old, stock) ->
                rebuildDetail(detailArea, stock, player, exchange, portfolioItems, refreshRef, overlayRef));

        // Show detail immediately for first stock
        if (selectedStock.get() != null) {
            rebuildDetail(detailArea, selectedStock.get(), player, exchange, portfolioItems, refreshRef, overlayRef);
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

        ImageView appTitle;
        var logoUrl = GameView.class.getResource("/images/After_Hours_Logo.png");
        if (logoUrl != null) {
            Image logoImg = new Image(logoUrl.toExternalForm());
            appTitle = new ImageView(logoImg);
            appTitle.setPreserveRatio(true);
            appTitle.setFitHeight(40);
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
        root.getStyleClass().add("home-page");

        VBox topSection = new VBox(0, topBar, subBar);
        root.setTop(topSection);
        root.setCenter(body);

        StackPane overlay = new StackPane(root);
        overlayRef[0] = overlay;
        return overlay;
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
                                       ObservableList<Share> portfolioItems, Runnable[] refreshRef,
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

        decBtn.setOnAction(e -> {
            try { int v = Math.max(1, Integer.parseInt(qtyField.getText().trim()) - 1);
                  qtyField.setText(String.valueOf(v)); }
            catch (NumberFormatException ignored) { qtyField.setText("1"); }
        });
        incBtn.setOnAction(e -> {
            try { qtyField.setText(String.valueOf(Integer.parseInt(qtyField.getText().trim()) + 1)); }
            catch (NumberFormatException ignored) { qtyField.setText("1"); }
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
            BigDecimal qty;
            try { qty = new BigDecimal(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { buyAmountLbl.setText("\u2014"); return; }
            buyAmountLbl.setText(fmt(stock.getSalesPrice().multiply(qty)
                    .multiply(new BigDecimal("1.005"))));
        };
        final Runnable updateSellAmount = () -> {
            BigDecimal qty;
            try { qty = new BigDecimal(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { sellAmountLbl.setText("\u2014"); return; }
            BigDecimal[] p = previewSell(player, stock, qty);
            sellAmountLbl.setText(p == null ? "\u2014" : fmt(p[3]));
        };
        updateBuyAmount.run();
        updateSellAmount.run();
        qtyField.textProperty().addListener((obs, old, val) -> { updateBuyAmount.run(); updateSellAmount.run(); });

        // ── BUY button (primary — flex) ────────────────────────────────────────
        Label buyTopLbl = new Label("\u2197  BUY");
        buyTopLbl.getStyleClass().add("trade-btn-label");
        VBox buyGraphic = new VBox(3, buyTopLbl, buyAmountLbl);
        buyGraphic.setAlignment(Pos.CENTER);

        Button buyBtn = new Button();
        buyBtn.setGraphic(buyGraphic);
        buyBtn.getStyleClass().add("trade-buy-button");
        buyBtn.setOnAction(e -> {
            BigDecimal parsedQty;
            try { parsedQty = new BigDecimal(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { showError("Enter a valid quantity."); return; }
            BigDecimal qty   = parsedQty;
            BigDecimal gross = stock.getSalesPrice().multiply(qty);
            BigDecimal fee   = gross.multiply(new BigDecimal("0.005"));
            BigDecimal total = gross.add(fee);
            showTradeConfirm(overlayRef[0], "BUY", stock, qty, gross, fee, BigDecimal.ZERO, total, () -> {
                try {
                    Transaction tx = exchange.buy(stock.getSymbol(), qty, player);
                    tx.commit(player);
                    showReceipt(overlayRef[0], "BUY", stock, qty, total, fee, BigDecimal.ZERO, player.getMoney());
                    refreshRef[0].run();
                } catch (Exception ex) { showError(ex.getMessage()); }
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
            BigDecimal parsedQty;
            try { parsedQty = new BigDecimal(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { showError("Enter a valid quantity."); return; }
            BigDecimal sellQty = parsedQty;
            BigDecimal totalOwned = player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalOwned.compareTo(BigDecimal.ZERO) == 0) {
                showError("You don't own any shares of " + stock.getSymbol()); return;
            }
            if (sellQty.compareTo(totalOwned) > 0) {
                showError("You only own " + totalOwned.stripTrailingZeros().toPlainString()
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
                } catch (Exception ex) { showError(ex.getMessage()); }
            });
        });

        HBox tradeRow = new HBox(8, stepper, buyBtn, sellBtn);
        tradeRow.setAlignment(Pos.CENTER_LEFT);

        VBox tradePanel = new VBox(0, tradeRow);
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

        table.getColumns().addAll(symCol, qtyCol, boughtCol, nowCol, plCol);
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
        boolean isBuy = "BUY".equals(action);

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
        boolean isBuy = "BUY".equals(action);

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

    private static void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "An unexpected error occurred.");
        alert.showAndWait();
    }
}

