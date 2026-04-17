package edu.ntnu.idatt2003.g23.ui.views.game;

import edu.ntnu.idatt2003.g23.model.Exchange;
import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.model.Stock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class GameController {

    private final Player player;
    private final Exchange exchange;
    private final GameView view;

    public GameController(Player player, Exchange exchange, GameView view) {
        this.player = player;
        this.exchange = exchange;
        this.view = view;
    }

}
