package edu.ntnu.idatt2003.g23.ui.views.csveditor;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Platform;

import edu.ntnu.idatt2003.g23.io.CsvParseResult;
import edu.ntnu.idatt2003.g23.io.CsvRow;
import edu.ntnu.idatt2003.g23.io.StockCsvLoader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;

/**
 * In-game CSV editor that opens whenever a stock CSV file fails to parse.
 *
 * <p>All rows (valid and invalid) are shown in an editable {@link TableView}.
 * Invalid rows are highlighted in red.  The user must fix all errors before
 * the "Save &amp; Continue" button becomes active.  On confirmation the fixed
 * data is written to a new file chosen by the user, then passed to
 * {@code onSuccess} so the game can start.</p>
 */
public final class CsvEditorView {

  private CsvEditorView() {
  }

  /**
   * Key stored in {@link TableView#getProperties()} to allow a programmatic edit to start.
   */
  private static final String EDIT_ALLOWED_KEY = "csv-edit-allowed";

  /**
   * Builds the CSV editor view.
   *
   * @param result     the parse result to display and edit
   * @param onCancel   called when the user presses Back
   * @param onContinue called with the validated {@link CsvRow} list when the
   *                   user chooses "Continue (no save)"
   * @param onSaveAs   called with the validated row list and the chosen
   *                   destination file when the user chooses "Save As &amp; Continue";
   *                   the controller is responsible for writing and starting the game
   * @param onReset    called when the user clicks "Reset to Defaults" to reload original data
   */
  public static Parent build(CsvParseResult result, Runnable onCancel,
                             Consumer<List<CsvRow>> onContinue,
                             BiConsumer<List<CsvRow>, File> onSaveAs,
                             Runnable onReset) {

    ObservableList<CsvRow> rows = createEditableRows(result);

    // Single source of truth for whether any row has an error.
    // Updated explicitly whenever rows are edited, added, or removed.
    SimpleBooleanProperty hasErrors = new SimpleBooleanProperty(
        rows.stream().anyMatch(CsvRow::hasError));

    BorderPane root = new BorderPane();
    root.getStyleClass().addAll("home-page", "background-overlay");
    // Wrap in a StackPane so in-game overlays (e.g. the prices sub-editor) can be layered on top.
    StackPane overlayRoot = new StackPane(root);

    // ── Top bar ───────────────────────────────────────────────────────────
    Button backButton = new Button("\u2190 Back");
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> onCancel.run());

