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
import javafx.scene.layout.VBox;           // For VBox
import javafx.scene.control.Label;         // For Label
import javafx.scene.shape.SVGPath;         // For SVGPath
import javafx.geometry.Pos;                // For Pos.CENTER
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

// Added for Truncation & Tooltips
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;

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

        // Center align "Type" and "Sent By" (Plain Text Columns)
        colType.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
            }
        });

        colSentBy.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
            }
        });

        // 2. Priority Badge Factory (Already centered)
        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(com.example.csit228capstone.model.alert.AlertPriority item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String priorityName = item.name().toUpperCase();
                    Label badge = new Label(priorityName);
                    badge.getStyleClass().add("priority-badge");
                    if (priorityName.equals("CRITICAL")) badge.getStyleClass().add("badge-critical");
                    else if (priorityName.equals("HIGH")) badge.getStyleClass().add("badge-high");
                    else if (priorityName.equals("MEDIUM")) badge.getStyleClass().add("badge-medium");
                    else badge.getStyleClass().add("badge-low");

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // 3. Message Truncation & Tooltip (Centered + Double Click to open)
        colMessage.setCellFactory(column -> new TableCell<>() {
            private final Tooltip tooltip = new Tooltip();
            {
                tooltip.setWrapText(true);
                tooltip.setPrefWidth(400);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setOnMouseClicked(null); // Clear the click event for empty rows
                } else {
                    setText(item);
                    setTextOverrun(OverrunStyle.ELLIPSIS);
                    setAlignment(Pos.CENTER);

                    // Tooltip logic
                    tooltip.setText(item);
                    setTooltip(tooltip);

                    // --- NUMBER 2: Double Click Logic ---
                    setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) { // Detects double-click
                            javafx.scene.control.Alert alertPopup = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            alertPopup.setTitle("Full Message");
                            alertPopup.setHeaderText(null);
                            alertPopup.setContentText(item);

                            // Optional: This makes the popup wider for long messages
                            alertPopup.getDialogPane().setPrefWidth(500);

                            alertPopup.showAndWait();
                        }
                    });
                    // ------------------------------------
                }
            }
        });

        // 4. Date Factory (Centered)
        colSentDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(item));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // --- EMPTY STATE ILLUSTRATION ---
        // --- REFINED EMPTY STATE ---
        VBox emptyState = new VBox(20); // Slightly more spacing
        emptyState.setAlignment(Pos.CENTER);

// 1. Modern Notification Bell Icon
        javafx.scene.shape.SVGPath emptyIcon = new javafx.scene.shape.SVGPath();
// Clean, rounded Bell icon path
        emptyIcon.setContent("M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0");
        emptyIcon.setScaleX(5.0);
        emptyIcon.setScaleY(5.0);
        emptyIcon.getStyleClass().add("empty-state-icon");

// 2. Main Text (Clean and modern)
        Label emptyTitle = new Label("All Quiet Here");
        emptyTitle.getStyleClass().add("empty-state-title");

// 3. Sub-text (Helpful instruction)
        Label emptySubtitle = new Label("There are no broadcasts to display right now.\nSent alerts will appear here.");
        emptySubtitle.getStyleClass().add("empty-state-subtitle");
        emptySubtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle);

// Set the placeholder
        alertsTable.setPlaceholder(emptyState);
        formatTableColumns();
        setupActionColumn();
    }

    private void formatTableColumns() {
        alertsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // UI Tip: To center the Header Text as well as the cells:
        alertsTable.getColumns().forEach(column -> {
            column.setReorderable(false);
            // This CSS trick centers the header labels
            column.setStyle("-fx-alignment: CENTER;");
        });

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
            dialog.setResizable(false);
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