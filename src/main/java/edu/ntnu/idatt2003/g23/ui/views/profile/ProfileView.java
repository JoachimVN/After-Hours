package edu.ntnu.idatt2003.g23.ui.views.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

import edu.ntnu.idatt2003.g23.model.PlayerStatus;
import edu.ntnu.idatt2003.g23.model.Share;
import edu.ntnu.idatt2003.g23.ui.util.CurrencyFormatter;
import edu.ntnu.idatt2003.g23.ui.views.game.GameController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.util.Duration;

public final class ProfileView {

    private static final List<String> BUILT_IN_AVATARS = List.of(
            "\uD83E\uDDD1", "\uD83D\uDC68", "\uD83D\uDC69", "\uD83E\uDDD4",
            "\uD83E\uDDD1\u200D\uD83D\uDCBC", "\uD83D\uDC69\u200D\uD83D\uDCBC",
            "\uD83D\uDC7D", "\uD83E\uDD16", "\uD83E\uDD84", "\uD83D\uDC3A"
    );

    private ProfileView() {
    }

    public static Parent build(
            ProfileController controller,
            Runnable onBackToGame,
            Runnable onOpenSettings,
            String currentAvatar,
            Consumer<String> onAvatarChanged) {

        String initialAvatar = (currentAvatar == null || currentAvatar.isBlank()) ? "\uD83E\uDDD1" : currentAvatar;

        Button backBtn = new Button("\u2190 Back To Market");
        backBtn.getStyleClass().add("profile-back-btn");
        backBtn.setOnAction(e -> onBackToGame.run());

        Button settingsBtn = new Button("\u2699 Settings");
        settingsBtn.getStyleClass().add("profile-settings-btn");
        settingsBtn.setOnAction(e -> onOpenSettings.run());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, backBtn, topSpacer, settingsBtn);
        topBar.getStyleClass().add("profile-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label avatarDisplay = new Label(initialAvatar);
        avatarDisplay.getStyleClass().add("profile-avatar-display");

        Label profileTitle = new Label(controller.getPlayerName());
        profileTitle.getStyleClass().add("profile-title");

        PlayerStatus status = controller.getPlayerStatus();
        Label profileSubtitle = new Label("Status: " + status.name() + "  •  Week " + controller.getCurrentWeek());
        profileSubtitle.getStyleClass().add("profile-subtitle");

        FlowPane avatarPicker = new FlowPane(8, 8);
        avatarPicker.getStyleClass().add("profile-avatar-picker");
        for (String avatar : BUILT_IN_AVATARS) {
            Button avatarBtn = new Button(avatar);
            avatarBtn.getStyleClass().add("profile-avatar-btn");
            if (avatar.equals(initialAvatar)) {
                avatarBtn.getStyleClass().add("profile-avatar-btn-active");
            }
            avatarBtn.setOnAction(e -> {
                avatarDisplay.setText(avatar);
                onAvatarChanged.accept(avatar);
                avatarPicker.getChildren().forEach(node -> node.getStyleClass().remove("profile-avatar-btn-active"));
                avatarBtn.getStyleClass().add("profile-avatar-btn-active");
            });
            avatarPicker.getChildren().add(avatarBtn);
        }

        VBox heroText = new VBox(6, profileTitle, profileSubtitle, avatarPicker);
        heroText.setAlignment(Pos.TOP_LEFT);

        HBox hero = new HBox(16, avatarDisplay, heroText);
        hero.getStyleClass().add("profile-hero");
        hero.setAlignment(Pos.CENTER_LEFT);

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

        VBox statsCard = statCard("Run Statistics",
            statLine("Transactions", String.valueOf(controller.getTransactionCount())),
            statLine("Weeks Traded", String.valueOf(controller.getPlayerWeeksTraded())),
                statLine("Growth Ratio", growthRatio.toPlainString() + "x"),
                statLine("Performance", growthPercent)
        );

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
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(1);
        xAxis.setUpperBound(Math.max(2, maxWeek));
        xAxis.setTickUnit(Math.max(1, maxWeek / 8.0));

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

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(center - deviation);
        yAxis.setUpperBound(center + deviation);
        yAxis.setTickUnit(Math.max(1.0, deviation / 4.0));

        LineChart<Number, Number> replayChart = new LineChart<>(xAxis, yAxis);
        replayChart.getStyleClass().add("profile-replay-chart");
        replayChart.setAnimated(false);
        replayChart.setLegendVisible(false);
        replayChart.setCreateSymbols(false);
        replayChart.setMinHeight(320);
        replayChart.setMouseTransparent(true);
        replayChart.setFocusTraversable(false);

        XYChart.Series<Number, Number> replaySeries = new XYChart.Series<>();
        replayChart.getData().add(replaySeries);

        Runnable applyReplayLineGradient = () -> Platform.runLater(() -> {
            Node lineNode = replaySeries.getNode();
            if (lineNode == null) return;
            Node chartLine = lineNode.lookup(".chart-series-line");
            if (chartLine instanceof Path path) {
                path.setStyle("-fx-stroke: linear-gradient(from 0% 100% to 0% 0%, #e05a5a 0%, #4ecb71 100%); -fx-stroke-width: 3;");
            }
        });

        Label replayStatus = new Label();
        replayStatus.getStyleClass().add("profile-replay-status");

        Slider replaySlider = new Slider(1, Math.max(1, maxWeek), Math.max(1, maxWeek));
        replaySlider.getStyleClass().add("profile-replay-slider");
        replaySlider.setMajorTickUnit(Math.max(1, maxWeek / 10.0));
        replaySlider.setMinorTickCount(4);
        replaySlider.setShowTickMarks(true);
        replaySlider.setShowTickLabels(true);

        final Runnable refreshReplay = () -> {
            if (replayPoints.isEmpty()) {
                replaySeries.getData().clear();
                replayStatus.setText("No replay data available yet.");
                return;
            }
            int week = (int) Math.max(1, Math.round(replaySlider.getValue()));
            replaySeries.getData().clear();
            for (GameController.ReplayPoint point : replayPoints) {
                if (point.week() > week) break;
                replaySeries.getData().add(new XYChart.Data<>(point.week(), point.netWorth().doubleValue()));
            }
            GameController.ReplayPoint point = replayPoints.get(Math.min(week - 1, replayPoints.size() - 1));
            replayStatus.setText("Week " + point.week() + " • Net Worth " + CurrencyFormatter.format(point.netWorth()));

            applyReplayLineGradient.run();
        };

        replaySlider.valueProperty().addListener((obs, oldV, newV) -> refreshReplay.run());

        final double[] speedStep = {1.0};
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
            if (speedStep[0] == 0.5) speedStep[0] = 1.0;
            else if (speedStep[0] == 1.0) speedStep[0] = 2.0;
            else if (speedStep[0] == 2.0) speedStep[0] = 4.0;
            else speedStep[0] = 0.5;
            speedBtn.setText("Speed: " + (speedStep[0] == 0.5 ? "0.5" : String.valueOf((int) speedStep[0])) + "x");
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

        HBox replayControls = new HBox(10, playBtn, restartBtn, speedBtn, replayStatus);
        replayControls.setAlignment(Pos.CENTER_LEFT);

        VBox replayCard = statCard("Net Worth Timeline",
            new Label("Playback view from saved weekly snapshots."),
            replayChart,
                replaySlider,
                replayControls
        );
        replayCard.getStyleClass().add("profile-replay-card");

        HBox cardsRow = new HBox(16, infoCard, statsCard);
        cardsRow.getStyleClass().add("profile-card-row");
        HBox.setHgrow(infoCard, Priority.ALWAYS);
        HBox.setHgrow(statsCard, Priority.ALWAYS);

        VBox content = new VBox(18, hero, cardsRow, portfolioCard, replayCard);
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
}
