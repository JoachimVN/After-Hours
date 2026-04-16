package edu.ntnu.idatt2003.g23.ui.views.game;

import edu.ntnu.idatt2003.g23.model.Player;
import edu.ntnu.idatt2003.g23.model.Share;
import java.util.List;

public final class GameController {

    private final Player player;

    public GameController(Player player) {
        this.player = player;
    }

    public List<Share> getOwnedShares() {
        return Share.getOwnedShares(player.getPortfolio().getShares());
    }
}
