package com.example.csit228capstone.controller.dashboard;

import com.example.csit228capstone.controller.evacuation.EvacuationController;
import com.example.csit228capstone.service.EvacuationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.repository.ResidentRepository;
import com.example.csit228capstone.service.DocumentService;
import com.example.csit228capstone.service.IncidentService;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    @FXML private Label lblTotalResidents, lblActiveIncidents, lblPendingDocs, lblEvacuees;
    @FXML private Label lblSenior, lblPwd, lblPregnant, lblChildren;
    @FXML private TableView<Incident> incidentTable;
    @FXML private TableColumn<Incident, String> colSeverity, colType, colLocation, colTime, colStatus;
    @FXML private ScrollPane mainContent;

    private final IncidentService incidentService = new IncidentService();
    private final DocumentService documentService = new DocumentService();
    private final ResidentRepository residentRepo = new ResidentRepository();
    private final EvacuationService evacuationService = new EvacuationService();

    @FXML
    public void initialize() {
        setupIncidentTable();
        loadData();
    }

    private void setupIncidentTable() {
        colSeverity.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSeverity().name()));
        colSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Circle dot = new Circle(6);
                dot.setFill(switch (item) {
                    case "CRITICAL" -> Color.web("#C0392B");
                    case "MAJOR" -> Color.web("#BA7517");
                    default -> Color.web("#3498DB");
                });
                setGraphic(dot);
            }
        });

        colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        colLocation.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getLocationPurok() +
                (cd.getValue().getLocationDetail() != null ? ", " + cd.getValue().getLocationDetail() : "")));
        colTime.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getReportedAt().format(DateTimeFormatter.ofPattern("HH:mm"))));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("status-badge", "status-" + item.toLowerCase());
                setGraphic(badge);
            }
        });
    }

    private void loadData() {
        try {
            lblTotalResidents.setText(String.valueOf(residentRepo.countAll()));
            lblActiveIncidents.setText(String.valueOf(incidentService.getActiveCount()));
            lblPendingDocs.setText(String.valueOf(documentService.getPendingCount()));
            lblEvacuees.setText(String.valueOf(evacuationService.getTotalEvacuees()));


            List<Incident> active = incidentService.getActiveIncidents();
            incidentTable.setItems(FXCollections.observableArrayList(active));

            lblSenior.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.SENIOR_CITIZEN)));
            lblPwd.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.PWD)));
            lblPregnant.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.PREGNANT)));
            lblChildren.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.CHILD_0_5)));
        } catch (Exception e) {
            System.err.println("Dashboard load error: " + e.getMessage());
        }
    }

    @FXML void viewAllIncidents(ActionEvent e) {

    }

    @FXML void reportIncident(ActionEvent e)  {

    }

    @FXML void evacuationOrder(ActionEvent e) {

    }

    @FXML
    void addResident(ActionEvent e) throws Exception {
        try {
            // 1. Load the FXML for the pop-up
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/resident/ResidentForm.fxml"));
            Parent root = loader.load();

            // 2. Create a new Stage (Window)
            Stage stage = new Stage();
            stage.setTitle("Resident Registration");

            // 3. Set the Scene with the loaded FXML
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // 4. Set Modality (Optional: Prevents clicking the dashboard until this is closed)
            stage.initModality(Modality.APPLICATION_MODAL);

            // 5. Show the window
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
