package com.example.csit228capstone.controller.emergency;

import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.model.emergency.*;
import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.incident.IncidentSeverity;
import com.example.csit228capstone.model.incident.IncidentStatus;
import com.example.csit228capstone.model.incident.IncidentType;
import com.example.csit228capstone.model.responder.DispatchedResponder;
import com.example.csit228capstone.model.responder.Responder;
import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.repository.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EmergencyController implements Initializable {

    private final Map<UUID, String> residentStatusCache = new HashMap<>();

    // ── Repositories ─────────────────────────────────────────────────────────
    private final IncidentRepository               incidentRepo  = new IncidentRepository();
    private final ResidentRepository               residentRepo  = new ResidentRepository();
    private final ResponderRepository              responderRepo = new ResponderRepository();
    private final DispatchedResponderRepository    dispatchRepo  = new DispatchedResponderRepository();
    private final ResourceRepository               resourceRepo  = new ResourceRepository();

    // ── State ─────────────────────────────────────────────────────────────────
    private EmergencyContext ctx;
    private UUID             incidentId;
    private Timeline         clockTimeline;

    private final ObservableList<String> logEntries = FXCollections.observableArrayList();

    // ── Analytics counters ────────────────────────────────────────────────────
    private int totalAffected = 0;
    private int rescued       = 0;
    private int missing       = 0;
    private int evacuated     = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // FXML injections
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private Label lblSystemTime;

    // ── NEW: Declare Incident button in the command bar ───────────────────────
    @FXML private Button btnDeclareIncident;

    // KPI labels
    @FXML private Label lblEmergencyType;
    @FXML private Label lblSeverityDot;
    @FXML private Label lblSeverity;
    @FXML private Label lblTimestamp;
    @FXML private Label lblStatus;
    @FXML private Label lblCoordinates;
    @FXML private Label lblAffectedRadius;

    // Action buttons
    @FXML private Button btnActivateDispatch;
    @FXML private Button btnNotifyResidents;
    @FXML private Button btnRefreshData;
    @FXML private Button btnEndEmergency;


    // Left – Priority residents
    @FXML private Label                                    lblResidentCount;
    @FXML private TableView<PriorityResidentRow>           tblPriorityResidents;
    @FXML private TableColumn<PriorityResidentRow, String> colResidentName;
    @FXML private TableColumn<PriorityResidentRow, String> colVulnerability;
    @FXML private TableColumn<PriorityResidentRow, String> colDistance;
    @FXML private TableColumn<PriorityResidentRow, String> colRescueStatus;
    @FXML private TableColumn<PriorityResidentRow, Void> colResidentAction;

    // Left – Missing residents
    @FXML private Label                                   lblMissingCount;
    @FXML private TableView<MissingResidentRow>           tblMissingResidents;
    @FXML private TableColumn<MissingResidentRow, String> colMissingName;
    @FXML private TableColumn<MissingResidentRow, String> colMissingContact;
    @FXML private TableColumn<MissingResidentRow, String> colLastKnownLocation;

    // Center – Map
    @FXML private WebView mapWebView;
    @FXML private Label   lblMapCoords;

    // Right – Evacuation centers
    @FXML private Label                                       lblEvacCenterCount;
    @FXML private TableView<EvacuationCenterRow>              tblEvacuationCenters;
    @FXML private TableColumn<EvacuationCenterRow, String>    colEvacName;
    @FXML private TableColumn<EvacuationCenterRow, String>    colEvacCapacity;
    @FXML private TableColumn<EvacuationCenterRow, String>    colEvacOccupancy;
    @FXML private TableColumn<EvacuationCenterRow, String>    colEvacDistance;
    @FXML private TableColumn<EvacuationCenterRow, String>    colEvacStatus;

    // Right – Rescue teams
    @FXML private Label                                  lblTeamCount;
    @FXML private TableView<RescueTeamRow>               tblRescueTeams;
    @FXML private TableColumn<RescueTeamRow, String>     colTeamName;
    @FXML private TableColumn<RescueTeamRow, String>     colTeamAvailability;
    @FXML private TableColumn<RescueTeamRow, String>     colTeamVehicle;
    @FXML private TableColumn<RescueTeamRow, String>     colTeamETA;

    // Right – Resources
    @FXML private Label       lblResourceCount;
    @FXML private Label       lblReliefPct;
    @FXML private Label       lblMedPct;
    @FXML private Label       lblEvacCapPct;
    @FXML private ProgressBar pbRelief;
    @FXML private ProgressBar pbMed;
    @FXML private ProgressBar pbEvacCap;
    @FXML private TableView<ResourceRow>           tblResources;
    @FXML private TableColumn<ResourceRow, String> colResourceType;
    @FXML private TableColumn<ResourceRow, String> colResourceQuantity;
    @FXML private TableColumn<ResourceRow, String> colResourceStatus;

    // Bottom – Logs
    @FXML private ListView<String> listIncidentLogs;

    // Bottom – Analytics
    @FXML private BarChart<String, Number> chartAnalytics;
    @FXML private CategoryAxis             chartXAxis;
    @FXML private NumberAxis               chartYAxis;
    @FXML private Label lblTotalAffected;
    @FXML private Label lblRescued;
    @FXML private Label lblMissing;
    @FXML private Label lblEvacuated;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialise
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bindTableColumns();
        setupResidentActions();
        listIncidentLogs.setItems(logEntries);
        startClock();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── NEW: Open Trigger-Incident dialog from the command bar button ─────────
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleDeclareIncident(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/csit228capstone/emergency/TriggerEmergency.fxml"));
            Parent root = loader.load();

            TriggerEmergencyController dlgCtrl = loader.getController();
            // Pass ourselves so the dialog can call initWithContext() on us
            dlgCtrl.setEmergencyController(this);


            Stage dlgStage = new Stage();

            if (btnDeclareIncident.getScene() != null) {
                dlgStage.initOwner(btnDeclareIncident.getScene().getWindow());
            }

            dlgStage.initModality(Modality.APPLICATION_MODAL);
            dlgStage.initOwner(btnDeclareIncident.getScene().getWindow());
            dlgStage.setTitle("Declare Emergency Incident");
            dlgStage.setScene(new Scene(root, 920, 600));
            dlgStage.setResizable(false);


            dlgStage.setResizable(false);
            dlgStage.setFullScreen(false);

            dlgStage.showAndWait();

            // initWithContext() has already been called by the time we get here,
            // so the dashboard will already be populating in the background.

        } catch (IOException ex) {
            addLog("⚠ Could not open Declare Incident dialog: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entry-point called by TriggerIncidentController after confirmation
    // ─────────────────────────────────────────────────────────────────────────

    public void initWithContext(EmergencyContext context) {
        this.ctx = context;

        Platform.runLater(() -> {
            btnDeclareIncident.setDisable(true);
            btnDeclareIncident.setText("Incident Active");
        });

        CompletableFuture.runAsync(() -> {
            try {
                UUID id;
                if (context.getIncidentId() != null) {
                    id = context.getIncidentId();
                } else {
                    Incident inc = buildIncident(context);
                    // The repo insert must happen first
                    id = incidentRepo.insert(inc);
                }

                // CRITICAL: Only assign to the class variable AFTER the DB insert is done
                this.incidentId = id;

                addLog("🚨 INCIDENT CREATED — ID: " + id.toString().substring(0, 8).toUpperCase());
                Platform.runLater(this::populateDashboard);
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnDeclareIncident.setDisable(false);
                    btnDeclareIncident.setText("＋ Declare Incident");
                    showAlert("Database Error", "Failed to create incident record. Check connection.");
                });
                addLog("⚠ DB ERROR: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard population
    // ─────────────────────────────────────────────────────────────────────────

    private void populateDashboard() {
        updateKpiCards();
        loadMap();
        loadAffectedResidents();
        loadEvacuationCenters();
        loadRescueTeams();
        loadResources();
    }

    private void updateKpiCards() {
        if (ctx == null) return;
        lblEmergencyType.setText(prettyType(ctx.getType()));
        lblSeverity.setText(ctx.getSeverity().name());
        lblSeverityDot.setStyle(severityDotStyle(ctx.getSeverity()));
        lblTimestamp.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lblStatus.setText("ACTIVE");
        lblCoordinates.setText(String.format("%.5f, %.5f",
                ctx.getLatitude(), ctx.getLongitude()));
        lblAffectedRadius.setText((int) ctx.getRadiusMeters() + " m");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Map
    // ─────────────────────────────────────────────────────────────────────────

    private void loadMap() {
        if (mapWebView == null || ctx == null) return;
        WebEngine engine = mapWebView.getEngine();
        URL mapUrl = getClass().getResource("/map/map.html");
        if (mapUrl == null) { addLog("⚠ map.html not found at /map/map.html"); return; }
        engine.load(mapUrl.toExternalForm());
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                pushEmergencyDataToMap(engine);
            }
        });
    }

    private void pushEmergencyDataToMap(WebEngine engine) {
        engine.executeScript(String.format(
                "loadEmergencyOverlay(%f, %f, %f);",
                ctx.getLatitude(), ctx.getLongitude(), ctx.getRadiusMeters()));

        lblMapCoords.setText(String.format("Lat: %.5f | Lng: %.5f",
                ctx.getLatitude(), ctx.getLongitude()));

        CompletableFuture.supplyAsync(() -> residentRepo.findAll())
                .thenAcceptAsync(residents -> {
                    engine.executeScript("clearResidentMarkers();");
                    for (Resident r : residents) {
                        if (r.getLatitude() == 0 && r.getLongitude() == 0) continue;
                        double dist  = haversineMeters(ctx.getLatitude(), ctx.getLongitude(),
                                r.getLatitude(), r.getLongitude());
                        if (dist > ctx.getRadiusMeters() * 1.5) continue;
                        boolean inZone = dist <= ctx.getRadiusMeters();
                        boolean vuln   = r.getVulnerabilities() != null && !r.getVulnerabilities().isEmpty();
                        String name      = escapeJs(r.getFirstName() + " " + r.getLastName());
                        String address   = escapeJs(r.getAddress() != null ? r.getAddress() : "");
                        String vulnTypes = escapeJs(vulnSummary(r.getVulnerabilities()));
                        engine.executeScript(String.format(
                                "addResidentMarker(%f, %f, '%s', '%s', %b, %b, %.0f, '%s');",
                                r.getLatitude(), r.getLongitude(),
                                name, address, vuln, inZone, dist, vulnTypes));
                    }
                }, Platform::runLater);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Affected Residents
    // ─────────────────────────────────────────────────────────────────────────

    private void loadAffectedResidents() {
        CompletableFuture.supplyAsync(() -> residentRepo.findAll())
                .thenAcceptAsync(residents -> {
                    addLog("🔍 DEBUG: Total residents in DB = " + residents.size());

                    List<PriorityResidentRow> rows = new ArrayList<>();
                    List<MissingResidentRow> missingRows = new ArrayList<>();

                    for (Resident r : residents) {
                        double lat = r.getLatitude();
                        double lng = r.getLongitude();

                        if (Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001) {
                            continue;
                        }

                        double dist = haversineMeters(ctx.getLatitude(), ctx.getLongitude(), lat, lng);

                        if (dist > ctx.getRadiusMeters()) continue;

                        boolean vuln = r.getVulnerabilities() != null && !r.getVulnerabilities().isEmpty();
                        String name = r.getFirstName() + " " + r.getLastName();

                        // 1. Create the row
                        PriorityResidentRow row = new PriorityResidentRow(r.getId(), name,
                                vulnSummary(r.getVulnerabilities()), dist, vuln);

                        // 2. CHECK CACHE: Restore the status if it exists
                        String savedStatus = residentStatusCache.getOrDefault(r.getId(), "Pending");
                        row.setRescueStatus(savedStatus);

                        // 3. If they were Missing, add them back to the Missing table as well
                        if ("Missing".equals(savedStatus)) {
                            missingRows.add(new MissingResidentRow(r.getId(), name, r.getContactNumber(), "Last seen in zone"));
                        }

                        rows.add(row);
                    }

                    rows.sort(Comparator
                            .comparing((PriorityResidentRow row) -> !row.isVulnerable())
                            .thenComparingDouble(row -> parseDistance(row.getDistanceFromIncident())));

                    totalAffected = rows.size();

                    Platform.runLater(() -> {
                        tblPriorityResidents.setItems(FXCollections.observableArrayList(rows));
                        lblResidentCount.setText(String.valueOf(rows.size()));

                        // Restore Missing Table
                        tblMissingResidents.setItems(FXCollections.observableArrayList(missingRows));
                        lblMissingCount.setText(String.valueOf(missingRows.size()));

                        updateAnalytics();
                        addLog("📋 Dashboard Refreshed: Positions and statuses synchronized.");
                    });
                }, Platform::runLater);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evacuation Centers
    // ─────────────────────────────────────────────────────────────────────────

    private void loadEvacuationCenters() {
        CompletableFuture.runAsync(() -> {
            try (var conn = com.example.csit228capstone.util.SupabaseConnectionManager
                    .getInstance().getConnection();
                 var ps = conn.prepareStatement(
                         "SELECT id, name, max_capacity, current_occupancy, is_active, latitude, longitude " +
                                 "FROM evacuation_centers WHERE is_active = TRUE ORDER BY name");
                 var rs = ps.executeQuery()) {

                ObservableList<EvacuationCenterRow> rows = FXCollections.observableArrayList();
                int totalCap = 0, totalOcc = 0;
                while (rs.next()) {
                    UUID   cid  = rs.getObject("id", UUID.class);
                    String name = rs.getString("name");
                    int    cap  = rs.getInt("max_capacity");
                    int    occ  = rs.getInt("current_occupancy");
                    totalCap += cap; totalOcc += occ;
                    double eLat = rs.getDouble("latitude");
                    double eLng = rs.getDouble("longitude");
                    String dist = (eLat == 0 && eLng == 0) ? "—"
                            : String.format("%.1f km",
                            haversineMeters(ctx.getLatitude(), ctx.getLongitude(), eLat, eLng) / 1000.0);
                    String stat = occ >= cap ? "Full" : occ >= cap * 0.8 ? "Near Full" : "Available";
                    rows.add(new EvacuationCenterRow(cid, name, cap, occ, dist, stat));
                }
                final int fCap = totalCap, fOcc = totalOcc;
                Platform.runLater(() -> {
                    tblEvacuationCenters.setItems(rows);
                    lblEvacCenterCount.setText(String.valueOf(rows.size()));
                    if (fCap > 0) {
                        double pct = (double) fOcc / fCap;
                        pbEvacCap.setProgress(pct);
                        lblEvacCapPct.setText(String.format("%.0f%%", pct * 100));
                    }
                });
            } catch (Exception e) {
                addLog("⚠ Could not load evacuation centers: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rescue Teams
    // ─────────────────────────────────────────────────────────────────────────

    private void loadRescueTeams() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Responder> responders = responderRepo.findAll();
                ObservableList<RescueTeamRow> rows = FXCollections.observableArrayList();
                for (Responder r : responders)
                    rows.add(new RescueTeamRow(r.getId(), r.getName(), r.getStatus(), r.getAgency(), "—"));
                Platform.runLater(() -> {
                    tblRescueTeams.setItems(rows);
                    lblTeamCount.setText(String.valueOf(rows.size()));
                });
            } catch (Exception e) {
                addLog("⚠ Could not load rescue teams: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resources
    // ─────────────────────────────────────────────────────────────────────────

    private void loadResources() {
        CompletableFuture.runAsync(() -> {
            List<Resource> resources = resourceRepo.findAll();
            ObservableList<ResourceRow> rows = FXCollections.observableArrayList();
            double reliefTotal = 0, reliefAvail = 0, medTotal = 0, medAvail = 0;
            for (Resource r : resources) {
                rows.add(new ResourceRow(r.getName(), r.getAvailableQty(), r.getTotalQty(), r.getUnit()));
                String cat = r.getCategory() != null ? r.getCategory().toLowerCase() : "";
                if (cat.contains("relief") || cat.contains("food")) {
                    reliefTotal += r.getTotalQty(); reliefAvail += r.getAvailableQty();
                } else if (cat.contains("med") || cat.contains("health")) {
                    medTotal += r.getTotalQty(); medAvail += r.getAvailableQty();
                }
            }
            final double rPct = reliefTotal > 0 ? reliefAvail / reliefTotal : 0;
            final double mPct = medTotal    > 0 ? medAvail    / medTotal    : 0;
            Platform.runLater(() -> {
                tblResources.setItems(rows);
                lblResourceCount.setText(String.valueOf(rows.size()));
                pbRelief.setProgress(rPct);
                lblReliefPct.setText(String.format("%.0f%%", rPct * 100));
                pbMed.setProgress(mPct);
                lblMedPct.setText(String.format("%.0f%%", mPct * 100));
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Analytics
    // ─────────────────────────────────────────────────────────────────────────

    private void updateAnalytics() {
        lblTotalAffected.setText(String.valueOf(totalAffected));
        lblRescued.setText(String.valueOf(rescued));
        lblMissing.setText(String.valueOf(missing));
        lblEvacuated.setText(String.valueOf(evacuated));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Affected",  totalAffected));
        series.getData().add(new XYChart.Data<>("Rescued",   rescued));
        series.getData().add(new XYChart.Data<>("Missing",   missing));
        series.getData().add(new XYChart.Data<>("Evacuated", evacuated));
        chartAnalytics.getData().clear();
        chartAnalytics.getData().add(series);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button handlers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void assignDispatch(ActionEvent e) {

        if (this.incidentId == null) {
            showAlert("System Synchronizing", "The incident is still being registered in the database. Please wait a few seconds.");
            return;
        }

        if (incidentId == null) {
            showAlert("Incident not ready", "Incident is still being created. Please wait.");
            return;
        }
        List<RescueTeamRow> available = tblRescueTeams.getItems().stream()
                .filter(r -> "Available".equalsIgnoreCase(r.getAvailability()))
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            showAlert("No Available Teams", "All rescue teams are currently dispatched or unavailable.");
            return;
        }
        ChoiceDialog<String> dlg = new ChoiceDialog<>(
                available.get(0).getTeamName(),
                available.stream().map(RescueTeamRow::getTeamName).collect(Collectors.toList()));

        if (btnActivateDispatch.getScene() != null) {
            dlg.initOwner(btnActivateDispatch.getScene().getWindow());
        }

        dlg.setTitle("Activate Dispatch");
        dlg.setHeaderText("Select a rescue team to dispatch:");
        dlg.setContentText("Team:");
        dlg.showAndWait().ifPresent(teamName -> {
            RescueTeamRow row = available.stream()
                    .filter(r -> r.getTeamName().equals(teamName)).findFirst().orElse(null);
            if (row == null) return;
            CompletableFuture.runAsync(() -> {
                try {
                    DispatchedResponder dr = new DispatchedResponder();
                    dr.setResponderId(row.getResponderId());
                    dr.setIncidentId(incidentId);
                    dr.setSeverity(ctx.getSeverity().name().toLowerCase());
                    dr.setTimeOccurred(LocalDateTime.now());
                    dr.setDispatchedAt(LocalDateTime.now());
                    dr.setLatitude(java.math.BigDecimal.valueOf(ctx.getLatitude()));
                    dr.setLongitude(java.math.BigDecimal.valueOf(ctx.getLongitude()));
                    dr.setStatus("dispatched");
                    dispatchRepo.save(dr);
                    responderRepo.updateStatus(row.getResponderId(), "on_mission");
                    incidentRepo.updateStatus(incidentId, IncidentStatus.RESPONDING,
                            ctx.getReportedBy() != null ? ctx.getReportedBy()
                                    : UUID.fromString("00000000-0000-0000-0000-000000000000"),
                            "Dispatch activated: " + teamName);
                    Platform.runLater(() -> {
                        row.setAvailability("Dispatched");
                        tblRescueTeams.refresh();
                        lblStatus.setText("IN PROGRESS");
                        addLog("🚔 DISPATCH: " + teamName + " dispatched to incident.");
                        showInfo("Dispatch Activated", teamName + " has been dispatched.");
                    });
                } catch (Exception ex) {
                    addLog("⚠ Dispatch error: " + ex.getMessage());
                }
            });
        });
    }

    @FXML
    void notifyResidents(ActionEvent e) {
        int count = tblPriorityResidents.getItems().size();
        if (count == 0) {
            showAlert("No Affected Residents", "No residents loaded within the affected radius.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

        if (btnNotifyResidents.getScene() != null) {
            confirm.initOwner(btnNotifyResidents.getScene().getWindow());
        }

        confirm.setTitle("Notify Residents");
        confirm.setHeaderText("Send emergency notification?");
        confirm.setContentText("This will log a notification for " + count + " affected residents.\n\nContinue?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                addLog("NOTIFICATION SENT to " + count + " affected residents.");
                addLog("   Type: " + prettyType(ctx.getType()) + " | Radius: " + (int) ctx.getRadiusMeters() + " m");
                showInfo("Notification Sent", "Emergency notification dispatched for " + count + " residents.");
            }
        });
    }

    @FXML
    void refreshDashboard(ActionEvent e) {
        addLog("Dashboard refreshed at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        if (ctx != null) populateDashboard();
    }

    @FXML
    void endEmergency(ActionEvent e) {
        if (incidentId == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        if (btnEndEmergency.getScene() != null) {
            confirm.initOwner(btnEndEmergency.getScene().getWindow());
        }
        confirm.setTitle("End Emergency");
        confirm.setHeaderText("Mark this incident as RESOLVED?");
        confirm.setContentText("This will release all responders and return the dashboard to IDLE.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            CompletableFuture.runAsync(() -> {
                try {
                    UUID adminId = ctx.getReportedBy() != null ? ctx.getReportedBy()
                            : UUID.fromString("00000000-0000-0000-0000-000000000000");

                    incidentRepo.updateStatus(incidentId, IncidentStatus.RESOLVED, adminId, "Emergency ended.");
                    List<DispatchedResponder> dispatched = dispatchRepo.findByIncidentId(incidentId);
                    for (DispatchedResponder dr : dispatched) {
                        dispatchRepo.updateStatus(dr.getId(), "returned");
                        responderRepo.updateStatus(dr.getResponderId(), "available");
                    }

                    Platform.runLater(() -> {

                        showInfo("Incident Resolved", "Emergency status set to RESOLVED.");

                        resetDashboard();

                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> addLog("Error: " + ex.getMessage()));
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
// Resident Action Column
// ─────────────────────────────────────────────────────────────────────────

    private void setupResidentActions() {
        colResidentAction.setCellFactory(col -> new TableCell<>() {

            private final Button btnRescued   = new Button("Rescued");
            private final Button btnMissing   = new Button("Missing");
            private final Button btnEvacuated = new Button("Evacuated");
            private final HBox box          = new HBox(4, btnRescued, btnEvacuated, btnMissing);

            {
                box.setAlignment(Pos.CENTER);
                btnRescued.setStyle("""
                    -fx-background-color: #3aab8a;
                    -fx-text-fill: white;
                    -fx-font-size: 11px;
                    -fx-padding: 5 12 5 12;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """);

                btnMissing.setStyle("""
                    -fx-background-color: 1e3a5f;
                    -fx-text-fill: white;
                    -fx-font-size: 11px;
                    -fx-padding: 5 12 5 12;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """);

                btnMissing.setStyle("""
                    -fx-background-color: #c0392b;
                    -fx-text-fill: white;
                    -fx-font-size: 11px;
                    -fx-padding: 5 12 5 12;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """);
                styleResidentBtn(btnRescued,   "#052e16", "#34d399");
                styleResidentBtn(btnEvacuated, "#1e3a5f", "#60a5fa");
                styleResidentBtn(btnMissing,   "#3b0f0f", "#fca5a5");

                btnRescued.setOnAction(e -> {
                    PriorityResidentRow row = getTableView().getItems().get(getIndex());
                    markResident(row, "Rescued");
                });

                btnEvacuated.setOnAction(e -> {
                    PriorityResidentRow row = getTableView().getItems().get(getIndex());
                    handleEvacuated(row);
                });

                btnMissing.setOnAction(e -> {
                    PriorityResidentRow row = getTableView().getItems().get(getIndex());
                    handleMissing(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                PriorityResidentRow row = getTableView().getItems().get(getIndex());
                String status = row.getRescueStatus();

                // Disable all buttons once a status is set
                boolean locked = status != null && !status.isBlank() && !status.equals("Pending");
                btnRescued.setDisable(locked);
                btnEvacuated.setDisable(locked);
                btnMissing.setDisable(locked);

                setGraphic(box);
            }
        });
    }

// ── Rescued ───────────────────────────────────────────────────────────────

    private void markResident(PriorityResidentRow row, String status) {
        String prev = row.getRescueStatus();
        row.setRescueStatus(status);

        residentStatusCache.put(row.getResidentId(), status);

        tblPriorityResidents.refresh();
        recomputeAnalytics(prev, status);
        addLog("✅ " + row.getResidentName() + " marked as " + status.toUpperCase());
    }

// ── Evacuated → find nearest evac center ─────────────────────────────────

    private void handleEvacuated(PriorityResidentRow row) {
        // Find closest available evac center from the already-loaded table
        EvacuationCenterRow nearest = tblEvacuationCenters.getItems().stream()
                .filter(ec -> !"Full".equals(ec.getStatus()))
                .min(Comparator.comparingDouble(ec -> parseKmDistance(ec.getDistance())))
                .orElse(null);

        String centerInfo = nearest != null
                ? nearest.getCenterName() + "  (" + nearest.getDistance() + " away)"
                : "No available center found";

        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);

        if (tblPriorityResidents.getScene() != null) {
            dlg.initOwner(tblPriorityResidents.getScene().getWindow());
        }

        dlg.setTitle("Mark as Evacuated");
        dlg.setHeaderText("Evacuate " + row.getResidentName() + "?");
        dlg.setContentText("Nearest available center:\n📍 " + centerInfo +
                "\n\nMark this resident as evacuated?");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String prev = row.getRescueStatus();
            row.setRescueStatus("Evacuated");
            tblPriorityResidents.refresh();
            recomputeAnalytics(prev, "Evacuated");
            addLog("🏠 " + row.getResidentName() + " EVACUATED → " +
                    (nearest != null ? nearest.getCenterName() : "unassigned center"));
        });
    }

// ── Missing → capture last seen ───────────────────────────────────────────

    private void handleMissing(PriorityResidentRow row) {
        TextInputDialog dlg = new TextInputDialog();
        if (tblPriorityResidents.getScene() != null) {
            dlg.initOwner(tblPriorityResidents.getScene().getWindow());
        }
        dlg.setTitle("Report Missing");
        dlg.setHeaderText("Report " + row.getResidentName() + " as missing");
        dlg.setContentText("Last known location:");

        dlg.showAndWait().ifPresent(lastSeen -> {
            String contact = "N/A";
            try {
                // FIX: Add .orElse(null) at the end of the findById call
                Resident r = residentRepo.findById(row.getResidentId()).orElse(null);

                if (r != null && r.getContactNumber() != null && !r.getContactNumber().isBlank()) {
                    contact = r.getContactNumber();
                }
            } catch (Exception e) {
                System.err.println("Could not fetch contact: " + e.getMessage());
            }

            String prev = row.getRescueStatus();
            row.setRescueStatus("Missing");
            residentStatusCache.put(row.getResidentId(), "Missing");
            tblPriorityResidents.refresh();

            // Add to the missing residents table with the contact number found
            MissingResidentRow missingRow = new MissingResidentRow(
                    row.getResidentId(),
                    row.getResidentName(),
                    contact, // <--- NOW AUTOMATICALLY FILLED
                    lastSeen.isBlank() ? "Last seen in zone" : lastSeen
            );
            tblMissingResidents.getItems().add(missingRow);
            lblMissingCount.setText(String.valueOf(tblMissingResidents.getItems().size()));

            recomputeAnalytics(prev, "Missing");
            addLog("❓ " + row.getResidentName() + " reported MISSING (Contact: " + contact + ")");
        });
    }

// ── Analytics sync ────────────────────────────────────────────────────────

    /**
     * Adjusts the analytics counters when a resident's status changes.
     * @param prev old status (may be null/"Pending")
     * @param next new status
     */
    private void recomputeAnalytics(String prev, String next) {
        // Roll back old status
        if ("Rescued".equals(prev))   rescued--;
        if ("Missing".equals(prev))   missing--;
        if ("Evacuated".equals(prev)) evacuated--;

        // Apply new status
        if ("Rescued".equals(next))   rescued++;
        if ("Missing".equals(next))   missing++;
        if ("Evacuated".equals(next)) evacuated++;

        updateAnalytics();
    }

// ── Style helper ──────────────────────────────────────────────────────────

    private void styleResidentBtn(Button btn, String bg, String fg) {
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-border-color: " + fg + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-font-size: 10px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 3 6;"
        );
    }

    private double parseKmDistance(String dist) {
        // dist is formatted as "1.2 km" or "—"
        try { return Double.parseDouble(dist.replace(" km", "").trim()); }
        catch (Exception e) { return Double.MAX_VALUE; }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Clock
    // ─────────────────────────────────────────────────────────────────────────

    private void startClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1),
                ev -> lblSystemTime.setText(LocalDateTime.now().format(fmt))));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Incident buildIncident(EmergencyContext ctx) {
        Incident i = new Incident();
        i.setType(ctx.getType());
        i.setSeverity(ctx.getSeverity());
        i.setStatus(IncidentStatus.REPORTED);
        i.setTitle(ctx.getTitle() != null ? ctx.getTitle() : prettyType(ctx.getType()) + " Incident");
        i.setDescription(ctx.getDescription() != null ? ctx.getDescription() : "");
        i.setLatitude(ctx.getLatitude());
        i.setLongitude(ctx.getLongitude());
        i.setLocationPurok(ctx.getLocationPurok());
        i.setLocationDetail(ctx.getLocationDetail());
        i.setReportedBy(ctx.getReportedBy());
        i.setReportedAt(LocalDateTime.now());
        return i;
    }

    private static double haversineMeters(double lat1, double lon1,
                                          double lat2, double lon2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double parseDistance(String distStr) {
        try { return Double.parseDouble(distStr.replace(" m", "").trim()); }
        catch (Exception e) { return Double.MAX_VALUE; }
    }

    private String vulnSummary(List<VulnerabilityTag> tags) {
        if (tags == null || tags.isEmpty()) return "";
        return tags.stream().map(t -> t.name().replace("_", " ")).collect(Collectors.joining(", "));
    }

    private String prettyType(IncidentType type) {
        if (type == null) return "Unknown";
        return type.name().charAt(0) + type.name().substring(1).toLowerCase().replace("_", " ");
    }

    private String severityDotStyle(IncidentSeverity sev) {
        String color = switch (sev) {
            case CRITICAL -> "#ff4757";
            case MAJOR    -> "#f97316";
            case MINOR    -> "#fbbf24";
            default       -> "#60a5fa";
        };
        return "-fx-text-fill: " + color + "; -fx-font-size: 12px;";
    }

    private void addLog(String message) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = "[" + ts + "]  " + message;
        Platform.runLater(() -> {
            logEntries.add(0, entry);
            if (logEntries.size() > 200) logEntries.remove(logEntries.size() - 1);
        });
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);

            if (btnDeclareIncident.getScene() != null) {
                a.initOwner(btnDeclareIncident.getScene().getWindow());
            }

            a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
            a.showAndWait();
        });
    }

    private void showInfo(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);

            if (btnDeclareIncident.getScene() != null) {
                a.initOwner(btnDeclareIncident.getScene().getWindow());
            }

            a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
            a.showAndWait();
        });
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", "");
    }

    private void closeWindow() {
        try {
            // Use btnDeclareIncident or any node that is definitely on the stage
            if (btnDeclareIncident.getScene() != null && btnDeclareIncident.getScene().getWindow() != null) {
                Stage stage = (Stage) btnDeclareIncident.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing emergency window: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Table column binding
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void bindTableColumns() {
        ((TableColumn<PriorityResidentRow, String>) colResidentName)
                .setCellValueFactory(c -> c.getValue().residentNameProperty());
        ((TableColumn<PriorityResidentRow, String>) colVulnerability)
                .setCellValueFactory(c -> c.getValue().vulnerabilityTypeProperty());
        ((TableColumn<PriorityResidentRow, String>) colDistance)
                .setCellValueFactory(c -> c.getValue().distanceFromIncidentProperty());
        ((TableColumn<PriorityResidentRow, String>) colRescueStatus)
                .setCellValueFactory(c -> c.getValue().rescueStatusProperty());

        ((TableColumn<MissingResidentRow, String>) colMissingName)
                .setCellValueFactory(c -> c.getValue().residentNameProperty());
        ((TableColumn<MissingResidentRow, String>) colMissingContact)
                .setCellValueFactory(c -> c.getValue().contactNumberProperty());
        ((TableColumn<MissingResidentRow, String>) colLastKnownLocation)
                .setCellValueFactory(c -> c.getValue().lastKnownLocationProperty());

        ((TableColumn<EvacuationCenterRow, String>) colEvacName)
                .setCellValueFactory(c -> c.getValue().centerNameProperty());
        ((TableColumn<EvacuationCenterRow, String>) colEvacCapacity)
                .setCellValueFactory(c -> c.getValue().capacityProperty());
        ((TableColumn<EvacuationCenterRow, String>) colEvacOccupancy)
                .setCellValueFactory(c -> c.getValue().occupancyProperty());
        ((TableColumn<EvacuationCenterRow, String>) colEvacDistance)
                .setCellValueFactory(c -> c.getValue().distanceProperty());
        ((TableColumn<EvacuationCenterRow, String>) colEvacStatus)
                .setCellValueFactory(c -> c.getValue().statusProperty());

        ((TableColumn<RescueTeamRow, String>) colTeamName)
                .setCellValueFactory(c -> c.getValue().teamNameProperty());
        ((TableColumn<RescueTeamRow, String>) colTeamAvailability)
                .setCellValueFactory(c -> c.getValue().availabilityProperty());
        ((TableColumn<RescueTeamRow, String>) colTeamVehicle)
                .setCellValueFactory(c -> c.getValue().vehicleProperty());
        ((TableColumn<RescueTeamRow, String>) colTeamETA)
                .setCellValueFactory(c -> c.getValue().etaProperty());

        ((TableColumn<ResourceRow, String>) colResourceType)
                .setCellValueFactory(c -> c.getValue().resourceTypeProperty());
        ((TableColumn<ResourceRow, String>) colResourceQuantity)
                .setCellValueFactory(c -> c.getValue().quantityProperty());
        ((TableColumn<ResourceRow, String>) colResourceStatus)
                .setCellValueFactory(c -> c.getValue().statusProperty());


        List<TableColumn<?, ?>> allCols = List.of(
                colResidentName, colVulnerability, colDistance, colRescueStatus, colResidentAction,
                colMissingName, colMissingContact, colLastKnownLocation,
                colEvacName, colEvacCapacity, colEvacOccupancy, colEvacDistance, colEvacStatus,
                colTeamName, colTeamAvailability, colTeamVehicle, colTeamETA,
                colResourceType, colResourceQuantity, colResourceStatus
        );

        for (TableColumn<?, ?> col : allCols) {
            col.setReorderable(false);
            col.setResizable(false);
            if (col != colResidentAction) {
                col.setStyle("-fx-alignment: CENTER-LEFT;");
            }
        }
    }

    private void resetDashboard() {
        Platform.runLater(() -> {
            // 1. Re-enable the Declare button
            btnDeclareIncident.setDisable(false);
            btnDeclareIncident.setText("＋ Declare Incident");

            // 2. Reset KPI Labels
            lblStatus.setText("IDLE");
            lblStatus.setStyle("-fx-text-fill: #9ca3af;"); // Gray color
            lblEmergencyType.setText("No Active Incident");
            lblSeverity.setText("—");
            lblSeverityDot.setStyle("-fx-text-fill: #4b5563;");
            lblCoordinates.setText("0.000, 0.000");
            lblAffectedRadius.setText("0 m");

            // 3. Clear all tables
            tblPriorityResidents.getItems().clear();
            tblMissingResidents.getItems().clear();
            tblRescueTeams.getItems().clear();
            tblEvacuationCenters.getItems().clear();
            tblResources.getItems().clear();

            // 4. Reset Analytics Counters
            totalAffected = 0; rescued = 0; missing = 0; evacuated = 0;
            updateAnalytics();

            // 5. Reset Class State
            this.incidentId = null;
            this.ctx = null;

            // 6. Clear Map (Optional: reloads empty map)
            if (mapWebView != null) {
                mapWebView.getEngine().executeScript("if(window.clearResidentMarkers) clearResidentMarkers();");
                mapWebView.getEngine().executeScript("if(window.map && window.emergencyCircle) map.removeLayer(emergencyCircle);");
            }

            addLog("Dashboard reset. System in IDLE mode.");
            residentStatusCache.clear();
        });
    }
}