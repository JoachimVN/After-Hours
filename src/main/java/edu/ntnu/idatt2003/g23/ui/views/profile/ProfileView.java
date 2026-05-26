package edu.ntnu.idatt2003.g23.ui.views.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import edu.ntnu.idatt2003.g23.AppConfig;
import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.ui.util.AvatarUtil;
import edu.ntnu.idatt2003.g23.ui.util.CurrencyFormatter;
import edu.ntnu.idatt2003.g23.ui.views.game.GameController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import javafx.util.StringConverter;

public final class ProfileView {

  private static final double PROFILE_GRID_MIN_CARD_WIDTH = 560;
  private static final int PROFILE_GRID_MAX_COLUMNS = 4;
  private static final double PROFILE_CARD_LEFT_COLUMN_WIDTH = 195;
  private static final double PROFILE_CARD_RIGHT_COLUMN_WIDTH = 145;

  private ProfileView() {
  }

  public static Parent build(
      ProfileController controller,
      Runnable onBackToGame,
      Runnable onOpenSettings,
      Runnable onReplayControlSelect,
      String currentAvatar,
      Consumer<String> onAvatarChanged,
      Consumer<String> onNameChanged,
      Consumer<String> onOpenStockFromPortfolio) {

    List<String> avatarNames = new ArrayList<>(AvatarUtil.loadSelectableAvatarNames());
    String initialAvatar = (currentAvatar == null || currentAvatar.isBlank())
        ? "bust-in-silhouette"
        : AvatarUtil.normalizeAvatarStem(currentAvatar);
    final String[] selectedAvatar = { initialAvatar };

    // Track pending name edits while the field is focused
    final String[] originalName = { controller.getPlayerName() };
    final boolean[] nameChanged = { false };

    TextField profileNameField = new TextField(controller.getPlayerName());
    profileNameField.getStyleClass().addAll("profile-title", "profile-name-field");
    profileNameField.setMaxWidth(Double.MAX_VALUE);
    profileNameField.setPrefColumnCount(Math.max(18, controller.getPlayerName().length() + 2));

    Runnable commitPendingName = () -> {
      String newName = profileNameField.getText().strip();
      if (newName.isEmpty()) {
        profileNameField.setText(originalName[0]);
        nameChanged[0] = false;
        return;
      }
      if (!newName.equals(originalName[0])) {
        onNameChanged.accept(newName);
        originalName[0] = newName;
      }
      nameChanged[0] = false;
    };

    Button backBtn = new Button("\u2190 Back To Market");
    backBtn.getStyleClass().add("profile-back-btn");
    backBtn.setOnAction(e -> {
      if (nameChanged[0]) {
        commitPendingName.run();
      }
      onBackToGame.run();
    });

    Button settingsBtn = new Button("\u2699 Settings");
    settingsBtn.getStyleClass().add("profile-settings-btn");
    settingsBtn.setOnAction(e -> {
      if (nameChanged[0]) {
        commitPendingName.run();
      }
      onOpenSettings.run();
    });

    Region topSpacer = new Region();
    HBox.setHgrow(topSpacer, Priority.ALWAYS);
    HBox topBar = new HBox(12, backBtn, topSpacer, settingsBtn);
    topBar.getStyleClass().add("profile-top-bar");
    topBar.setAlignment(Pos.CENTER_LEFT);

    // Chick avatar logic
    boolean isChick = controller.isChickAvatarEquipped();
    int chickPhaseUnlocked = 0;
    String displayedAvatar = controller.getDisplayedPlayerAvatar();
    try {
      chickPhaseUnlocked = controller.getChickPhaseUnlocked();
      controller.getWeeksUsingChickAvatar();
    } catch (Exception ignored) {
    }
    final int chickPhaseUnlockedValue = chickPhaseUnlocked;
    Label avatarDisplay = new Label();
    if (isChick) {
      avatarDisplay.setGraphic(AvatarUtil.createImageView(displayedAvatar, 57.6));
    } else {
      avatarDisplay.setGraphic(AvatarUtil.createImageView(displayedAvatar, 57.6));
      avatarDisplay.setTooltip(null);
    }
    avatarDisplay.getStyleClass().add("profile-avatar-display");

    PlayerStatus status = controller.getPlayerStatus();
    Label profileSubtitle = new Label("Status: " + status.name() + "  •  Week " + controller.getCurrentWeek());
    profileSubtitle.getStyleClass().add("profile-subtitle");

    profileNameField.focusedProperty().addListener((obs, oldV, focused) -> {
      if (!focused) {
        String newName = profileNameField.getText().strip();
        if (newName.isEmpty()) {
          profileNameField.setText(originalName[0]);
          nameChanged[0] = false;
        } else {
          nameChanged[0] = !newName.equals(originalName[0]);
        }
      }
    });
    profileNameField.setOnKeyPressed(e -> {
      if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
        String newName = profileNameField.getText().strip();
        if (newName.isEmpty()) {
          profileNameField.setText(originalName[0]);
          nameChanged[0] = false;
        } else {
          nameChanged[0] = !newName.equals(originalName[0]);
        }
        profileNameField.getParent().requestFocus();
      }
    });

    List<Button> avatarButtons = new ArrayList<>();
    GridPane avatarPicker = new GridPane();
    avatarPicker.getStyleClass().add("profile-avatar-picker");
    avatarPicker.setHgap(8);
    avatarPicker.setVgap(8);
    avatarPicker.setMaxWidth(8 * 38 + 7 * 8);
    int selectableSlots = 16;
    for (int row = 0; row < 2; row++) {
      for (int col = 0; col < 8; col++) {
        int index = row * 8 + col;
        if (index < avatarNames.size() && index < selectableSlots) {
          String avatar = avatarNames.get(index);
          Button avatarBtn = new Button();
          avatarButtons.add(avatarBtn);
          String pickerDisplayAvatar = AvatarUtil.getDisplayAvatarStem(avatar, chickPhaseUnlockedValue);
          var avatarGraphic = AvatarUtil.createImageView(pickerDisplayAvatar, 25.2);
          avatarBtn.setGraphic(avatarGraphic);
          avatarBtn.setMinSize(38, 38);
          avatarBtn.setPrefSize(38, 38);
          avatarBtn.setMaxSize(38, 38);
          avatarBtn.getStyleClass().add("profile-avatar-btn");
          PlayerStatus requiredStatus = requiredStatusForAvatar(avatar);
          boolean unlocked = isAvatarUnlocked(status, requiredStatus);
          if (!unlocked) {
            ColorAdjust grayscale = new ColorAdjust();
            grayscale.setSaturation(-1.0);
            avatarGraphic.setEffect(grayscale);
            avatarGraphic.setOpacity(0.45);
            avatarBtn.getStyleClass().add("profile-avatar-btn-locked");
            avatarBtn.setTooltip(buildLockedHintTooltip(
                "Unlocks at " + formatStatusName(requiredStatus)));
          }
          if (avatar.equals(initialAvatar)) {
            avatarBtn.getStyleClass().add("profile-avatar-btn-active");
          }
          avatarBtn.setOnAction(e -> {
            if (!unlocked) {
              return;
            }
            if (onReplayControlSelect != null) {
              onReplayControlSelect.run();
            }
            avatarButtons.forEach(node -> node.getStyleClass().remove("profile-avatar-btn-active"));
            if (avatar.equals(selectedAvatar[0])) {
              selectedAvatar[0] = "bust-in-silhouette";
              avatarDisplay.setGraphic(AvatarUtil.createImageView(selectedAvatar[0], 57.6));
              avatarDisplay.setTooltip(null);
              onAvatarChanged.accept(selectedAvatar[0]);
              return;
            }
            selectedAvatar[0] = avatar;
            String nextDisplayAvatar = AvatarUtil.getDisplayAvatarStem(avatar, chickPhaseUnlockedValue);
            avatarDisplay.setGraphic(AvatarUtil.createImageView(nextDisplayAvatar, 57.6));
            onAvatarChanged.accept(avatar);
            avatarBtn.getStyleClass().add("profile-avatar-btn-active");
          });
          avatarPicker.add(avatarBtn, col, row);
        } else {
          Region slot = new Region();
          slot.getStyleClass().add("profile-avatar-placeholder");
          slot.setMinSize(38, 38);
          slot.setPrefSize(38, 38);
          slot.setMaxSize(38, 38);
          avatarPicker.add(slot, col, row);
        }
      }
    }

