package com.example.csit228capstone.controller.incidents;

import com.example.csit228capstone.model.incident.*;
import com.example.csit228capstone.repository.IncidentRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class IncidentsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<IncidentSeverity> filterSeverity;
    @FXML private ComboBox<IncidentStatus> filterStatus;
    @FXML private ComboBox<IncidentType> filterType;

    @FXML private TableView<Incident> incidentsTable;
    @FXML private TableColumn<Incident, String> colId;
    @FXML private TableColumn<Incident, String> colType;
    @FXML private TableColumn<Incident, String> colLocation;
    @FXML private TableColumn<Incident, String> colReporter;
    @FXML private TableColumn<Incident, String> colSeverity;
    @FXML private TableColumn<Incident, IncidentStatus> colStatus;
    @FXML private TableColumn<Incident, String> colTime;
    @FXML private TableColumn<Incident, String> colResolved;
    @FXML private TableColumn<Incident, Void> colActions;

    @FXML private Label lblCritical;
    @FXML private Label lblOngoing;
    @FXML private Label lblResolved;

    private final IncidentRepository repository = new IncidentRepository();
    private final ObservableList<Incident> masterData = FXCollections.observableArrayList();
    private FilteredList<Incident> filteredData;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupFilterComboBoxes();
        refreshData();
        setupFilteringLogic();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getId().toString().substring(0, 8)));

        colType.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getType().getDisplayName()));

        colLocation.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(
                f.getValue().getLocationPurok() + " - " + f.getValue().getLocationDetail()
        ));

        colReporter.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(
                f.getValue().getReportedBy().toString().substring(0, 5)
        ));

        colSeverity.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getSeverity().getDisplayName()));

        colStatus.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getStatus()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(IncidentStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.getDisplayName());
                    badge.getStyleClass().add("status-badge");

                    String statusClass = "status-" + item.name().toLowerCase();
                    badge.getStyleClass().add(statusClass);
                    setGraphic(badge);
                }
            }
        });

        colTime.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getReportedAt().format(timeFormatter)));

        colResolved.setCellValueFactory(f -> {
            java.time.LocalDateTime resolvedTime = f.getValue().getResolvedAt();
            if (resolvedTime == null) {
                return new javafx.beans.property.ReadOnlyObjectWrapper<>("---");
            }
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(resolvedTime.format(timeFormatter));
        });

        setupActionButtons();
    }

    private void setupFilterComboBoxes() {
        filterSeverity.setItems(FXCollections.observableArrayList(IncidentSeverity.values()));
        filterStatus.setItems(FXCollections.observableArrayList(IncidentStatus.values()));
        filterType.setItems(FXCollections.observableArrayList(IncidentType.values()));

        javafx.util.StringConverter<Object> converter = new javafx.util.StringConverter<>() {
            @Override public String toString(Object object) {
                if (object instanceof IncidentSeverity) return ((IncidentSeverity) object).getDisplayName();
                if (object instanceof IncidentStatus) return ((IncidentStatus) object).getDisplayName();
                if (object instanceof IncidentType) return ((IncidentType) object).getDisplayName();
                return "";
            }
            @Override public Object fromString(String string) { return null; }
        };

        filterSeverity.setConverter((javafx.util.StringConverter) converter);
        filterStatus.setConverter((javafx.util.StringConverter) converter);
        filterType.setConverter((javafx.util.StringConverter) converter);
    }

    private void setupFilteringLogic() {
        filteredData = new FilteredList<>(masterData, p -> true);

        java.util.function.Predicate<Incident> combinedFilter = incident -> {
            String search = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase();

            boolean matchesSearch = incident.getTitle().toLowerCase().contains(search) ||
                    incident.getLocationPurok().toLowerCase().contains(search);

            boolean matchesSeverity = filterSeverity.getValue() == null ||
                    incident.getSeverity() == filterSeverity.getValue();

            boolean matchesStatus = filterStatus.getValue() == null ||
                    incident.getStatus() == filterStatus.getValue();

            boolean matchesType = filterType.getValue() == null ||
                    incident.getType() == filterType.getValue();

            return matchesSearch && matchesSeverity && matchesStatus && matchesType;
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filteredData.setPredicate(combinedFilter));
        filterSeverity.valueProperty().addListener((obs, oldVal, newVal) -> filteredData.setPredicate(combinedFilter));
        filterStatus.valueProperty().addListener((obs, oldVal, newVal) -> filteredData.setPredicate(combinedFilter));
        filterType.valueProperty().addListener((obs, oldVal, newVal) -> filteredData.setPredicate(combinedFilter));

        incidentsTable.setItems(filteredData);
    }

    public void refreshData() {
        masterData.setAll(repository.findAll());
        updateKPIs();
    }

    private void updateKPIs() {
        long critical = masterData.stream().filter(i -> i.getSeverity() == IncidentSeverity.CRITICAL).count();
        long ongoing = masterData.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED).count();
        long resolved = masterData.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED).count();

        lblCritical.setText(String.valueOf(critical));
        lblOngoing.setText(String.valueOf(ongoing));
        lblResolved.setText(String.valueOf(resolved));
    }

    private void setupActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button resolveBtn = new Button("Resolve");

            {
                resolveBtn.getStyleClass().add("btn-outline");

                resolveBtn.setOnAction(event -> {
                    Incident incident = getTableView().getItems().get(getIndex());

                    repository.updateStatus(
                            incident.getId(),
                            IncidentStatus.RESOLVED,
                            incident.getReportedBy(),
                            "Status updated to Resolved by Admin"
                    );

                    refreshData();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Incident incident = getTableView().getItems().get(getIndex());

                    if (incident.getStatus() == IncidentStatus.RESOLVED) {
                        Label doneLabel = new Label("Completed");
                        doneLabel.setStyle("-fx-text-fill: -gray-500; -fx-font-style: italic;");
                        setGraphic(doneLabel);
                    } else {
                        setGraphic(resolveBtn);
                    }
                }
            }
        });
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        filterSeverity.getSelectionModel().clearSelection();
        filterStatus.getSelectionModel().clearSelection();
        filterType.getSelectionModel().clearSelection();
    }

    @FXML
    private void openNewIncident() {
        System.out.println("Opening Create Form...");
    }
}