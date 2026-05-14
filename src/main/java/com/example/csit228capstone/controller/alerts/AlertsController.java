package com.example.csit228capstone.controller.alerts;

import com.example.csit228capstone.model.alert.Alert;
import com.example.csit228capstone.repository.AlertsRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AlertsController implements Initializable {

    @FXML private Label lblBroadcasts;
    @FXML private Label lblScheduled;
    @FXML private TableView<Alert> alertsTable;
    @FXML private TableColumn<Alert, String> colType;
    @FXML private TableColumn<Alert, com.example.csit228capstone.model.alert.AlertPriority> colPriority;
    @FXML private TableColumn<Alert, String> colMessage;
    @FXML private TableColumn<Alert, String> colSentBy;
    @FXML private TableColumn<Alert, LocalDateTime> colSentDate;
    @FXML private TableColumn<Alert, Void> colAction;

    private final AlertsRepository repo = new AlertsRepository();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        refresh();
    }

    private void setupTable() {
        // 1. Value Factories
        colType.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("body"));
        colSentBy.setCellValueFactory(new PropertyValueFactory<>("sentByName"));
        colSentDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // 2. SAFE Priority Badge Factory
        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(com.example.csit228capstone.model.alert.AlertPriority item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        // 'item' is the Enum, so we use item.name() to get the text
                        String priorityName = item.name().toUpperCase();
                        Label badge = new Label(priorityName);
                        badge.getStyleClass().add("priority-badge");

                        // Assign style classes based on the Enum name
                        if (priorityName.equals("CRITICAL")) {
                            badge.getStyleClass().add("badge-critical");
                        } else if (priorityName.equals("HIGH")) {
                            badge.getStyleClass().add("badge-high");
                        } else if (priorityName.equals("MEDIUM")) {
                            badge.getStyleClass().add("badge-medium");
                        } else {
                            badge.getStyleClass().add("badge-low");
                        }

                        setGraphic(badge);
                        setText(null);
                        setAlignment(Pos.CENTER);
                    } catch (Exception e) {
                        // If something goes wrong, just show the enum name as text
                        setText(item.toString());
                        setGraphic(null);
                    }
                }
            }
        });

        // 3. SAFE Date Factory
        colSentDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(item));
                }
            }
        });

        formatTableColumns();
        setupActionColumn();
    }

    private void formatTableColumns() {
        // Force the table to have height so it doesn't disappear
        alertsTable.setMinHeight(400);
        alertsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colType.setPrefWidth(120);
        colPriority.setPrefWidth(100);
        colMessage.setPrefWidth(250);
        colSentBy.setPrefWidth(140);
        colSentDate.setPrefWidth(170);

        colAction.setMinWidth(190);
        colAction.setMaxWidth(190);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().addAll("quick-action-btn", "qa-info");
                btnDelete.getStyleClass().addAll("quick-action-btn", "qa-danger");
                box.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> openDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // Logic Methods
    @FXML public void openCreateBroadcast(ActionEvent event) { openDialog(null); }

    private void openDialog(Alert alert) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/alerts/AlertsForm.fxml"));
            Parent root = loader.load();
            AlertsFormController ctrl = loader.getController();
            ctrl.setOnSaveCallback(this::refresh);
            if (alert != null) ctrl.loadAlert(alert);

            Stage dialog = new Stage();
            dialog.setTitle(alert == null ? "Create Broadcast" : "Edit Broadcast");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Error opening dialog", e);
        }
    }

    private void confirmAndDelete(Alert alert) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Delete \"" + alert.getTitle() + "\"?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repo.delete(alert.getId());
                refresh();
            } catch (SQLException e) { showError("Delete failed", e); }
        }
    }

    private void refresh() {
        loadTableData();
        updateKPIs();
    }

    private void loadTableData() {
        try {
            List<Alert> alerts = repo.findAll();
            // DEBUG: Check if data is actually coming from the DB
            System.out.println("Loaded alerts: " + alerts.size());
            alertsTable.setItems(FXCollections.observableArrayList(alerts));
        } catch (SQLException e) {
            showError("Load failed", e);
        }
    }

    private void updateKPIs() {
        try {
            lblBroadcasts.setText(String.valueOf(repo.countBroadcasts()));
            lblScheduled.setText(String.valueOf(repo.countScheduled()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showError(String message, Exception e) {
        e.printStackTrace();
        javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        err.setContentText(message + ": " + e.getMessage());
        err.showAndWait();
    }
}