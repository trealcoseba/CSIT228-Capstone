package com.example.csit228capstone.controller.alerts;

import com.example.csit228capstone.model.alert.Alert;
import com.example.csit228capstone.model.alert.AlertPriority;
import com.example.csit228capstone.repository.AlertsRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

public class AlertsFormController implements Initializable {

    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbPriority;
    @FXML private TextArea         txtMessage;
    @FXML private TextField        txtSentBy;
    @FXML private CheckBox         chkSchedule;
    @FXML private DatePicker       dpExpires;
    @FXML private Label            lblStatus;

    private Alert existingAlert;
    private Runnable onSaveCallback;
    private final AlertsRepository repo = new AlertsRepository();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbType.getItems().addAll(
                "Weather Advisory", "Flood Warning", "Evacuation Order",
                "Health Notice", "Security Alert", "General Announcement"
        );

        // Populate UI with Uppercase names for professional look
        cmbPriority.getItems().clear();
        for (AlertPriority p : AlertPriority.values()) {
            cmbPriority.getItems().add(p.name());
        }
        cmbPriority.setValue("MEDIUM");

        dpExpires.setDisable(true);
        chkSchedule.selectedProperty().addListener((obs, old, on) -> dpExpires.setDisable(!on));
    }

    public void loadAlert(Alert alert) {
        this.existingAlert = alert;
        cmbType.setValue(alert.getTitle());

        // Ensure the UI shows the priority in Uppercase even if DB is lowercase
        if (alert.getPriority() != null) {
            cmbPriority.setValue(alert.getPriority().name().toUpperCase());
        }

        txtMessage.setText(alert.getBody());
        txtSentBy.setText(alert.getSentByName() != null ? alert.getSentByName() : "");

        if (alert.getExpiresAt() != null) {
            chkSchedule.setSelected(true);
            dpExpires.setValue(alert.getExpiresAt().toLocalDate());
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSend(ActionEvent event) {
        if (!validateForm()) return;
        try {
            if (existingAlert != null) {
                applyFormTo(existingAlert, false);
                repo.update(existingAlert);
            } else {
                repo.save(buildAlert(false));
            }
            notifyAndClose();
        } catch (SQLException e) {
            showStatus("DB error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSchedule(ActionEvent event) {
        if (!validateForm()) return;
        if (dpExpires.getValue() == null) {
            showStatus("Please pick a date to schedule.", true);
            return;
        }
        try {
            if (existingAlert != null) {
                applyFormTo(existingAlert, true);
                repo.update(existingAlert);
            } else {
                repo.save(buildAlert(true));
            }
            notifyAndClose();
        } catch (SQLException e) {
            showStatus("DB error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private Alert buildAlert(boolean useSchedule) {
        Alert a = new Alert();
        a.setId(UUID.randomUUID());
        a.setBroadcast(true);
        a.setCreatedAt(LocalDateTime.now());
        applyFormTo(a, useSchedule);
        return a;
    }

    private void applyFormTo(Alert a, boolean useSchedule) {
        a.setTitle(cmbType.getValue());
        a.setBody(txtMessage.getText().trim());

        // CRITICAL FIX: Convert UI String to Uppercase before valueOf
        String selectedPriority = cmbPriority.getValue();
        if (selectedPriority != null) {
            try {
                // valueOf is case-sensitive, so we MUST use toUpperCase()
                a.setPriority(AlertPriority.valueOf(selectedPriority.toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid priority: " + selectedPriority);
                a.setPriority(AlertPriority.MEDIUM); // safe fallback
            }
        }

        a.setSentByName(txtSentBy.getText().trim());
        a.setExpiresAt(useSchedule && dpExpires.getValue() != null
                ? dpExpires.getValue().atTime(23, 59) : null);
    }

    private boolean validateForm() {
        if (cmbType.getValue() == null || cmbType.getValue().isBlank()) {
            showStatus("Please select an alert type.", true); return false;
        }
        if (cmbPriority.getValue() == null) {
            showStatus("Please select a priority.", true); return false;
        }
        if (txtMessage.getText() == null || txtMessage.getText().isBlank()) {
            showStatus("Please enter a message.", true); return false;
        }
        if (txtSentBy.getText() == null || txtSentBy.getText().isBlank()) {
            showStatus("Please enter who is sending this broadcast.", true); return false;
        }
        return true;
    }

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setStyle(error
                ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
                : "-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void notifyAndClose() {
        if (onSaveCallback != null) onSaveCallback.run();
        closeWindow();
    }

    private void closeWindow() {
        if (txtMessage.getScene() != null) {
            ((Stage) txtMessage.getScene().getWindow()).close();
        }
    }
}