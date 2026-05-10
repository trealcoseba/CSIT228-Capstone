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

    // Data handling
    private final IncidentRepository repository = new IncidentRepository();
    private final ObservableList<Incident> masterData = FXCollections.observableArrayList();
    private FilteredList<Incident> filteredData;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupFilterComboBoxes();
//        refreshData();
        loadMockData();
        setupFilteringLogic();
    }

    private void loadMockData() {
        Incident mock = new Incident();
        mock.setId(java.util.UUID.randomUUID());
        mock.setTitle("Sample Fire Incident");
        mock.setType(IncidentType.FIRE);
        mock.setSeverity(IncidentSeverity.CRITICAL);
        mock.setStatus(IncidentStatus.RESPONDING);
        mock.setLocationPurok("Purok 1");
        mock.setLocationDetail("Corner Store");
        mock.setReportedBy(java.util.UUID.randomUUID());
        mock.setReportedAt(java.time.LocalDateTime.now());

        masterData.add(mock);
        updateKPIs();
    }

    private void setupTableColumns() {
        // ID - Truncated UUID
        colId.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getId().toString().substring(0, 8)));

        // Type
        colType.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getType().getDisplayName()));

        // Location
        colLocation.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(
                f.getValue().getLocationPurok() + " - " + f.getValue().getLocationDetail()
        ));

        // Reporter (ID)
        colReporter.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(
                f.getValue().getReportedBy().toString().substring(0, 5)
        ));

        // Severity
        colSeverity.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getSeverity().getDisplayName()));

        // Status - With CSS Styling from your application.css
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

                    // Maps to your CSS: .status-resolved, .status-reported, etc.
                    String statusClass = "status-" + item.name().toLowerCase();
                    badge.getStyleClass().add(statusClass);
                    setGraphic(badge);
                }
            }
        });

        // Time
        colTime.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().getReportedAt().format(timeFormatter)));

        colResolved.setCellValueFactory(f -> {
            java.time.LocalDateTime resolvedTime = f.getValue().getResolvedAt();
            if (resolvedTime == null) {
                return new javafx.beans.property.ReadOnlyObjectWrapper<>("---");
            }
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(resolvedTime.format(timeFormatter));
        });

        // Actions
        setupActionButtons();
    }

    private void setupFilterComboBoxes() {
        filterSeverity.setItems(FXCollections.observableArrayList(IncidentSeverity.values()));
        filterStatus.setItems(FXCollections.observableArrayList(IncidentStatus.values()));
        filterType.setItems(FXCollections.observableArrayList(IncidentType.values()));

        // Ensure ComboBoxes show DisplayName instead of Enum Name
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

        // Combined listener for all filter inputs
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
            // Create the button
            private final Button resolveBtn = new Button("Resolve");

            {
                // Apply your LIGTAS-Brgy CSS style
                resolveBtn.getStyleClass().add("btn-outline");

                resolveBtn.setOnAction(event -> {
                    // Get the incident data for this specific row
                    Incident incident = getTableView().getItems().get(getIndex());

                    // 1. Update the Database using your Repository
                    // This will set the status to 'resolved' and set 'resolved_at' to now()
                    repository.updateStatus(
                            incident.getId(),
                            IncidentStatus.RESOLVED,
                            incident.getReportedBy(), // The ID of the person changing it
                            "Status updated to Resolved by Admin" // Note for the timeline
                    );

                    // 2. Refresh the table and the KPI cards at the bottom
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

                    // LOGIC: If it's already resolved, don't show the button anymore
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
        // Here you would open your New Incident FXML Modal
        System.out.println("Opening Create Form...");
    }
}