    VBox heroText = new VBox(6, profileNameField, profileSubtitle, avatarPicker);
    heroText.setFillWidth(true);
    heroText.setMaxWidth(Double.MAX_VALUE);
    heroText.setAlignment(Pos.TOP_LEFT);

    HBox hero = new HBox(16, avatarDisplay, heroText);
    hero.getStyleClass().add("profile-hero");
    hero.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(heroText, Priority.ALWAYS);

    VBox infoCard = statCard("Player",
        statLine("Starting Cash", CurrencyFormatter.format(controller.getPlayerStartingMoney())),
        statLine("Available Cash", CurrencyFormatter.format(controller.getPlayerCash())),
        statLine("Portfolio Value", CurrencyFormatter.format(controller.getPortfolioNetWorth())),
        statLine("Net Worth", CurrencyFormatter.format(controller.getPlayerNetWorth())));

    BigDecimal growthRatio = controller.getPlayerGrowthRatio();
    BigDecimal startingCash = controller.getPlayerStartingMoney();
    String growthPercent = growthRatio.subtract(BigDecimal.ONE)
        .multiply(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString() + "%";
    boolean growthUp = growthRatio.compareTo(BigDecimal.ONE) >= 0;
    Label growthRatioValue = valueText(growthRatio.toPlainString() + "x");
    growthRatioValue.getStyleClass().add(growthUp ? "profile-value-up" : "profile-value-down");
    Label performanceValue = valueText(growthPercent);
    performanceValue.getStyleClass().add(growthUp ? "profile-value-up" : "profile-value-down");

    VBox statsCard = statCard("Run Statistics",
        statLine("Transactions", String.valueOf(controller.getTransactionCount())),
        statLine("Weeks Traded", String.valueOf(controller.getPlayerWeeksTraded())),
        statLine("Growth Ratio", growthRatioValue),
        statLine("Performance", performanceValue));

    PlayerStatus[] statusPath = PlayerStatus.values();
    int currentStatusIndex = status.ordinal();
    PlayerStatus defaultSelectedStatus = statusPath[Math.min(currentStatusIndex + 1, statusPath.length - 1)];

    HBox statusSteps = new HBox(8);
    statusSteps.getStyleClass().add("profile-status-steps");
    List<Label> stepLabels = new ArrayList<>();
    final PlayerStatus[] selectedStatusStep = { defaultSelectedStatus };

    ProgressBar statusProgressBar = new ProgressBar(0);
    statusProgressBar.getStyleClass().add("profile-status-progress");
    statusProgressBar.setMaxWidth(Double.MAX_VALUE);

    Label statusProgressText = new Label();
    statusProgressText.getStyleClass().add("profile-status-progress-text");

    final Label[] weeksMetricLabelRef = { null };
    final Label[] growthMetricLabelRef = { null };

    HBox statusMetrics = new HBox(10);
    statusMetrics.getStyleClass().add("profile-status-metrics");

    final Label[] weeksMetricChipRef = { null };
    final Label[] growthMetricChipRef = { null };

    Label levelingGuide = new Label();
    levelingGuide.getStyleClass().add("profile-leveling-guide");

    Runnable refreshStatusRequirement = () -> {
      ProfileController.StatusRequirementInfo requirement = controller.getStatusRequirement(selectedStatusStep[0]);
      statusProgressBar.setProgress(requirement.totalProgressRatio().doubleValue());

      statusProgressText.setText(
          "Total progress: "
              + String.valueOf(requirement.totalProgressRatio()
                  .multiply(BigDecimal.valueOf(100))
                  .setScale(0, RoundingMode.HALF_UP)
                  .intValue())
              + "%");

      if (weeksMetricLabelRef[0] != null && growthMetricLabelRef[0] != null) {
        weeksMetricLabelRef[0].setText(
            "Total weeks " + requirement.weeksCurrent() + "/" + requirement.weeksRequired());
        growthMetricLabelRef[0].setText(
            "Growth " + requirement.growthCurrent().stripTrailingZeros().toPlainString()
                + "x/" + requirement.growthRequired().stripTrailingZeros().toPlainString() + "x");
      }

      boolean weeksMet = requirement.weeksCurrent() >= requirement.weeksRequired();
      boolean growthMet = requirement.growthCurrent().compareTo(requirement.growthRequired()) >= 0;
      if (weeksMetricChipRef[0] != null && growthMetricChipRef[0] != null) {
        updateMetricChipState(weeksMetricChipRef[0], weeksMet);
        updateMetricChipState(growthMetricChipRef[0], growthMet);
      }

      int weeksRemaining = Math.max(0, requirement.weeksRequired() - requirement.weeksCurrent());
      BigDecimal growthRemaining = requirement.growthRequired()
          .subtract(requirement.growthCurrent())
          .max(BigDecimal.ZERO)
          .setScale(2, RoundingMode.HALF_UP);

        levelingGuide.setText(requirement.statusName() + ": "
          + (weeksRemaining == 0 && growthRemaining.compareTo(BigDecimal.ZERO) == 0
              ? "Target met."
              : "Remaining " + weeksRemaining + " week(s) traded and "
                  + growthRemaining.stripTrailingZeros().toPlainString() + "x growth."));
    };

    for (int i = 0; i < statusPath.length; i++) {
      PlayerStatus stepStatus = statusPath[i];
      Label stepLabel = new Label(formatStatusName(stepStatus));
      stepLabel.getStyleClass().add("profile-status-step");
      stepLabel.getStyleClass().add("profile-status-step-clickable");
      if (i < currentStatusIndex) {
        stepLabel.getStyleClass().add("profile-status-step-complete");
      } else if (i == currentStatusIndex) {
        stepLabel.getStyleClass().add("profile-status-step-current");
      } else {
        stepLabel.getStyleClass().add("profile-status-step-upcoming");
      }
      if (stepStatus == selectedStatusStep[0]) {
        stepLabel.getStyleClass().add("profile-status-step-selected");
      }
      stepLabel.setOnMouseClicked(e -> {
        selectedStatusStep[0] = stepStatus;
        for (Label other : stepLabels) {
          other.getStyleClass().remove("profile-status-step-selected");
        }
        stepLabel.getStyleClass().add("profile-status-step-selected");
        refreshStatusRequirement.run();
      });
      stepLabels.add(stepLabel);
      statusSteps.getChildren().add(stepLabel);
    }

    Label weeksMetricLabel = new Label();
    weeksMetricLabelRef[0] = weeksMetricLabel;
    Label growthMetricLabel = new Label();
    growthMetricLabelRef[0] = growthMetricLabel;
    Label weeksMetricChip = statusMetricChip(weeksMetricLabel.getText(), false);
    Label growthMetricChip = statusMetricChip(growthMetricLabel.getText(), false);
    weeksMetricChipRef[0] = weeksMetricChip;
    growthMetricChipRef[0] = growthMetricChip;
    weeksMetricChip.textProperty().bind(weeksMetricLabel.textProperty());
    growthMetricChip.textProperty().bind(growthMetricLabel.textProperty());
    statusMetrics.getChildren().addAll(weeksMetricChip, growthMetricChip);

    refreshStatusRequirement.run();

  VBox statusBody = new VBox(10, statusSteps, statusProgressBar, statusProgressText,
        statusMetrics, levelingGuide);
    statusBody.getStyleClass().add("profile-status-body");
    VBox statusCard = statCard("Status Progression", statusBody);
    statusCard.getStyleClass().add("profile-status-card");

    VBox favorites = new VBox(8);
    favorites.getStyleClass().add("profile-list");
    GridPane favoriteGrid = new GridPane();
    favoriteGrid.getStyleClass().add("profile-stock-grid");
    favoriteGrid.setHgap(12);
    favoriteGrid.setVgap(10);

    Runnable[] refreshFavoriteRows = { null };
    refreshFavoriteRows[0] = () -> {
      favorites.getChildren().clear();
      List<ProfileController.FavoriteStockView> favoriteStocks = controller.getFavoriteStocks();
      if (favoriteStocks.isEmpty()) {
        Label none = new Label("No favorite stocks yet. Star stocks in Market to pin them here.");
        none.getStyleClass().add("profile-empty");
        favorites.getChildren().add(none);
        return;
      }
      List<Node> favoriteCards = new ArrayList<>();
      for (ProfileController.FavoriteStockView favoriteStock : favoriteStocks) {
        Label symbol = badge(favoriteStock.symbol());
        Label company = new Label(favoriteStock.company());
        company.getStyleClass().add("profile-position-sub");
        company.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(company, Priority.ALWAYS);
        HBox header = new HBox(8, symbol, company);
        header.getStyleClass().add("profile-stock-row-main");
        header.setAlignment(Pos.CENTER_LEFT);
        Label owned = new Label(favoriteStock.owned() ? "Owned" : "Watchlist");
        owned.getStyleClass().add("profile-favorite-owned-chip");
        if (favoriteStock.owned()) {
          owned.getStyleClass().add("profile-favorite-owned-chip-owned");
        }
        VBox left = new VBox(4, header, owned);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMinWidth(PROFILE_CARD_LEFT_COLUMN_WIDTH);
        left.setPrefWidth(PROFILE_CARD_LEFT_COLUMN_WIDTH);
        left.setMaxWidth(PROFILE_CARD_LEFT_COLUMN_WIDTH);

        Label price = new Label(CurrencyFormatter.format(favoriteStock.currentPrice()));
        price.getStyleClass().add("profile-position-value");
        BigDecimal pct = favoriteStock.changePct();
        BigDecimal pctDisplay = pct.setScale(2, RoundingMode.HALF_UP);
        int pctSign = pctDisplay.compareTo(BigDecimal.ZERO);
        String pctText = pctSign == 0
            ? "0.00%"
            : (pctSign > 0 ? "+" : "") + pctDisplay.toPlainString() + "%";
        Label pctLabel = new Label(pctText);
        pctLabel.getStyleClass().add("profile-position-pnl");
        if (pctSign == 0) {
          pctLabel.getStyleClass().add("profile-value-neutral");
        } else {
          pctLabel.getStyleClass().add(pctSign > 0
              ? "profile-value-up"
              : "profile-value-down");
        }
        int trendSign = trendSignFromHistory(favoriteStock.priceHistory());
        StackPane trendChart = buildStockSparkline(
          favoriteStock.priceHistory(),
          trendSign);
        VBox metricColumn = new VBox(4, price, pctLabel);
        metricColumn.getStyleClass().add("profile-stock-metrics");
        metricColumn.setAlignment(Pos.CENTER_RIGHT);
        metricColumn.setMinWidth(PROFILE_CARD_RIGHT_COLUMN_WIDTH);
        metricColumn.setPrefWidth(PROFILE_CARD_RIGHT_COLUMN_WIDTH);
        metricColumn.setMaxWidth(PROFILE_CARD_RIGHT_COLUMN_WIDTH);
        StackPane centeredChart = new StackPane(trendChart);
        centeredChart.setMaxWidth(Double.MAX_VALUE);

        BorderPane row = new BorderPane();
        row.setLeft(left);
        row.setCenter(centeredChart);
        row.setRight(metricColumn);
        BorderPane.setAlignment(left, Pos.CENTER_LEFT);
        BorderPane.setAlignment(centeredChart, Pos.CENTER);
        BorderPane.setAlignment(metricColumn, Pos.CENTER_RIGHT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("profile-favorite-row");
        row.setOnMouseClicked(e -> onOpenStockFromPortfolio.accept(favoriteStock.symbol()));
        favoriteCards.add(row);
      }
      bindResponsiveGrid(favoriteGrid, favoriteCards, PROFILE_GRID_MIN_CARD_WIDTH);
      favorites.getChildren().add(favoriteGrid);
    };
    refreshFavoriteRows[0].run();
    VBox favoritesCard = statCard("Favorite Stocks", favorites);

    VBox holdings = new VBox(8);
    holdings.getStyleClass().add("profile-list");
    GridPane portfolioGrid = new GridPane();
    portfolioGrid.getStyleClass().add("profile-stock-grid");
    portfolioGrid.setHgap(12);
    portfolioGrid.setVgap(10);
    List<ProfileController.PortfolioPositionView> positions = controller.getPortfolioPositions();
    if (positions.isEmpty()) {
      Label none = new Label("No open positions yet.");
      none.getStyleClass().add("profile-empty");
      holdings.getChildren().add(none);
    } else {
      List<Node> positionCards = new ArrayList<>();
      for (ProfileController.PortfolioPositionView position : positions) {
        String symbolText = position.symbol();
        Label symbol = badge(symbolText);
        Label company = new Label(position.company());
        company.getStyleClass().add("profile-position-sub");
        company.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(company, Priority.ALWAYS);
        HBox header = new HBox(8, symbol, company);
        header.getStyleClass().add("profile-stock-row-main");
        header.setAlignment(Pos.CENTER_LEFT);
        Label line1 = valueText("Quantity " + position.quantity().toPlainString());
        Label line2 = new Label("Now " + CurrencyFormatter.format(position.currentPrice()));
        line2.getStyleClass().add("profile-position-sub");
        VBox left = new VBox(4, header, line1, line2);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMinWidth(PROFILE_CARD_LEFT_COLUMN_WIDTH);
        left.setPrefWidth(PROFILE_CARD_LEFT_COLUMN_WIDTH);
        left.setMaxWidth(PROFILE_CARD_LEFT_COLUMN_WIDTH);

        Label positionValue = new Label(CurrencyFormatter.format(position.marketValue()));
        positionValue.getStyleClass().add("profile-position-value");

        BigDecimal pnlDisplay = position.pnl().setScale(2, RoundingMode.HALF_UP);
        BigDecimal pnlPctDisplay = position.pnlPct().setScale(2, RoundingMode.HALF_UP);
        int pnlSign = pnlDisplay.compareTo(BigDecimal.ZERO);
        String pnlText = pnlSign == 0
            ? CurrencyFormatter.format(BigDecimal.ZERO) + " (0.00%)"
            : (pnlSign > 0 ? "+" : "")
                + CurrencyFormatter.format(pnlDisplay)
                + " (" + pnlPctDisplay.toPlainString() + "%)";
        Label pnlLabel = new Label(pnlText);
        pnlLabel.getStyleClass().add("profile-position-pnl");
        if (pnlSign == 0) {
          pnlLabel.getStyleClass().add("profile-value-neutral");
        } else {
          pnlLabel.getStyleClass().add(pnlSign > 0
              ? "profile-value-up"
              : "profile-value-down");
        }
        int trendSign = trendSignFromHistory(position.priceHistory());
        StackPane trendChart = buildStockSparkline(
            position.priceHistory(),
            trendSign);
        VBox metricColumn = new VBox(4, positionValue, pnlLabel);
        metricColumn.getStyleClass().add("profile-stock-metrics");
        metricColumn.setAlignment(Pos.CENTER_RIGHT);
        metricColumn.setMinWidth(PROFILE_CARD_RIGHT_COLUMN_WIDTH);
        metricColumn.setPrefWidth(PROFILE_CARD_RIGHT_COLUMN_WIDTH);
        metricColumn.setMaxWidth(PROFILE_CARD_RIGHT_COLUMN_WIDTH);
        StackPane centeredChart = new StackPane(trendChart);
        centeredChart.setMaxWidth(Double.MAX_VALUE);

        BorderPane row = new BorderPane();
        row.setLeft(left);
        row.setCenter(centeredChart);
        row.setRight(metricColumn);
        BorderPane.setAlignment(left, Pos.CENTER_LEFT);
        BorderPane.setAlignment(centeredChart, Pos.CENTER);
        BorderPane.setAlignment(metricColumn, Pos.CENTER_RIGHT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("profile-position-row");
        row.getStyleClass().add("profile-stock-nav-row");
        row.setOnMouseClicked(e -> onOpenStockFromPortfolio.accept(symbolText));
        positionCards.add(row);
      }
      bindResponsiveGrid(portfolioGrid, positionCards, PROFILE_GRID_MIN_CARD_WIDTH);
      holdings.getChildren().add(portfolioGrid);
    }
    VBox portfolioCard = statCard("Portfolio Positions", holdings);

    List<GameController.ReplayPoint> replayPoints = controller.getReplaySeries();
    if (AppConfig.PERFORMANCE_MODE.get()) {
      int capWeeks = Math.max(50, AppConfig.PERFORMANCE_MAX_HISTORY_WEEKS.get());
      if (replayPoints.size() > capWeeks) {
        replayPoints = new ArrayList<>(replayPoints.subList(replayPoints.size() - capWeeks,
            replayPoints.size()));
      }
    }
    final List<GameController.ReplayPoint> timelinePoints = replayPoints;
    final boolean noReplayHistoryYet = controller.getCurrentWeek() <= 1;
    final List<GameController.ReplayPoint> chartPoints = new ArrayList<>();
    if (!timelinePoints.isEmpty()) {
      chartPoints.add(timelinePoints.get(0));
      for (int i = 1; i < timelinePoints.size(); i++) {
        GameController.ReplayPoint previous = timelinePoints.get(i - 1);
        GameController.ReplayPoint current = timelinePoints.get(i);
        if (current.week() > previous.week()
            && current.netWorth().compareTo(previous.netWorth()) != 0) {
          chartPoints.add(new GameController.ReplayPoint(current.week() - 1, previous.netWorth()));
        }
        chartPoints.add(current);
      }
    }
    NumberAxis xAxis = new NumberAxis();
    NumberAxis yAxis = new NumberAxis();
    xAxis.setLabel("Week");
    yAxis.setLabel("Net Worth");

    int minWeek = timelinePoints.isEmpty() ? 1 : timelinePoints.get(0).week();
    int maxWeek = timelinePoints.isEmpty() ? 1 : timelinePoints.get(timelinePoints.size() - 1).week();
    int replayTickUnit = Math.max(1, (int) Math.ceil(Math.max(1, maxWeek - minWeek) / 10.0));
    xAxis.setAutoRanging(false);
    xAxis.setLowerBound(minWeek);
    xAxis.setUpperBound(Math.max(minWeek + 1, maxWeek));
    xAxis.setTickUnit(replayTickUnit);
    xAxis.setMinorTickVisible(false);
    xAxis.setMinorTickCount(0);
    xAxis.setTickLabelFormatter(new StringConverter<>() {
      @Override
      public String toString(Number object) {
        return String.valueOf((int) Math.round(object.doubleValue()));
      }

      @Override
      public Number fromString(String string) {
        throw new UnsupportedOperationException("Axis label parsing is not supported.");
      }
    });

    ProfileTimelineBuilder.AxisSetup axisSetup = ProfileTimelineBuilder.configureYAxis(
        yAxis,
        replayPoints,
        timelinePoints,
        chartPoints,
        startingCash,
        minWeek,
        maxWeek);
    double minNetWorth = axisSetup.minNetWorth();
    double maxNetWorth = axisSetup.maxNetWorth();
    final boolean flatTimeline = axisSetup.flatTimeline();

    LineChart<Number, Number> replayChart = new LineChart<>(xAxis, yAxis);
    replayChart.getStyleClass().add("profile-replay-chart");
    replayChart.setAnimated(false);
    replayChart.setLegendVisible(false);
    replayChart.setCreateSymbols(false);
    replayChart.setMinHeight(320);
    replayChart.setMouseTransparent(false);
    replayChart.setFocusTraversable(false);

    Label hoverValueChip = new Label();
    hoverValueChip.getStyleClass().add("profile-crosshair-label");
    hoverValueChip.setManaged(false);
    hoverValueChip.setMouseTransparent(true);
    hoverValueChip.setVisible(false);

    StackPane replayChartLayer = new StackPane(replayChart, hoverValueChip);
    replayChartLayer.getStyleClass().add("profile-crosshair-layer");

    if (noReplayHistoryYet) {
      Region emptyDim = new Region();
      emptyDim.getStyleClass().add("profile-replay-empty-dim");
      emptyDim.setMouseTransparent(true);
      StackPane.setAlignment(emptyDim, Pos.CENTER);

      Label emptyState = new Label("No history yet.");
      emptyState.setWrapText(true);
      emptyState.getStyleClass().addAll("profile-empty", "profile-replay-empty-text");
      emptyState.setStyle("-fx-text-alignment: center;");
      StackPane.setAlignment(emptyState, Pos.CENTER);
      replayChartLayer.getChildren().add(emptyDim);
      replayChartLayer.getChildren().add(emptyState);
    }

    XYChart.Series<Number, Number> replaySeries = new XYChart.Series<>();
    XYChart.Series<Number, Number> verticalMarkerSeries = new XYChart.Series<>();
    replayChart.getData().add(replaySeries);
    replayChart.getData().add(verticalMarkerSeries);

    Runnable applyReplayLineGradient = () -> {
      Node lineNode = replaySeries.getNode();
      if (lineNode == null) {
        return;
      }
      Node plotBackground = replayChart.lookup(".chart-plot-background");
      if (plotBackground == null) {
        return;
      }
      Node chartLine = lineNode.lookup(".chart-series-line");
      if (chartLine instanceof Path path) {
        if (flatTimeline) {
          path.setStroke(javafx.scene.paint.Color.web("#4ecb71"));
          path.setStrokeWidth(3);
          return;
        }
        double valueRange = yAxis.getUpperBound() - yAxis.getLowerBound();
        if (valueRange <= 0) {
          path.setStyle("-fx-stroke: #4ecb71; -fx-stroke-width: 3;");
          return;
        }

        double minOffset = Math.max(0.0, Math.min(1.0,
            (minNetWorth - yAxis.getLowerBound()) / valueRange));
        double maxOffset = Math.max(0.0, Math.min(1.0,
            (maxNetWorth - yAxis.getLowerBound()) / valueRange));

        Bounds plotBoundsScene = plotBackground.localToScene(plotBackground.getBoundsInLocal());
        Point2D bottomInPath = path.sceneToLocal(plotBoundsScene.getMinX(), plotBoundsScene.getMaxY());
        Point2D topInPath = path.sceneToLocal(plotBoundsScene.getMinX(), plotBoundsScene.getMinY());

        path.setStroke(new LinearGradient(
            0, bottomInPath.getY(),
            0, topInPath.getY(),
            false,
            CycleMethod.NO_CYCLE,
            List.of(
                new Stop(0.0, javafx.scene.paint.Color.web("#e05a5a")),
                new Stop(minOffset, javafx.scene.paint.Color.web("#e05a5a")),
                new Stop(maxOffset, javafx.scene.paint.Color.web("#4ecb71")),
                new Stop(1.0, javafx.scene.paint.Color.web("#4ecb71")))));
        path.setStrokeWidth(3);
      }
    };

    Label replayStatus = new Label();
    replayStatus.getStyleClass().add("profile-replay-status");

    Slider replaySlider = new Slider(minWeek, Math.max(minWeek, maxWeek), Math.max(minWeek, maxWeek));
    replaySlider.getStyleClass().add("profile-replay-slider");
    replaySlider.setMajorTickUnit(replayTickUnit);
    replaySlider.setMinorTickCount(0);
    replaySlider.setShowTickMarks(true);
    replaySlider.setShowTickLabels(true);
    installSliderFill(replaySlider);

    Region replaySliderLeftPad = new Region();
    Region replaySliderRightPad = new Region();
    HBox replaySliderRow = new HBox(replaySliderLeftPad, replaySlider, replaySliderRightPad);
    replaySliderRow.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(replaySlider, Priority.ALWAYS);
    replaySlider.setMaxWidth(Double.MAX_VALUE);

    Runnable alignReplaySliderToPlot = () -> Platform.runLater(() -> {
      Node plotBackground = replayChart.lookup(".chart-plot-background");
      final double extraSideWidth = 8.0;
      if (plotBackground == null) {
        replaySliderLeftPad.setMinWidth(0);
        replaySliderLeftPad.setPrefWidth(0);
        replaySliderRightPad.setMinWidth(0);
        replaySliderRightPad.setPrefWidth(0);
        return;
      }

      Bounds plotBoundsScene = plotBackground.localToScene(plotBackground.getBoundsInLocal());
      Bounds plotBounds = replayChartLayer.sceneToLocal(plotBoundsScene);
      double leftPad = Math.max(0, plotBounds.getMinX() - extraSideWidth);
      double rightPad = Math.max(0, replayChartLayer.getWidth() - plotBounds.getMaxX() - extraSideWidth);

      replaySliderLeftPad.setMinWidth(leftPad);
      replaySliderLeftPad.setPrefWidth(leftPad);
      replaySliderRightPad.setMinWidth(rightPad);
      replaySliderRightPad.setPrefWidth(rightPad);
    });

    final int[] hoverWeekRef = { -1 };
    final int[] lastRenderedWeekRef = { -1 };
    final int[] lastRenderedPointIndexRef = { -1 };
    final javafx.beans.property.ObjectProperty<XYChart.Data<Number, Number>> trailingPointRef =
      new javafx.beans.property.SimpleObjectProperty<>(null);

    final BigDecimal[] replayNetWorthByWeek = new BigDecimal[Math.max(1, maxWeek - minWeek + 1)];
    if (!timelinePoints.isEmpty()) {
      BigDecimal carry = timelinePoints.get(0).netWorth();
      int sourceIndex = 0;
      for (int week = minWeek; week <= maxWeek; week++) {
        while (sourceIndex + 1 < timelinePoints.size()
            && timelinePoints.get(sourceIndex + 1).week() <= week) {
          sourceIndex++;
          carry = timelinePoints.get(sourceIndex).netWorth();
        }
        replayNetWorthByWeek[week - minWeek] = carry;
      }
    }

    int renderCap = 2000;
    int renderStride = chartPoints.isEmpty() ? 1 : Math.max(1, chartPoints.size() / renderCap);

    java.util.function.IntFunction<BigDecimal> netWorthAtWeek = week -> {
      if (timelinePoints.isEmpty()) {
        return BigDecimal.ZERO;
      }
      int targetWeek = Math.max(minWeek, Math.min(maxWeek, week));
      BigDecimal value = replayNetWorthByWeek[targetWeek - minWeek];
      return value != null ? value : BigDecimal.ZERO;
    };

    Runnable refreshMarkerAndHover = () -> {
      if (noReplayHistoryYet) {
        verticalMarkerSeries.getData().clear();
        replayStatus.setText("No replay data available yet.");
        hoverValueChip.setVisible(false);
        return;
      }

      int nearestWeek = (int) Math.max(minWeek, Math.round(replaySlider.getValue()));
      int markerWeek = hoverWeekRef[0] > 0 ? hoverWeekRef[0] : nearestWeek;
      BigDecimal markerWorth = netWorthAtWeek.apply(markerWeek);

      verticalMarkerSeries.getData().clear();
      verticalMarkerSeries.getData().add(new XYChart.Data<>(markerWeek, yAxis.getLowerBound()));
      verticalMarkerSeries.getData().add(new XYChart.Data<>(markerWeek, yAxis.getUpperBound()));
      replayStatus.setText(
          "Week " + markerWeek + " • Net Worth " + CurrencyFormatter.format(markerWorth));

      Platform.runLater(() -> {
        Node markerNode = verticalMarkerSeries.getNode();
        if (markerNode != null) {
          Node markerLine = markerNode.lookup(".chart-series-line");
          if (markerLine instanceof Path markerPath) {
            markerPath.getStyleClass().add("profile-crosshair-line");
          }
        }

        if (hoverWeekRef[0] <= 0) {
          hoverValueChip.setVisible(false);
          return;
        }

        Node plotBackground = replayChart.lookup(".chart-plot-background");
        if (plotBackground == null) {
          hoverValueChip.setVisible(false);
          return;
        }

        hoverValueChip.setText(
            "Week " + markerWeek + "  " + CurrencyFormatter.format(markerWorth));

        Bounds plotBoundsScene = plotBackground.localToScene(plotBackground.getBoundsInLocal());
        Bounds plotBounds = replayChartLayer.sceneToLocal(plotBoundsScene);

        double xFrac = maxWeek <= minWeek ? 0.0 : (markerWeek - minWeek) / (double) (maxWeek - minWeek);
        double lineX = plotBounds.getMinX() + xFrac * plotBounds.getWidth();

        double yRange = yAxis.getUpperBound() - yAxis.getLowerBound();
        double yFrac = yRange <= 0 ? 0.5 : (markerWorth.doubleValue() - yAxis.getLowerBound()) / yRange;
        yFrac = Math.max(0.0, Math.min(1.0, yFrac));
        double lineY = plotBounds.getMaxY() - yFrac * plotBounds.getHeight();

        hoverValueChip.applyCss();
        hoverValueChip.autosize();

        double chipW = hoverValueChip.prefWidth(-1);
        double chipH = hoverValueChip.prefHeight(-1);
        double chipX = Math.min(Math.max(lineX + 8, plotBounds.getMinX() + 4),
            plotBounds.getMaxX() - chipW - 4);
        double chipY = Math.min(Math.max(lineY - chipH - 8, plotBounds.getMinY() + 4),
            plotBounds.getMaxY() - chipH - 4);

        hoverValueChip.resizeRelocate(chipX, chipY, chipW, chipH);
        hoverValueChip.setVisible(true);
      });
    };

    final Runnable refreshReplay = () -> {
      if (noReplayHistoryYet || timelinePoints.isEmpty()) {
        replaySeries.getData().clear();
        lastRenderedWeekRef[0] = -1;
        lastRenderedPointIndexRef[0] = -1;
        trailingPointRef.set(null);
        refreshMarkerAndHover.run();
        return;
      }

      double sliderValue = replaySlider.getValue();
      int floorWeek = (int) Math.max(minWeek, Math.floor(sliderValue));

      if (trailingPointRef.get() != null) {
        replaySeries.getData().remove(trailingPointRef.get());
        trailingPointRef.set(null);
      }

      if (lastRenderedWeekRef[0] < 0 || floorWeek < lastRenderedWeekRef[0]) {
        replaySeries.getData().clear();
        lastRenderedPointIndexRef[0] = -1;
        for (int i = 0; i < chartPoints.size(); i++) {
          GameController.ReplayPoint point = chartPoints.get(i);
          if (point.week() > floorWeek) {
            break;
          }
          if (i % renderStride == 0 || point.week() == floorWeek) {
            replaySeries.getData().add(new XYChart.Data<>(point.week(), point.netWorth().doubleValue()));
          }
          lastRenderedPointIndexRef[0] = i;
        }
      } else if (floorWeek > lastRenderedWeekRef[0]) {
        for (int i = lastRenderedPointIndexRef[0] + 1; i < chartPoints.size(); i++) {
          GameController.ReplayPoint point = chartPoints.get(i);
          if (point.week() > floorWeek) {
            break;
          }
          if (i % renderStride == 0 || point.week() == floorWeek) {
            replaySeries.getData().add(new XYChart.Data<>(point.week(), point.netWorth().doubleValue()));
          }
          lastRenderedPointIndexRef[0] = i;
        }
      }

      lastRenderedWeekRef[0] = floorWeek;

      double fraction = sliderValue - floorWeek;
      if (fraction > 0 && floorWeek < maxWeek) {
        BigDecimal p0 = netWorthAtWeek.apply(floorWeek);
        BigDecimal p1 = netWorthAtWeek.apply(floorWeek + 1);
        double interpY = p0.doubleValue() + fraction * (p1.doubleValue() - p0.doubleValue());
        trailingPointRef.set(new XYChart.Data<>(sliderValue, interpY));
        replaySeries.getData().add(trailingPointRef.get());
      }

      Platform.runLater(applyReplayLineGradient);
      refreshMarkerAndHover.run();
    };

    replaySlider.valueProperty().addListener((obs, oldV, newV) -> refreshReplay.run());
    replayChart.widthProperty().addListener((obs, oldV, newV) -> alignReplaySliderToPlot.run());
    replayChart.heightProperty().addListener((obs, oldV, newV) -> alignReplaySliderToPlot.run());
    replayChart.layoutBoundsProperty()
        .addListener((obs, oldV, newV) -> alignReplaySliderToPlot.run());

    replayChart.setOnMouseMoved(e -> {
      if (noReplayHistoryYet || timelinePoints.isEmpty()) {
        return;
      }
      Node plotBackground = replayChart.lookup(".chart-plot-background");
      if (plotBackground == null) {
        return;
      }
      Bounds plotBounds = plotBackground.localToScene(plotBackground.getBoundsInLocal());
      double sceneX = e.getSceneX();
      if (sceneX < plotBounds.getMinX() || sceneX > plotBounds.getMaxX() ||
          plotBounds.getWidth() <= 0) {
        if (hoverWeekRef[0] != -1) {
          hoverWeekRef[0] = -1;
          refreshMarkerAndHover.run();
        }
        return;
      }
      double frac = (sceneX - plotBounds.getMinX()) / plotBounds.getWidth();
      int week = (int) Math.round(minWeek + frac * (maxWeek - minWeek));
      week = Math.max(minWeek, Math.min(maxWeek, week));
      if (hoverWeekRef[0] != week) {
        hoverWeekRef[0] = week;
        refreshMarkerAndHover.run();
      }
    });
    replayChart.setOnMouseExited(e -> {
      if (hoverWeekRef[0] != -1) {
        hoverWeekRef[0] = -1;
        refreshMarkerAndHover.run();
      }
    });

    List<Double> replaySpeedOptions = new ArrayList<>(List.of(0.5, 1.0, 2.0, 4.0));
    final double tickMillis = 75.0;
    final double targetFinishSeconds = 6.0;
    final double ticksPerSecond = 1000.0 / tickMillis;
    final double weeksToTraverse = Math.max(1, maxWeek - minWeek);
    final double requiredMaxSpeedX = Math.max(4.0,
        (weeksToTraverse / (targetFinishSeconds * ticksPerSecond)) * 4.0);
    while (replaySpeedOptions.get(replaySpeedOptions.size() - 1) < requiredMaxSpeedX) {
      double prev = replaySpeedOptions.get(replaySpeedOptions.size() - 1);
      replaySpeedOptions.add(prev * 2.0);
    }

    // For short timelines, default below 1x so playback lasts at least targetFinishSeconds.
    double speedForSixSeconds = (weeksToTraverse * 4.0) / (targetFinishSeconds * ticksPerSecond);
    double defaultSpeedX = speedForSixSeconds < 1.0
        ? Math.max(0.1, speedForSixSeconds)
        : 1.0;
    boolean hasDefaultSpeed = replaySpeedOptions.stream()
        .anyMatch(s -> Math.abs(s - defaultSpeedX) < 1e-9);
    if (!hasDefaultSpeed) {
      replaySpeedOptions.add(defaultSpeedX);
      replaySpeedOptions.sort(Double::compareTo);
    }

    int defaultSpeedIndex = 0;
    for (int i = 0; i < replaySpeedOptions.size(); i++) {
      if (Math.abs(replaySpeedOptions.get(i) - defaultSpeedX) < 1e-9) {
        defaultSpeedIndex = i;
        break;
      }
    }
    final int[] speedIndex = {defaultSpeedIndex};
    final double[] speedStep = { replaySpeedOptions.get(speedIndex[0]) / 4.0 };
    final boolean[] playing = { false };
    final Button[] playBtnRef = new Button[1];
    final Timeline[] replayTimelineRef = new Timeline[1];
    final Timeline replayTimeline = new Timeline(new KeyFrame(Duration.millis(75), e -> {
      double next = replaySlider.getValue() + speedStep[0];
      if (next >= replaySlider.getMax()) {
        replaySlider.setValue(replaySlider.getMax());
        replayTimelineRef[0].stop();
        playing[0] = false;
        if (playBtnRef[0] != null) {
          playBtnRef[0].setText("▶  Play");
        }
      } else {
        replaySlider.setValue(next);
      }
    }));
    replayTimelineRef[0] = replayTimeline;
    replayTimeline.setCycleCount(Timeline.INDEFINITE);

    Button playBtn = new Button("▶  Play");
    playBtnRef[0] = playBtn;
    playBtn.getStyleClass().add("profile-action-btn");
    playBtn.setOnAction(e -> {
      if (noReplayHistoryYet || timelinePoints.isEmpty()) {
        return;
      }
      if (onReplayControlSelect != null) {
        onReplayControlSelect.run();
      }
      if (playing[0]) {
        replayTimeline.stop();
        playBtn.setText("▶  Play");
      } else {
        if (replaySlider.getValue() >= replaySlider.getMax()) {
          replaySlider.setValue(replaySlider.getMin());
        }
        replayTimeline.play();
        playBtn.setText("⏸  Pause");
      }
      playing[0] = !playing[0];
    });

    Button restartBtn = new Button("↺  Restart");
    restartBtn.getStyleClass().add("profile-secondary-btn");
    restartBtn.setOnAction(e -> {
      if (onReplayControlSelect != null) {
        onReplayControlSelect.run();
      }
      replayTimeline.stop();
      playing[0] = false;
      playBtn.setText("▶  Play");
      replaySlider.setValue(minWeek);
    });

    double initialSpeedX = replaySpeedOptions.get(speedIndex[0]);
    String initialSpeedLabel = initialSpeedX == Math.rint(initialSpeedX)
        ? String.valueOf((int) initialSpeedX)
        : String.format(Locale.US, "%.2f", initialSpeedX).replaceAll("0+$", "")
            .replaceAll("\\.$", "");
    Button speedBtn = new Button("⏩  " + initialSpeedLabel + "x");
    speedBtn.getStyleClass().add("profile-secondary-btn");
    speedBtn.setOnAction(e -> {
      if (onReplayControlSelect != null) {
        onReplayControlSelect.run();
      }
      speedIndex[0] = (speedIndex[0] + 1) % replaySpeedOptions.size();
      double speedX = replaySpeedOptions.get(speedIndex[0]);
      speedStep[0] = speedX / 4.0;
      String speedLabel = speedX == Math.rint(speedX)
          ? String.valueOf((int) speedX)
          : String.format(Locale.US, "%.2f", speedX).replaceAll("0+$", "")
            .replaceAll("\\.$", "");
      speedBtn.setText("⏩  " + speedLabel + "x");
    });

    if (noReplayHistoryYet || replayPoints.isEmpty()) {
      replaySlider.setDisable(true);
      playBtn.setDisable(true);
      restartBtn.setDisable(true);
      speedBtn.setDisable(true);
    }

    replayTimeline.setOnFinished(e -> {
      playing[0] = false;
      playBtn.setText("▶  Play");
    });

    replayChart.setOnMousePressed(e -> applyReplayLineGradient.run());
    replayChart.setOnMouseReleased(e -> applyReplayLineGradient.run());
    replayChart.setOnMouseClicked(e -> applyReplayLineGradient.run());
    replayChart.setOnContextMenuRequested(e -> {
      applyReplayLineGradient.run();
      e.consume();
    });

    // JavaFX can re-apply chart CSS on focus/style passes; keep line gradient
    // stable.
    replayChart.focusedProperty()
      .addListener((obs, oldV, focused) -> applyReplayLineGradient.run());
    replaySeries.nodeProperty().addListener((obs, oldV, newV) -> applyReplayLineGradient.run());
    replayChart.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene == null) {
        return;
      }
      newScene.focusOwnerProperty().addListener((o, oldOwner, newOwner) -> applyReplayLineGradient.run());
      newScene.windowProperty().addListener((o, oldWindow, newWindow) -> {
        if (newWindow != null) {
          newWindow.focusedProperty().addListener((wObs, wasFocused, isFocused) ->
              applyReplayLineGradient.run());
        }
      });
      if (newScene.getWindow() != null) {
        newScene.getWindow().focusedProperty().addListener((wObs, wasFocused, isFocused) ->
            applyReplayLineGradient.run());
      }
    });

