package edu.ntnu.idatt2003.g23.ui.views.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Path;
import javafx.util.StringConverter;
import javafx.util.Duration;

public final class ProfileView {

    private ProfileView() {
    }

    public static Parent build(
            ProfileController controller,
            Runnable onBackToGame,
            Runnable onOpenSettings,
            String currentAvatar,
            Consumer<String> onAvatarChanged,
            Consumer<String> onNameChanged) {

        List<String> avatarNames = AvatarUtil.loadSelectableAvatarNames();
        String initialAvatar = (currentAvatar == null || currentAvatar.isBlank())
            ? "bust-in-silhouette"
            : AvatarUtil.normalizeAvatarStem(currentAvatar);
        final String[] selectedAvatar = {initialAvatar};

        // Track name changes for auto-save
        final String[] originalName = {controller.getPlayerName()};
        final boolean[] nameChanged = {false};

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
        int weeksUsingChick = 0;
        String displayedAvatar = controller.getDisplayedPlayerAvatar();
        try {
            chickPhaseUnlocked = controller.getChickPhaseUnlocked();
            weeksUsingChick = controller.getWeeksUsingChickAvatar();
        } catch (Exception ignored) {}
        final int chickPhaseUnlockedValue = chickPhaseUnlocked;
        final int weeksUsingChickValue = weeksUsingChick;
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

        TilePane avatarPicker = new TilePane();
        avatarPicker.getStyleClass().add("profile-avatar-picker");
        avatarPicker.setHgap(8);
        avatarPicker.setVgap(8);
        avatarPicker.setPrefColumns(8);
        avatarPicker.setPrefRows(2);
        avatarPicker.setMaxWidth(8 * 38 + 7 * 8);
        for (String avatar : avatarNames) {
            Button avatarBtn = new Button();
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
                Tooltip tooltip = new Tooltip("Unlocks at " + formatStatusName(requiredStatus));
                tooltip.setShowDelay(Duration.millis(120));
                tooltip.setShowDuration(Duration.seconds(20));
                avatarBtn.setTooltip(tooltip);
            }
            if (avatar.equals(initialAvatar)) {
                avatarBtn.getStyleClass().add("profile-avatar-btn-active");
            }
            avatarBtn.setOnAction(e -> {
                if (!unlocked) {
                    return;
                }
                avatarPicker.getChildren().forEach(node -> node.getStyleClass().remove("profile-avatar-btn-active"));
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
            avatarPicker.getChildren().add(avatarBtn);
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
            statLine("Net Worth", CurrencyFormatter.format(controller.getPlayerNetWorth()))
        );

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
            statLine("Performance", performanceValue)
        );

        PlayerStatus[] statusPath = PlayerStatus.values();
        int currentStatusIndex = status.ordinal();
        PlayerStatus nextStatus = currentStatusIndex < statusPath.length - 1
            ? statusPath[currentStatusIndex + 1]
            : null;

        BigDecimal weeksProgressValue = controller.getPlayerWeeksProgress();
        BigDecimal netWorthProgressValue = controller.getPlayerNetWorthProgress();
        BigDecimal statusProgressValue = controller.getPlayerStatusProgress();
        int weeksTargetForNextStatus = controller.getPlayerWeeksTargetForNextStatus();
        BigDecimal growthTargetForNextStatus = controller.getPlayerGrowthTargetForNextStatus();

        double weeksProgress = weeksProgressValue.doubleValue();
        double netWorthProgress = netWorthProgressValue.doubleValue();
        double statusProgress = statusProgressValue.doubleValue();

        HBox statusSteps = new HBox(8);
        statusSteps.getStyleClass().add("profile-status-steps");
        for (int i = 0; i < statusPath.length; i++) {
            PlayerStatus stepStatus = statusPath[i];
            Label stepLabel = new Label(formatStatusName(stepStatus));
            stepLabel.getStyleClass().add("profile-status-step");
            if (i < currentStatusIndex) {
                stepLabel.getStyleClass().add("profile-status-step-complete");
            } else if (i == currentStatusIndex) {
                stepLabel.getStyleClass().add("profile-status-step-current");
            } else {
                stepLabel.getStyleClass().add("profile-status-step-upcoming");
            }
            statusSteps.getChildren().add(stepLabel);
        }

        Label statusTargetLine = new Label(nextStatus == null
            ? "Maximum rank reached. Every avatar tier is unlocked."
            : "Progress toward " + formatStatusName(nextStatus));
        statusTargetLine.getStyleClass().add("profile-status-target");

        ProgressBar statusProgressBar = new ProgressBar(statusProgress);
        statusProgressBar.getStyleClass().add("profile-status-progress");
        statusProgressBar.setMaxWidth(Double.MAX_VALUE);

        Label statusProgressText = new Label(nextStatus == null
            ? "100%"
            : String.valueOf(statusProgressValue
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue()) + "%");
        statusProgressText.getStyleClass().add("profile-status-progress-text");

        HBox statusMetrics = new HBox(10,
            statusMetricChip(
                "Weeks " + controller.getPlayerWeeksTraded() + "/" + weeksTargetForNextStatus,
                weeksProgress >= 1.0),
            statusMetricChip(
                "Growth " + growthRatio.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + "x/" + growthTargetForNextStatus.toPlainString() + "x",
                netWorthProgress >= 1.0)
        );
        statusMetrics.getStyleClass().add("profile-status-metrics");

        VBox statusBody = new VBox(10, statusSteps, statusTargetLine, statusProgressBar, statusProgressText, statusMetrics);
        statusBody.getStyleClass().add("profile-status-body");
        VBox statusCard = statCard("Status Progression", statusBody);
        statusCard.getStyleClass().add("profile-status-card");

        VBox holdings = new VBox(8);
        holdings.getStyleClass().add("profile-list");
        List<Share> shares = controller.getPortfolioShares();
        if (shares.isEmpty()) {
            Label none = new Label("No open positions yet.");
            none.getStyleClass().add("profile-empty");
            holdings.getChildren().add(none);
        } else {
            for (Share share : shares) {
            BigDecimal quantity = share.getQuantity();
            BigDecimal avgPrice = share.getPurchasePrice();
            BigDecimal currentPrice = share.getStock().getSalesPrice();
            BigDecimal costBasis = avgPrice.multiply(quantity);
            BigDecimal marketValue = currentPrice.multiply(quantity);
            BigDecimal pnl = marketValue.subtract(costBasis);
            BigDecimal pnlPct = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : pnl.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            Label symbol = badge(share.getStock().getSymbol());
            Label line1 = valueText("Quantity " + quantity.toPlainString() + "  •  Average "
                + CurrencyFormatter.format(avgPrice));
            Label line2 = new Label("Now " + CurrencyFormatter.format(currentPrice));
            line2.getStyleClass().add("profile-position-sub");
            VBox left = new VBox(3, line1, line2);

            Label positionValue = new Label(CurrencyFormatter.format(marketValue));
            positionValue.getStyleClass().add("profile-position-value");

            String pnlText = (pnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                + CurrencyFormatter.format(pnl)
                + " (" + pnlPct.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%)";
            Label pnlLabel = new Label(pnlText);
            pnlLabel.getStyleClass().add("profile-position-pnl");
            pnlLabel.getStyleClass().add(pnl.compareTo(BigDecimal.ZERO) >= 0
                ? "profile-value-up"
                : "profile-value-down");
            VBox right = new VBox(3, positionValue, pnlLabel);
            right.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(10, symbol, left, spacer, right);
            row.getStyleClass().add("profile-position-row");
                holdings.getChildren().add(row);
            }
        }
        VBox portfolioCard = statCard("Portfolio Positions", holdings);

        List<GameController.ReplayPoint> replayPoints = controller.getReplaySeries();
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Week");
        yAxis.setLabel("Net Worth");

        int maxWeek = replayPoints.isEmpty() ? 1 : replayPoints.get(replayPoints.size() - 1).week();
        int replayTickUnit = Math.max(1, (int) Math.ceil(Math.max(1, maxWeek - 1) / 10.0));
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(1);
        xAxis.setUpperBound(Math.max(2, maxWeek));
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

        double minNetWorth = replayPoints.stream()
            .map(GameController.ReplayPoint::netWorth)
            .mapToDouble(BigDecimal::doubleValue)
            .min()
            .orElse(startingCash.doubleValue());
        double maxNetWorth = replayPoints.stream()
            .map(GameController.ReplayPoint::netWorth)
            .mapToDouble(BigDecimal::doubleValue)
            .max()
            .orElse(startingCash.doubleValue());
        double center = startingCash.doubleValue();
        double deviation = Math.max(Math.abs(maxNetWorth - center), Math.abs(center - minNetWorth));
        if (deviation < 1.0) {
            deviation = Math.max(10.0, Math.abs(center) * 0.05 + 1.0);
        }

        double yLowerBound = center - deviation;
        double yUpperBound = center + deviation;
        if (yLowerBound < 0.0) {
            yLowerBound = 0.0;
            yUpperBound = Math.max(yUpperBound, maxNetWorth);
        }
        if (yUpperBound <= yLowerBound) {
            yUpperBound = yLowerBound + 1.0;
        }

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(yLowerBound);
        yAxis.setUpperBound(yUpperBound);
        yAxis.setTickUnit(Math.max(1.0, (yUpperBound - yLowerBound) / 4.0));
        DecimalFormat integerFormatter = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return integerFormatter.format(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                throw new UnsupportedOperationException("Axis label parsing is not supported.");
            }
        });

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

        XYChart.Series<Number, Number> replaySeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> verticalMarkerSeries = new XYChart.Series<>();
        replayChart.getData().add(replaySeries);
        replayChart.getData().add(verticalMarkerSeries);

        Runnable applyReplayLineGradient = () -> Platform.runLater(() -> {
            Node lineNode = replaySeries.getNode();
            if (lineNode == null) return;
            Node plotBackground = replayChart.lookup(".chart-plot-background");
            if (plotBackground == null) return;
            Node chartLine = lineNode.lookup(".chart-series-line");
            if (chartLine instanceof Path path) {
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
                    new Stop(1.0, javafx.scene.paint.Color.web("#4ecb71"))
                )));
            path.setStrokeWidth(3);
            }
        });

        Label replayStatus = new Label();
        replayStatus.getStyleClass().add("profile-replay-status");

        Slider replaySlider = new Slider(1, Math.max(1, maxWeek), Math.max(1, maxWeek));
        replaySlider.getStyleClass().add("profile-replay-slider");
        replaySlider.setMajorTickUnit(replayTickUnit);
        replaySlider.setMinorTickCount(0);
        replaySlider.setShowTickMarks(true);
        replaySlider.setShowTickLabels(true);

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

        final int[] hoverWeekRef = {-1};

        final Runnable refreshReplay = () -> {
            if (replayPoints.isEmpty()) {
                replaySeries.getData().clear();
                verticalMarkerSeries.getData().clear();
                replayStatus.setText("No replay data available yet.");
                return;
            }
            double sliderValue = replaySlider.getValue();
            int floorWeek = (int) Math.max(1, Math.floor(sliderValue));
            int nearestWeek = (int) Math.max(1, Math.round(sliderValue));
            int markerWeek = hoverWeekRef[0] > 0 ? hoverWeekRef[0] : nearestWeek;
            replaySeries.getData().clear();
            verticalMarkerSeries.getData().clear();
            for (GameController.ReplayPoint point : replayPoints) {
                if (point.week() > floorWeek) break;
                replaySeries.getData().add(new XYChart.Data<>(point.week(), point.netWorth().doubleValue()));
            }
            // Interpolate a fractional trailing point for smooth animation
            double fraction = sliderValue - floorWeek;
            if (fraction > 0 && floorWeek >= 1 && floorWeek < replayPoints.size()) {
                GameController.ReplayPoint p0 = replayPoints.get(floorWeek - 1);
                GameController.ReplayPoint p1 = replayPoints.get(floorWeek);
                double interpY = p0.netWorth().doubleValue()
                        + fraction * (p1.netWorth().doubleValue() - p0.netWorth().doubleValue());
                replaySeries.getData().add(new XYChart.Data<>(sliderValue, interpY));
            }
            verticalMarkerSeries.getData().add(new XYChart.Data<>(markerWeek, yAxis.getLowerBound()));
            verticalMarkerSeries.getData().add(new XYChart.Data<>(markerWeek, yAxis.getUpperBound()));
            GameController.ReplayPoint point = replayPoints.get(Math.min(markerWeek - 1, replayPoints.size() - 1));
            replayStatus.setText("Week " + point.week() + " • Net Worth " + CurrencyFormatter.format(point.netWorth()));

            applyReplayLineGradient.run();

            Platform.runLater(() -> {
                Node markerNode = verticalMarkerSeries.getNode();
                if (markerNode != null) {
                    Node markerLine = markerNode.lookup(".chart-series-line");
                    if (markerLine instanceof Path markerPath) {
                        markerPath.getStyleClass().add("profile-crosshair-line");
                    }
                }

                if (hoverWeekRef[0] > 0) {
                    Node plotBackground = replayChart.lookup(".chart-plot-background");
                    if (plotBackground == null) {
                        hoverValueChip.setVisible(false);
                        return;
                    }
                    GameController.ReplayPoint hoverPoint = replayPoints.get(Math.min(markerWeek - 1, replayPoints.size() - 1));
                    hoverValueChip.setText("Week " + markerWeek + "  " + CurrencyFormatter.format(hoverPoint.netWorth()));

                    Bounds plotBoundsScene = plotBackground.localToScene(plotBackground.getBoundsInLocal());
                    Bounds plotBounds = replayChartLayer.sceneToLocal(plotBoundsScene);

                    double xFrac = maxWeek <= 1 ? 0.0 : (markerWeek - 1.0) / (maxWeek - 1.0);
                    double lineX = plotBounds.getMinX() + xFrac * plotBounds.getWidth();

                    double yRange = yAxis.getUpperBound() - yAxis.getLowerBound();
                    double yFrac = yRange <= 0 ? 0.5 : (hoverPoint.netWorth().doubleValue() - yAxis.getLowerBound()) / yRange;
                    yFrac = Math.max(0.0, Math.min(1.0, yFrac));
                    double lineY = plotBounds.getMaxY() - yFrac * plotBounds.getHeight();

                    hoverValueChip.applyCss();
                    hoverValueChip.autosize();

                    double chipW = hoverValueChip.prefWidth(-1);
                    double chipH = hoverValueChip.prefHeight(-1);
                    double chipX = Math.min(Math.max(lineX + 8, plotBounds.getMinX() + 4), plotBounds.getMaxX() - chipW - 4);
                    double chipY = Math.min(Math.max(lineY - chipH - 8, plotBounds.getMinY() + 4), plotBounds.getMaxY() - chipH - 4);

                    hoverValueChip.resizeRelocate(chipX, chipY, chipW, chipH);
                    hoverValueChip.setVisible(true);
                } else {
                    hoverValueChip.setVisible(false);
                }
            });
        };

        replaySlider.valueProperty().addListener((obs, oldV, newV) -> refreshReplay.run());
        replayChart.widthProperty().addListener((obs, oldV, newV) -> alignReplaySliderToPlot.run());
        replayChart.heightProperty().addListener((obs, oldV, newV) -> alignReplaySliderToPlot.run());
        replayChart.layoutBoundsProperty().addListener((obs, oldV, newV) -> alignReplaySliderToPlot.run());

        replayChart.setOnMouseMoved(e -> {
            if (replayPoints.isEmpty()) return;
            Node plotBackground = replayChart.lookup(".chart-plot-background");
            if (plotBackground == null) return;
            Bounds plotBounds = plotBackground.localToScene(plotBackground.getBoundsInLocal());
            double sceneX = e.getSceneX();
            if (sceneX < plotBounds.getMinX() || sceneX > plotBounds.getMaxX() || plotBounds.getWidth() <= 0) {
                if (hoverWeekRef[0] != -1) {
                    hoverWeekRef[0] = -1;
                    refreshReplay.run();
                }
                return;
            }
            double frac = (sceneX - plotBounds.getMinX()) / plotBounds.getWidth();
            int week = (int) Math.round(1 + frac * (maxWeek - 1));
            week = Math.max(1, Math.min(maxWeek, week));
            if (hoverWeekRef[0] != week) {
                hoverWeekRef[0] = week;
                refreshReplay.run();
            }
        });
        replayChart.setOnMouseExited(e -> {
            if (hoverWeekRef[0] != -1) {
                hoverWeekRef[0] = -1;
                refreshReplay.run();
            }
        });

        List<Double> replaySpeedOptions = new ArrayList<>(List.of(0.5, 1.0, 2.0, 4.0));
        if (maxWeek > 100) replaySpeedOptions.add(8.0);
        if (maxWeek > 200) replaySpeedOptions.add(16.0);
        if (maxWeek > 500) replaySpeedOptions.add(32.0);
        if (maxWeek > 1000) replaySpeedOptions.add(64.0);

        final int[] speedIndex = {Math.max(0, replaySpeedOptions.indexOf(1.0))};
        final double[] speedStep = {replaySpeedOptions.get(speedIndex[0]) / 4.0};
        final boolean[] playing = {false};
        final Timeline[] replayTimelineRef = new Timeline[1];
        final Timeline replayTimeline = new Timeline(new KeyFrame(Duration.millis(75), e -> {
            double next = replaySlider.getValue() + speedStep[0];
            if (next >= replaySlider.getMax()) {
                replaySlider.setValue(replaySlider.getMax());
                replayTimelineRef[0].stop();
                playing[0] = false;
            } else {
                replaySlider.setValue(next);
            }
        }));
        replayTimelineRef[0] = replayTimeline;
        replayTimeline.setCycleCount(Timeline.INDEFINITE);

        Button playBtn = new Button("Play");
        playBtn.getStyleClass().add("profile-action-btn");
        playBtn.setOnAction(e -> {
            if (replayPoints.isEmpty()) return;
            if (playing[0]) {
                replayTimeline.stop();
                playBtn.setText("Play");
            } else {
                replayTimeline.play();
                playBtn.setText("Pause");
            }
            playing[0] = !playing[0];
        });

        Button restartBtn = new Button("Restart");
        restartBtn.getStyleClass().add("profile-secondary-btn");
        restartBtn.setOnAction(e -> {
            replayTimeline.stop();
            playing[0] = false;
            playBtn.setText("Play");
            replaySlider.setValue(1);
        });

        Button speedBtn = new Button("Speed: 1x");
        speedBtn.getStyleClass().add("profile-secondary-btn");
        speedBtn.setOnAction(e -> {
            speedIndex[0] = (speedIndex[0] + 1) % replaySpeedOptions.size();
            double speedX = replaySpeedOptions.get(speedIndex[0]);
            speedStep[0] = speedX / 4.0;
            String speedLabel = speedX == Math.rint(speedX)
                ? String.valueOf((int) speedX)
                : String.valueOf(speedX);
            speedBtn.setText("Speed: " + speedLabel + "x");
        });

        if (replayPoints.isEmpty()) {
            replaySlider.setDisable(true);
            playBtn.setDisable(true);
            restartBtn.setDisable(true);
            speedBtn.setDisable(true);
        }

        // JavaFX can re-apply chart CSS on focus/style passes; keep line gradient stable.
        replayChart.focusedProperty().addListener((obs, oldV, focused) -> applyReplayLineGradient.run());

        refreshReplay.run();
        alignReplaySliderToPlot.run();

        HBox replayControls = new HBox(10, playBtn, restartBtn, speedBtn, replayStatus);
        replayControls.setAlignment(Pos.CENTER_LEFT);

        VBox replayCard = statCard("Net Worth Timeline",
            new Label("Playback view from saved weekly snapshots."),
                replayChartLayer,
                replaySliderRow,
                replayControls
        );
        replayCard.getStyleClass().add("profile-replay-card");

        HBox cardsRow = new HBox(16, infoCard, statsCard);
        cardsRow.getStyleClass().add("profile-card-row");
        HBox.setHgrow(infoCard, Priority.ALWAYS);
        HBox.setHgrow(statsCard, Priority.ALWAYS);

        VBox content = new VBox(18, hero, statusCard, cardsRow, portfolioCard, replayCard);
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

    private static Label statusMetricChip(String text, boolean completed) {
        Label chip = new Label(text);
        chip.getStyleClass().add("profile-status-metric-chip");
        chip.getStyleClass().add(completed
            ? "profile-status-metric-chip-complete"
            : "profile-status-metric-chip-pending");
        return chip;
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

    private static String formatStatusName(PlayerStatus status) {
        if (status == null) {
            return "";
        }
        String lower = status.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
