package com.example.csit228capstone.controller.responder;

import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.responder.DispatchedResponder;
import com.example.csit228capstone.model.responder.Responder;
import com.example.csit228capstone.repository.DispatchedResponderRepository;
import com.example.csit228capstone.repository.IncidentRepository;
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


public class DispatchedFormController implements Initializable {

    @FXML private ComboBox<Responder>  cbResponder;
    @FXML private ComboBox<String>     cbIncident;
    @FXML private ComboBox<String>     cbSeverity;
    @FXML private TextField            tfLocation;
    @FXML private DatePicker           dpTimeOccurred;
    @FXML private TextField            tfTimeOccurred;
    @FXML private ComboBox<String>     cbDispatchStatus;
    @FXML private Button               btnDispatch;

    @FXML private Label errResponder;
    @FXML private Label errIncident;
    @FXML private Label errLocation;

    private double selectedLat = 0.0;
    private double selectedLng = 0.0;

    private final ResponderRepository           responderRepo = new ResponderRepository();
    private final DispatchedResponderRepository  dispatchRepo  = new DispatchedResponderRepository();
    private final IncidentRepository incidentRepo = new IncidentRepository();

    private Runnable onDispatched;
    private List<com.example.csit228capstone.model.incident.Incident> incidentCache = new java.util.ArrayList<>();

    private static final List<String> SEVERITIES =
            List.of("Low", "Moderate", "High", "Critical");
    private static final List<String> DISPATCH_STATUSES =
            List.of("dispatched", "returned", "cancelled");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbSeverity.setItems(FXCollections.observableArrayList(SEVERITIES));
        cbSeverity.getSelectionModel().select("Moderate");

        cbDispatchStatus.setItems(FXCollections.observableArrayList(DISPATCH_STATUSES));
        cbDispatchStatus.getSelectionModel().select("dispatched");

        dpTimeOccurred.setValue(LocalDate.now());
        tfTimeOccurred.setText(LocalTime.now().withSecond(0).withNano(0).toString());

        loadResponders();
        loadIncidents();

        cbResponder.setConverter(new StringConverter<>() {
            @Override public String toString(Responder r) {
                return r == null ? "" : r.getName() + " [" + r.getAgency() + "]";
            }
            @Override public Responder fromString(String s) { return null; }
        });

        cbIncident.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> autoFillFromIncident(newIdx.intValue()));
    }

    private void autoFillFromIncident(int idx) {
        if (idx < 0 || idx >= incidentCache.size()) return;

        com.example.csit228capstone.model.incident.Incident incident = incidentCache.get(idx);

        String mappedSeverity = resolveSeverity(incident);
        if (mappedSeverity != null) {
            cbSeverity.getSelectionModel().select(mappedSeverity);
        }

        String loc = resolveLocation(incident);
        if (loc != null && !loc.isBlank()) {
            tfLocation.setText(loc);
        }

        double incLat = resolveLatitude(incident);
        double incLng = resolveLongitude(incident);
        if (incLat != 0.0 || incLng != 0.0) {
            selectedLat = incLat;
            selectedLng = incLng;
        }

        LocalDateTime incidentTime = resolveTime(incident);
        if (incidentTime != null) {
            dpTimeOccurred.setValue(incidentTime.toLocalDate());
            tfTimeOccurred.setText(
                    incidentTime.toLocalTime().withSecond(0).withNano(0).toString());
        }

        clearFieldError(errIncident);
    }

    private String resolveSeverity(Incident i) {
        if (i.getSeverity() != null) {
            for (String canonical : SEVERITIES) {
                if (canonical.equalsIgnoreCase(i.getSeverity().getDisplayName())) return canonical;
            }
        }
        if (i.getType() != null) {
            return switch (i.getType().name().toUpperCase()) {
                case "FIRE", "EXPLOSION"   -> "Critical";
                case "FLOOD", "EARTHQUAKE" -> "High";
                case "MEDICAL", "ACCIDENT" -> "Moderate";
                default                    -> "Low";
            };
        }
        return null;
    }

    private String resolveLocation(com.example.csit228capstone.model.incident.Incident i) {
        String detail = i.getLocationDetail();
        return (detail != null && !detail.isBlank()) ? detail : null;
    }

    private double resolveLatitude(Incident i) {
        for (String getter : new String[]{"getLatitude", "getLat"}) {
            try {
                Object val = i.getClass().getMethod(getter).invoke(i);
                if (val instanceof Number n) return n.doubleValue();
                if (val instanceof BigDecimal bd) return bd.doubleValue();
            } catch (Exception ignored) { }
        }
        return 0.0;
    }

    private double resolveLongitude(Incident i) {
        for (String getter : new String[]{"getLongitude", "getLng"}) {
            try {
                Object val = i.getClass().getMethod(getter).invoke(i);
                if (val instanceof Number n) return n.doubleValue();
                if (val instanceof BigDecimal bd) return bd.doubleValue();
            } catch (Exception ignored) { }
        }
        return 0.0;
    }

    private LocalDateTime resolveTime(Incident i) {
        return i.getReportedAt();
    }

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

    @FXML
    private void handleOpenMap() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            com.example.csit228capstone.controller.map.MapPickerController mapCtrl = loader.getController();

            Stage stage = new Stage();

            if (btnDispatch.getScene() != null) {
                stage.initOwner(btnDispatch.getScene().getWindow());
            }

            stage.setTitle("Pin Deployment Location");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setResizable(false);
            stage.setFullScreen(false);

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

    public void preSelectResponder(Responder r) {
        if (r == null) return;
        cbResponder.getItems().stream()
                .filter(x -> x.getId().equals(r.getId()))
                .findFirst()
                .ifPresent(cbResponder.getSelectionModel()::select);
    }

    public void setOnDispatched(Runnable callback) {
        this.onDispatched = callback;
    }

    @FXML
    private void handleDispatch(ActionEvent event) {
        if (!validate()) return;

        Responder selected = cbResponder.getValue();

        UUID incidentId;
        int idx = cbIncident.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < incidentCache.size()) {
            incidentId = incidentCache.get(idx).getId();
        } else {
            try {
                String raw = cbIncident.getValue();
                incidentId = UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                showFieldError(errIncident, "Please select a valid incident from the list.");
                return;
            }
        }

        if (selectedLat == 0.0 && selectedLng == 0.0) {
            showFieldError(errLocation, "Please pin the deployment location on the map.");
            return;
        }
        clearFieldError(errLocation);

        DispatchedResponder dr = new DispatchedResponder();
        dr.setResponderId(selected.getId());
        dr.setIncidentId(incidentId);
        dr.setSeverity(cbSeverity.getValue());
        dr.setStatus(cbDispatchStatus.getValue());
        dr.setDispatchedAt(LocalDateTime.now());

        dr.setLatitude(new BigDecimal(String.valueOf(selectedLat)));
        dr.setLongitude(new BigDecimal(String.valueOf(selectedLng)));

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
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);

        if (btnDispatch.getScene() != null && btnDispatch.getScene().getWindow() != null) {
            alert.initOwner(btnDispatch.getScene().getWindow());
        }

        alert.showAndWait();
    }

    private void closeStage() {
        ((Stage) btnDispatch.getScene().getWindow()).close();
    }
}