    Label titleLabel = new Label("CSV Editor");
    titleLabel.getStyleClass().add("page-title");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox topBar = new HBox(12, backButton, spacer, titleLabel);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(24, 32, 8, 32));
    root.setTop(topBar);

    // ── Error summary label ───────────────────────────────────────────────
    Label errorCountLabel = new Label();
    errorCountLabel.getStyleClass().add("csv-error-count");

    // Tooltip removed as per user request

    // Shared refresh: call this after any mutation that may change error state.
    Runnable refreshState = () -> {
      updateErrorCount(errorCountLabel, rows);
      hasErrors.set(rows.stream().anyMatch(CsvRow::hasError));
    };

    refreshState.run();

    // ── Table ─────────────────────────────────────────────────────────────
    TableView<CsvRow> table = new TableView<>(rows);
    table.setEditable(true);
    table.getStyleClass().add("csv-editor-table");
    table.getSelectionModel().setCellSelectionEnabled(true);

    Label placeholder = new Label(
        "No rows yet \u2014 click '+ Add Row' to create one.");
    placeholder.getStyleClass().add("sub-tagline");
    table.setPlaceholder(placeholder);

    // Line # (read-only)
    TableColumn<CsvRow, Number> lineCol = new TableColumn<>("#");
    lineCol.setCellValueFactory(c -> c.getValue().lineNumberProperty());
    lineCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Number item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : String.valueOf(getIndex() + 1));
      }
    });
    lineCol.setPrefWidth(52);
    lineCol.setMinWidth(52);
    lineCol.setMaxWidth(80);
    lineCol.setSortable(false);
    lineCol.setEditable(false);

    // Symbol
    TableColumn<CsvRow, String> symbolCol = new TableColumn<>("Symbol");
    symbolCol.setCellValueFactory(c -> c.getValue().symbolProperty());
    symbolCol.setPrefWidth(100);
    symbolCol.setSortable(false);
    symbolCol.setOnEditCommit(e -> {
      e.getRowValue().setSymbol(e.getNewValue());
      StockCsvLoader.validateRow(e.getRowValue());
      table.refresh();
      refreshState.run();
    });

    // Company
    TableColumn<CsvRow, String> companyCol = new TableColumn<>("Company");
    companyCol.setCellValueFactory(c -> c.getValue().companyProperty());
    companyCol.setPrefWidth(220);
    companyCol.setSortable(false);
    companyCol.setOnEditCommit(e -> {
      e.getRowValue().setCompany(e.getNewValue());
      StockCsvLoader.validateRow(e.getRowValue());
      table.refresh();
      refreshState.run();
    });

    // Prices — summary cell with an "Edit…" button; inline text editing is intentionally
    // removed because a semicolon-joined string with thousands of entries is unworkable.
    TableColumn<CsvRow, String> pricesCol = new TableColumn<>("Prices");
    pricesCol.setCellValueFactory(c -> c.getValue().pricesProperty());
    pricesCol.setPrefWidth(220);
    pricesCol.setSortable(false);
    pricesCol.setEditable(false);
    pricesCol.setCellFactory(col -> new TableCell<>() {
      private final Label summaryLbl = new Label();
      private final Button editBtn = new Button("\u270e");
      private final Region spacer = new Region();
      private final HBox box = new HBox(6, summaryLbl, spacer, editBtn);

      private void openPricesEditor() {
        int idx = getIndex();
        if (idx < 0 || idx >= getTableView().getItems().size()) {
          return;
        }
        CsvRow row = getTableView().getItems().get(idx);
        PricesEditorDialog.open(overlayRoot, row, () -> {
          table.refresh();
          refreshState.run();
        });
      }

      {
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        summaryLbl.getStyleClass().add("csv-prices-summary");
        editBtn.getStyleClass().addAll("csv-inline-icon-btn", "csv-inline-edit-btn");
        editBtn.setTooltip(new Tooltip("Edit price history"));
        editBtn.setOnAction(e -> openPricesEditor());
      }

      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
          setOnMouseClicked(null);
          return;
        }
        summaryLbl.setText(buildPriceSummary(item));
        setGraphic(box);
        setOnMouseClicked(event -> {
          if (event.getClickCount() == 2) {
            openPricesEditor();
            event.consume();
          }
        });
      }
    });

    // Ordered list of editable columns — used by smartCell for Tab/Enter navigation.
    // Prices are excluded: they are edited via the sub-editor dialog, not inline.
    List<TableColumn<CsvRow, String>> editableCols = List.of(symbolCol, companyCol);
    symbolCol.setCellFactory(tc -> smartCell("symbol", table, editableCols));
    companyCol.setCellFactory(tc -> smartCell("company", table, editableCols));

    // Table-level keyboard navigation — only active when no cell is being edited
    table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (table.getEditingCell() != null) {
        return; // Let the editing cell handle it
      }
      var selectedCells = table.getSelectionModel().getSelectedCells();
      if (selectedCells.isEmpty()) {
        return;
      }
      var pos = selectedCells.get(0);
      int ri = pos.getRow();
      int ci = editableCols.indexOf(pos.getTableColumn());
      int rc = table.getItems().size();
      if (event.getCode() == KeyCode.TAB) {
        if (event.isShiftDown()) {
          if (ci > 0) {
            selectCell(table, ri, editableCols.get(ci - 1));
          } else if (ri > 0) {
            selectCell(table, ri - 1, editableCols.get(editableCols.size() - 1));
          }
        } else {
          if (ci >= 0 && ci < editableCols.size() - 1) {
            selectCell(table, ri, editableCols.get(ci + 1));
          } else if (ri < rc - 1) {
            selectCell(table, ri + 1, editableCols.get(0));
          } else if (ci < 0) {
            selectCell(table, ri, editableCols.get(0));
          }
        }
        event.consume();
      } else if (event.getCode() == KeyCode.ENTER) {
        TableColumn<CsvRow, ?> col = pos.getTableColumn() != null
            ? (TableColumn<CsvRow, ?>) pos.getTableColumn() : editableCols.get(0);
        if (event.isShiftDown()) {
          if (ri > 0) {
            selectCell(table, ri - 1, col);
          }
        } else {
          if (ri < rc - 1) {
            selectCell(table, ri + 1, col);
          }
        }
        event.consume();
      }
    });

    // Error
    TableColumn<CsvRow, String> errorCol = new TableColumn<>("Error");
    errorCol.setCellValueFactory(c -> c.getValue().errorMessageProperty());
    errorCol.setPrefWidth(300);
    errorCol.setSortable(false);
    errorCol.setEditable(false);
    errorCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setStyle("");
        if (empty || item == null || item.isBlank()) {
          setText(null);
          setTooltip(null);
        } else {
          setText(item);
          setTooltip(new Tooltip(item));
          setStyle("-fx-text-fill: #ff6b6b;");
        }
      }
    });
    errorCol.visibleProperty().bind(hasErrors);

    // Skip (per-row button)
    TableColumn<CsvRow, Void> skipRowCol = new TableColumn<>("");
    skipRowCol.setPrefWidth(38);
    skipRowCol.setMinWidth(38);
    skipRowCol.setMaxWidth(44);
    skipRowCol.setSortable(false);
    skipRowCol.setEditable(false);
    skipRowCol.setCellFactory(col -> new TableCell<>() {
      private final Button btn = new Button("\u2715");

      {
        btn.getStyleClass().addAll("csv-inline-icon-btn", "csv-inline-delete-btn");
        btn.setTooltip(new Tooltip("Delete row"));
        btn.setOnAction(e -> {
          CsvRow row = getTableView().getItems().get(getIndex());
          rows.remove(row);
          refreshState.run();
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
          setGraphic(null);
          return;
        }
        setGraphic(btn);
      }
    });

    table.getColumns()
        .addAll(List.of(lineCol, symbolCol, companyCol, pricesCol, errorCol, skipRowCol));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    // Row factory: highlight error rows
    table.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
      @Override
      protected void updateItem(CsvRow item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll("csv-row-error", "csv-row-ok");
        if (!empty && item != null) {
          if (item.hasError()) {
            getStyleClass().add("csv-row-error");
          } else {
            getStyleClass().add("csv-row-ok");
          }
        }
      }
    });

    // ── Navigation toolbar ────────────────────────────────────────────────
    Button prevErrBtn = new Button("\u2191 Prev Error");
    prevErrBtn.getStyleClass().addAll("secondary-button", "csv-nav-button");
    prevErrBtn.disableProperty().bind(hasErrors.not());
    prevErrBtn.setOnAction(e -> navigateError(table, rows, -1));

    Button nextErrBtn = new Button("Next Error \u2193");
    nextErrBtn.getStyleClass().addAll("secondary-button", "csv-nav-button");
    nextErrBtn.disableProperty().bind(hasErrors.not());
    nextErrBtn.setOnAction(e -> navigateError(table, rows, +1));

    Label navSep = new Label("|");
    navSep.getStyleClass().add("csv-nav-sep");

    TextField jumpField = new TextField();
    jumpField.setPromptText("Jump to line\u2026");
    jumpField.setPrefWidth(140);
    jumpField.getStyleClass().add("csv-jump-field");
    jumpField.setOnAction(e -> {
      handleJumpToLine(table, rows, jumpField.getText().trim());
      jumpField.clear();
    });

    Button jumpBtn = new Button("Go \u2192");
    jumpBtn.getStyleClass().addAll("secondary-button", "csv-nav-button");
    jumpBtn.setOnAction(e -> {
      handleJumpToLine(table, rows, jumpField.getText().trim());
      jumpField.clear();
    });

    HBox navBar = new HBox(8, prevErrBtn, nextErrBtn, navSep, jumpField, jumpBtn);
    navBar.setAlignment(Pos.CENTER_LEFT);
    navBar.getStyleClass().add("csv-nav-bar");

    // ── Bottom bar ────────────────────────────────────────────────────────
    Label hintLabel = new Label(
        "Double-click Symbol / Company to edit \u2014 click \u201cEdit\u2026\u201d in the Prices column to manage price points.");
    hintLabel.getStyleClass().add("sub-tagline");

    // Add Row button
    Button addRowBtn = new Button("+ Add Row");
    addRowBtn.getStyleClass().add("secondary-button");
    addRowBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
    addRowBtn.setOnAction(e -> {
      int nextLine = rows.size() + 1;
      CsvRow newRow = new CsvRow(nextLine, "", "", "", "");
      rows.add(newRow);
      table.getSelectionModel().select(newRow);
      table.scrollTo(newRow);
      refreshState.run();
    });

    // "Skip all broken rows" — removes every row that still has an error
    Button skipAllBtn = new Button("\u2715  Skip All Broken Rows");
    skipAllBtn.getStyleClass().addAll("secondary-button", "csv-skip-button");
    skipAllBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
    skipAllBtn.disableProperty().bind(hasErrors.not());
    skipAllBtn.setOnAction(e -> {
      rows.removeIf(CsvRow::hasError);
      refreshState.run();
    });

    // "Reset to Defaults" — reloads the original parse result
    Button resetBtn = new Button("\u27F3  Reset to Defaults");
    resetBtn.getStyleClass().addAll("secondary-button", "csv-skip-button");
    resetBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
    resetBtn.setOnAction(e -> onReset.run());

    // "Continue without saving" — starts game in memory, no file picker
    Button continueBtn = new Button("\u25B6  Continue (no save)");
    continueBtn.getStyleClass().add("secondary-button");
    continueBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 13;");
    continueBtn.disableProperty().bind(hasErrors);
    continueBtn.setOnAction(e -> {
      if (rows.stream().anyMatch(CsvRow::hasError)) {
        return;
      }
      onContinue.accept(List.copyOf(rows));
    });

    // "Save As & Continue" — shows file picker, delegates I/O to the controller
    Button saveBtn = new Button("\u2714  Save As & Continue");
    saveBtn.getStyleClass().add("start-button");
    saveBtn.setStyle("-fx-pref-height: 44; -fx-font-size: 15;");
    saveBtn.disableProperty().bind(hasErrors);
    saveBtn.setOnAction(e -> {
      if (rows.stream().anyMatch(CsvRow::hasError)) {
        return;
      }
      FileChooser fc = new FileChooser();
      fc.setTitle("Save Fixed CSV As\u2026");
      fc.setInitialFileName("stocks_fixed.csv");
      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
      File target = fc.showSaveDialog(root.getScene().getWindow());
      if (target == null) {
        return; // user cancelled
      }
      onSaveAs.accept(List.copyOf(rows), target);
    });

    Region bottomSpacer = new Region();
    HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

    HBox bottomBar =
        new HBox(12, hintLabel, addRowBtn, resetBtn, bottomSpacer, skipAllBtn, continueBtn, saveBtn);
    bottomBar.setAlignment(Pos.CENTER_LEFT);
    bottomBar.setPadding(new Insets(12, 32, 24, 32));

    // ── Assemble ──────────────────────────────────────────────────────────
    VBox centerBox = new VBox(8, errorCountLabel, navBar, table);
    VBox.setVgrow(table, Priority.ALWAYS);
    centerBox.setPadding(new Insets(0, 32, 0, 32));

    root.setCenter(centerBox);
    root.setBottom(bottomBar);

    // Escape → back (only when no overlay is open and no cell is being edited)
    root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.ESCAPE
          && table.getEditingCell() == null
          && overlayRoot.getChildren().size() == 1) {
        onCancel.run();
        e.consume();
      }
    });

    return overlayRoot;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static ObservableList<CsvRow> createEditableRows(CsvParseResult result) {
    return FXCollections.observableArrayList(
        result.getRows().stream()
            .map(CsvEditorView::copyRow)
            .collect(Collectors.toList()));
  }

  private static CsvRow copyRow(CsvRow source) {
    CsvRow copy = new CsvRow(
        source.getLineNumber(),
        source.getSymbol(),
        source.getCompany(),
        source.getPrices(),
        source.getErrorMessage());
    copy.setErrorColumn(source.getErrorColumn());
    return copy;
  }

  private static void updateErrorCount(Label label, ObservableList<CsvRow> rows) {
    long errors = rows.stream().filter(CsvRow::hasError).count();
    if (errors == 0) {
      label.setText("\u2714  All rows are valid \u2014 you can save and continue.");
      label.getStyleClass().removeAll("csv-error-count-bad");
      label.getStyleClass().add("csv-error-count-ok");
    } else {
      label.setText("\u2716  " + errors + " row" + (errors == 1 ? "" : "s")
          + " still " + (errors == 1 ? "has" : "have") + " errors.");
      label.getStyleClass().removeAll("csv-error-count-ok");
      label.getStyleClass().add("csv-error-count-bad");
    }
  }

  /**
   * A smart {@link TextFieldTableCell} with Google Sheets-style interaction:
   * <ul>
   *   <li>Single-click → select only (no edit mode)</li>
   *   <li>Double-click → enter edit mode</li>
   *   <li>Tab / Shift+Tab (editing) → commit + move to next/prev editable column, wrapping rows</li>
   *   <li>Enter / Shift+Enter (editing) → commit + move down/up same column</li>
   *   <li>Arrow keys (editing) → normal text-cursor movement (not consumed)</li>
   *   <li>Escape (editing) → discard changes and exit edit mode</li>
   *   <li>Click any cell (editing) → commits current edit</li>
   *   <li>Error-column highlighting and ⚠ badge</li>
   * </ul>
   */
  private static TextFieldTableCell<CsvRow, String> smartCell(
      String colId,
      TableView<CsvRow> table,
      List<TableColumn<CsvRow, String>> editableCols) {

    return new TextFieldTableCell<>(new DefaultStringConverter()) {

      private TextField editField = null;
      private boolean cancelViaEscape = false;

      /** Commit current edit then start editing the specified cell. */
      private void navigateToEdit(int row, TableColumn<CsvRow, String> col) {
        Platform.runLater(() -> {
          table.getProperties().put(EDIT_ALLOWED_KEY, Boolean.TRUE);
          table.getSelectionModel().clearAndSelect(row, col);
          table.edit(row, col);
        });
      }

      @Override
      public void startEdit() {
        // Block the default single-click auto-start; only proceed when explicitly allowed
        Object allowed = table.getProperties().remove(EDIT_ALLOWED_KEY);
        if (allowed != Boolean.TRUE) {
          return;
        }
        super.startEdit();
        if (!isEditing()) {
          return;
        }
        editField = (TextField) getGraphic();
        if (editField == null) {
          return;
        }

        // Commit when focus moves to any other node (click anywhere else)
        editField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
          if (!isFocused && isEditing()) {
            commitEdit(editField.getText());
          }
        });

        // Keyboard shortcuts while in edit mode
        editField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
          KeyCode code = event.getCode();
          if (code == KeyCode.ESCAPE) {
            cancelViaEscape = true;
            cancelEdit();      // Discards changes (super path)
            event.consume();   // Prevent bubbling to the root Escape → back handler
          } else if (code == KeyCode.TAB) {
            int ci = editableCols.indexOf(getTableColumn());
            int ri = getIndex();
            int rc = table.getItems().size();
            String text = editField.getText();
            commitEdit(text);
            if (event.isShiftDown()) {
              if (ci > 0) {
                navigateToEdit(ri, editableCols.get(ci - 1));
              } else if (ri > 0) {
                navigateToEdit(ri - 1, editableCols.get(editableCols.size() - 1));
              }
            } else {
              if (ci < editableCols.size() - 1) {
                navigateToEdit(ri, editableCols.get(ci + 1));
              } else if (ri < rc - 1) {
                navigateToEdit(ri + 1, editableCols.get(0));
              }
            }
            event.consume();
          } else if (code == KeyCode.ENTER) {
            int ri = getIndex();
            TableColumn<CsvRow, String> col = getTableColumn();
            String text = editField.getText();
            commitEdit(text);
            if (event.isShiftDown()) {
              if (ri > 0) {
                navigateToEdit(ri - 1, col);
              }
            } else {
              if (ri < table.getItems().size() - 1) {
                navigateToEdit(ri + 1, col);
              }
            }
            event.consume();
          }
          // Arrow keys: NOT consumed — TextField handles text-cursor movement normally
        });
      }

      @Override
      public void cancelEdit() {
        boolean escape = cancelViaEscape;
        cancelViaEscape = false;
        if (!escape && editField != null && isEditing()) {
          // Clicking away: commit instead of discarding
          String text = editField.getText();
          editField = null;   // Clear before commitEdit to prevent re-entry
          commitEdit(text);
        } else {
          editField = null;
          super.cancelEdit();
        }
      }

      @Override
      public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (isEditing()) {
          return;  // Don't disturb the active text field
        }
        editField = null;
        getStyleClass().remove("csv-cell-error-col");
        setGraphic(null);
        if (!empty && getTableRow() != null
            && getTableRow().getItem() instanceof CsvRow row
            && colId.equals(row.getErrorColumn())) {
          getStyleClass().add("csv-cell-error-col");
          Label badge = new Label("\u26A0");
          badge.getStyleClass().add("csv-cell-error-badge");
          setGraphic(badge);
        }
        // Double-click to enter edit mode; single-click just selects
        setOnMouseClicked(empty ? null : event -> {
          if (event.getClickCount() == 2) {
            table.getProperties().put(EDIT_ALLOWED_KEY, Boolean.TRUE);
            table.edit(getIndex(), getTableColumn());
            event.consume();
          }
        });
      }
    };
  }

  /**
   * Selects the given cell without entering edit mode.
   */
  private static void selectCell(TableView<CsvRow> table, int row, TableColumn<CsvRow, ?> col) {
    table.getSelectionModel().clearAndSelect(row, col);
    table.scrollTo(row);
  }

  /**
   * Navigates to the previous ({@code direction < 0}) or next
   * ({@code direction > 0}) error row, wrapping around if needed.
   */
  private static void navigateError(TableView<CsvRow> table,
                                    ObservableList<CsvRow> rows, int direction) {
    int selected = table.getSelectionModel().getSelectedIndex();
    int n = rows.size();
    if (n == 0) {
      return;
    }

    if (direction > 0) {
      int start = selected < 0 ? 0 : selected + 1;
      for (int i = 0; i < n; i++) {
        int idx = (start + i) % n;
        if (rows.get(idx).hasError()) {
          table.scrollTo(idx);
          table.getSelectionModel().select(idx);
          return;
        }
      }
    } else {
      int start = selected <= 0 ? n - 1 : selected - 1;
      for (int i = 0; i < n; i++) {
        int idx = ((start - i) % n + n) % n;
        if (rows.get(idx).hasError()) {
          table.scrollTo(idx);
          table.getSelectionModel().select(idx);
          return;
        }
      }
    }
  }

  /**
   * Returns a short human-readable summary of a semicolon-separated price string.
   * Examples: {@code "0 prices"}, {@code "1 price · 214.10"},
   * {@code "10,000 prices · 100.00 → 214.10"}.
   */
  private static String buildPriceSummary(String raw) {
    if (raw == null || raw.isBlank()) {
      return "0 prices";
    }
    String[] parts = raw.split(";");
    String first = null;
    String last = null;
    long count = 0;
    for (String p : parts) {
      String t = p.trim();
      if (!t.isEmpty()) {
        if (first == null) first = t;
        last = t;
        count++;
      }
    }
    if (count == 0) return "0 prices";
    if (count == 1) return "Week 1 \u00B7 " + first;
    return "Week " + String.format("%,d", count) + " \u00B7 "
      + first + " \u2192 " + last;
  }

  /**
   * Jumps to a displayed row number (1-based).
   * Silently does nothing if the text is not a valid integer or not found.
   */
  private static void handleJumpToLine(TableView<CsvRow> table,
                                       ObservableList<CsvRow> rows, String text) {
    try {
      int target = Integer.parseInt(text);
      for (int i = 0; i < rows.size(); i++) {
        if (i + 1 == target) {
          table.scrollTo(i);
          table.getSelectionModel().select(i);
          return;
        }
      }
    } catch (NumberFormatException ignored) {
      // non-numeric input — ignore silently
    }
  }
}
