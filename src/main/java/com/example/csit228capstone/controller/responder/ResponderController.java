package com.example.csit228capstone.controller.responder;

import com.example.csit228capstone.model.responder.DispatchedResponder;
import com.example.csit228capstone.model.responder.Responder;
import com.example.csit228capstone.repository.DispatchedResponderRepository;
import com.example.csit228capstone.repository.ResponderRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Predicate;

public class ResponderController implements Initializable {

    // ══════════════════════════════════════════════════════════════
    // FXML — Header
    // ══════════════════════════════════════════════════════════════
    @FXML private TextField  tfSearch;
    @FXML private TabPane    mainTabPane;

    // ══════════════════════════════════════════════════════════════
    // FXML — Registry Tab
    // ══════════════════════════════════════════════════════════════
    @FXML private Label      lblAvailable;
    @FXML private Label      lblTotal;
    @FXML private Label      lblOnMission;

    @FXML private ComboBox<String> filterAgency;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> sortOrder;

    @FXML private TableView<Responder>       tblResponders;
    @FXML private TableColumn<Responder, String> colId;
    @FXML private TableColumn<Responder, String> colName;
    @FXML private TableColumn<Responder, String> colAgency;
    @FXML private TableColumn<Responder, String> colContact;
    @FXML private TableColumn<Responder, Void>   colDispatches; // custom card
    @FXML private TableColumn<Responder, Void>   colStatus;     // badge
    @FXML private TableColumn<Responder, Void>   colAction;

    // ══════════════════════════════════════════════════════════════
    // FXML — Mission Log Tab
    // ══════════════════════════════════════════════════════════════
    @FXML private Label      lblTotalMissions;
    @FXML private Label      lblActiveMissions;
    @FXML private Label      lblCompletedMissions;
    @FXML private Label      lblMissionFilter;

    @FXML private ComboBox<String> filterMissionStatus;
    @FXML private ComboBox<String> filterMissionSeverity;
    @FXML private ComboBox<String> filterMissionIncident;

    @FXML private TableView<DispatchedResponder>       tblDispatched;
    @FXML private TableColumn<DispatchedResponder, String> colDispatchId;
    @FXML private TableColumn<DispatchedResponder, String> colDispatchName;
    @FXML private TableColumn<DispatchedResponder, String> colIncidentId;
    @FXML private TableColumn<DispatchedResponder, String> colSeverity;
    @FXML private TableColumn<DispatchedResponder, String> colLocation;
    @FXML private TableColumn<DispatchedResponder, String> colDispatchTime;
    @FXML private TableColumn<DispatchedResponder, String> colDuration;
    @FXML private TableColumn<DispatchedResponder, Void>   colMissionStatus;
    @FXML private TableColumn<DispatchedResponder, Void>   colDispatchActions;

    // ══════════════════════════════════════════════════════════════
    // State
    // ══════════════════════════════════════════════════════════════
    private final ResponderRepository           responderRepo  = new ResponderRepository();
    private final DispatchedResponderRepository dispatchRepo   = new DispatchedResponderRepository();

    private ObservableList<Responder>           responderList  = FXCollections.observableArrayList();
    private FilteredList<Responder>             filteredResponders;

    private ObservableList<DispatchedResponder> missionList    = FXCollections.observableArrayList();
    private FilteredList<DispatchedResponder>   filteredMissions;

    /** When non-null, Mission Log is filtered to this responder */
    private UUID missionFilterResponderId = null;

