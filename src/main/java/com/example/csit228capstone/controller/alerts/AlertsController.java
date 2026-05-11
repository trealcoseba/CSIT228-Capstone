package com.example.csit228capstone.controller.alerts;

import com.example.csit228capstone.model.alert.Alert;
import com.example.csit228capstone.repository.AlertsRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AlertsController implements Initializable {

    // KPI
    @FXML private Label lblBroadcasts;
    @FXML private Label lblScheduled;

    // Table
    @FXML private TableView<Alert>           alertsTable;
    @FXML private TableColumn<Alert, String> colType;
    @FXML private TableColumn<Alert, String> colPriority;
    @FXML private TableColumn<Alert, String> colMessage;
    @FXML private TableColumn<Alert, String> colSentBy;
    @FXML private TableColumn<Alert, String> colSentDate;
    @FXML private TableColumn<Alert, Void>   colAction;

    private final AlertsRepository repo = new AlertsRepository();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        refresh();
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    public void openCreateBroadcast(ActionEvent event) {
        openDialog(null);
    }

    @FXML public void sendBroadcast(ActionEvent e) { openCreateBroadcast(e); }
    @FXML public void sendAlert(ActionEvent e)     { openCreateBroadcast(e); }
    @FXML public void sendNow(ActionEvent e)       { openCreateBroadcast(e); }

    // ── private helpers ───────────────────────────────────────────────────────

    private void setupTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("body"));

        // ← uses sentByName instead of issuedBy (UUID) so the column shows a name
        colSentBy.setCellValueFactory(new PropertyValueFactory<>("sentByName"));

        colSentDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        setupActionColumn();
    }

    private void setupActionColumn() {
        Callback<TableColumn<Alert, Void>, TableCell<Alert, Void>> factory =
                col -> new TableCell<>() {

                    private final Button btnEdit   = new Button("Edit");
                    private final Button btnDelete = new Button("Delete");
                    private final HBox   box       = new HBox(6, btnEdit, btnDelete);

                    {
                        btnEdit.getStyleClass().addAll("quick-action-btn", "qa-info");
                        btnDelete.getStyleClass().addAll("quick-action-btn", "qa-danger");

                        btnEdit.setOnAction(e -> {
                            Alert alert = getTableView().getItems().get(getIndex());
                            openDialog(alert);
                        });

                        btnDelete.setOnAction(e -> {
                            Alert alert = getTableView().getItems().get(getIndex());
                            confirmAndDelete(alert);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
                    }
                };

        colAction.setCellFactory(factory);
    }

    private void openDialog(Alert alert) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/csit228capstone/alerts/AlertsForm.fxml"));
            Parent root = loader.load();

            AlertsFormController ctrl = loader.getController();
            ctrl.setOnSaveCallback(this::refresh);
            if (alert != null) ctrl.loadAlert(alert);

            Stage dialog = new Stage();
            dialog.setTitle(alert == null ? "Create Broadcast" : "Edit Broadcast");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (IOException e) {
            showError("Could not open the broadcast dialog.", e);
        }
    }

    private void confirmAndDelete(Alert alert) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Broadcast");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Delete \"" + alert.getTitle() + "\"? This cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repo.delete(alert.getId());
                refresh();
            } catch (SQLException e) {
                showError("Failed to delete the alert.", e);
            }
        }
    }

    private void refresh() {
        loadTableData();
        updateKPIs();
    }

    private void loadTableData() {
        try {
            List<Alert> alerts = repo.findAll();
            alertsTable.setItems(FXCollections.observableArrayList(alerts));
        } catch (SQLException e) {
            showError("Failed to load alerts.", e);
        }
    }

    private void updateKPIs() {
        try {
            lblBroadcasts.setText(String.valueOf(repo.countBroadcasts()));
            lblScheduled.setText(String.valueOf(repo.countScheduled()));
        } catch (SQLException e) {
            System.err.println("KPI update failed: " + e.getMessage());
        }
    }

    private void showError(String message, Exception e) {
        e.printStackTrace();
        javafx.scene.control.Alert err =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        err.setTitle("Error");
        err.setHeaderText(null);
        err.setContentText(message + "\n" + e.getMessage());
        err.showAndWait();
    }
}