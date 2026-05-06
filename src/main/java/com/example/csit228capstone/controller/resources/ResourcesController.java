package com.example.csit228capstone.controller.resources;

import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.repository.ResourceRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ResourcesController {

    @FXML private TabPane resourcesTabPane;
    @FXML private Tab inventoryTab;
    @FXML private Tab addResourceTab;

    @FXML private TableView<Resource> resourcesTable;
    @FXML private TableColumn<Resource, String> colId;
    @FXML private TableColumn<Resource, String> colName;
    @FXML private TableColumn<Resource, String> colCategory;
    @FXML private TableColumn<Resource, Double> colQuantity;
    @FXML private TableColumn<Resource, String> colUnit;
    @FXML private TableColumn<Resource, String> colStatus;
    @FXML private TableColumn<Resource, String> colLocation;
    @FXML private TableColumn<Resource, String> colLastUpdated;
    @FXML private TableColumn<Resource, Void> colActions;

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

    private final ResourceRepository repository = new ResourceRepository();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFormDefaults();
        loadData();

        // Keep inventory clean by opening the add tab only when requested.
        resourcesTabPane.getTabs().remove(addResourceTab);
    }

    public void addResource(ActionEvent actionEvent) {
        if (!resourcesTabPane.getTabs().contains(addResourceTab)) {
            resourcesTabPane.getTabs().add(addResourceTab);
        }
        resetForm();
        resourcesTabPane.getSelectionModel().select(addResourceTab);
    }

    public void saveResource(ActionEvent actionEvent) {
        String name = tfResourceName.getText() == null ? "" : tfResourceName.getText().trim();
        String category = cbResourceCategory.getValue();
        String unit = tfUnit.getText() == null ? "" : tfUnit.getText().trim();

        if (name.isBlank()) {
            showFormError("Resource name is required.");
            return;
        }
        if (category == null || category.isBlank()) {
            showFormError("Category is required.");
            return;
        }

        Double totalQty = parseQty(tfTotalQty.getText(), "Total quantity");
        if (totalQty == null) return;
        Double availableQty = parseQty(tfAvailableQty.getText(), "Available quantity");
        if (availableQty == null) return;
        Double warningLevel = parseQty(tfWarningLevel.getText(), "Warning level");
        if (warningLevel == null) return;

        if (availableQty > totalQty) {
            showFormError("Available quantity cannot be greater than total quantity.");
            return;
        }
        if (warningLevel > totalQty) {
            showFormError("Warning level cannot be greater than total quantity.");
            return;
        }

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

    public void newPurchaseRequest(ActionEvent actionEvent) {
    }

    public void newTransferRequest(ActionEvent actionEvent) {

    }

    private void setupTableColumns() {
        resourcesTable.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));

        colId.setCellValueFactory(cd -> {
            String value = cd.getValue().getId() == null ? "-" : cd.getValue().getId().toString();
            String shortId = value.length() > 8 ? value.substring(0, 8).toUpperCase() : value.toUpperCase();
            return new ReadOnlyStringWrapper(shortId);
        });
        colName.setCellValueFactory(cd -> new ReadOnlyStringWrapper(safeText(cd.getValue().getName())));
        colCategory.setCellValueFactory(cd -> new ReadOnlyStringWrapper(safeText(cd.getValue().getCategory())));
        colQuantity.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getAvailableQty()));
        colUnit.setCellValueFactory(cd -> new ReadOnlyStringWrapper(safeText(cd.getValue().getUnit())));
        colStatus.setCellValueFactory(cd -> new ReadOnlyStringWrapper(computeStatus(cd.getValue())));
        colLocation.setCellValueFactory(cd -> new ReadOnlyStringWrapper("Barangay Storage"));
        colLastUpdated.setCellValueFactory(cd -> {
            LocalDateTime dt = cd.getValue().getUpdatedAt();
            return new ReadOnlyStringWrapper(dt == null ? "-" : dt.format(DATE_FMT));
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            {
                viewBtn.getStyleClass().addAll("quick-action-btn", "qa-navy");
                viewBtn.setOnAction(e -> {
                    Resource resource = getTableRow() == null ? null : getTableRow().getItem();
                    if (resource != null) {
                        openEditDialog(resource);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });
    }

    private void setupFormDefaults() {
        cbResourceCategory.setItems(FXCollections.observableArrayList(
                "Relief Goods",
                "Medical Supplies",
                "Emergency Fund",
                "Equipment",
                "Transport",
                "Other"
        ));
    }

    private void loadData() {
        try {
            List<Resource> resources = repository.findAll();
            resourcesTable.setItems(FXCollections.observableArrayList(resources));
            updateOverview(resources);
        } catch (RuntimeException ex) {
            resourcesTable.setItems(FXCollections.observableArrayList());
            showInfo("Database connection error", "Could not fetch resources from the database.");
        }
    }

    private void updateOverview(List<Resource> resources) {
        double relief = sumAvailable(resources, "relief");
        double medical = sumAvailable(resources, "medical");
        double fund = sumAvailable(resources, "fund");
        long lowStock = resources.stream()
                .filter(r -> r.getAvailableQty() <= r.getWarningLevel())
                .count();

        lblReliefGoods.setText(formatQty(relief));
        lblMedicalSupplies.setText(formatQty(medical));
        lblFund.setText("P" + formatQty(fund));
        lblLowStock.setText(String.valueOf(lowStock));

        setProgress(lblReliefPct, pbRelief, averagePct(resources, "relief"));
        setProgress(lblMedPct, pbMed, averagePct(resources, "medical"));
        setProgress(lblFundPct, pbFund, averagePct(resources, "fund"));
    }

    private void setProgress(Label label, ProgressBar bar, double pct) {
        int rounded = (int) Math.round(pct);
        label.setText(rounded + "%");
        bar.setProgress(Math.max(0, Math.min(1, pct / 100.0)));
    }

    private double sumAvailable(List<Resource> list, String categoryKeyword) {
        return list.stream()
                .filter(r -> safeText(r.getCategory()).toLowerCase().contains(categoryKeyword))
                .mapToDouble(Resource::getAvailableQty)
                .sum();
    }

    private double averagePct(List<Resource> list, String categoryKeyword) {
        List<Resource> filtered = list.stream()
                .filter(r -> safeText(r.getCategory()).toLowerCase().contains(categoryKeyword))
                .toList();
        if (filtered.isEmpty()) return 0;

        return filtered.stream().mapToDouble(Resource::getAvailablePercent).average().orElse(0);
    }

    private String computeStatus(Resource resource) {
        if (resource.getAvailableQty() <= 0) return "OUT";
        if (resource.getAvailableQty() <= resource.getWarningLevel()) return "LOW";
        return "OK";
    }

    private Double parseQty(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            showFormError(label + " is required.");
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0) {
                showFormError(label + " cannot be negative.");
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            showFormError(label + " must be a valid number.");
            return null;
        }
    }

    private void openEditDialog(Resource resource) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("View / Edit Resource");
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        pane.getStyleClass().add("dialog-pane");

        ButtonType saveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteType = new ButtonType("Delete Resource", ButtonBar.ButtonData.LEFT);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(saveType, deleteType, cancelType);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));

        TextField tfId = new TextField(resource.getId() == null ? "-" : resource.getId().toString());
        tfId.setEditable(false);
        tfId.setDisable(true);

        TextField tfName = new TextField(resource.getName());
        ComboBox<String> cbCategory = new ComboBox<>(FXCollections.observableArrayList(
                "Relief Goods", "Medical Supplies", "Emergency Fund", "Equipment", "Transport", "Other"
        ));
        cbCategory.setValue(resource.getCategory());
        TextField tfTotal = new TextField(String.valueOf(resource.getTotalQty()));
        TextField tfAvailable = new TextField(String.valueOf(resource.getAvailableQty()));
        TextField tfWarning = new TextField(String.valueOf(resource.getWarningLevel()));
        TextField tfUnitEdit = new TextField(resource.getUnit() == null ? "" : resource.getUnit());

        grid.add(new Label("ID"), 0, 0);
        grid.add(tfId, 1, 0);
        grid.add(new Label("Name"), 0, 1);
        grid.add(tfName, 1, 1);
        grid.add(new Label("Category"), 0, 2);
        grid.add(cbCategory, 1, 2);
        grid.add(new Label("Total Qty"), 0, 3);
        grid.add(tfTotal, 1, 3);
        grid.add(new Label("Available Qty"), 0, 4);
        grid.add(tfAvailable, 1, 4);
        grid.add(new Label("Warning Level"), 0, 5);
        grid.add(tfWarning, 1, 5);
        grid.add(new Label("Unit"), 0, 6);
        grid.add(tfUnitEdit, 1, 6);

        pane.setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == cancelType) {
            return;
        }

        if (result.get() == deleteType) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete resource");
            confirm.setHeaderText(null);
            confirm.setContentText("Delete this resource permanently?");
            Optional<ButtonType> confirmResult = confirm.showAndWait();
            if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                try {
                    repository.deleteById(resource.getId());
                    loadData();
                    showInfo("Resource deleted", "Resource was removed successfully.");
                } catch (RuntimeException ex) {
                    showError("Delete failed", "Could not delete resource.");
                }
            }
            return;
        }

        if (result.get() == saveType) {
            String name = tfName.getText() == null ? "" : tfName.getText().trim();
            String category = cbCategory.getValue();
            String unit = tfUnitEdit.getText() == null ? "" : tfUnitEdit.getText().trim();

            if (name.isBlank()) {
                showError("Validation error", "Name is required.");
                return;
            }
            if (category == null || category.isBlank()) {
                showError("Validation error", "Category is required.");
                return;
            }

            try {
                double totalQty = parseQtyValue(tfTotal.getText(), "Total quantity");
                double availableQty = parseQtyValue(tfAvailable.getText(), "Available quantity");
                double warningLevel = parseQtyValue(tfWarning.getText(), "Warning level");

                if (availableQty > totalQty) {
                    showError("Validation error", "Available quantity cannot be greater than total quantity.");
                    return;
                }
                if (warningLevel > totalQty) {
                    showError("Validation error", "Warning level cannot be greater than total quantity.");
                    return;
                }

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

    private double parseQtyValue(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException(label + " cannot be negative.");
            }
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

    private void showFormError(String message) {
        lblFormStatus.setText(message);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safeText(String value) {
        return value == null ? "-" : value;
    }

    private String formatQty(double value) {
        if (Math.rint(value) == value) {
            return String.format("%.0f", value);
        }
        return String.format("%.2f", value);
    }
}
