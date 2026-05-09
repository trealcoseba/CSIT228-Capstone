package com.example.csit228capstone.controller.responder;

import com.example.csit228capstone.model.responder.DispatchedResponder;
import com.example.csit228capstone.model.responder.Responder;
import com.example.csit228capstone.repository.DispatchedResponderRepository;
import com.example.csit228capstone.repository.ResponderRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for dispatch_form.fxml.
 * Usage:
 *   DispatchFormController ctrl = FxmlUtil.openDialog(...);
 *   ctrl.preSelectResponder(responder); // optional — pre-fill from registry click
 *   ctrl.setIncidentIds(incidentIds);   // pass in list of UUIDs
 *   ctrl.setOnDispatched(this::loadData);
 */
public class DispatchedFormController implements Initializable {

    // ─── FXML ────────────────────────────────────────────────────────────────────
    @FXML private ComboBox<Responder>  cbResponder;
    @FXML private ComboBox<String>     cbIncident;
    @FXML private ComboBox<String>     cbSeverity;
    @FXML private TextField            tfLocation;
    @FXML private DatePicker           dpTimeOccurred;
    @FXML private TextField            tfTimeOccurred;
    @FXML private ComboBox<String>     cbDispatchStatus;
    @FXML private Button               btnDispatch;

    // Error labels
    @FXML private Label errResponder;
    @FXML private Label errIncident;
    @FXML private Label errLocation;

    // Coordinates captured from map picker
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;

    // ─── State ────────────────────────────────────────────────────────────────────
    private final ResponderRepository           responderRepo = new ResponderRepository();
    private final DispatchedResponderRepository  dispatchRepo  = new DispatchedResponderRepository();
    private final com.example.csit228capstone.repository.IncidentRepository incidentRepo =
            new com.example.csit228capstone.repository.IncidentRepository();

    private Runnable onDispatched;
    private List<com.example.csit228capstone.model.incident.Incident> incidentCache = new java.util.ArrayList<>();

    private static final List<String> SEVERITIES =
            List.of("Low", "Moderate", "High", "Critical");
    private static final List<String> DISPATCH_STATUSES =
            List.of("dispatched", "returned", "cancelled");

    // ─── Init ─────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbSeverity.setItems(FXCollections.observableArrayList(SEVERITIES));
        cbSeverity.getSelectionModel().select("Moderate");

        cbDispatchStatus.setItems(FXCollections.observableArrayList(DISPATCH_STATUSES));
        cbDispatchStatus.getSelectionModel().select("dispatched");

        dpTimeOccurred.setValue(LocalDate.now());
        tfTimeOccurred.setText(LocalTime.now().withSecond(0).withNano(0).toString());

        // Load responders and incidents into ComboBoxes
        loadResponders();
        loadIncidents();

        // Display name in combobox
        cbResponder.setConverter(new StringConverter<>() {
            @Override public String toString(Responder r) {
                return r == null ? "" : r.getName() + " [" + r.getAgency() + "]";
            }
            @Override public Responder fromString(String s) { return null; }
        });

