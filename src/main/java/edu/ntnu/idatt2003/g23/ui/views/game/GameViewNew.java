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
    }

    private void updateData(Player player, Exchange exchange) {
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
            BigDecimal unitCostWithFee = stock.getSalesPrice().multiply(new BigDecimal("1.005"));

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
            buyAmountLbl.setText(CurrencyFormatter.format(stock.getSalesPrice().multiply(BigDecimal.valueOf(quantity))
                    .multiply(new BigDecimal("1.005"))));
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
            BigDecimal unitCostWithFee = stock.getSalesPrice().multiply(new BigDecimal("1.005"));
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
            BigDecimal totalOwned = player.getPortfolio().getShareBySymbol(stock.getSymbol())
                    .stream().map(Share::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            int maxSellquantity = totalOwned.setScale(0, RoundingMode.DOWN).intValue();
            maxSellquantity = Math.max(0, maxSellquantity);
            amountTracksSell[0] = true;
            syncingFields[0] = true;
                quantityField.setText(String.valueOf(maxSellquantity));
                BigDecimal[] sellPreview = previewSell(player, stock, BigDecimal.valueOf(maxSellquantity));
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
                BigDecimal[] sellPreview = previewSell(player, stock, BigDecimal.valueOf(quantity));
                amountField.setText((sellPreview == null ? BigDecimal.ZERO : sellPreview[3])
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
            } else {
                amountField.setText(stock.getSalesPrice().multiply(new BigDecimal("1.005")).multiply(BigDecimal.valueOf(quantity))
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
            BigDecimal unitCostWithFee = stock.getSalesPrice().multiply(new BigDecimal("1.005"));
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
            BigDecimal[] preview = previewSell(player, stock, sellquantity);
            showTradeConfirm(overlayRef[0], "SELL", stock, sellquantity,
                    preview[0], preview[1], preview[2], preview[3], () -> {
                try {
                    BigDecimal[] result = executeSell(player, exchange, stock, sellquantity);
                    showReceipt(overlayRef[0], "SELL", stock, sellquantity,
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

        detailArea.getChildren().add(header);
        VBox.setVgrow(header, Priority.ALWAYS);
    }

    
}