package com.example.csit228capstone.controller.incidents;
import com.example.csit228capstone.model.incident.*;
import com.example.csit228capstone.repository.DispatchedResponderRepository;
import com.example.csit228capstone.repository.IncidentRepository;
import com.example.csit228capstone.repository.ResponderRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
public class IncidentsController implements Initializable {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterSeverity;
    @FXML
    private ComboBox<String> filterStatus;
    @FXML
    private ComboBox<String> filterType;
    @FXML
    private TableView<Incident> incidentsTable;
    @FXML
    private TableColumn<Incident, String> colId;
    @FXML
    private TableColumn<Incident, String> colType;
    @FXML
    private TableColumn<Incident, String> colLocation;
    @FXML
    private TableColumn<Incident, String> colReporter;
    @FXML
    private TableColumn<Incident, String> colSeverity;
    @FXML
    private TableColumn<Incident, String> colStatus;
    @FXML
    private TableColumn<Incident, String> colTime;
    @FXML
    private TableColumn<Incident, String> colResolved;
    @FXML
    private TableColumn<Incident, Void> colActions;
    @FXML
    private Label lblCritical;
    @FXML
    private Label lblOngoing;
    @FXML
    private Label lblResolved;
    private IncidentRepository repository;
    private ObservableList<Incident> incidentList;
    private FilteredList<Incident> filteredData;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            repository = new IncidentRepository();
            incidentList = FXCollections.observableArrayList();
            filteredData = new FilteredList<>(incidentList, b -> true);
            setupFilters();
            setupTableColumns();
            formatTable();
            setupFilteringLogic();
            loadIncidentsData();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN INITIALIZE:");
            e.printStackTrace();
        }
    }

    private void setupFilters() {
        filterSeverity.getItems().addAll("All", "Critical", "Major", "Minor");
        filterStatus.getItems().addAll("All", "Reported", "Dispatched", "Responding", "Monitoring", "Resolved");
        filterType.getItems().addAll("All", "NATURAL_DISASTER", "FIRE", "CRIME", "MEDICAL", "OTHER");

        filterSeverity.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        filterType.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getReportedBy() != null ? cell.getValue().getReportedBy().toString().substring(0, 8) : "N/A"
        ));

        colType.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getType() != null ? cell.getValue().getType().name() : "N/A"
        ));

        colSeverity.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSeverity() != null ? cell.getValue().getSeverity().getDisplayName() : "N/A"
        ));

        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStatus() != null ? cell.getValue().getStatus().getDisplayName() : "N/A"
        ));

        colLocation.setCellValueFactory(cell -> {
            String purok = cell.getValue().getLocationPurok() != null ? cell.getValue().getLocationPurok() : "";
            String detail = cell.getValue().getLocationDetail() != null ? cell.getValue().getLocationDetail() : "";
            return new SimpleStringProperty(purok + (!detail.isEmpty() ? " - " + detail : ""));
        });

        colReporter.setCellValueFactory(cell -> new SimpleStringProperty("Resident"));

        colTime.setCellValueFactory(cell -> {
            if (cell.getValue().getReportedAt() != null) {
                return new SimpleStringProperty(cell.getValue().getReportedAt().format(timeFormatter));
            }
            return new SimpleStringProperty("N/A");
        });

        colResolved.setCellValueFactory(cell -> {
            if (cell.getValue().getStatus() == IncidentStatus.RESOLVED && cell.getValue().getUpdatedAt() != null) {
                return new SimpleStringProperty(cell.getValue().getUpdatedAt().format(timeFormatter));
            }
            return new SimpleStringProperty("-");
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnResolve = new Button("Resolved");
            private final Button btnDelete = new Button("Delete");
            private final HBox container = new HBox(5, btnEdit, btnResolve, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color: #D4A017; -fx-text-fill: white;  -fx-background-radius: 8; -fx-cursor: hand;");
                btnEdit.setPrefWidth(60);

                btnResolve.setStyle("-fx-background-color: #20a074; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                btnResolve.setPrefWidth(75);

                btnResolve.setOnMouseEntered(e -> btnResolve.setStyle("-fx-background-color: #1b8a63; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;"));
                btnResolve.setOnMouseExited(e -> btnResolve.setStyle("-fx-background-color: #20a074; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;"));

                btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                btnDelete.setPrefWidth(70);

                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.setPadding(new Insets(0, 2, 0, 2));

                btnEdit.setOnAction(event -> openViewModal(getTableView().getItems().get(getIndex())));
                btnResolve.setOnAction(event -> handleDirectResolve(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDirectDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Incident incident = getTableView().getItems().get(getIndex());

                    if (incident.getStatus() == IncidentStatus.RESOLVED) {
                        btnResolve.setDisable(true);
                        btnResolve.setStyle("-fx-background-color: #167d5b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-opacity: 1.0;");
                        btnResolve.setText("Done");
                    } else {
                        btnResolve.setDisable(false);
                        btnResolve.setStyle("-fx-background-color: #20a074; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-opacity: 1.0;");
                        btnResolve.setText("Resolve");
                    }

                    setGraphic(container);
                }
            }
        });
    }

    private void handleDirectResolve(Incident incident) {
        Stage resolveStage = new Stage();

        if (incidentsTable.getScene() != null) {
            resolveStage.initOwner(incidentsTable.getScene().getWindow());
        }

        resolveStage.initModality(Modality.APPLICATION_MODAL);
        resolveStage.setTitle("Resolve Incident");

        VBox root = new VBox(20);
        root.setPadding(new javafx.geometry.Insets(30));
        root.setStyle("-fx-background-color: white; -fx-border-color: #dcdde1; -fx-border-radius: 8; -fx-background-radius: 8;");
        root.setPrefWidth(450);

        Label lblHeader = new Label("Finalize Incident");
        lblHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #224263;");

        Separator sep = new Separator();

        Label lblMessage = new Label("Are you sure you want to mark this incident as Resolved?");
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label lblWarning = new Label("Note: Once resolved, the status becomes final and cannot be changed back.");
        lblWarning.setWrapText(true);
        lblWarning.setStyle("-fx-font-size: 12px; -fx-text-fill: #20a074; -fx-font-weight: bold;");

        Button btnCancel = new Button("No, Go Back");
        btnCancel.setPrefHeight(40);
        btnCancel.setPrefWidth(125);
        btnCancel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        Button btnConfirm = new Button("Yes, Resolve");
        btnConfirm.setPrefHeight(40);
        btnConfirm.setPrefWidth(125);
        btnConfirm.setStyle("-fx-background-color: #20a074; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> resolveStage.close());

        btnConfirm.setOnAction(e -> {
            try {
                repository.updateStatus(incident.getId(), IncidentStatus.RESOLVED,
                        java.util.UUID.randomUUID(), "Quick Resolved from Dashboard");

                // ✅ Auto-resolve all dispatched responders tied to this incident
                new DispatchedResponderRepository()
                        .updateStatusByIncidentId(incident.getId(), "returned");

                new ResponderRepository()
                        .markRespondersAvailableByIncident(incident.getId());


                loadIncidentsData();
                resolveStage.close();
                System.out.println("Incident marked as resolved.");
            } catch (Exception ex) {
                ex.printStackTrace();
                resolveStage.close();
                new Alert(Alert.AlertType.ERROR, "Error updating status: " + ex.getMessage()).show();
            }
        });

        HBox buttonBox = new HBox(15, btnCancel, btnConfirm);
        buttonBox.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        buttonBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

        root.getChildren().addAll(lblHeader, sep, lblMessage, lblWarning, buttonBox);

        Scene scene = new Scene(root);
        resolveStage.setScene(scene);
        resolveStage.setResizable(false);
        resolveStage.showAndWait();
    }

    private void handleDirectDelete(Incident incident) {
        Stage confirmStage = new Stage();

        if (incidentsTable.getScene() != null) {
            confirmStage.initOwner(incidentsTable.getScene().getWindow());
        }

        confirmStage.initModality(Modality.APPLICATION_MODAL);
        confirmStage.setTitle("Confirm Deletion");

        VBox root = new VBox(20);
        root.setPadding(new javafx.geometry.Insets(30));
        root.setStyle("-fx-background-color: white; -fx-border-color: #dcdde1; -fx-border-radius: 8; -fx-background-radius: 8;");
        root.setPrefWidth(450);

        Label lblHeader = new Label("Confirm Deletion");
        lblHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #224263;");

        Separator sep = new Separator();

        Label lblMessage = new Label("Are you sure you want to delete this incident?\n\"" + incident.getTitle() + "\"");
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label lblWarning = new Label("This action is permanent and cannot be undone.");
        lblWarning.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b; -fx-font-weight: bold;");

        Button btnCancel = new Button("No, Keep it");
        btnCancel.setPrefHeight(40);
        btnCancel.setPrefWidth(120);
        btnCancel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        Button btnConfirm = new Button("Yes, Delete");
        btnConfirm.setPrefHeight(40);
        btnConfirm.setPrefWidth(120);
        btnConfirm.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> confirmStage.close());

        btnConfirm.setOnAction(e -> {
            try {
                repository.delete(incident.getId());
                loadIncidentsData();
                confirmStage.close();

                System.out.println("Incident deleted successfully.");
            } catch (Exception ex) {
                ex.printStackTrace();
                confirmStage.close();
                new Alert(Alert.AlertType.ERROR, "Error deleting: " + ex.getMessage()).show();
            }
        });

        HBox buttonBox = new HBox(15, btnCancel, btnConfirm);
        buttonBox.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        buttonBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

        root.getChildren().addAll(lblHeader, sep, lblMessage, lblWarning, buttonBox);

        Scene scene = new Scene(root);
        confirmStage.setScene(scene);
        confirmStage.setResizable(false);
        confirmStage.showAndWait();
    }

    private void setupFilteringLogic() {
        Runnable updatePredicate = () -> {
            filteredData.setPredicate(incident -> {
                if (incident == null) return false;

                String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
                String sevFilter = filterSeverity.getValue();
                String statFilter = filterStatus.getValue();
                String typeFilter = filterType.getValue();

                boolean matchesSearch = searchText.isEmpty() ||
                        (incident.getTitle() != null && incident.getTitle().toLowerCase().contains(searchText)) ||
                        (incident.getLocationPurok() != null && incident.getLocationPurok().toLowerCase().contains(searchText)) ||
                        (incident.getLocationDetail() != null && incident.getLocationDetail().toLowerCase().contains(searchText));

                boolean matchesSeverity = sevFilter == null || sevFilter.equals("All") ||
                        (incident.getSeverity() != null && incident.getSeverity().getDisplayName().equalsIgnoreCase(sevFilter));

                boolean matchesStatus = statFilter == null || statFilter.equals("All") ||
                        (incident.getStatus() != null && incident.getStatus().getDisplayName().equalsIgnoreCase(statFilter));

                boolean matchesType = typeFilter == null || typeFilter.equals("All") ||
                        (incident.getType() != null && incident.getType().name().equalsIgnoreCase(typeFilter));

                return matchesSearch && matchesSeverity && matchesStatus && matchesType;
            });
        };

        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate.run());
        filterSeverity.valueProperty().addListener((obs, oldV, newV) -> updatePredicate.run());
        filterStatus.valueProperty().addListener((obs, oldV, newV) -> updatePredicate.run());
        filterType.valueProperty().addListener((obs, oldV, newV) -> updatePredicate.run());

        SortedList<Incident> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(incidentsTable.comparatorProperty());
        incidentsTable.setItems(sortedData);
    }

    private void loadIncidentsData() {
        try {
            List<Incident> data = repository.findAll();
            if (data != null) {
                incidentList.setAll(data);
                updateKPIs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to fetch incidents: " + e.getMessage());
        }
    }

    private void updateKPIs() {
        long criticalCount = 0;
        long ongoingCount = 0;
        long resolvedTodayCount = 0;
        LocalDate today = LocalDate.now();

        for (Incident i : incidentList) {
            if (i.getSeverity() == IncidentSeverity.CRITICAL) {
                criticalCount++;
            }
            if (i.getStatus() == IncidentStatus.DISPATCHED ||
                    i.getStatus() == IncidentStatus.RESPONDING ||
                    i.getStatus() == IncidentStatus.MONITORING) {
                ongoingCount++;
            }
            if (i.getStatus() == IncidentStatus.RESOLVED && i.getUpdatedAt() != null) {
                if (i.getUpdatedAt().toLocalDate().isEqual(today)) {
                    resolvedTodayCount++;
                }
            }
        }

        lblCritical.setText(String.valueOf(criticalCount));
        lblOngoing.setText(String.valueOf(ongoingCount));
        lblResolved.setText(String.valueOf(resolvedTodayCount));
    }

    @FXML
    public void openNewIncident(ActionEvent actionEvent) {
        try {
            URL fxmlLocation = getClass().getResource("/com/example/csit228capstone/incident/AddIncident.fxml");
            if (fxmlLocation == null) {
                new Alert(Alert.AlertType.ERROR, "Cannot find AddIncident.fxml file!").show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            AddIncidentController addController = loader.getController();

            Stage stage = new Stage();

            if (incidentsTable.getScene() != null) {
                stage.initOwner(incidentsTable.getScene().getWindow());
            }

            stage.setTitle("Report New Incident");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setFullScreen(false);
            stage.setResizable(false);
            stage.showAndWait();

            if (addController.isSaveClicked()) {
                loadIncidentsData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading screen: " + e.getMessage()).show();
        }
    }

    private void openViewModal(Incident selectedIncident) {
        try {
            URL fxmlLocation = getClass().getResource("/com/example/csit228capstone/incident/ViewIncident.fxml");

            if (fxmlLocation == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Cannot find ViewIncident.fxml! Please check your file path.");
                alert.show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            ViewIncidentController controller = loader.getController();
            controller.setIncidentData(selectedIncident);

            Stage stage = new Stage();

            if (incidentsTable.getScene() != null) {
                stage.initOwner(incidentsTable.getScene().getWindow());
            }

            stage.setTitle("Incident Details - " + selectedIncident.getTitle());
            stage.setScene(new Scene(root));

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setFullScreen(false);
            stage.setResizable(false);

            stage.showAndWait();

            if (controller.isUpdated()) {
                System.out.println("Changes detected. Refreshing table...");
                loadIncidentsData();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error opening the details view: " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    public void resetFilters(ActionEvent actionEvent) {
        searchField.clear();
        filterSeverity.getSelectionModel().select("All");
        filterStatus.getSelectionModel().select("All");
        filterType.getSelectionModel().select("All");
    }

    private void formatTable() {
        colId.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.05));
        colType.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.08));
        colLocation.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.15));
        colReporter.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.08));
        colSeverity.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.08));
        colStatus.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.08));
        colTime.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.12));
        colResolved.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.12));
        colActions.prefWidthProperty().bind(incidentsTable.widthProperty().multiply(0.24));

        colId.setResizable(false);
        colType.setResizable(false);
        colLocation.setResizable(false);
        colReporter.setResizable(false);
        colSeverity.setResizable(false);
        colStatus.setResizable(false);
        colTime.setResizable(false);
        colResolved.setResizable(false);
        colActions.setResizable(false);

        colId.setReorderable(false);
        colType.setReorderable(false);
        colLocation.setReorderable(false);
        colReporter.setReorderable(false);
        colSeverity.setReorderable(false);
        colStatus.setReorderable(false);
        colTime.setReorderable(false);
        colResolved.setReorderable(false);
        colActions.setReorderable(false);
    }

}