    // ══════════════════════════════════════════════════════════════
    // Initialise
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupRegistryTable();
        setupMissionTable();
        setupRegistryFilters();
        setupMissionFilters();
        setupSearchBar();
        loadAll();
    }

    // ══════════════════════════════════════════════════════════════
    // DATA LOADING
    // ══════════════════════════════════════════════════════════════

    private void loadAll() {
        loadResponders();
        loadMissions();

        // Click on empty row area to deselect
        tblDispatched.setRowFactory(tv -> {
            TableRow<DispatchedResponder> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) tblDispatched.getSelectionModel().clearSelection();
            });
            return row;
        });

        tblResponders.setRowFactory(tv -> {
            TableRow<Responder> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) tblResponders.getSelectionModel().clearSelection();
            });
            return row;
        });
    }

    private void loadResponders() {
        try {
            List<Responder> all = responderRepo.findAll();
            responderList.setAll(all);
            updateRegistryKPIs(all);
            refreshAgencyFilter(all);
        } catch (SQLException ex) {
            showError("Failed to load responders: " + ex.getMessage());
        }
    }

    private void loadMissions() {
        try {
            List<DispatchedResponder> all = dispatchRepo.findAll();
            missionList.setAll(all);
            updateMissionKPIs(all);
        } catch (SQLException ex) {
            showError("Failed to load missions: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // KPI UPDATES
    // ══════════════════════════════════════════════════════════════

    private void updateRegistryKPIs(List<Responder> all) {
        long available = all.stream().filter(r -> "available".equals(r.getStatus())).count();
        long onMission = all.stream().filter(r -> "on_mission".equals(r.getStatus())).count();
        lblAvailable.setText(String.valueOf(available));
        lblTotal.setText(String.valueOf(all.size()));
        lblOnMission.setText(String.valueOf(onMission));
    }

    private void updateMissionKPIs(List<DispatchedResponder> all) {
        long active    = all.stream().filter(d -> "dispatched".equals(d.getStatus())).count();
        long completed = all.stream().filter(d -> "returned".equals(d.getStatus())).count();
        lblTotalMissions.setText(String.valueOf(all.size()));
        lblActiveMissions.setText(String.valueOf(active));
        lblCompletedMissions.setText(String.valueOf(completed));
    }

    // ══════════════════════════════════════════════════════════════
    // REGISTRY TABLE SETUP
    // ══════════════════════════════════════════════════════════════

    private void setupRegistryTable() {
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getShortId()));

        colName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getName()));

        colAgency.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAgency()));

        colContact.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getContact()));

        // ── DISPATCH CARD COLUMN ──────────────────────────────────────────────────
        colDispatches.setCellFactory(col -> new TableCell<>() {

            // Header chip (always visible, clickable)
            private final Button chip = new Button();

            // Mini overview rows (up to 3 most recent missions)
            private final VBox overviewBox = new VBox(3);
            private final VBox container   = new VBox(5, chip, overviewBox);

            {
                chip.setMaxWidth(Double.MAX_VALUE);
                container.setStyle("-fx-padding: 4 0;");
                chip.setOnAction(e -> {
                    Responder r = getTableView().getItems().get(getIndex());
                    jumpToMissionLog(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                overviewBox.getChildren().clear();

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Responder r = getTableView().getItems().get(getIndex());
                int total  = r.getTotalDispatches();
                int active = r.getActiveDispatches();

                // ── Chip label & color ──
                String dotColor;
                String chipLabel;
                if (active > 0) {
                    dotColor  = "#ffb300";
                    chipLabel = "● " + active + " active  /  " + total + " total";
                } else if (total > 0) {
                    dotColor  = "#60a5fa";
                    chipLabel = "○ " + total + " mission" + (total > 1 ? "s" : "");
                } else {
                    dotColor  = "#6b7280";
                    chipLabel = "○ No missions";
                }

                chip.setText(chipLabel);
                chip.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-border-color: #374151;" +
                                "-fx-border-radius: 6;" +
                                "-fx-background-radius: 6;" +
                                "-fx-text-fill: " + dotColor + ";" +
                                "-fx-font-size: 10px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 3 8;"
                );

                // ── Mini mission overview (pulled from the already-loaded missionList) ──
                if (total > 0) {
                    missionList.stream()
                            .filter(dr -> r.getId().equals(dr.getResponderId()))
                            .limit(3)
                            .forEach(dr -> {
                                // Status dot
                                String dot = switch (dr.getStatus() != null ? dr.getStatus() : "") {
                                    case "dispatched" -> "🟡";
                                    case "returned"   -> "🟢";
                                    default           -> "⚫";
                                };
                                // Severity / incident type (fall back to short incident ID)
                                String type = (dr.getSeverity() != null && !dr.getSeverity().isBlank())
                                        ? dr.getSeverity()
                                        : "INC-" + dr.getShortIncidentId();

                                // Date (dispatch date only, compact)
                                String date = dr.getDispatchedAt() != null
                                        ? dr.getDispatchedAt().toLocalDate()
                                          .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))
                                        : "—";

                                Label row = new Label(dot + " " + type + "  ·  " + date);
                                row.setStyle(
                                        "-fx-font-size: 9px;" +
                                                "-fx-text-fill: #9ca3af;" +
                                                "-fx-padding: 0 2;"
                                );
                                overviewBox.getChildren().add(row);
                            });
                }

                setGraphic(container);
            }
        });

        // ── STATUS BADGE COLUMN ────────────────────────────────────────────────────
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Responder r = getTableView().getItems().get(getIndex());

                String bg, fg;
                switch (r.getStatus() != null ? r.getStatus() : "") {
                    case "available"  -> { bg = "#052e16"; fg = "#00ff88"; }
                    case "on_mission" -> { bg = "#451a03"; fg = "#ffb300"; }
                    default           -> { bg = "#1f2937"; fg = "#9ca3af"; }
                }

                badge.setText(r.getStatusLabel());

                // ✅ Set the FULL style fresh every time — never append to getStyle()
                badge.setStyle(
                        "-fx-padding: 3 10;" +
                                "-fx-background-radius: 12;" +
                                "-fx-font-size: 10px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-color: " + bg + ";" +
                                "-fx-text-fill: " + fg + ";"
                );

                setAlignment(Pos.CENTER);
                // ✅ Clear the cell's own background so selection doesn't bleed through
                setStyle("-fx-background-color: transparent;");
                setGraphic(badge);
            }
        });

        // ── ACTION BUTTONS COLUMN ─────────────────────────────────────────────────
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit     = new Button("Edit");
            private final Button btnDelete   = new Button("Delete");
            private final Button btnDispatch = new Button("Dispatch");
            private final HBox   box         = new HBox(4, btnEdit, btnDispatch, btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnDispatch.setStyle("""
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
                    Responder r = getTableView().getItems().get(getIndex());
                    openResponderForm(r);
                });

                btnDelete.setOnAction(e -> {
                    Responder r = getTableView().getItems().get(getIndex());
                    confirmDelete(r);
                });

                btnDispatch.setOnAction(e -> {
                    Responder r = getTableView().getItems().get(getIndex());
                    openDispatchFormFor(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Wrap in FilteredList
        filteredResponders = new FilteredList<>(responderList, p -> true);
        tblResponders.setItems(filteredResponders);
    }

    private void styleActionButton(Button btn, String bg, String fg) {
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-border-color: " + fg + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 3 7;"
        );
    }

    // ══════════════════════════════════════════════════════════════
    // MISSION LOG TABLE SETUP
    // ══════════════════════════════════════════════════════════════

    private void setupMissionTable() {
        colDispatchId.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getShortId()));

        colDispatchName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getResponderName()));

        colIncidentId.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getShortIncidentId()));

        colSeverity.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSeverity()));

        colLocation.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDeploymentSite()));

        colDispatchTime.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFormattedDispatchTime()));

        colDuration.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDurationLabel()));

        // ── STATUS BADGE ──────────────────────────────────────────────────────────
        colMissionStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 10px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                DispatchedResponder dr = getTableView().getItems().get(getIndex());
                String bg, fg;
                switch (dr.getStatus() != null ? dr.getStatus() : "") {
                    case "dispatched" -> { bg = "#451a03"; fg = "#ffb300"; }
                    case "returned"   -> { bg = "#052e16"; fg = "#00ff88"; }
                    case "cancelled"  -> { bg = "#1f2937"; fg = "#9ca3af"; }
                    default           -> { bg = "#1f2937"; fg = "#9ca3af"; }
                }
                badge.setText(dr.getStatusLabel());
                badge.setStyle("-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 10px;" +
                        "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";");
                setAlignment(Pos.CENTER);
                setGraphic(badge);
            }
        });

        // ── OPERATIONS COLUMN ─────────────────────────────────────────────────────
        colDispatchActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnReturn  = new Button("✅ Return");
            private final Button btnCancel  = new Button("✖ Cancel");
            private final HBox   box        = new HBox(6, btnReturn, btnCancel);

            {
                box.setAlignment(Pos.CENTER);
                styleActionButton(btnReturn, "#052e16", "#00ff88");
                styleActionButton(btnCancel, "#3b0f0f", "#f87171");

                btnReturn.setOnAction(e -> {
                    DispatchedResponder dr = getTableView().getItems().get(getIndex());
                    updateMissionStatus(dr, "returned");
                });

                btnCancel.setOnAction(e -> {
                    DispatchedResponder dr = getTableView().getItems().get(getIndex());
                    updateMissionStatus(dr, "cancelled");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                DispatchedResponder dr = getTableView().getItems().get(getIndex());
                // Hide buttons if already finished
                boolean active = "dispatched".equalsIgnoreCase(dr.getStatus());
                btnReturn.setDisable(!active);
                btnCancel.setDisable(!active);
                setGraphic(box);
            }
        });

        filteredMissions = new FilteredList<>(missionList, p -> true);
        tblDispatched.setItems(filteredMissions);
    }

    // ══════════════════════════════════════════════════════════════
    // FILTER SETUP
    // ══════════════════════════════════════════════════════════════

    private void setupRegistryFilters() {
        filterStatus.setItems(FXCollections.observableArrayList(
                "All", "available", "on_mission", "off_duty"));
        filterStatus.getSelectionModel().select("All");

        sortOrder.setItems(FXCollections.observableArrayList(
                "Name (A–Z)", "Name (Z–A)", "Most Missions", "Least Missions"));
        sortOrder.getSelectionModel().select("Name (A–Z)");

        // Apply filter on any change
        filterAgency.setOnAction(e -> applyRegistryFilter());
        filterStatus.setOnAction(e -> applyRegistryFilter());
        sortOrder.setOnAction(e -> applyRegistrySort());
    }

    private void setupMissionFilters() {
        filterMissionStatus.setItems(FXCollections.observableArrayList(
                "All", "dispatched", "returned", "cancelled"));
        filterMissionStatus.getSelectionModel().select("All");

        filterMissionSeverity.setItems(FXCollections.observableArrayList(
                "All", "Low", "Moderate", "High", "Critical"));
        filterMissionSeverity.getSelectionModel().select("All");

        filterMissionStatus.setOnAction(e -> applyMissionFilter());
        filterMissionSeverity.setOnAction(e -> applyMissionFilter());
        filterMissionIncident.setOnAction(e -> applyMissionFilter());
    }

    private void setupSearchBar() {
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            applyRegistryFilter();
            applyMissionFilter();
        });
    }

    // ─── Apply registry filter ─────────────────────────────────────────────────

    private void applyRegistryFilter() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase();
        String agency = filterAgency.getValue();
        String status = filterStatus.getValue();

        Predicate<Responder> pred = r -> {
            boolean matchSearch = search.isEmpty()
                    || r.getName().toLowerCase().contains(search)
                    || r.getShortId().toLowerCase().contains(search);

            boolean matchAgency = agency == null || agency.isBlank()
                    || agency.equals(r.getAgency());

            boolean matchStatus = status == null || status.equals("All")
                    || status.equals(r.getStatus());

            return matchSearch && matchAgency && matchStatus;
        };
        filteredResponders.setPredicate(pred);
        applyRegistrySort();
    }

    private void applyRegistrySort() {
        String sort = sortOrder.getValue();
        if (sort == null) return;

        List<Responder> sorted = new java.util.ArrayList<>(filteredResponders.getSource());
        switch (sort) {
            case "Name (Z–A)"      -> sorted.sort((a, b) -> b.getName().compareTo(a.getName()));
            case "Most Missions"   -> sorted.sort((a, b) -> b.getTotalDispatches() - a.getTotalDispatches());
            case "Least Missions"  -> sorted.sort((a, b) -> a.getTotalDispatches() - b.getTotalDispatches());
            default                -> sorted.sort((a, b) -> a.getName().compareTo(b.getName()));
        }
        responderList.setAll(sorted);
    }

    // ─── Apply mission filter ─────────────────────────────────────────────────

    private void applyMissionFilter() {
        String search   = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase();
        String status   = filterMissionStatus.getValue();
        String severity = filterMissionSeverity.getValue();
        String incident = filterMissionIncident.getValue();

        Predicate<DispatchedResponder> pred = dr -> {
            boolean matchSearch = search.isEmpty()
                    || (dr.getResponderName() != null && dr.getResponderName().toLowerCase().contains(search))
                    || dr.getShortIncidentId().toLowerCase().contains(search);

            boolean matchStatus = status == null || status.equals("All")
                    || status.equals(dr.getStatus());

            boolean matchSeverity = severity == null || severity.equals("All")
                    || severity.equalsIgnoreCase(dr.getSeverity());

            boolean matchIncident = incident == null || incident.isBlank() || incident.equals("All")
                    || dr.getShortIncidentId().equalsIgnoreCase(incident);

            boolean matchResponder = missionFilterResponderId == null
                    || missionFilterResponderId.equals(dr.getResponderId());

            return matchSearch && matchStatus && matchSeverity && matchIncident && matchResponder;
        };
        filteredMissions.setPredicate(pred);
    }

    private void refreshAgencyFilter(List<Responder> all) {
        String current = filterAgency.getValue();
        List<String> agencies = new java.util.ArrayList<>();
        agencies.add("");
        all.stream().map(Responder::getAgency)
                .filter(a -> a != null && !a.isBlank())
                .distinct().sorted()
                .forEach(agencies::add);
        filterAgency.setItems(FXCollections.observableArrayList(agencies));
        filterAgency.setValue(current);
    }

    // ══════════════════════════════════════════════════════════════
    // DISPATCH CARD → JUMP TO MISSION LOG
    // ══════════════════════════════════════════════════════════════

    /**
     * Called when clicking a responder's dispatch-card in the Registry table.
     * Switches to Mission Log tab and highlights rows for that responder.
     */
    private void jumpToMissionLog(Responder r) {
        missionFilterResponderId = r.getId();

        // Show filter banner
        lblMissionFilter.setText("Showing missions for: " + r.getName());
        lblMissionFilter.setVisible(true);
        lblMissionFilter.setManaged(true);

        applyMissionFilter();

        // Switch tab
        mainTabPane.getSelectionModel().select(1);

        // Highlight all rows for this responder
        tblDispatched.refresh();
        Platform.runLater(() -> {
            tblDispatched.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(DispatchedResponder item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && r.getId().equals(item.getResponderId())) {
                        setStyle("-fx-background-color: #adccf7;");
                    } else {
                        setStyle("");
                    }
                }
            });
        });
    }

    // ══════════════════════════════════════════════════════════════
    // FORM DIALOGS
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void handleRegisterResponder(ActionEvent event) {
        openResponderForm(null);
    }

    private void openResponderForm(Responder editTarget) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/responder/ResponderForm.fxml"));
            Parent root = loader.load();
            ResponderFormController ctrl = loader.getController();
            ctrl.setResponder(editTarget);
            ctrl.setOnSaved(this::loadAll);

            Stage stage = new Stage();
            stage.setTitle(editTarget == null ? "Add Responder" : "Edit Responder");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException ex) {
            showError("Could not open form: " + ex.getMessage());
        }
    }

    @FXML
    public void handleNewDispatch(ActionEvent event) {
        openDispatchFormFor(null);
    }

    private void openDispatchFormFor(Responder preSelected) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/responder/DispatchForm.fxml"));
            Parent root = loader.load();
            DispatchedFormController ctrl = loader.getController();

            if (preSelected != null) ctrl.preSelectResponder(preSelected);

            // Incidents are loaded automatically inside DispatchFormController.initialize()

            ctrl.setOnDispatched(this::loadAll);

            Stage stage = new Stage();
            stage.setTitle("Log Dispatch Mission");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException ex) {
            showError("Could not open dispatch form: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE / STATUS UPDATES
    // ══════════════════════════════════════════════════════════════

    private void confirmDelete(Responder r) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete responder \"" + r.getName() + "\"? This cannot be undone.",
                ButtonType.YES, ButtonType.CANCEL
        );

        alert.setTitle("Confirm Delete");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    responderRepo.delete(r.getId());
                    loadAll();
                } catch (SQLException ex) {
                    showError("Delete failed: " + ex.getMessage());
                }
            }
        });
    }

    private void updateMissionStatus(DispatchedResponder dr, String newStatus) {
        try {
            dispatchRepo.updateStatus(dr.getId(), newStatus);

            // If returned/cancelled → check if responder has any remaining active dispatches
            if ("returned".equals(newStatus) || "cancelled".equals(newStatus)) {
                List<DispatchedResponder> active = dispatchRepo.findByResponderId(dr.getResponderId())
                        .stream().filter(d -> "dispatched".equals(d.getStatus())).toList();
                if (active.isEmpty()) {
                    responderRepo.updateStatus(dr.getResponderId(), "available");
                }
            }
            loadAll();
        } catch (SQLException ex) {
            showError("Failed to update mission: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // CLEAR FILTERS
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void clearRegistryFilters(ActionEvent event) {
        filterAgency.setValue(null);
        filterStatus.getSelectionModel().select("All");
        sortOrder.getSelectionModel().select("Name (A–Z)");
        tfSearch.clear();
        applyRegistryFilter();
    }

    @FXML
    public void clearMissionFilters(ActionEvent event) {
        missionFilterResponderId = null;
        lblMissionFilter.setVisible(false);
        lblMissionFilter.setManaged(false);
        filterMissionStatus.getSelectionModel().select("All");
        filterMissionSeverity.getSelectionModel().select("All");
        filterMissionIncident.getSelectionModel().select(null);
        tfSearch.clear();

        // Reset row highlighting
        tblDispatched.setRowFactory(null);
        tblDispatched.refresh();
        applyMissionFilter();
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void clearDispatchFilters(ActionEvent event) {
        clearMissionFilters(event);
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}