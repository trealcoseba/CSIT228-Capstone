package com.example.csit228capstone.controller.emergency;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.web.WebView;

public class EmergencyController {

    // ── Top Bar ──────────────────────────────────────────────────────
    @FXML private Label lblSystemTime;

    // ── KPI Cards ────────────────────────────────────────────────────
    @FXML private Label lblEmergencyType;
    @FXML private Label lblSeverityDot;
    @FXML private Label lblSeverity;
    @FXML private Label lblTimestamp;
    @FXML private Label lblBarangay;
    @FXML private Label lblStatus;
    @FXML private Label lblCoordinates;
    @FXML private Label lblAffectedRadius;

    // ── Action Buttons ───────────────────────────────────────────────
    @FXML private Button btnActivateDispatch;
    @FXML private Button btnNotifyResidents;
    @FXML private Button btnRefreshData;
    @FXML private Button btnEndEmergency;

    // ── Left: Priority Residents ─────────────────────────────────────
    @FXML private Label lblResidentCount;
    @FXML private TableView<?> tblPriorityResidents;
    @FXML private TableColumn<?, ?> colResidentName;
    @FXML private TableColumn<?, ?> colVulnerability;
    @FXML private TableColumn<?, ?> colDistance;
    @FXML private TableColumn<?, ?> colRescueStatus;

    // ── Left: Missing Residents ───────────────────────────────────────
    @FXML private Label lblMissingCount;
    @FXML private TableView<?> tblMissingResidents;
    @FXML private TableColumn<?, ?> colMissingName;
    @FXML private TableColumn<?, ?> colMissingContact;
    @FXML private TableColumn<?, ?> colLastKnownLocation;

    // ── Center: Map ───────────────────────────────────────────────────
    @FXML private WebView mapWebView;
    @FXML private Label lblMapCoords;

    // ── Right: Evacuation Centers ─────────────────────────────────────
    @FXML private Label lblEvacCenterCount;
    @FXML private TableView<?> tblEvacuationCenters;
    @FXML private TableColumn<?, ?> colEvacName;
    @FXML private TableColumn<?, ?> colEvacCapacity;
    @FXML private TableColumn<?, ?> colEvacOccupancy;
    @FXML private TableColumn<?, ?> colEvacDistance;
    @FXML private TableColumn<?, ?> colEvacStatus;

    // ── Right: Rescue Teams ───────────────────────────────────────────
    @FXML private Label lblTeamCount;
    @FXML private TableView<?> tblRescueTeams;
    @FXML private TableColumn<?, ?> colTeamName;
    @FXML private TableColumn<?, ?> colTeamAvailability;
    @FXML private TableColumn<?, ?> colTeamVehicle;
    @FXML private TableColumn<?, ?> colTeamETA;

    // ── Right: Resources ──────────────────────────────────────────────
    @FXML private Label lblResourceCount;
    @FXML private Label lblReliefPct;
    @FXML private Label lblMedPct;
    @FXML private Label lblEvacCapPct;
    @FXML private ProgressBar pbRelief;
    @FXML private ProgressBar pbMed;
    @FXML private ProgressBar pbEvacCap;
    @FXML private TableView<?> tblResources;
    @FXML private TableColumn<?, ?> colResourceType;
    @FXML private TableColumn<?, ?> colResourceQuantity;
    @FXML private TableColumn<?, ?> colResourceStatus;

    // ── Bottom: Logs ──────────────────────────────────────────────────
    @FXML private ListView<?> listIncidentLogs;

    // ── Bottom: Analytics ─────────────────────────────────────────────
    @FXML private BarChart<?, ?> chartAnalytics;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private Label lblTotalAffected;
    @FXML private Label lblRescued;
    @FXML private Label lblMissing;
    @FXML private Label lblEvacuated;

    // ── Button Handlers ───────────────────────────────────────────────

    @FXML
    void assignDispatch(ActionEvent e) {
        // TODO: implement dispatch activation logic
    }

    @FXML
    void notifyResidents(ActionEvent e) {
        // TODO: implement resident notification logic
    }

    @FXML
    void refreshDashboard(ActionEvent e) {
        // TODO: implement dashboard refresh logic
    }

    @FXML
    void endEmergency(ActionEvent e) {
        // TODO: implement end emergency logic
    }
}