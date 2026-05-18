package com.example.csit228capstone.controller.resources;

import com.example.csit228capstone.model.EvacuationCenter;
import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.model.ResourceLog;
import com.example.csit228capstone.repository.ResourceRepository;
import com.example.csit228capstone.service.EvacuationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResourcesController {

    @FXML private TabPane resourcesTabPane;
    @FXML private Tab inventoryTab;
    @FXML private Tab logTab;
    @FXML private Tab addResourceTab;
    @FXML private Tab useResourceTab;

    @FXML private TableView<Resource> resourcesTable;
    @FXML private TableColumn<Resource, String> colId;
    @FXML private TableColumn<Resource, String> colName;
    @FXML private TableColumn<Resource, String> colCategory;
    @FXML private TableColumn<Resource, Double> colQuantity;
    @FXML private TableColumn<Resource, String> colUnit;
    @FXML private TableColumn<Resource, String> colStatus;
    @FXML private TableColumn<Resource, String> colLocation;
    @FXML private TableColumn<Resource, Void> colLastUpdated;  // changed to Void for custom cell
    @FXML private TableColumn<Resource, Void> colActions;

    @FXML private TableView<ResourceLog> resourceLogTable;
    @FXML private TableColumn<ResourceLog, String> colLogDate;
    @FXML private TableColumn<ResourceLog, String> colLogResource;
    @FXML private TableColumn<ResourceLog, String> colLogCenter;
    @FXML private TableColumn<ResourceLog, String> colLogPurpose;
    @FXML private TableColumn<ResourceLog, String> colLogQty;
    @FXML private TableColumn<ResourceLog, String> colLogAvailable;

    @FXML private ComboBox<String> cbLogSort;
    @FXML private Button btnResetLogFilter;

    @FXML private Label lblReliefGoods;
    @FXML private Label lblMedicalSupplies;
    @FXML private Label lblFund;
    @FXML private Label lblLowStock;
    @FXML private Label lblReliefPct;
    @FXML private Label lblMedPct;
    @FXML private Label lblFundPct;
    @FXML private ProgressBar pbRelief;
    @FXML private ProgressBar pbMed;
    @FXML private ProgressBar pbFund;

    @FXML private TextField tfResourceName;
    @FXML private ComboBox<String> cbResourceCategory;
    @FXML private TextField tfTotalQty;
    @FXML private TextField tfAvailableQty;
    @FXML private TextField tfWarningLevel;
    @FXML private TextField tfUnit;
    @FXML private Label lblFormStatus;

    @FXML private ComboBox<Resource> cbUseResource;
    @FXML private Label lblUseAvailable;
    @FXML private ComboBox<EvacuationCenter> cbUseEvacCenter;
    @FXML private TextField tfUseQuantity;
    @FXML private TextArea taUsePurpose;
    @FXML private Label lblUseStatus;

    private final ResourceRepository repository = new ResourceRepository();
    private final EvacuationService evacuationService = EvacuationService.getInstance();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private ObservableList<ResourceLog> allLogs = FXCollections.observableArrayList();
    private UUID highlightedResourceId = null;

    private static final String SORT_DATE_DESC   = "Date Used (Newest)";
    private static final String SORT_DATE_ASC    = "Date Used (Oldest)";
    private static final String SORT_RESOURCE    = "Resource Name (A–Z)";
    private static final String SORT_QTY_DESC    = "Qty Used (Highest)";

    @FXML
    public void initialize() {
        setupTableColumns();
        setupLogTableColumns();
        setupLogSort();
        setupFormDefaults();
        setupUseFormDefaults();
        loadData();
        loadEvacuationCenters();

        resourcesTabPane.getTabs().remove(addResourceTab);
        resourcesTabPane.getTabs().remove(useResourceTab);
    }


    private void setupLogSort() {
        if (cbLogSort == null) return; // guard for older FXML without the field

        cbLogSort.setItems(FXCollections.observableArrayList(
                SORT_DATE_DESC, SORT_DATE_ASC, SORT_RESOURCE, SORT_QTY_DESC
        ));
        cbLogSort.setValue(SORT_DATE_DESC);
        cbLogSort.valueProperty().addListener((obs, o, n) -> applyLogView());
    }


    private void applyLogView() {
        List<ResourceLog> base = (highlightedResourceId != null)
                ? allLogs.stream()
                  .filter(rl -> highlightedResourceId.equals(rl.getResourceId()))
                  .collect(Collectors.toList())
                : allLogs;

        String sortVal = cbLogSort != null ? cbLogSort.getValue() : SORT_DATE_DESC;
        Comparator<ResourceLog> cmp = switch (sortVal == null ? SORT_DATE_DESC : sortVal) {
            case SORT_DATE_ASC  -> Comparator.comparing(
                    rl -> rl.getDateUsed() == null ? LocalDateTime.MIN : rl.getDateUsed());
            case SORT_RESOURCE  -> Comparator.comparing(
                    rl -> rl.getResourceName() == null ? "" : rl.getResourceName().toLowerCase());
            case SORT_QTY_DESC  -> Comparator.comparingDouble(ResourceLog::getQuantityUsed).reversed();
            default             -> Comparator.comparing(
                    (ResourceLog rl) -> rl.getDateUsed() == null ? LocalDateTime.MIN : rl.getDateUsed()
            ).reversed();
        };

        List<ResourceLog> sorted = base.stream().sorted(cmp).collect(Collectors.toList());
        resourceLogTable.setItems(FXCollections.observableArrayList(sorted));
        resourceLogTable.refresh();

        if (!sorted.isEmpty()) resourceLogTable.scrollTo(0);

        if (btnResetLogFilter != null) {
            btnResetLogFilter.setVisible(highlightedResourceId != null);
            btnResetLogFilter.setManaged(highlightedResourceId != null);
        }
    }




    public void addResource(ActionEvent actionEvent) {
        if (!resourcesTabPane.getTabs().contains(addResourceTab)) {
            resourcesTabPane.getTabs().add(addResourceTab);
        }
        resetForm();
        resourcesTabPane.getSelectionModel().select(addResourceTab);
    }

    public void saveResource(ActionEvent actionEvent) {
        String name     = tfResourceName.getText() == null ? "" : tfResourceName.getText().trim();
        String category = cbResourceCategory.getValue();
        String unit     = tfUnit.getText() == null ? "" : tfUnit.getText().trim();

        if (name.isBlank()) { showFormError("Resource name is required."); return; }
        if (category == null || category.isBlank()) { showFormError("Category is required."); return; }

        Double totalQty     = parseQty(tfTotalQty.getText(), "Total quantity");     if (totalQty == null) return;
        Double availableQty = parseQty(tfAvailableQty.getText(), "Available quantity"); if (availableQty == null) return;
        Double warningLevel = parseQty(tfWarningLevel.getText(), "Warning level");  if (warningLevel == null) return;

        if (availableQty > totalQty)   { showFormError("Available quantity cannot be greater than total quantity."); return; }
        if (warningLevel > totalQty)   { showFormError("Warning level cannot be greater than total quantity."); return; }

        Resource resource = new Resource();
        resource.setName(name);
        resource.setCategory(category);
        resource.setTotalQty(totalQty);
        resource.setAvailableQty(availableQty);
        resource.setWarningLevel(warningLevel);
        resource.setUnit(unit.isBlank() ? null : unit);
        resource.setUpdatedAt(LocalDateTime.now());

        try {
            repository.insert(resource);
            loadData();
            closeAddResourceTab(null);
            showInfo("Resource added", "New resource has been saved to the database.");
        } catch (RuntimeException ex) {
            showFormError("Failed to save resource. Please check database connection.");
        }
    }

    public void closeAddResourceTab(ActionEvent actionEvent) {
        resourcesTabPane.getTabs().remove(addResourceTab);
        resourcesTabPane.getSelectionModel().select(inventoryTab);
    }

    public void openUseResourceForm(ActionEvent actionEvent) {
        openUseResourceFormForResource(null);
    }

    public void closeUseResourceTab(ActionEvent actionEvent) {
        resourcesTabPane.getTabs().remove(useResourceTab);
        resourcesTabPane.getSelectionModel().select(inventoryTab);
    }

    public void saveResourceUsage(ActionEvent actionEvent) {
        Resource resource       = cbUseResource.getValue();
        EvacuationCenter center = cbUseEvacCenter.getValue();
        String purpose          = taUsePurpose.getText() == null ? "" : taUsePurpose.getText().trim();

        if (resource == null) { showUseError("Resource is required."); return; }
        if (center == null)   { showUseError("Evacuation center is required."); return; }
        if (purpose.isBlank()) { showUseError("Purpose is required."); return; }

        Double quantityUsed = parseUseQty(tfUseQuantity.getText(), "Quantity used");
        if (quantityUsed == null) return;
        if (quantityUsed > resource.getAvailableQty()) {
            showUseError("Quantity used cannot be greater than available quantity.");
            return;
        }

        try {
            repository.useResource(resource.getId(), center.getId(), center.getName(), purpose, quantityUsed);
            loadData();
            loadEvacuationCenters();
            closeUseResourceTab(null);
            resourcesTabPane.getSelectionModel().select(logTab);
            showInfo("Usage recorded", "Resource inventory and usage log were updated.");
        } catch (IllegalArgumentException ex) {
            showUseError(ex.getMessage());
        } catch (RuntimeException ex) {
            showUseError("Failed to save usage log. Please check database connection.");
        }
    }


    private void setupTableColumns() {
        resourcesTable.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));
        resourcesTable.getColumns().forEach(col -> col.setReorderable(false));

        colId.setCellValueFactory(cd -> {
            String value  = cd.getValue().getId() == null ? "-" : cd.getValue().getId().toString();
            String shortId = value.length() > 8 ? value.substring(0, 8).toUpperCase() : value.toUpperCase();
            return new ReadOnlyStringWrapper(shortId);
        });
        colName.setCellValueFactory(cd     -> new ReadOnlyStringWrapper(safeText(cd.getValue().getName())));
        colCategory.setCellValueFactory(cd -> new ReadOnlyStringWrapper(safeText(cd.getValue().getCategory())));
        colQuantity.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getAvailableQty()));
        colUnit.setCellValueFactory(cd     -> new ReadOnlyStringWrapper(safeText(cd.getValue().getUnit())));
        colStatus.setCellValueFactory(cd   -> new ReadOnlyStringWrapper(computeStatus(cd.getValue())));
        colLocation.setCellValueFactory(cd -> new ReadOnlyStringWrapper("Barangay Storage"));

        colLastUpdated.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Resource resource = getTableRow().getItem();

                long logCount = allLogs.stream()
                        .filter(rl -> resource.getId() != null && resource.getId().equals(rl.getResourceId()))
                        .count();

                Button badgeBtn = new Button(logCount + (logCount == 1 ? " log" : " logs"));
                badgeBtn.getStyleClass().add("log-badge-btn");
                badgeBtn.setOnAction(e -> navigateToResourceLogs(resource));

                LocalDateTime dt = resource.getUpdatedAt();
                Label dateLabel = new Label(dt == null ? "-" : dt.format(DATE_FMT));
                dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

                VBox cell = new VBox(3, badgeBtn, dateLabel);
                cell.setStyle("-fx-alignment: CENTER;");
                setGraphic(cell);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actions     = new HBox(4, editBtn, deleteBtn);
            {
                actions.setStyle("-fx-alignment: CENTER;");

                editBtn.getStyleClass().add("tbl-action-btn-yellow");
                editBtn.setOnAction(e -> {
                    Resource resource = getTableRow() == null ? null : getTableRow().getItem();
                    if (resource != null) openEditDialog(resource);
                });

                deleteBtn.getStyleClass().add("tbl-action-btn-red");
                deleteBtn.setOnAction(e -> {
                    Resource resource = getTableRow() == null ? null : getTableRow().getItem();
                    if (resource != null) confirmAndDelete(resource);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    private void setupLogTableColumns() {
        resourceLogTable.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));
        resourceLogTable.getColumns().forEach(col -> col.setReorderable(false));
        colLogPurpose.setStyle("-fx-alignment: CENTER-LEFT;");

        colLogDate.setCellValueFactory(cd -> {
            LocalDateTime dt = cd.getValue().getDateUsed();
            return new ReadOnlyStringWrapper(dt == null ? "-" : dt.format(DATE_FMT));
        });
        colLogResource.setCellValueFactory(cd  -> new ReadOnlyStringWrapper(safeText(cd.getValue().getResourceName())));
        colLogCenter.setCellValueFactory(cd    -> new ReadOnlyStringWrapper(safeText(cd.getValue().getEvacuationCenterName())));
        colLogPurpose.setCellValueFactory(cd   -> new ReadOnlyStringWrapper(safeText(cd.getValue().getPurpose())));
        colLogQty.setCellValueFactory(cd       -> new ReadOnlyStringWrapper(formatQtyWithUnit(
                cd.getValue().getQuantityUsed(), cd.getValue().getResourceUnit())));
        colLogAvailable.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatQtyWithUnit(
                cd.getValue().getQuantityAvailableAtTime(), cd.getValue().getResourceUnit())));

    }



    private void navigateToResourceLogs(Resource resource) {
        highlightedResourceId = resource.getId();
        applyLogView();   // always call directly so highlight + button state update
        if (cbLogSort != null) cbLogSort.setValue(SORT_DATE_DESC);
        resourcesTabPane.getSelectionModel().select(logTab);
    }

    /** Called by the Reset Filter button in the Log tab. */
    public void resetLogFilter(ActionEvent actionEvent) {
        highlightedResourceId = null;
        applyLogView();
    }


    private void setupFormDefaults() {
        cbResourceCategory.setItems(FXCollections.observableArrayList(
                "Relief Goods", "Medical Supplies", "Emergency Fund",
                "Equipment", "Transport", "Other"
        ));
    }

    private void setupUseFormDefaults() {
        cbUseResource.setConverter(new StringConverter<>() {
            @Override public String toString(Resource r) {
                return r == null ? "" : r.getName() + " (" + formatQtyWithUnit(r.getAvailableQty(), r.getUnit()) + ")";
            }
            @Override public Resource fromString(String s) { return null; }
        });

        cbUseEvacCenter.setConverter(new StringConverter<>() {
            @Override public String toString(EvacuationCenter c) { return c == null ? "" : c.getName(); }
            @Override public EvacuationCenter fromString(String s) { return null; }
        });

        cbUseResource.valueProperty().addListener((obs, oldVal, newVal) -> updateUseAvailability(newVal));
    }

    private void loadData() {
        try {
            List<Resource> resources = repository.findAll();
            resourcesTable.setItems(FXCollections.observableArrayList(resources));
            cbUseResource.setItems(FXCollections.observableArrayList(resources));

            allLogs = FXCollections.observableArrayList(repository.findUsageLogs());
            applyLogView();

            updateOverview(resources);
            // Refresh badge cells now that allLogs is populated
            resourcesTable.refresh();
        } catch (RuntimeException ex) {
            resourcesTable.setItems(FXCollections.observableArrayList());
            cbUseResource.setItems(FXCollections.observableArrayList());
            allLogs = FXCollections.observableArrayList();
            resourceLogTable.setItems(FXCollections.observableArrayList());
            showInfo("Database connection error", "Could not fetch resources from the database.");
        }
    }

    private void loadEvacuationCenters() {
        try {
            cbUseEvacCenter.setItems(FXCollections.observableArrayList(evacuationService.fetchAllCenters()));
        } catch (Exception ex) {
            cbUseEvacCenter.setItems(FXCollections.observableArrayList());
        }
    }

    private void updateOverview(List<Resource> resources) {
        double relief  = sumAvailable(resources, "relief");
        double medical = sumAvailable(resources, "medical");
        double fund    = sumAvailable(resources, "fund");
        long lowStock  = resources.stream()
                .filter(r -> r.getAvailableQty() <= r.getWarningLevel()).count();

        lblReliefGoods.setText(formatQty(relief));
        lblMedicalSupplies.setText(formatQty(medical));
        lblFund.setText("P" + formatQty(fund));
        lblLowStock.setText(String.valueOf(lowStock));

        setProgress(lblReliefPct, pbRelief, totalPct(resources, "relief"));
        setProgress(lblMedPct,    pbMed,    totalPct(resources, "medical"));
        setProgress(lblFundPct,   pbFund,   totalPct(resources, "fund"));
    }

    private void setProgress(Label label, ProgressBar bar, double pct) {
        int rounded = (int) Math.round(pct);
        label.setText(rounded + "%");
        bar.setProgress(Math.max(0, Math.min(1, pct / 100.0)));
    }

    private double sumAvailable(List<Resource> list, String categoryKeyword) {
        return list.stream()
                .filter(r -> safeText(r.getCategory()).toLowerCase().contains(categoryKeyword))
                .mapToDouble(Resource::getAvailableQty).sum();
    }

    private double totalPct(List<Resource> list, String categoryKeyword) {
        List<Resource> filtered = list.stream()
                .filter(r -> safeText(r.getCategory()).toLowerCase().contains(categoryKeyword))
                .toList();
        if (filtered.isEmpty()) return 0;

        double totalAvailable = filtered.stream().mapToDouble(Resource::getAvailableQty).sum();
        double totalCapacity  = filtered.stream().mapToDouble(Resource::getTotalQty).sum();

        return totalCapacity == 0 ? 0 : (totalAvailable / totalCapacity) * 100.0;
    }

    private String computeStatus(Resource resource) {
        if (resource.getAvailableQty() <= 0)                        return "OUT";
        if (resource.getAvailableQty() <= resource.getWarningLevel()) return "LOW";
        return "OK";
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private void openEditDialog(Resource resource) {
        Dialog<ButtonType> dialog = new Dialog<>();

        if (resourcesTabPane.getScene() != null) {
            dialog.initOwner(resourcesTabPane.getScene().getWindow());
        }

        dialog.setTitle("Edit Resource");
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        pane.getStyleClass().add("dialog-pane");

        ButtonType saveType   = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel",        ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(saveType, cancelType);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(8));

        TextField tfId = new TextField(resource.getId() == null ? "-" : resource.getId().toString());
        tfId.setEditable(false); tfId.setDisable(true);

        TextField tfName = new TextField(resource.getName());
        ComboBox<String> cbCategory = new ComboBox<>(FXCollections.observableArrayList(
                "Relief Goods", "Medical Supplies", "Emergency Fund", "Equipment", "Transport", "Other"));
        cbCategory.setValue(resource.getCategory());
        TextField tfTotal     = new TextField(String.valueOf(resource.getTotalQty()));
        TextField tfAvailable = new TextField(String.valueOf(resource.getAvailableQty()));
        TextField tfWarning   = new TextField(String.valueOf(resource.getWarningLevel()));
        TextField tfUnitEdit  = new TextField(resource.getUnit() == null ? "" : resource.getUnit());

        grid.add(new Label("ID"),             0, 0); grid.add(tfId,         1, 0);
        grid.add(new Label("Name"),           0, 1); grid.add(tfName,       1, 1);
        grid.add(new Label("Category"),       0, 2); grid.add(cbCategory,   1, 2);
        grid.add(new Label("Total Qty"),      0, 3); grid.add(tfTotal,      1, 3);
        grid.add(new Label("Available Qty"),  0, 4); grid.add(tfAvailable,  1, 4);
        grid.add(new Label("Warning Level"),  0, 5); grid.add(tfWarning,    1, 5);
        grid.add(new Label("Unit"),           0, 6); grid.add(tfUnitEdit,   1, 6);
        pane.setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == cancelType) return;

        if (result.get() == saveType) {
            String name     = tfName.getText() == null ? "" : tfName.getText().trim();
            String category = cbCategory.getValue();
            String unit     = tfUnitEdit.getText() == null ? "" : tfUnitEdit.getText().trim();

            if (name.isBlank())                    { showError("Validation error", "Name is required."); return; }
            if (category == null || category.isBlank()) { showError("Validation error", "Category is required."); return; }

            try {
                double totalQty     = parseQtyValue(tfTotal.getText(),     "Total quantity");
                double availableQty = parseQtyValue(tfAvailable.getText(), "Available quantity");
                double warningLevel = parseQtyValue(tfWarning.getText(),   "Warning level");

                if (availableQty > totalQty)   { showError("Validation error", "Available quantity cannot be greater than total quantity."); return; }
                if (warningLevel > totalQty)   { showError("Validation error", "Warning level cannot be greater than total quantity."); return; }

                Resource edited = new Resource();
                edited.setId(resource.getId());
                edited.setName(name);
                edited.setCategory(category);
                edited.setTotalQty(totalQty);
                edited.setAvailableQty(availableQty);
                edited.setWarningLevel(warningLevel);
                edited.setUnit(unit.isBlank() ? null : unit);

                repository.update(edited);
                loadData();
                showInfo("Resource updated", "Resource changes were saved.");
            } catch (IllegalArgumentException ex) {
                showError("Validation error", ex.getMessage());
            } catch (RuntimeException ex) {
                showError("Update failed", "Could not update resource.");
            }
        }
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private void confirmAndDelete(Resource resource) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

        if (resourcesTable.getScene() != null) confirm.initOwner(resourcesTable.getScene().getWindow());
        confirm.setTitle("Delete Resource");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete \"" + resource.getName() + "\" permanently? This cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.deleteById(resource.getId());
                loadData();
                showInfo("Resource deleted", "Resource was removed successfully.");
            } catch (RuntimeException ex) {
                showError("Delete failed", "Could not delete resource.");
            }
        }
    }

    private void openUseResourceFormForResource(Resource selectedResource) {
        if (!resourcesTabPane.getTabs().contains(useResourceTab)) {
            int idx = resourcesTabPane.getTabs().contains(addResourceTab)
                    ? resourcesTabPane.getTabs().indexOf(addResourceTab)
                    : resourcesTabPane.getTabs().size();
            resourcesTabPane.getTabs().add(idx, useResourceTab);
        }
        resetUseForm();
        if (selectedResource != null) cbUseResource.setValue(selectedResource);
        resourcesTabPane.getSelectionModel().select(useResourceTab);
    }

    private void updateUseAvailability(Resource resource) {
        if (resource == null) {
            lblUseAvailable.setText("-");
            return;
        }
        lblUseAvailable.setText(formatQtyWithUnit(resource.getAvailableQty(), resource.getUnit()));
    }

    private double parseQtyValue(String raw, String label) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException(label + " is required.");
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0) throw new IllegalArgumentException(label + " cannot be negative.");
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
    }

    private void resetForm() {
        tfResourceName.clear();
        cbResourceCategory.getSelectionModel().clearSelection();
        tfTotalQty.clear();
        tfAvailableQty.clear();
        tfWarningLevel.clear();
        tfUnit.clear();
        lblFormStatus.setText("");
    }

    private void resetUseForm() {
        cbUseResource.getSelectionModel().clearSelection();
        cbUseEvacCenter.getSelectionModel().clearSelection();
        tfUseQuantity.clear();
        taUsePurpose.clear();
        lblUseStatus.setText("");
        updateUseAvailability(null);
    }

    private void showFormError(String message) { lblFormStatus.setText(message); }
    private void showUseError(String message)  { lblUseStatus.setText(message); }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        if (resourcesTabPane.getScene() != null) alert.initOwner(resourcesTabPane.getScene().getWindow());
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (resourcesTabPane.getScene() != null) alert.initOwner(resourcesTabPane.getScene().getWindow());
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private Double parseQty(String raw, String label) {
        if (raw == null || raw.isBlank()) { showFormError(label + " is required."); return null; }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0) { showFormError(label + " cannot be negative."); return null; }
            return value;
        } catch (NumberFormatException ex) {
            showFormError(label + " must be a valid number."); return null;
        }
    }

    private Double parseUseQty(String raw, String label) {
        if (raw == null || raw.isBlank()) { showUseError(label + " is required."); return null; }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value <= 0) { showUseError(label + " must be greater than zero."); return null; }
            return value;
        } catch (NumberFormatException ex) {
            showUseError(label + " must be a valid number."); return null;
        }
    }

    private String safeText(String value) { return value == null ? "-" : value; }

    private String formatQty(double value) {
        return Math.rint(value) == value ? String.format("%.0f", value) : String.format("%.2f", value);
    }

    private String formatQtyWithUnit(double value, String unit) {
        String formatted = formatQty(value);
        return unit == null || unit.isBlank() ? formatted : formatted + " " + unit;
    }
}