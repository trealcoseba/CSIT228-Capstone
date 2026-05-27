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

    private double pinnedLat = 0;
    private double pinnedLng = 0;
    private boolean locationPinned = false;
    private String pinnedAddress = "";

    private EmergencyController emergencyController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbIncidentType.getItems().setAll(IncidentType.values());
        cbIncidentType.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(IncidentType t) { return t == null ? "" : t.getDisplayName(); }
            @Override public IncidentType fromString(String s) { return null; }
        });

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

        txtRadius.textProperty().addListener((obs, oldVal, newVal) -> {
            if (locationPinned) refreshPreviewCircle();
        });

        loadPreviewMap();
        styleComboBox(cbIncidentType);
        styleComboBox(cbSeverity);
    }

    private void loadPreviewMap() {
        URL mapUrl = getClass().getResource("/map/map.html");
        if (mapUrl == null) return;
        WebEngine engine = previewMap.getEngine();
        engine.load(mapUrl.toExternalForm());
    }

    private void refreshPreviewCircle() {
        double radius = parseRadius();
        if (radius <= 0 || !locationPinned) return;
        WebEngine engine = previewMap.getEngine();
        if (engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            engine.executeScript(String.format(
                    "loadEmergencyOverlay(%f, %f, %f);", pinnedLat, pinnedLng, radius));
        }
    }

    @FXML
    private void handlePickLocation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            MapPickerController mapCtrl = loader.getController();

            Stage stage = new Stage();

            // FIX: Set ownership and window properties BEFORE showAndWait()
            if (btnDeclare.getScene() != null) {
                stage.initOwner(btnDeclare.getScene().getWindow());
            }
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Pin Incident Location");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.setFullScreen(false);

            stage.showAndWait();

            double lat = mapCtrl.getSelectedLatitude();
            double lng = mapCtrl.getSelectedLongitude();
            String address = mapCtrl.getSelectedAddress();

            if (lat != 0 || lng != 0) {
                pinnedLat      = lat;
                pinnedLng      = lng;
                pinnedAddress  = (address != null && !address.isBlank()) ? address : "";
                locationPinned = true;
                lblLatLng.setText(String.format("%.5f,  %.5f  %s",
                        lat, lng, !pinnedAddress.isBlank() ? "— " + truncate(pinnedAddress, 35) : ""));
                clearError();
                refreshPreviewCircle();
            }
        } catch (IOException e) {
            showErrorDialog("Map Error", "Could not open map picker: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeclare() {
        if (cbIncidentType.getValue() == null) { showError("Please select an incident type."); return; }
        if (cbSeverity.getValue() == null) { showError("Please select a severity level."); return; }
        if (!locationPinned) { showError("Please pin the incident location on the map."); return; }
        double radius = parseRadius();
        if (radius <= 0) { showError("Enter a valid radius in meters."); return; }
        clearError();

        IncidentType     type     = cbIncidentType.getValue();
        IncidentSeverity severity = cbSeverity.getValue();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

        // FIX: Set ownership for full-screen compatibility
        if (btnDeclare.getScene() != null) {
            confirm.initOwner(btnDeclare.getScene().getWindow());
        }

        confirm.setTitle("Confirm Emergency Declaration");
        confirm.setHeaderText("⚠  Declare a " + type.getDisplayName() + " emergency?");
        confirm.setContentText("Type: " + type.getDisplayName() + "\n" +
                "Severity: " + severity.name() + "\n" +
                "Radius: " + (int) radius + " m\n\n" +
                "Are you sure you want to proceed?");

        confirm.getButtonTypes().setAll(
                new ButtonType("Yes, Declare", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        confirm.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) return;

            EmergencyContext ctx = new EmergencyContext();
            ctx.setType(type);
            ctx.setSeverity(severity);
            ctx.setLatitude(pinnedLat);
            ctx.setLongitude(pinnedLng);
            ctx.setRadiusMeters(radius);
            ctx.setLocationDetail(pinnedAddress);
            ctx.setReportedBy(UUID.randomUUID());

            String title = txtTitle.getText() != null ? txtTitle.getText().trim() : "";
            ctx.setTitle(title.isBlank() ? type.getDisplayName() + " Incident" : title);
            ctx.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");

            if (emergencyController != null) emergencyController.initWithContext(ctx);
            closeStage();
        });
    }

    @FXML private void handleCancel() { closeStage(); }

    public void setEmergencyController(EmergencyController ctrl) { this.emergencyController = ctrl; }

    // --- Helpers ---

    private void showError(String msg) { lblValidationError.setText("⚠  " + msg); }

    private void clearError() { lblValidationError.setText(""); }

    private void showErrorDialog(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (btnDeclare.getScene() != null && btnDeclare.getScene().getWindow() != null) {
            alert.initOwner(btnDeclare.getScene().getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void closeStage() {
        Stage stage = (Stage) btnDeclare.getScene().getWindow();
        stage.close();
    }

    private double parseRadius() {
        try { return Double.parseDouble(txtRadius.getText().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, max) + "…"; }

    private <T> void styleComboBox(ComboBox<T> comboBox) {
        Callback<ListView<T>, ListCell<T>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white; -fx-background-color: #1f2937; -fx-font-size: 13px;");
                }
            }
        };
        comboBox.setCellFactory(cellFactory);
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                }
            }
        });
    }
}