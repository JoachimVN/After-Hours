package edu.ntnu.idatt2003.g23.ui.views.profile;

import java.math.BigDecimal;
import java.util.List;

import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.ui.views.game.GameController;

/**
 * Controller for ProfileView (MVC).
 */
public final class ProfileController {
    private final GameController gameController;

    public ProfileController(GameController gameController) {
        if (gameController == null) {
            throw new IllegalArgumentException("gameController cannot be null");
        }
        this.gameController = gameController;
    }

    public String getPlayerName() {
        return gameController.getPlayerName();
    }

    public PlayerStatus getPlayerStatus() {
        return gameController.getPlayerStatus();
    }

    public int getCurrentWeek() {
        return gameController.getCurrentWeek();
    }

    public BigDecimal getPlayerStartingMoney() {
        return gameController.getPlayerStartingMoney();
    }

    public BigDecimal getPlayerCash() {
        return gameController.getPlayerCash();
    }

    public BigDecimal getPortfolioNetWorth() {
        return gameController.getPortfolioNetWorth();
    }

    public BigDecimal getPlayerNetWorth() {
        return gameController.getPlayerNetWorth();
    }

    public BigDecimal getPlayerGrowthRatio() {
        return gameController.getPlayerGrowthRatio();
    }

    public int getTransactionCount() {
        return gameController.getTransactionCount();
    }

    public int getPlayerWeeksTraded() {
        return gameController.getPlayerWeeksTraded();
    }

    public BigDecimal getPlayerStatusProgress() {
        return gameController.getPlayerStatusProgress();
    }

    public int getPlayerWeeksTargetForNextStatus() {
        return gameController.getPlayerWeeksTargetForNextStatus();
    }

    public BigDecimal getPlayerWeeksProgress() {
        return gameController.getPlayerWeeksProgress();
    }

    public BigDecimal getPlayerGrowthTargetForNextStatus() {
        return gameController.getPlayerGrowthTargetForNextStatus();
    }

    public BigDecimal getPlayerNetWorthProgress() {
        return gameController.getPlayerNetWorthProgress();
    }

    public List<Share> getPortfolioShares() {
        return gameController.getPortfolioShares();
    }

    public List<GameController.ReplayPoint> getReplaySeries() {
        return gameController.getReplaySeries();
    }

    public String getDisplayedPlayerAvatar() {
        return gameController.getPlayerAvatar();
    }

    public boolean isChickAvatarEquipped() {
        return gameController.isChickAvatarEquipped();
    }

    public int getChickPhaseUnlocked() {
        return gameController.getChickPhaseUnlocked();
    }

    public int getWeeksUsingChickAvatar() {
        return gameController.getWeeksUsingChickAvatar();
    }
}
