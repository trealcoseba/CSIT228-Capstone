package com.example.csit228capstone.controller.evacuation;

import com.example.csit228capstone.model.EvacuationCenter;
import com.example.csit228capstone.util.SupabaseConnectionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Worker;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.Spinner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class EvacuationController {


    @FXML private Label lblTotalEvacuees;
    @FXML private Label lblCentersActive;

    @FXML private TableColumn colAddress;
    @FXML private WebView mapWebView;
    @FXML private TableView centersTable;
    @FXML private TableColumn<EvacuationCenter, String> colName;
    @FXML private TableColumn<EvacuationCenter, String> colCapacity;
    @FXML private TableColumn<EvacuationCenter, Integer> colOccupancy;
    @FXML private TableColumn<EvacuationCenter, Integer> colStatus;
    @FXML private TableColumn<EvacuationCenter, String> colManager;
    @FXML private TableColumn<EvacuationCenter, String> colContact;
    @FXML private TableColumn<EvacuationCenter, Void> colActions;
    @FXML private ListView<EvacuationCenter> centersList;

    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterAscOrDesc;
    @FXML private TextField tfSearch;

    private FilteredList<EvacuationCenter> filteredData;
    private SortedList<EvacuationCenter> sortedData;


    private final ObservableList<EvacuationCenter> centersData = FXCollections.observableArrayList();
    private WebEngine webEngine;

    @FXML
    public void initialize() {
        setUpMap();
        setupFiltersAndSearch();
        setupCentersList();
        setUpTables();
    }

    @FXML  private void addCenter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/evacuation/EvacuationForm.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Center");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCentersData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void activateEvacuation(ActionEvent actionEvent) {
        System.out.println("Evacuation activated");
    }

    private void setUpTables() {
        // Column bindings — only need to run once but safe to repeat
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("maxCapacity"));
        colOccupancy.setCellValueFactory(new PropertyValueFactory<>("currentOccupancy"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colManager.setCellValueFactory(new PropertyValueFactory<>("managerName"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        setupActionButtons(); // move colActions cellFactory here
        loadCentersData(); // ← separate data loading
        centersTable.setEditable(false);
    }

    private void setUpMap(){
        if (mapWebView == null) return;
        webEngine = mapWebView.getEngine();
        mapWebView.widthProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView width changed: " + newVal)
        );
        mapWebView.heightProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView height changed: " + newVal)
        );

        webEngine.setOnAlert(event ->
                System.out.println("JS Alert: " + event.getData())
        );

        try {
            String mapUrl = getClass()
                    .getResource("/map/map.html")
                    .toExternalForm();
            webEngine.load(mapUrl);

        } catch (Exception e) {
            System.err.println("Failed to load map.html");
            e.printStackTrace();
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    webEngine.executeScript("fixMapSize()");
                    refreshMapMarkers();
                });
            }
        });

        mapWebView.widthProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView width changed: " + newVal)
        );
        mapWebView.heightProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView height changed: " + newVal)
        );

        // Fix map on resize
        mapWebView.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("fixMapSize()");
            }
        });

        mapWebView.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("fixMapSize()");
            }
        });
    }

    private void refreshMapMarkers() {
        if (webEngine == null) return;
        if (webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
            return;
        }

        try {
            webEngine.executeScript("clearMarkers()");

            Iterable<EvacuationCenter> source = (filteredData != null) ? filteredData : centersData;

            for (EvacuationCenter center : source) {
                if (center.getLatitude() != null && center.getLongitude() != null) {
                    String safeName = center.getName().replace("'", "\\'");
                    String script = String.format(
                            "addCenterMarker('%s', %f, %f, '%s', %d, %d, %s, '%s')",
                            center.getId(),
                            center.getLatitude(),
                            center.getLongitude(),
                            safeName,
                            center.getCurrentOccupancy(),
                            center.getMaxCapacity(),
                            center.isActive(),
                            center.getStatus()
                    );
                    webEngine.executeScript(script);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCentersList() {
        // DO NOT call centersList.setItems() here — setupFiltersAndSearch() owns that binding

        centersList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EvacuationCenter center, boolean empty) {
                super.updateItem(center, empty);
                if (empty || center == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(6);
                String status = center.getStatus();
                if (status.equalsIgnoreCase("inactive")) {
                    indicator.setFill(javafx.scene.paint.Color.web("#6b7280"));
                } else if (status.equalsIgnoreCase("full")) {
                    indicator.setFill(javafx.scene.paint.Color.web("#ef4444"));
                } else {
                    indicator.setFill(javafx.scene.paint.Color.web("#22c55e"));
                }

                Label lblName = new Label(center.getName());
                lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a1a;");

                String rawAddress = center.getAddress() != null ? center.getAddress() : "No address";
                String shortAddress = rawAddress.length() > 28 ? rawAddress.substring(0, 28) + "…" : rawAddress;
                Label lblAddress = new Label(shortAddress);
                lblAddress.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

                HBox nameRow = new HBox(8, indicator, lblName);
                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                VBox leftBox = new VBox(2, nameRow, lblAddress);
                leftBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(leftBox, javafx.scene.layout.Priority.ALWAYS);

                int available = center.getMaxCapacity() - center.getCurrentOccupancy();
                Label lblAvailable = new Label(available + " slots");
                lblAvailable.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");

                Label lblEvacuees = new Label(center.getCurrentOccupancy() + " evacuees");
                lblEvacuees.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

                VBox rightBox = new VBox(2, lblAvailable, lblEvacuees);
                rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                HBox card = new HBox(leftBox, rightBox);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));

                selectedProperty().addListener((obs, wasSelected, isSelected) -> applyCardStyle(card, lblName, lblAddress, lblAvailable, lblEvacuees, isSelected));
                applyCardStyle(card, lblName, lblAddress, lblAvailable, lblEvacuees, isSelected());

                setGraphic(card);
                setText(null);
                setStyle("-fx-padding: 4 0 4 0; -fx-background-color: transparent !important;");
            }
        });

        // Register selection → map fly-to ONCE
        centersList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null && selected.getLatitude() != null && selected.getLongitude() != null) {
                if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                    webEngine.executeScript(String.format(
                            "map.setView([%f, %f], 16)",
                            selected.getLatitude(),
                            selected.getLongitude()
                    ));
                }
            }
        });
    }


    private void openEditForm(EvacuationCenter center) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/evacuation/EvacuationForm.fxml"));
            Parent root = loader.load();

            EvacuationFormController formController = loader.getController();
            formController.populateForEdit(center); // ← we'll add this method

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Center");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadCentersData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confirmAndDelete(EvacuationCenter center) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Center");
        confirm.setHeaderText("Delete \"" + center.getName() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "DELETE FROM evacuation_centers WHERE id = ?";
                try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setObject(1, center.getId());
                    stmt.executeUpdate();

                    loadCentersData();
                    setUpTables();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateStats() {
        int totalEvacuees = centersData.stream()
                .mapToInt(EvacuationCenter::getCurrentOccupancy)
                .sum();

        long activeCenters = centersData.stream()
                .filter(EvacuationCenter::isActive)
                .count();

        lblTotalEvacuees.setText(String.valueOf(totalEvacuees));
        lblCentersActive.setText(String.valueOf(activeCenters));
    }

    private void setupFiltersAndSearch() {
        filterStatus.getItems().setAll("All", "Available", "Full", "Inactive");
        filterStatus.setValue("All");

        filterAscOrDesc.getItems().setAll("Name A→Z", "Name Z→A", "Occupancy ↑", "Occupancy ↓", "Capacity ↑", "Capacity ↓");
        filterAscOrDesc.setValue("Name A→Z");

        filteredData = new FilteredList<>(centersData, p -> true);
        sortedData = new SortedList<>(filteredData);

        // These bindings are set ONCE and never overwritten
        centersTable.setItems(sortedData);
        centersList.setItems(sortedData);

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterAscOrDesc.valueProperty().addListener((obs, oldVal, newVal) -> applySort());

        applySort();
    }

    private void applyFilters() {
        if (filteredData == null) return;

        String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
        String status = filterStatus.getValue();

        filteredData.setPredicate(center -> {
            // Status filter
            boolean statusMatch = switch (status) {
                case "Available" -> center.getStatus().equalsIgnoreCase("available");
                case "Full"      -> center.getStatus().equalsIgnoreCase("full");
                case "Inactive"  -> !center.isActive();
                default          -> true; // "All"
            };

            // Search filter — matches name, address, or manager
            boolean searchMatch = search.isEmpty()
                    || center.getName().toLowerCase().contains(search)
                    || (center.getAddress() != null && center.getAddress().toLowerCase().contains(search))
                    || (center.getManagerName() != null && center.getManagerName().toLowerCase().contains(search));

            return statusMatch && searchMatch;
        });

        // Refresh map markers to match filtered results
        refreshMapMarkers();
        updateStats();
    }

    private void applySort() {
        if (sortedData == null) return;
        String sort = filterAscOrDesc.getValue();
        if (sort == null) return;

        Comparator<EvacuationCenter> comparator = switch (sort) {
            case "Name Z→A"    -> Comparator.comparing(EvacuationCenter::getName, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Occupancy ↑" -> Comparator.comparingInt(EvacuationCenter::getCurrentOccupancy);
            case "Occupancy ↓" -> Comparator.comparingInt(EvacuationCenter::getCurrentOccupancy).reversed();
            case "Capacity ↑"  -> Comparator.comparingInt(EvacuationCenter::getMaxCapacity);
            case "Capacity ↓"  -> Comparator.comparingInt(EvacuationCenter::getMaxCapacity).reversed();
            default            -> Comparator.comparing(EvacuationCenter::getName, String.CASE_INSENSITIVE_ORDER); // "Name A→Z"
        };

        sortedData.setComparator(comparator);
    }

    private void openManageEvacueesDialog(EvacuationCenter center) {
        // Input dialog
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Manage Evacuees");
        dialog.setHeaderText("Update occupancy for:\n" + center.getName());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Layout
        Spinner<Integer> spinner = new Spinner<>(0, center.getMaxCapacity(), center.getCurrentOccupancy());
        spinner.setEditable(true);
        spinner.setPrefWidth(120);

        int available = center.getMaxCapacity() - center.getCurrentOccupancy();
        Label lblInfo = new Label(String.format(
                "Current: %d / %d  (%d slots remaining)",
                center.getCurrentOccupancy(), center.getMaxCapacity(), available));
        lblInfo.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        VBox content = new VBox(10, lblInfo, new Label("New occupancy count:"), spinner);
        content.setPadding(new javafx.geometry.Insets(16));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn == saveBtn ? spinner.getValue() : null);

        dialog.showAndWait().ifPresent(newOccupancy -> {
            String sql = "UPDATE evacuation_centers SET current_occupancy = ? WHERE id = ?";
            try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, newOccupancy);
                stmt.setObject(2, center.getId());
                stmt.executeUpdate();

                loadCentersData();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupActionButtons(){
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<EvacuationCenter, Void> call(TableColumn<EvacuationCenter, Void> param) {
                return new TableCell<>() {
                    private final Button btnEdit   = new Button("Edit");
                    private final Button btnDelete = new Button("Delete");
                    private final Button btnManage = new Button("Manage");

                    {
                        btnManage.setStyle("""
                            -fx-background-color: #bbf7d0;
                            -fx-text-fill: #166534;
                            -fx-font-size: 10px;
                            -fx-padding: 3 8 3 8;
                            -fx-background-radius: 4;
                            -fx-cursor: hand;
                            """);

                        btnEdit.setStyle("""
                            -fx-background-color: #bfdbfe;
                            -fx-text-fill: #1e40af;
                            -fx-font-size: 10px;
                            -fx-padding: 3 8 3 8;
                            -fx-background-radius: 4;
                            -fx-cursor: hand;
                            """);

                        btnDelete.setStyle("""
                            -fx-background-color: #fecaca;
                            -fx-text-fill: #991b1b;
                            -fx-font-size: 10px;
                            -fx-padding: 3 8 3 8;
                            -fx-background-radius: 4;
                            -fx-cursor: hand;
                            """);

                        btnEdit.setOnAction(e -> {
                            EvacuationCenter center = getTableView().getItems().get(getIndex());
                            openEditForm(center);
                        });

                        btnDelete.setOnAction(e -> {
                            EvacuationCenter center = getTableView().getItems().get(getIndex());
                            confirmAndDelete(center);
                        });

                        btnManage.setOnAction(e -> {
                            EvacuationCenter center = getTableView().getItems().get(getIndex());
                            openManageEvacueesDialog(center);
                        });
                    }


                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // Re-apply text fill every render to fight TableView selection CSS
                            btnManage.setTextFill(javafx.scene.paint.Color.WHITE);
                            btnEdit.setTextFill(javafx.scene.paint.Color.WHITE);
                            btnDelete.setTextFill(javafx.scene.paint.Color.WHITE);

                            HBox box = new HBox(4, btnManage, btnEdit, btnDelete);
                            box.setAlignment(javafx.geometry.Pos.CENTER);
                            setGraphic(box);
                        }
                    }

                };

            }


        });
    }

    private void loadCentersData() {
        centersData.clear();
        String query = "SELECT * FROM evacuation_centers";

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                EvacuationCenter center = new EvacuationCenter();
                center.setId(UUID.fromString(rs.getString("id")));
                center.setName(rs.getString("name"));
                center.setAddress(rs.getString("address"));
                center.setMaxCapacity(rs.getInt("max_capacity"));
                center.setCurrentOccupancy(rs.getInt("current_occupancy"));
                center.setActive(rs.getBoolean("is_active"));
                center.setManagerName(rs.getString("manager_of_center"));
                center.setLatitude(rs.getDouble("latitude"));
                center.setLongitude(rs.getDouble("longitude"));
                centersData.add(center);
            }

            refreshMapMarkers();
            updateStats();
            applyFilters();

        } catch (Exception e) {
            System.err.println("Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyCardStyle(HBox card, Label lblName, Label lblAddress, Label lblAvailable, Label lblEvacuees, boolean selected) {
        if (selected) {
            card.setStyle("""
            -fx-background-color: #fefce8;
            -fx-border-color: #eab308;
            -fx-border-width: 2;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            """);
        } else {
            card.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #e5e7eb;
            -fx-border-width: 1;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            """);
        }
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a1a;");
        lblAddress.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        lblAvailable.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
        lblEvacuees.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
    }


}