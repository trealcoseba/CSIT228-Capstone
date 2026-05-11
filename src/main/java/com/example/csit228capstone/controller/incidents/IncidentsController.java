package com.example.csit228capstone.controller.incidents;

import com.example.csit228capstone.model.incident.*;
import com.example.csit228capstone.repository.IncidentRepository;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class IncidentsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterSeverity;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterType;
    @FXML private TableView<Incident> incidentsTable;
    @FXML private TableColumn<Incident, String> colId;
    @FXML private TableColumn<Incident, String> colType;
    @FXML private TableColumn<Incident, String> colLocation;
    @FXML private TableColumn<Incident, String> colReporter;
    @FXML private TableColumn<Incident, String> colSeverity;
    @FXML private TableColumn<Incident, String> colStatus;
    @FXML private TableColumn<Incident, String> colTime;
    @FXML private TableColumn<Incident, String> colResolved;
    @FXML private TableColumn<Incident, Void> colActions;
    @FXML private Label lblCritical;
    @FXML private Label lblOngoing;
    @FXML private Label lblResolved;

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
            private final Button btnView = new Button("View");
            {
                btnView.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btnView.setOnAction(event -> {
                    Incident selectedIncident = getTableView().getItems().get(getIndex());
                    openViewModal(selectedIncident);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
            }
        });
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
            stage.setTitle("Report New Incident");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
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
                new Alert(Alert.AlertType.ERROR, "Cannot find ViewIncident.fxml file!").show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            ViewIncidentController controller = loader.getController();
            controller.setIncidentData(selectedIncident);

            Stage stage = new Stage();
            stage.setTitle("Incident Details - " + selectedIncident.getType().name());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isUpdated()) {
                loadIncidentsData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error opening view: " + e.getMessage()).show();
        }
    }

    @FXML
    public void resetFilters(ActionEvent actionEvent) {
        searchField.clear();
        filterSeverity.getSelectionModel().select("All");
        filterStatus.getSelectionModel().select("All");
        filterType.getSelectionModel().select("All");
    }
}