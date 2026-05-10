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
    // Parse existing prices into a mutable observable list — one string per entry
    String raw = row.getPrices();
    List<String> initial = (raw == null || raw.isBlank())
        ? new ArrayList<>()
        : Arrays.stream(raw.split(";"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));

    ObservableList<String> items = FXCollections.observableArrayList(initial);

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
      {
        textField.getStyleClass().add("csv-jump-field");
        textField.setOnAction(ev -> {
          if (isEditing()) commitEdit(textField.getText().trim().isEmpty() ? "0.00" : textField.getText().trim());
        });
        textField.setOnKeyPressed(ev -> {
          if (ev.getCode() == KeyCode.ESCAPE) { cancelEdit(); ev.consume(); }
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
        if (empty || item == null) { setText(null); setGraphic(null); return; }
        if (isEditing()) { textField.setText(item); setGraphic(textField); setText(null); }
        else { setGraphic(null); setText("Week " + (getIndex() + 1) + "  \u00b7  " + item); }
      }
    });
    listView.getStyleClass().add("prices-dialog-list");
    VBox.setVgrow(listView, Priority.ALWAYS);

    // ── Toolbar: count + add + delete ─────────────────────────────────────
    Button addBtn = new Button("+ Add");
    addBtn.getStyleClass().add("prices-dialog-tool-btn");
    addBtn.setOnAction(e -> {
      items.add("0.00");
      int last = items.size() - 1;
      listView.scrollTo(last);
      listView.getSelectionModel().select(last);
      updateLabels.run();
    });

    Button deleteBtn = new Button("\u2715 Delete");
    deleteBtn.getStyleClass().addAll("prices-dialog-tool-btn", "prices-dialog-delete-btn");
    deleteBtn.disableProperty().bind(
        listView.getSelectionModel().selectedItemProperty().isNull());
    deleteBtn.setOnAction(e -> {
      int idx = listView.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        items.remove(idx);
        updateLabels.run();
      }
    });

    Region toolSpacer = new Region();
    HBox.setHgrow(toolSpacer, Priority.ALWAYS);

    HBox toolbar = new HBox(8, countLabel, toolSpacer, addBtn, deleteBtn);
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
        quickAddField.clear();
      }
    });

    HBox quickRow = new HBox(quickAddField);
    quickRow.getStyleClass().add("prices-dialog-quick-row");

    // ── Body ──────────────────────────────────────────────────────────────
    Label hintLabel = new Label("Double-click a price to edit inline.");
    hintLabel.getStyleClass().add("prices-dialog-hint");

  VBox body = new VBox(8, warningLabel, listView, hintLabel, quickRow, toolbar);
  body.getStyleClass().add("prices-dialog-body");
  VBox.setVgrow(listView, Priority.ALWAYS);

    // ── Footer ────────────────────────────────────────────────────────────
    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");

    Button okBtn = new Button("\u2714  OK");
    okBtn.getStyleClass().add("dialog-confirm-buy-btn");

    Region footerSpacer = new Region();
    HBox.setHgrow(footerSpacer, Priority.ALWAYS);

    HBox footer = new HBox(8, cancelBtn, footerSpacer, okBtn);
    footer.setAlignment(Pos.CENTER_RIGHT);
    footer.getStyleClass().add("dialog-btn-row");

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

    Runnable dismiss = () -> container.getChildren().remove(popup);

    cancelBtn.setOnAction(ev -> dismiss.run());
    backdrop.setOnMouseClicked(ev -> dismiss.run());

    okBtn.setOnAction(ev -> {
      String joined = String.join(";", items);
      row.setPrices(joined);
      StockCsvLoader.validateRow(row);
      onCommit.run();
      dismiss.run();
    });

    popup.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
      if (ev.getCode() == KeyCode.ESCAPE) {
        dismiss.run();
        ev.consume();
      }
    });

    container.getChildren().add(popup);
    popup.requestFocus();
  }
}
