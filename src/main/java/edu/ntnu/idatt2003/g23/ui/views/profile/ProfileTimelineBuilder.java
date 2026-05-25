package edu.ntnu.idatt2003.g23.ui.views.profile;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import edu.ntnu.idatt2003.g23.ui.views.game.GameController;
import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;

/**
 * Shared builder logic for profile replay timeline axis configuration.
 */
public final class ProfileTimelineBuilder {

  private ProfileTimelineBuilder() {
  }

  public static AxisSetup configureYAxis(
      NumberAxis yAxis,
      List<GameController.ReplayPoint> replayPoints,
      List<GameController.ReplayPoint> timelinePoints,
      List<GameController.ReplayPoint> chartPoints,
      BigDecimal startingCash,
      int minWeek,
      int maxWeek) {

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

    final boolean flatTimeline = Math.abs(maxNetWorth - minNetWorth) < 1e-9;
    if (flatTimeline && !timelinePoints.isEmpty()) {
      chartPoints.clear();
      BigDecimal flatValue = timelinePoints.get(timelinePoints.size() - 1).netWorth();
      for (int week = minWeek; week <= maxWeek; week++) {
        chartPoints.add(new GameController.ReplayPoint(week, flatValue));
      }
    }

    double yLowerBound = Math.max(0.0, minNetWorth);
    double yUpperBound = Math.max(yLowerBound, maxNetWorth);

    double range = yUpperBound - yLowerBound;
    double pad = range > 0.0 ? range * 0.08 : Math.max(10.0, Math.abs(yUpperBound) * 0.05 + 1.0);

    yLowerBound = Math.max(0.0, yLowerBound - pad);
    yUpperBound = yUpperBound + pad;
    if (yUpperBound <= yLowerBound) {
      yUpperBound = yLowerBound + 1.0;
    }

    double tickUnit = niceTickUnit((yUpperBound - yLowerBound) / 4.0);
    yLowerBound = Math.max(0.0, snapToNearestTick(yLowerBound, tickUnit));
    yUpperBound = snapToNearestTick(yUpperBound, tickUnit);
    if (yLowerBound > minNetWorth) {
      yLowerBound = Math.max(0.0, yLowerBound - tickUnit);
    }
    if (yUpperBound < maxNetWorth) {
      yUpperBound += tickUnit;
    }
    if (yUpperBound <= yLowerBound) {
      yUpperBound = yLowerBound + tickUnit;
    }

    yAxis.setAutoRanging(false);
    yAxis.setLowerBound(yLowerBound);
    yAxis.setUpperBound(yUpperBound);
    yAxis.setTickUnit(tickUnit);

    DecimalFormat integerFormatter =
        new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
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

    return new AxisSetup(minNetWorth, maxNetWorth, flatTimeline);
  }

  private static double niceTickUnit(double rawUnit) {
    double unit = Math.max(1.0, rawUnit);
    double exponent = Math.floor(Math.log10(unit));
    double scale = Math.pow(10.0, exponent);
    double normalized = unit / scale;

    double[] candidates = { 1.0, 2.0, 2.5, 5.0, 10.0 };
    double best = candidates[0];
    double bestDistance = Math.abs(normalized - best);
    for (double candidate : candidates) {
      double distance = Math.abs(normalized - candidate);
      if (distance < bestDistance) {
        bestDistance = distance;
        best = candidate;
      }
    }
    return best * scale;
  }

  private static double snapToNearestTick(double value, double tickUnit) {
    return Math.round(value / tickUnit) * tickUnit;
  }

  public record AxisSetup(double minNetWorth, double maxNetWorth, boolean flatTimeline) {
  }
}