    refreshReplay.run();
    alignReplaySliderToPlot.run();
    Platform.runLater(() -> {
      applyReplayLineGradient.run();
      refreshMarkerAndHover.run();
    });

    HBox replayControls = new HBox(10, playBtn, restartBtn, speedBtn, replayStatus);
    replayControls.setAlignment(Pos.CENTER_LEFT);

    VBox replayCard = statCard("Net Worth Timeline",
        new Label("Playback view from saved weekly snapshots."),
        replayChartLayer,
        replaySliderRow,
        replayControls);
    replayCard.getStyleClass().add("profile-replay-card");

    HBox cardsRow = new HBox(16, infoCard, statsCard);
    cardsRow.getStyleClass().add("profile-card-row");
    HBox.setHgrow(infoCard, Priority.ALWAYS);
    HBox.setHgrow(statsCard, Priority.ALWAYS);

    VBox content = new VBox(18, hero, statusCard, cardsRow, favoritesCard, portfolioCard, replayCard);
    content.getStyleClass().add("profile-content");

    ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.getStyleClass().add("profile-scroll");

    BorderPane root = new BorderPane();
    root.getStyleClass().add("profile-page");
    root.setTop(topBar);
    root.setCenter(scroll);

    StackPane wrapper = new StackPane(root);
    wrapper.getStyleClass().add("profile-page-wrapper");
    wrapper.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.ESCAPE) {
        if (nameChanged[0]) {
          commitPendingName.run();
        }
        onBackToGame.run();
        e.consume();
      } else if (e.getCode() == KeyCode.S) {
        if (nameChanged[0]) {
          commitPendingName.run();
        }
        if (onOpenSettings != null) {
          onOpenSettings.run();
          e.consume();
        }
      }
    });
    return wrapper;
  }

  private static VBox statCard(String title, javafx.scene.Node... lines) {
    Label heading = new Label(title);
    heading.getStyleClass().add("profile-card-title");
    VBox body = new VBox(8, lines);
    body.getStyleClass().add("profile-card-body");

    VBox card = new VBox(10, heading, body);
    card.getStyleClass().add("profile-card");
    card.setMaxWidth(Double.MAX_VALUE);
    return card;
  }

  private static void bindResponsiveGrid(GridPane grid, List<Node> cards, double minCardWidth) {
    Runnable relayout = () -> {
      double hGap = grid.getHgap();
      double available = grid.getWidth();
      if (available <= 0) {
        available = grid.prefWidth(-1);
      }

      int columns = Math.max(1, (int) Math.floor((available + hGap) / (minCardWidth + hGap)));
      columns = Math.min(columns, Math.max(1, cards.size()));
      columns = Math.min(columns, PROFILE_GRID_MAX_COLUMNS);

      grid.getChildren().clear();
      grid.getColumnConstraints().clear();
      for (int i = 0; i < columns; i++) {
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setPercentWidth(100.0 / columns);
        constraint.setHgrow(Priority.ALWAYS);
        constraint.setFillWidth(true);
        grid.getColumnConstraints().add(constraint);
      }

      for (int i = 0; i < cards.size(); i++) {
        Node card = cards.get(i);
        if (card instanceof Region region) {
          region.setMaxWidth(Double.MAX_VALUE);
        }
        grid.add(card, i % columns, i / columns);
      }
    };

    grid.widthProperty().addListener((obs, oldV, newV) -> relayout.run());
    Platform.runLater(relayout);
  }

  private static HBox statLine(String key, String value) {
    Label keyLbl = new Label(key);
    keyLbl.getStyleClass().add("profile-key");
    Label valueLbl = valueText(value);
    return statLine(keyLbl, valueLbl);
  }

  private static HBox statLine(String key, Label valueLbl) {
    Label keyLbl = new Label(key);
    keyLbl.getStyleClass().add("profile-key");
    return statLine(keyLbl, valueLbl);
  }

  private static HBox statLine(Label keyLbl, Label valueLbl) {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox row = new HBox(10, keyLbl, spacer, valueLbl);
    row.getStyleClass().add("profile-stat-row");
    return row;
  }

  private static Label valueText(String value) {
    Label label = new Label(value);
    label.getStyleClass().add("profile-value");
    return label;
  }

  private static Label badge(String value) {
    Label badge = new Label(value);
    badge.getStyleClass().add("profile-badge");
    return badge;
  }

  private static StackPane buildStockSparkline(List<BigDecimal> history, int trendSign) {
    Canvas canvas = new Canvas();
    StackPane frame = new StackPane(canvas);
    frame.getStyleClass().add("profile-stock-sparkline");
    frame.setMinWidth(120);
    frame.setPrefWidth(220);
    frame.setMaxWidth(Double.MAX_VALUE);
    frame.setMinHeight(76);
    frame.setPrefHeight(76);
    frame.setMaxHeight(76);
    canvas.setManaged(false);
    canvas.widthProperty().bind(frame.widthProperty());
    canvas.heightProperty().bind(frame.heightProperty());

    Runnable draw = () -> {
      double w = canvas.getWidth();
      double h = canvas.getHeight();
      if (w <= 0 || h <= 0) {
        return;
      }

      GraphicsContext gc = canvas.getGraphicsContext2D();
      gc.clearRect(0, 0, w, h);

      if (history == null || history.isEmpty()) {
        gc.setStroke(Color.web("#4a6899", 0.5));
        gc.setLineWidth(1.5);
        gc.strokeLine(8, h / 2.0, w - 8, h / 2.0);
        return;
      }

      if (history.size() == 1) {
        gc.setStroke(Color.web("#8fb6da", 0.6));
        gc.setLineWidth(1.8);
        gc.strokeLine(8, h / 2.0, w - 8, h / 2.0);
        return;
      }

      BigDecimal min = history.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
      BigDecimal max = history.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
      BigDecimal range = max.subtract(min);
      if (range.compareTo(BigDecimal.ZERO) == 0) {
        range = BigDecimal.ONE;
      }

      double padL = 8;
      double padR = 8;
      double padT = 8;
      double padB = 8;
      double cW = w - padL - padR;
      double cH = h - padT - padB;
      int n = history.size();

      double[] xs = new double[n];
      double[] ys = new double[n];
      for (int i = 0; i < n; i++) {
        xs[i] = padL + (i / (double) (n - 1)) * cW;
        double norm = history.get(i).subtract(min).divide(range, 6, RoundingMode.HALF_UP)
            .doubleValue();
        ys[i] = padT + cH - (norm * cH);
      }

        String lineHex = trendSign > 0 ? "#4ecb71" : trendSign < 0 ? "#e05a5a" : "#8fb6da";
      gc.setFill(new LinearGradient(0, padT, 0, padT + cH, false, CycleMethod.NO_CYCLE,
          new Stop(0, Color.web(lineHex, 0.30)),
          new Stop(1, Color.web(lineHex, 0.04))));
      gc.beginPath();
      gc.moveTo(xs[0], padT + cH);
      gc.lineTo(xs[0], ys[0]);
      for (int i = 1; i < n; i++) {
        gc.lineTo(xs[i], ys[i]);
      }
      gc.lineTo(xs[n - 1], padT + cH);
      gc.closePath();
      gc.fill();

      gc.setStroke(Color.web(lineHex, 0.92));
      gc.setLineWidth(2.2);
      gc.beginPath();
      gc.moveTo(xs[0], ys[0]);
      for (int i = 1; i < n; i++) {
        gc.lineTo(xs[i], ys[i]);
      }
      gc.stroke();

      gc.setFill(Color.web(lineHex));
      gc.fillOval(xs[n - 1] - 3.2, ys[n - 1] - 3.2, 6.4, 6.4);
      gc.setStroke(Color.web("#eaf3ff", 0.75));
      gc.setLineWidth(1.1);
      gc.strokeOval(xs[n - 1] - 3.2, ys[n - 1] - 3.2, 6.4, 6.4);
    };

    canvas.widthProperty().addListener((obs, oldV, newV) -> draw.run());
    canvas.heightProperty().addListener((obs, oldV, newV) -> draw.run());
    Platform.runLater(draw);
    return frame;
  }

  private static int trendSignFromHistory(List<BigDecimal> history) {
    if (history == null || history.size() < 2) {
      return 0;
    }
    BigDecimal first = history.get(0);
    BigDecimal last = history.get(history.size() - 1);
    if (first == null || last == null) {
      return 0;
    }
    BigDecimal delta = last.subtract(first).setScale(2, RoundingMode.HALF_UP);
    return delta.compareTo(BigDecimal.ZERO);
  }

  private static Label statusMetricChip(String text, boolean completed) {
    Label chip = new Label(text);
    chip.getStyleClass().add("profile-status-metric-chip");
    chip.getStyleClass().add(completed
        ? "profile-status-metric-chip-complete"
        : "profile-status-metric-chip-pending");
    return chip;
  }

  private static void updateMetricChipState(Label chip, boolean completed) {
    chip.getStyleClass().removeAll("profile-status-metric-chip-complete",
        "profile-status-metric-chip-pending");
    chip.getStyleClass().add(completed
        ? "profile-status-metric-chip-complete"
        : "profile-status-metric-chip-pending");
  }

  private static PlayerStatus requiredStatusForAvatar(String avatar) {
    return switch (avatar) {
      case "man-office-worker", "woman-office-worker" -> PlayerStatus.INVESTOR;
      case "man-in-tuxedo", "woman-in-tuxedo" -> PlayerStatus.SPECULATOR;
      default -> null;
    };
  }

  private static boolean isAvatarUnlocked(PlayerStatus currentStatus, PlayerStatus requiredStatus) {
    return requiredStatus == null || currentStatus.ordinal() >= requiredStatus.ordinal();
  }

  private static Tooltip buildLockedHintTooltip(String message) {
    Tooltip tooltip = new Tooltip(message);
    tooltip.setShowDelay(Duration.millis(120));
    tooltip.setShowDuration(Duration.INDEFINITE);
    tooltip.getStyleClass().add("profile-avatar-locked-tooltip");
    return tooltip;
  }

  private static String formatStatusName(PlayerStatus status) {
    if (status == null) {
      return "";
    }
    String lower = status.name().toLowerCase(Locale.ROOT);
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  private static void installSliderFill(Slider slider) {
    Runnable applyFill = () -> {
      Node track = slider.lookup(".track");
      if (!(track instanceof Region trackRegion)) {
        return;
      }

      double min = slider.getMin();
      double max = slider.getMax();
      double pct = max <= min ? 0.0 : (slider.getValue() - min) / (max - min);
      pct = Math.max(0.0, Math.min(1.0, pct));
      double stop = pct * 100.0;
      trackRegion.setStyle("-fx-background-color: linear-gradient(to right, "
          + "#f5a201 " + stop + "%, "
          + "#0f2d5e " + stop + "%);");
    };

    slider.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(applyFill));
    slider.valueProperty().addListener((obs, oldVal, newVal) -> applyFill.run());
    Platform.runLater(applyFill);
  }

}
