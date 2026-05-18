package edu.ntnu.idatt2003.g23.ui.views.csveditor;

import edu.ntnu.idatt2003.g23.io.CsvRow;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * In-game overlay for editing the price list of a single {@link CsvRow}.
 *
 * <p>Prices are shown one per row in a virtualized {@link ListView}, so even
 * stocks with tens of thousands of price points remain responsive. The overlay
 * is added directly to a {@link StackPane} container — no separate window is
 * opened. Changes are only committed to the row when the user clicks OK.</p>
 */
public final class PricesEditorDialog {

  /** Warn the user when the list exceeds this many entries. */
  private static final int LARGE_LIST_THRESHOLD = 10_000;
  private static final String OPEN_FLAG_KEY = "csv.pricesDialogOpen";

  private PricesEditorDialog() {
  }

  /**
   * Opens the prices sub-editor as an in-game overlay on top of {@code container}.
   *
   * @param container the {@link StackPane} that hosts the CSV editor page
   * @param row       the row whose prices will be edited
   * @param onCommit  called after the user clicks OK and the row has been updated
   */
  public static void open(StackPane container, CsvRow row, Runnable onCommit) {
    if (Boolean.TRUE.equals(container.getProperties().get(OPEN_FLAG_KEY))) {
      return;
    }
    container.getProperties().put(OPEN_FLAG_KEY, Boolean.TRUE);

    // Parse existing prices into a mutable observable list — one string per entry
    String raw = row.getPrices();
    List<String> initial = (raw == null || raw.isBlank())
        ? new ArrayList<>()
        : Arrays.stream(raw.split(";"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));

    ObservableList<String> items = FXCollections.observableArrayList(initial);

    Label pricesErrorLabel = new Label();
    pricesErrorLabel.getStyleClass().add("prices-dialog-error");
    pricesErrorLabel.managedProperty().bind(pricesErrorLabel.visibleProperty());
    pricesErrorLabel.setVisible(false);

    // ── Header ────────────────────────────────────────────────────────────
    Label titleLbl = new Label("Edit Prices");
    titleLbl.getStyleClass().add("prices-dialog-title");

    String symbol = row.getSymbol();
    javafx.scene.Node headerContent;
    if (symbol != null && !symbol.isBlank()) {
      Label symbolLbl = new Label(symbol);
      symbolLbl.getStyleClass().add("prices-dialog-symbol");
      HBox titleRow = new HBox(10, titleLbl, symbolLbl);
      titleRow.setAlignment(Pos.CENTER_LEFT);
      headerContent = titleRow;
    } else {
      headerContent = titleLbl;
    }

    VBox headerBox = new VBox(headerContent);
    headerBox.getStyleClass().add("prices-dialog-header");
    headerBox.setAlignment(Pos.CENTER_LEFT);

    // ── Count / warning label ─────────────────────────────────────────────
    Label countLabel = new Label();
    countLabel.getStyleClass().add("prices-dialog-count");

    Label warningLabel = new Label();
    warningLabel.getStyleClass().add("prices-dialog-warning");
    warningLabel.setWrapText(true);
    warningLabel.managedProperty().bind(warningLabel.visibleProperty());
    warningLabel.setVisible(false);

    Runnable updateLabels = () -> {
      int n = items.size();
      countLabel.setText(n + (n == 1 ? " price" : " prices"));
      boolean large = n > LARGE_LIST_THRESHOLD;
      warningLabel.setVisible(large);
      if (large) {
        warningLabel.setText("\u26A0  " + n
            + " price points — very large lists may be slow to edit.");
      }
    };
    updateLabels.run();

    // ── Virtualized ListView ──────────────────────────────────────────────
    ListView<String> listView = new ListView<>(items);
    listView.setEditable(true);
    listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    listView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
      private final TextField textField = new TextField();
      private final Label valueLabel = new Label();
      private final Button removeBtn = new Button("x");
      private final Region rowSpacer = new Region();
      private final HBox row = new HBox(8, valueLabel, rowSpacer, removeBtn);
      {
        textField.getStyleClass().add("csv-jump-field");
        textField.setOnAction(ev -> {
          if (isEditing()) commitEdit(textField.getText().trim().isEmpty() ? "0.00" : textField.getText().trim());
        });
        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
          if (!isFocused && isEditing()) {
            String value = textField.getText() == null ? "" : textField.getText().trim();
            commitEdit(value.isEmpty() ? "0.00" : value);
          }
        });
        textField.setOnKeyPressed(ev -> {
          if (ev.getCode() == KeyCode.ESCAPE) { cancelEdit(); ev.consume(); }
        });

