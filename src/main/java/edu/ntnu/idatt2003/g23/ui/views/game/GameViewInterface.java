package edu.ntnu.idatt2003.g23.ui.views.game;

import java.math.BigDecimal;

import edu.ntnu.idatt2003.g23.model.Stock;

/**
 * The operations that {@link GameController} may call on the game view.
 *
 * <p>Decoupling the controller from the concrete {@link GameView} class allows
 * the controller to be unit-tested without any JavaFX runtime.
 */
public interface GameViewInterface {

  /**
   * Refreshes all observable data (cash, portfolio, stock list).
   */
  void updateData();

  /**
   * Displays an error message to the user.
   */
  void showError(String message);

  /**
   * Shows an order-confirmation overlay for a single-stock trade.
   *
   * @param action   "BUY" or "SELL"
   * @param stock    the stock being traded
   * @param quantity number of shares
   * @param gross    gross value before fees / taxes
   * @param fee      commission charged
   * @param tax      capital-gains tax charged (0 for buys)
   * @param total    final amount paid / received
   */
  void showTradeConfirm(String action, Stock stock, BigDecimal quantity,
                        BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total);

  /**
   * Shows an order-confirmation overlay for a bulk (sell-all) trade.
   *
   * @param action   label, e.g. "SELL ALL HOLDINGS"
   * @param quantity total shares across all positions
   * @param gross    gross proceeds
   * @param fee      total commission
   * @param tax      total capital-gains tax
   * @param total    net proceeds
   */
  void showBulkTradeConfirm(String action, BigDecimal quantity,
                            BigDecimal gross, BigDecimal fee, BigDecimal tax, BigDecimal total);

  /**
   * Shows an order-receipt overlay for a completed single-stock trade.
   *
   * @param action   "BUY" or "SELL"
   * @param stock    the stock that was traded
   * @param quantity number of shares
   * @param total    net amount received / paid
   * @param fee      commission charged
   * @param tax      capital-gains tax charged (0 for buys)
   * @param newCash  player's cash balance after the trade
   */
  void showReceipt(String action, Stock stock, BigDecimal quantity,
                   BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash);

  /**
   * Shows an order-receipt overlay for a completed bulk trade.
   *
   * @param action   label, e.g. "SELL ALL HOLDINGS"
   * @param quantity total shares sold
   * @param total    net proceeds
   * @param fee      total commission
   * @param tax      total capital-gains tax
   * @param newCash  player's cash balance after the trade
   */
  void showBulkReceipt(String action, BigDecimal quantity,
                       BigDecimal total, BigDecimal fee, BigDecimal tax, BigDecimal newCash);
}
