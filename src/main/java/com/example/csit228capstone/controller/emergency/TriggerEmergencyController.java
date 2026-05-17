package com.example.csit228capstone.controller.emergency;

import com.example.csit228capstone.controller.map.MapPickerController;
import com.example.csit228capstone.model.emergency.EmergencyContext;
import com.example.csit228capstone.model.incident.IncidentSeverity;
import com.example.csit228capstone.model.incident.IncidentType;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

public class TriggerEmergencyController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private ComboBox<IncidentType>     cbIncidentType;
    @FXML private ComboBox<IncidentSeverity> cbSeverity;
    @FXML private TextField                  txtRadius;
    @FXML private TextField                  txtTitle;
    @FXML private TextArea                   txtDescription;
    @FXML private Label                      lblLatLng;
    @FXML private Label                      lblValidationError;
    @FXML private Button                     btnDeclare;
    @FXML private Button                     btnPickLocation;
    @FXML private WebView                    previewMap;

    // ── State ─────────────────────────────────────────────────────────────────
    private double pinnedLat = 0;
    private double pinnedLng = 0;
    private boolean locationPinned = false;
    private String pinnedAddress = "";

    /** The live dashboard controller that will receive the context. */
    private EmergencyController emergencyController;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialise
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Populate incident type combo with display names
        cbIncidentType.getItems().setAll(IncidentType.values());
        cbIncidentType.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(IncidentType t) {
                return t == null ? "" : t.getDisplayName();
            }
            @Override public IncidentType fromString(String s) { return null; }
        });

        // Populate severity combo
        cbSeverity.getItems().setAll(IncidentSeverity.values());
        cbSeverity.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(IncidentSeverity s) {
                if (s == null) return "";
                return switch (s) {
                    case CRITICAL -> "🔴  Critical";
                    case MAJOR    -> "🟠  Major";
                    case MINOR    -> "🟡  Minor";
                    default       -> s.name();
                };
            }
            @Override public IncidentSeverity fromString(String s) { return null; }
        });

        // Live radius preview: update the map circle whenever radius changes
        txtRadius.textProperty().addListener((obs, oldVal, newVal) -> {
            if (locationPinned) refreshPreviewCircle();
        });

        // Load the preview map (same map.html already used by EmergencyController)
        loadPreviewMap();
        styleComboBox(cbIncidentType);
        styleComboBox(cbSeverity);

    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preview map
    // ─────────────────────────────────────────────────────────────────────────

    private void loadPreviewMap() {
        URL mapUrl = getClass().getResource("/map/map.html");
        if (mapUrl == null) return;
        WebEngine engine = previewMap.getEngine();
        engine.load(mapUrl.toExternalForm());
    }

    /**
     * Draws (or redraws) the epicenter marker + radius circle on the preview map.
     * Safe to call any time after the map has loaded and a location has been pinned.
     */
    private void refreshPreviewCircle() {
        double radius = parseRadius();
        if (radius <= 0 || !locationPinned) return;
        WebEngine engine = previewMap.getEngine();
        if (engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            engine.executeScript(String.format(
                    "loadEmergencyOverlay(%f, %f, %f);", pinnedLat, pinnedLng, radius));
        } else {
            // If map isn't ready yet, wait for it
            engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> engine.executeScript(String.format(
                            "loadEmergencyOverlay(%f, %f, %f);", pinnedLat, pinnedLng, radius)));
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button handlers
    // ─────────────────────────────────────────────────────────────────────────

    /** Opens the existing MapPicker modal to let the admin pin the incident location. */
    @FXML
    private void handlePickLocation() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            MapPickerController mapCtrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Pin Incident Location");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            double lat = mapCtrl.getSelectedLatitude();
            double lng = mapCtrl.getSelectedLongitude();
            String address = mapCtrl.getSelectedAddress();

            if (lat != 0 || lng != 0) {
                pinnedLat      = lat;
                pinnedLng      = lng;
                pinnedAddress  = (address != null && !address.isBlank()) ? address : ""; // ← ADD THIS
                locationPinned = true;
                lblLatLng.setText(String.format("%.5f,  %.5f  %s",
                        lat, lng,
                        !pinnedAddress.isBlank() ? "— " + truncate(pinnedAddress, 35) : ""));
                clearError();
                refreshPreviewCircle();
            }
        } catch (IOException e) {
            showError("Could not open map picker: " + e.getMessage());
        }
    }

    /**
     * Validates inputs, shows a confirmation dialog summarising the incident,
     * then — on confirmation — builds an {@link EmergencyContext} and passes it
     * to the waiting {@link EmergencyController}.
     */
    @FXML
    private void handleDeclare() {
        // ── Validation ────────────────────────────────────────────────────────
        if (cbIncidentType.getValue() == null) {
            showError("Please select an incident type."); return;
        }
        if (cbSeverity.getValue() == null) {
            showError("Please select a severity level."); return;
        }
        if (!locationPinned) {
            showError("Please pin the incident location on the map."); return;
        }
        double radius = parseRadius();
        if (radius <= 0) {
            showError("Enter a valid radius in meters (e.g. 500)."); return;
        }
        clearError();

        // ── Confirmation dialog ───────────────────────────────────────────────
        IncidentType     type     = cbIncidentType.getValue();
        IncidentSeverity severity = cbSeverity.getValue();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Emergency Declaration");
        confirm.setHeaderText("⚠  Declare a " + type.getDisplayName() + " emergency?");
        confirm.setContentText(
                "Type       :  " + type.getDisplayName()     + "\n" +
                        "Severity   :  " + severity.name()            + "\n" +
                        "Coordinates:  " + String.format("%.5f, %.5f", pinnedLat, pinnedLng) + "\n" +
                        "Radius     :  " + (int) radius + " m\n\n" +
                        "This will activate the Emergency Operations Center.\n" +
                        "Are you sure you want to proceed?"
        );

        // Style the confirmation buttons
        confirm.getButtonTypes().setAll(
                new ButtonType("Yes, Declare", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Cancel",          ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        confirm.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) return;

            // ── Build context ─────────────────────────────────────────────────
            EmergencyContext ctx = new EmergencyContext();
            ctx.setType(type);
            ctx.setSeverity(severity);
            ctx.setLatitude(pinnedLat);
            ctx.setLongitude(pinnedLng);
            ctx.setRadiusMeters(radius);
            ctx.setLocationDetail(pinnedAddress);
            ctx.setReportedBy(UUID.randomUUID());


            String title = txtTitle.getText() != null ? txtTitle.getText().trim() : "";
            ctx.setTitle(title.isBlank()
                    ? type.getDisplayName() + " Incident"
                    : title);

            String desc = txtDescription.getText() != null ? txtDescription.getText().trim() : "";
            ctx.setDescription(desc);

            // ── Hand off to the dashboard controller ──────────────────────────
            if (emergencyController != null) {
                emergencyController.initWithContext(ctx);
            }

            // Close this dialog
            closeStage();
        });
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Must be called by the parent controller immediately after loading this
     * FXML, so the dialog knows where to send the finished context.
     */
    public void setEmergencyController(EmergencyController ctrl) {
        this.emergencyController = ctrl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private double parseRadius() {
        try {
            return Double.parseDouble(txtRadius.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void showError(String msg) {
        lblValidationError.setText("⚠  " + msg);
    }

    private void clearError() {
        lblValidationError.setText("");
    }

    private void closeStage() {
        Stage stage = (Stage) btnDeclare.getScene().getWindow();
        stage.close();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    // Call this helper for each ComboBox
    private <T> void styleComboBox(ComboBox<T> comboBox) {
        Callback<ListView<T>, ListCell<T>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white; -fx-background-color: #1f2937; -fx-font-size: 13px;");
                }
            }
        };

        comboBox.setCellFactory(cellFactory);

        // This fixes the SELECTED item shown in the button area
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                }
            }
        });
    }
}