        valueLabel.getStyleClass().add("csv-prices-summary");
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);

        removeBtn.getStyleClass().addAll("prices-dialog-tool-btn", "prices-dialog-delete-btn");
        removeBtn.setOnAction(ev -> {
          int idx = getIndex();
          if (idx >= 0 && idx < items.size()) {
            items.remove(idx);
            updateLabels.run();
          }
          ev.consume();
        });
      }
      @Override public void startEdit() {
        super.startEdit();
        textField.setText(getItem() == null ? "" : getItem());
        setGraphic(textField); setText(null);
        Platform.runLater(textField::selectAll);
      }
      @Override public void cancelEdit() { super.cancelEdit(); updateItem(getItem(), false); }
      @Override public void commitEdit(String val) {
        super.commitEdit(val);
        items.set(getIndex(), val);
        updateLabels.run();
      }
      @Override protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().remove("prices-dialog-cell-error");
        if (empty || item == null) { setText(null); setGraphic(null); return; }
        if (isEditing()) { textField.setText(item); setGraphic(textField); setText(null); }
        else {
          int week = getIndex() + 1;
          if (week <= 1) {
            valueLabel.setText("Price  \u00b7  " + item);
          } else {
            valueLabel.setText("Week " + week + "  \u00b7  " + item);
          }
          if (isInvalidPriceValue(item)) {
            getStyleClass().add("prices-dialog-cell-error");
          }
          setText(null);
          setGraphic(row);
        }
      }
    });
    listView.getStyleClass().add("prices-dialog-list");
    VBox.setVgrow(listView, Priority.ALWAYS);

    Button jumpErrorBtn = new Button("Jump To Error");
    jumpErrorBtn.getStyleClass().add("prices-dialog-tool-btn");

    Runnable refreshValidationUi = () -> {
      int badIndex = findFirstInvalidPriceIndex(items);
      if (badIndex >= 0) {
        jumpErrorBtn.setDisable(false);
        pricesErrorLabel.setVisible(true);
        pricesErrorLabel.setText(buildPriceEditorErrorText(badIndex, items.get(badIndex)));
      } else if (items.isEmpty()) {
        jumpErrorBtn.setDisable(true);
        pricesErrorLabel.setVisible(true);
        pricesErrorLabel.setText("No price set — add at least one price.");
      } else {
        jumpErrorBtn.setDisable(true);
        pricesErrorLabel.setVisible(false);
        pricesErrorLabel.setText("");
      }
      listView.refresh();
    };

    Runnable jumpToFirstError = () -> {
      int badIndex = findFirstInvalidPriceIndex(items);
      if (badIndex >= 0) {
        listView.scrollTo(badIndex);
        listView.getSelectionModel().clearAndSelect(badIndex);
      }
    };

    // ── Toolbar: count + add ──────────────────────────────────────────────
    Button addBtn = new Button("+ Add");
    addBtn.getStyleClass().add("prices-dialog-tool-btn");
    addBtn.setOnAction(e -> {
      items.add("0.00");
      int last = items.size() - 1;
      listView.scrollTo(last);
      listView.getSelectionModel().select(last);
      updateLabels.run();
      refreshValidationUi.run();
    });

    jumpErrorBtn.setOnAction(e -> jumpToFirstError.run());

    Region toolSpacer = new Region();
    HBox.setHgrow(toolSpacer, Priority.ALWAYS);

    HBox toolbar = new HBox(8, countLabel, toolSpacer, jumpErrorBtn, addBtn);
    toolbar.setAlignment(Pos.CENTER_LEFT);
    toolbar.getStyleClass().add("prices-dialog-toolbar");

    // ── Quick-add field ───────────────────────────────────────────────────
    TextField quickAddField = new TextField();
    quickAddField.setPromptText("Quick-add: type value + Enter\u2026");
    quickAddField.getStyleClass().add("csv-jump-field");
    HBox.setHgrow(quickAddField, Priority.ALWAYS);
    quickAddField.setOnAction(e -> {
      String val = quickAddField.getText().trim();
      if (!val.isEmpty()) {
        items.add(val);
        int last = items.size() - 1;
        listView.scrollTo(last);
        listView.getSelectionModel().select(last);
        updateLabels.run();
        refreshValidationUi.run();
        quickAddField.clear();
      }
    });

    HBox quickRow = new HBox(quickAddField);
    quickRow.getStyleClass().add("prices-dialog-quick-row");

    // ── Body ──────────────────────────────────────────────────────────────
    Label hintLabel = new Label("Double-click a price to edit inline.");
    hintLabel.getStyleClass().add("prices-dialog-hint");

  VBox body = new VBox(8, warningLabel, pricesErrorLabel, listView, hintLabel, quickRow, toolbar);
  body.getStyleClass().add("prices-dialog-body");
  VBox.setVgrow(listView, Priority.ALWAYS);

    // ── Footer ────────────────────────────────────────────────────────────
    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");

    Button okBtn = new Button("\u2714  OK");
    okBtn.getStyleClass().add("prices-dialog-confirm-btn");

    Region footerSpacer = new Region();
    HBox.setHgrow(footerSpacer, Priority.ALWAYS);

    HBox footer = new HBox(8, cancelBtn, footerSpacer, okBtn);
    footer.setAlignment(Pos.CENTER_RIGHT);
    footer.getStyleClass().add("prices-dialog-row");

    // ── Card ──────────────────────────────────────────────────────────────
    VBox card = new VBox(0, headerBox, body, footer);
    card.getStyleClass().add("prices-dialog-root");
    card.setMaxWidth(480);
    card.setPrefHeight(600);
    card.setMaxHeight(680);
    VBox.setVgrow(body, Priority.ALWAYS);

    Region backdrop = new Region();
    backdrop.getStyleClass().add("dialog-backdrop");

    StackPane popup = new StackPane(backdrop, card);
    StackPane.setAlignment(card, Pos.CENTER);

    Runnable dismiss = () -> {
      container.getChildren().remove(popup);
      container.getProperties().put(OPEN_FLAG_KEY, Boolean.FALSE);
    };

    cancelBtn.setOnAction(ev -> {
      ev.consume();
      dismiss.run();
    });
    backdrop.setOnMouseClicked(ev -> {
      ev.consume();
      dismiss.run();
    });

    okBtn.setOnAction(ev -> {
      ev.consume();

      refreshValidationUi.run();
      if (items.isEmpty()) {
        return;
      }

      int badIndex = findFirstInvalidPriceIndex(items);
      if (badIndex >= 0) {
        jumpToFirstError.run();
        return;
      }

      try {
        String joined = String.join(";", items);
        row.setPrices(joined);
        StockCsvLoader.validateRow(row);
        onCommit.run();
      } finally {
        dismiss.run();
      }
    });

    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    refreshValidationUi.run();
    container.getChildren().add(popup);
    popup.requestFocus();
  }

  private static int findFirstInvalidPriceIndex(List<String> items) {
    for (int i = 0; i < items.size(); i++) {
      if (isInvalidPriceValue(items.get(i))) {
        return i;
      }
    }
    return -1;
  }

  private static boolean isInvalidPriceValue(String raw) {
    String value = raw == null ? "" : raw.trim();
    if (value.isEmpty()) {
      return true;
    }
    try {
      BigDecimal bd = new BigDecimal(value);
      return bd.compareTo(BigDecimal.ZERO) <= 0;
    } catch (NumberFormatException ex) {
      return true;
    }
  }

  private static String buildPriceEditorErrorText(int index, String rawValue) {
    String value = rawValue == null ? "" : rawValue.trim();
    String detail;
    if (value.isEmpty()) {
      detail = "value is empty";
    } else {
      try {
        BigDecimal bd = new BigDecimal(value);
        detail = bd.compareTo(BigDecimal.ZERO) <= 0
            ? "must be greater than zero"
            : "is invalid";
      } catch (NumberFormatException ex) {
        detail = "isn't a valid number";
      }
    }
    String prefix = "Price \"" + (value.isEmpty() ? "(empty)" : value) + "\" " + detail;
    int week = index + 1;
    if (week <= 1) {
      return prefix;
    }
    return "Week " + week + " — " + prefix;
  }
}