        // ── Auto-fill fields when an incident is chosen ──────────────────────────
        cbIncident.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> autoFillFromIncident(newIdx.intValue()));
    }

    /**
     * Populates severity, location, and time fields from the selected incident.
     * Only overrides a field if the incident carries a non-null/non-blank value,
     * so manually pre-set values are preserved when the incident has no data.
     */
    private void autoFillFromIncident(int idx) {
        if (idx < 0 || idx >= incidentCache.size()) return;

        com.example.csit228capstone.model.incident.Incident incident = incidentCache.get(idx);

        // ── Severity ──────────────────────────────────────────────────────────────
        // Map the incident's severity/type to one of our ComboBox values if the
        // Incident model exposes a severity field.  Fall back to type-based mapping
        // if no explicit severity is present.
        String mappedSeverity = resolveSeverity(incident);
        if (mappedSeverity != null) {
            cbSeverity.getSelectionModel().select(mappedSeverity);
        }

        // ── Location / coordinates ────────────────────────────────────────────────
        // Prefer a dedicated location/address field; fall back to lat/lng display.
        String loc = resolveLocation(incident);
        if (loc != null && !loc.isBlank()) {
            tfLocation.setText(loc);
        }

        // If the incident carries coordinates, use them so the map pin is correct.
        double incLat = resolveLatitude(incident);
        double incLng = resolveLongitude(incident);
        if (incLat != 0.0 || incLng != 0.0) {
            selectedLat = incLat;
            selectedLng = incLng;
        }

        // ── Time occurred ─────────────────────────────────────────────────────────
        LocalDateTime incidentTime = resolveTime(incident);
        if (incidentTime != null) {
            dpTimeOccurred.setValue(incidentTime.toLocalDate());
            tfTimeOccurred.setText(
                    incidentTime.toLocalTime().withSecond(0).withNano(0).toString());
        }

        // Clear any stale incident error now that a proper selection was made
        clearFieldError(errIncident);
    }

    // ── Resolver helpers ─────────────────────────────────────────────────────────
    // These translate Incident fields → form values.  Adjust the method bodies to
    // match whatever getters your Incident model actually exposes.

    /**
     * Returns one of {"Low","Moderate","High","Critical"} based on the incident,
     * or null if no severity information is available.
     */
    private String resolveSeverity(com.example.csit228capstone.model.incident.Incident i) {
        // Option A — if Incident has getSeverity() returning a String
        try {
            String sev = (String) i.getClass().getMethod("getSeverity").invoke(i);
            if (sev != null) {
                // Normalize casing: "high" → "High", "CRITICAL" → "Critical", etc.
                for (String canonical : SEVERITIES) {
                    if (canonical.equalsIgnoreCase(sev.trim())) return canonical;
                }
                return sev; // return as-is if not a standard value
            }
        } catch (Exception ignored) { }

        // Option B — derive from incident type
        if (i.getType() != null) {
            return switch (i.getType().name().toUpperCase()) {
                case "FIRE", "EXPLOSION"          -> "Critical";
                case "FLOOD", "EARTHQUAKE"        -> "High";
                case "MEDICAL", "ACCIDENT"        -> "Moderate";
                default                           -> "Low";
            };
        }

        return null;
    }

    /** Returns a human-readable location string from the incident, or null. */
    private String resolveLocation(com.example.csit228capstone.model.incident.Incident i) {
        // Try getLocation() first, then getAddress()
        for (String getter : new String[]{"getLocation", "getAddress"}) {
            try {
                Object val = i.getClass().getMethod(getter).invoke(i);
                if (val instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private double resolveLatitude(com.example.csit228capstone.model.incident.Incident i) {
        for (String getter : new String[]{"getLatitude", "getLat"}) {
            try {
                Object val = i.getClass().getMethod(getter).invoke(i);
                if (val instanceof Number n) return n.doubleValue();
                if (val instanceof BigDecimal bd) return bd.doubleValue();
            } catch (Exception ignored) { }
        }
        return 0.0;
    }

    private double resolveLongitude(com.example.csit228capstone.model.incident.Incident i) {
        for (String getter : new String[]{"getLongitude", "getLng"}) {
            try {
                Object val = i.getClass().getMethod(getter).invoke(i);
                if (val instanceof Number n) return n.doubleValue();
                if (val instanceof BigDecimal bd) return bd.doubleValue();
            } catch (Exception ignored) { }
        }
        return 0.0;
    }

    private LocalDateTime resolveTime(com.example.csit228capstone.model.incident.Incident i) {
        for (String getter : new String[]{"getTimeOccurred", "getOccurredAt", "getCreatedAt", "getTimestamp"}) {
            try {
                Object val = i.getClass().getMethod(getter).invoke(i);
                if (val instanceof LocalDateTime ldt) return ldt;
            } catch (Exception ignored) { }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────────

    private void loadResponders() {
        try {
            List<Responder> all = responderRepo.findAll();
            cbResponder.setItems(FXCollections.observableArrayList(all));
        } catch (SQLException ex) {
            showError("Could not load responders: " + ex.getMessage());
        }
    }

    private void loadIncidents() {
        try {
            incidentCache = incidentRepo.findAll();
            // Label: "TYPE · Short title  [ID]"
            List<String> labels = incidentCache.stream()
                    .map(i -> {
                        String type  = i.getType() != null ? i.getType().name() : "—";
                        String title = i.getTitle() != null && !i.getTitle().isBlank()
                                ? (i.getTitle().length() > 28
                                   ? i.getTitle().substring(0, 28) + "…"
                                   : i.getTitle())
                                : "Untitled";
                        String shortId = i.getId().toString().substring(0, 8).toUpperCase();
                        return type + "  ·  " + title + "  [" + shortId + "]";
                    })
                    .toList();
            cbIncident.setItems(FXCollections.observableArrayList(labels));
        } catch (Exception ex) {
            showError("Could not load incidents: " + ex.getMessage());
        }
    }

    // ─── Map Picker ───────────────────────────────────────────────────────────────

    @FXML
    private void handleOpenMap() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            com.example.csit228capstone.controller.map.MapPickerController mapCtrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Pin Deployment Location");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            String address = mapCtrl.getSelectedAddress();
            selectedLat    = mapCtrl.getSelectedLatitude();
            selectedLng    = mapCtrl.getSelectedLongitude();

            if (address != null && !address.isEmpty()) {
                tfLocation.setText(address);
            }
        } catch (IOException ex) {
            showError("Could not open map: " + ex.getMessage());
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────────

    public void preSelectResponder(Responder r) {
        if (r == null) return;
        cbResponder.getItems().stream()
                .filter(x -> x.getId().equals(r.getId()))
                .findFirst()
                .ifPresent(cbResponder.getSelectionModel()::select);
    }

    /**
     * Optional — incidents are loaded from DB automatically in initialize().
     * Kept for API compatibility; passing IDs here will override the DB list.
     */
    public void setIncidentIds(List<UUID> ids) {
        incidentCache = ids.stream().map(id -> {
            var i = new com.example.csit228capstone.model.incident.Incident();
            i.setId(id);
            return i;
        }).toList();
        List<String> labels = ids.stream()
                .map(id -> id.toString().substring(0, 8).toUpperCase())
                .toList();
        cbIncident.setItems(FXCollections.observableArrayList(labels));
    }

    public void setOnDispatched(Runnable callback) {
        this.onDispatched = callback;
    }

    // ─── Handlers ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleDispatch(ActionEvent event) {
        if (!validate()) return;

        Responder selected = cbResponder.getValue();

        // Resolve incident UUID from cache index
        UUID incidentId;
        int idx = cbIncident.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < incidentCache.size()) {
            incidentId = incidentCache.get(idx).getId();
        } else {
            // Fallback: try to parse typed text as a raw UUID
            try {
                String raw = cbIncident.getValue();
                // Extract bracketed short ID if user typed the full label
                incidentId = UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                showFieldError(errIncident, "Please select a valid incident from the list.");
                return;
            }
        }

        // Location validation
        if (selectedLat == 0.0 && selectedLng == 0.0) {
            showFieldError(errLocation, "Please pin the deployment location on the map.");
            return;
        }
        clearFieldError(errLocation);

        // Build model
        DispatchedResponder dr = new DispatchedResponder();
        dr.setResponderId(selected.getId());
        dr.setIncidentId(incidentId);
        dr.setSeverity(cbSeverity.getValue());
        dr.setStatus(cbDispatchStatus.getValue());
        dr.setDispatchedAt(LocalDateTime.now());

        // Coordinates from map picker
        dr.setLatitude(new BigDecimal(String.valueOf(selectedLat)));
        dr.setLongitude(new BigDecimal(String.valueOf(selectedLng)));

        // Time occurred
        LocalDate date = dpTimeOccurred.getValue();
        String timeStr = tfTimeOccurred.getText().trim();
        if (date != null && !timeStr.isEmpty()) {
            try {
                LocalTime t = LocalTime.parse(timeStr);
                dr.setTimeOccurred(LocalDateTime.of(date, t));
            } catch (DateTimeParseException ignored) { }
        }

        try {
            dispatchRepo.save(dr);

            // If dispatched → mark responder as on_mission
            if ("dispatched".equals(dr.getStatus())) {
                responderRepo.updateStatus(selected.getId(), "on_mission");
            }

            if (onDispatched != null) onDispatched.run();
            closeStage();

        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    // ─── Validation ───────────────────────────────────────────────────────────────

    private boolean validate() {
        boolean ok = true;

        if (cbResponder.getValue() == null) {
            showFieldError(errResponder, "Please select a responder.");
            ok = false;
        } else {
            clearFieldError(errResponder);
        }

        if (cbIncident.getValue() == null || cbIncident.getValue().isBlank()) {
            showFieldError(errIncident, "Please select or enter an incident ID.");
            ok = false;
        } else {
            clearFieldError(errIncident);
        }

        return ok;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void showFieldError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void clearFieldError(Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void closeStage() {
        ((Stage) btnDispatch.getScene().getWindow()).close();
    }
}