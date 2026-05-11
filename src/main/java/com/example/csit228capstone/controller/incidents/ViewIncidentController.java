package com.example.csit228capstone.controller.incidents;

import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.incident.IncidentStatus;
import com.example.csit228capstone.repository.IncidentRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ViewIncidentController {

    @FXML private Label lblTitle;
    @FXML private Label lblDate;
    @FXML private Label lblType;
    @FXML private Label lblSeverity;
    @FXML private ComboBox<IncidentStatus> cbStatus;
    @FXML private Label lblLocation;
    @FXML private TextArea txtDescription;

    private Incident currentIncident;
    private IncidentRepository repository = new IncidentRepository();
    private boolean isUpdated = false;

    public void setIncidentData(Incident incident) {
        this.currentIncident = incident;

        lblTitle.setText(incident.getTitle() != null ? incident.getTitle() : "Untitled Incident");
        lblType.setText(incident.getType().name());
        lblLocation.setText(incident.getLocationPurok() +
                (incident.getLocationDetail() != null ? " - " + incident.getLocationDetail() : ""));
        txtDescription.setText(incident.getDescription() != null ? incident.getDescription() : "No description provided.");

        if (incident.getReportedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            lblDate.setText("Reported on: " + incident.getReportedAt().format(formatter));
        }

        String severityName = incident.getSeverity().name();
        lblSeverity.setText(severityName);
        String sevColor = switch (severityName) {
            case "CRITICAL" -> "#c0392b";
            case "MAJOR", "HIGH" -> "#e67e22";
            case "MODERATE" -> "#f39c12";
            default -> "#27ae60";
        };
        lblSeverity.setStyle("-fx-padding: 4 12; -fx-background-radius: 12; -fx-text-fill: white; -fx-background-color: " + sevColor + ";");

        cbStatus.setItems(FXCollections.observableArrayList(IncidentStatus.values()));
        cbStatus.setValue(incident.getStatus());
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        try {
            IncidentStatus newStatus = cbStatus.getValue();
                if (newStatus != currentIncident.getStatus()) {
                    UUID adminId = UUID.randomUUID();
                    String note = "Status updated via Admin Dashboard";
                    repository.updateStatus(currentIncident.getId(), newStatus, adminId, note);
                    currentIncident.setStatus(newStatus);
                    isUpdated = true;
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Incident status successfully updated to " + newStatus.getDisplayName() + "!");
                    alert.showAndWait();
                    handleClose(null);
                } else {
                    handleClose(null);
                }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update incident: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblTitle.getScene().getWindow();
        stage.close();
    }
}