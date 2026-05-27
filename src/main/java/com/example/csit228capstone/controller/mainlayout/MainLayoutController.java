package com.example.csit228capstone.controller.mainlayout;

import com.example.csit228capstone.controller.dashboard.DashboardController;
import com.example.csit228capstone.controller.emergency.EmergencyAuthController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.example.csit228capstone.service.IncidentService;
import com.example.csit228capstone.util.AlertEventBus;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainLayoutController {

    @FXML private StackPane contentPane;
    @FXML private BorderPane layoutPane;
    @FXML private Label lblActiveIncidents;
    @FXML private VBox sidebar;

    @FXML private Button btnDashboard, btnIncidents, btnResidents, btnResponder;
    @FXML private Button btnEvacuation, btnResources, btnAlerts;
    @FXML private Button btnReports, btnSettings;
    @FXML private Button btnAnalytics;
    @FXML private Button btnLogout;

    private List<Button> navButtons;
    private final IncidentService incidentService = new IncidentService();

    @FXML
    public void initialize() {
        navButtons = List.of(btnDashboard, btnIncidents, btnResidents, btnResponder,
                btnEvacuation, btnResources, btnAlerts, btnReports, btnSettings, btnAnalytics);

        AlertEventBus.getInstance().subscribe(event ->
                javafx.application.Platform.runLater(this::refreshBadge));

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

    private FXMLLoader loadView(String subPackage, String fxmlName, Button activeBtn) {
        setActiveNav(activeBtn);
        try {
            String path = String.format("/com/example/csit228capstone/%s/%s.fxml",
                    subPackage, fxmlName);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DashboardController dc) {
                dc.setMainLayoutController(this);
            }

            contentPane.getChildren().setAll(view);
            return loader;

        } catch (IOException e) {
            e.printStackTrace();
            contentPane.getChildren().setAll(
                    new Label("Error loading " + fxmlName + ": " + e.getMessage()));
            return null;
        }
    }

    private void setActiveNav(Button active) {
        navButtons.forEach(b -> b.getStyleClass().remove("nav-active"));
        if (active != null) active.getStyleClass().add("nav-active");
    }

    @FXML public void showDashboard(ActionEvent e)  { loadView("dashboard",  "Dashboard", btnDashboard); }
    @FXML public void showIncidents(ActionEvent e)  { loadView("incident",   "Incidents", btnIncidents); }
    @FXML public void showResidents(ActionEvent e)  { loadView("resident",   "Residents", btnResidents); }
    @FXML public void showResponders(ActionEvent e) { loadView("responder",  "Responder", btnResponder); }
    @FXML public void showEvacuation(ActionEvent e) { loadView("evacuation", "Evacuation", btnEvacuation); }
    @FXML public void showResources(ActionEvent e)  { loadView("resource",   "Resources", btnResources); }
    @FXML public void showAlerts(ActionEvent e)     { loadView("alerts",     "Alerts",    btnAlerts); }
    @FXML public void showReports(ActionEvent e)    { loadView("report",     "Reports",   btnReports); }
    @FXML public void showSettings(ActionEvent e)   { loadView("settings",   "Settings",  btnSettings); }
    @FXML public void showAnalytics(ActionEvent e)  { loadView("analytics",  "Analytics", btnAnalytics); }

    public void openAddResidentForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/resident/ResidentForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            if (layoutPane.getScene() != null) stage.initOwner(layoutPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setFullScreen(false);
            stage.setResizable(false);

            stage.setTitle("Resident Registration");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    public void openAddIncidentForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/incident/AddIncident.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            if (layoutPane.getScene() != null) stage.initOwner(layoutPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setFullScreen(false);
            stage.setResizable(false);

            stage.setTitle("Incident Registration");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    @FXML
    void activateEmergency(ActionEvent e) {
        try {
            Stage primaryStage = (Stage) layoutPane.getScene().getWindow();

            FXMLLoader authLoader = new FXMLLoader(getClass().getResource(
                    "/com/example/csit228capstone/emergency/EmergencyAuth.fxml"));
            Parent authRoot = authLoader.load();
            EmergencyAuthController authController = authLoader.getController();

            Stage authStage = new Stage();
            authStage.setTitle("Security Check");
            authStage.setScene(new Scene(authRoot));

            authStage.initOwner(primaryStage);
            authStage.initModality(Modality.APPLICATION_MODAL);
            authStage.setResizable(false);
            authStage.initStyle(javafx.stage.StageStyle.UTILITY);

            authStage.showAndWait();

            if (authController.isAuthorized()) {
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource(
                        "/com/example/csit228capstone/emergency/Emergency.fxml"));
                Parent mainRoot = mainLoader.load();

                Stage mainStage = new Stage();

                if (primaryStage.isFullScreen()) {
                    primaryStage.setFullScreen(false);
                    primaryStage.setMaximized(true);
                }

                mainStage.setTitle("LIGTAS-BRGY — Emergency Operations Center");
                mainStage.setScene(new Scene(mainRoot));

                mainStage.initOwner(primaryStage);
                mainStage.setMaximized(true);
                mainStage.show();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void handleLogout(ActionEvent actionEvent) throws Exception {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.setContentText("Any unsaved changes will be lost.");

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        confirm.initOwner(stage);
        confirm.initModality(Modality.APPLICATION_MODAL);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/csit228capstone/login/Login.fxml"));
        Parent root = loader.load();

        stage.getScene().setRoot(root);

        stage.setFullScreen(true);
        stage.centerOnScreen();
    }
}