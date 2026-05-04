package com.example.csit228capstone.controller.mainlayout;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.example.csit228capstone.service.IncidentService;
import com.example.csit228capstone.util.AlertEventBus;

import java.io.IOException;
import java.util.List;

public class MainLayoutController {

    @FXML private StackPane contentPane;
    @FXML private Label lblActiveIncidents;
    @FXML private VBox sidebar;

    @FXML private Button btnDashboard, btnIncidents, btnResidents, btnDocuments;
    @FXML private Button btnEvacuation, btnResources, btnAlerts;
    @FXML private Button btnReports, btnChatbot, btnSettings;

    private List<Button> navButtons;
    private final IncidentService incidentService = new IncidentService();

    @FXML
    public void initialize() {
        navButtons = List.of(btnDashboard, btnIncidents, btnResidents, btnDocuments,
                btnEvacuation, btnResources, btnAlerts, btnReports, btnChatbot, btnSettings);

        // Subscribe to events for live badge updates
        AlertEventBus.getInstance().subscribe(event -> javafx.application.Platform.runLater(this::refreshBadge));

        // Default view
        showDashboard(null);
        refreshBadge();
    }

    private void refreshBadge() {
        try {
            int count = incidentService.getActiveCount();
            lblActiveIncidents.setText("● " + count + " Active Incidents");
            lblActiveIncidents.getStyleClass().removeAll("badge-ok", "badge-alert");
            lblActiveIncidents.getStyleClass().add(count > 0 ? "badge-alert" : "badge-ok");
        } catch (Exception ignored) {
            lblActiveIncidents.setText("● -- Active Incidents");
        }
    }

    /**
     * MODIFIED: Now accepts a sub-package (folder) and the fxml filename.
     */
    private void loadView(String subPackage, String fxmlName, Button activeBtn) {
        setActiveNav(activeBtn);
        try {
            // Construct path dynamically: /com/example/csit228capstone/SUBPACKAGE/FILENAME.fxml
            String path = String.format("/com/example/csit228capstone/%s/%s.fxml", subPackage, fxmlName);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node view = loader.load();

            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            contentPane.getChildren().setAll(new Label("Error loading " + fxmlName + " in " + subPackage + ": " + e.getMessage()));
        }
    }

    private void setActiveNav(Button active) {
        navButtons.forEach(b -> b.getStyleClass().remove("nav-active"));
        if (active != null) active.getStyleClass().add("nav-active");
    }

    // UPDATED NAVIGATION CALLS
    @FXML void showDashboard(ActionEvent e)  { loadView("dashboard", "Dashboard", btnDashboard); }
    @FXML void showIncidents(ActionEvent e)  { loadView("incident", "Incidents", btnIncidents); }
    @FXML void showResidents(ActionEvent e)  { loadView("resident", "Residents", btnResidents); }

    // Example calls for other potential sub-packages
    @FXML void showDocuments(ActionEvent e)  { loadView("document", "Documents", btnDocuments); }
    @FXML void showEvacuation(ActionEvent e) { loadView("evacuation", "Evacuation", btnEvacuation); }
    @FXML void showResources(ActionEvent e)  { loadView("resource", "Resources", btnResources); }
    @FXML void showAlerts(ActionEvent e)     { loadView("alerts", "Alerts", btnAlerts); }
    @FXML void showReports(ActionEvent e)    { loadView("report", "Reports", btnReports); }
    @FXML void showChatbot(ActionEvent e)    { loadView("chatbot", "Chatbot", btnChatbot); }
    @FXML void showSettings(ActionEvent e)   { loadView("settings", "Settings", btnSettings); }

    @FXML
    void activateEmergency(ActionEvent e) {
        System.out.println("EMERGENCY MODE ACTIVATED");
    }
}