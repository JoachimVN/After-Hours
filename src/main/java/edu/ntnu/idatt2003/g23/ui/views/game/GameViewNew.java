package edu.ntnu.idatt2003.g23.ui.views.game;

import java.util.HashSet;
import java.util.Set;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import edu.ntnu.idatt2003.g23.ui.util.CurrencyFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class GameViewNew {
    private final GameController gameController;

    public GameViewNew(GameController controller, Player player, Exchange exchange) {
        this.gameController = controller;
        ObservableList<Stock> allStocks        = FXCollections.observableArrayList(exchange.getStocks());
        FilteredList<Stock>   filteredStocks   = new FilteredList<>(allStocks, s -> true);
        ObservableList<Share> portfolioItems   = FXCollections.observableArrayList(player.getPortfolio().getShares());
        ObjectProperty<Stock> selectedStock    = new SimpleObjectProperty<>(allStocks.isEmpty() ? null : allStocks.get(0));

        // ── Stat pill labels ─────────────────────────────────────────────────
        Label cashVal    = new Label(CurrencyFormatter.format(player.getMoney()));
        Label portVal    = new Label(CurrencyFormatter.format(player.getPortfolio().getNetWorth()));
        Label nwVal      = new Label(CurrencyFormatter.format(player.getNetWorth()));
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
    }
}