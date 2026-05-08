package com.example.csit228capstone.controller.mainlayout;

import com.example.csit228capstone.controller.emergency.EmergencyAuthController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.example.csit228capstone.service.IncidentService;
import com.example.csit228capstone.util.AlertEventBus;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

public class MainLayoutController {


    @FXML private StackPane contentPane;
    @FXML private BorderPane layoutPane;
    @FXML private Label lblActiveIncidents;
    @FXML private VBox sidebar;

    @FXML private Button btnDashboard, btnIncidents, btnResidents, btnDocuments;
    @FXML private Button btnEvacuation, btnResources, btnAlerts;
    @FXML private Button btnReports, btnChatbot, btnSettings;
    @FXML private Button btnAnalytics;
    @FXML private Button btnLogout;

    private List<Button> navButtons;
    private final IncidentService incidentService = new IncidentService();

    @FXML
    public void initialize() {
        navButtons = List.of(btnDashboard, btnIncidents, btnResidents, btnDocuments,
                btnEvacuation, btnResources, btnAlerts, btnReports, btnChatbot, btnSettings, btnAnalytics);

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
    @FXML void showAnalytics(ActionEvent e)  { loadView("analytics", "Analytics", btnAnalytics); }

    @FXML
    void activateEmergency(ActionEvent e) {
        try {
            FXMLLoader authLoader = new FXMLLoader(getClass().getResource(
                    "/com/example/csit228capstone/emergency/EmergencyAuth.fxml"
            ));
            Parent authRoot = authLoader.load();

            EmergencyAuthController authController = authLoader.getController();

            Stage authStage = new Stage();
            authStage.setTitle("Security Check");
            authStage.setScene(new Scene(authRoot));

            authStage.initModality(Modality.APPLICATION_MODAL);
            authStage.setResizable(false);
            authStage.initStyle(javafx.stage.StageStyle.UTILITY);

            authStage.showAndWait();

            if (authController.isAuthorized()) {
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource(
                        "/com/example/csit228capstone/emergency/Emergency.fxml"
                ));
                Parent mainRoot = mainLoader.load();

                Stage mainStage = new Stage();
                mainStage.setTitle("LIGTAS-BRGY — Emergency Operations Center");
                mainStage.setScene(new Scene(mainRoot));
                mainStage.setMaximized(true);
                mainStage.show();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void handleLogout(ActionEvent actionEvent) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/csit228capstone/login/Login.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1440, 900);
        scene.getStylesheets().add(
                getClass().getResource("/css/application.css").toExternalForm()
        );

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.show();
    }


}