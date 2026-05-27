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
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

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

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private ComboBox<String> cmbFilterPriority;


    private final ObservableList<Alert> masterData = FXCollections.observableArrayList();

    private final AlertsRepository repo = new AlertsRepository();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();
        refresh();
    }

    private void setupFilters() {
        cmbFilterType.getItems().addAll("All Types", "Weather Advisory", "Flood Warning", "Evacuation Order", "Security Alert");
        cmbFilterPriority.getItems().addAll("All Priorities", "CRITICAL", "HIGH", "MEDIUM", "LOW");

        txtSearch.textProperty().addListener((obs, old, val) -> applyFilters());
        cmbFilterType.valueProperty().addListener((obs, old, val) -> applyFilters());
        cmbFilterPriority.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void setupTable() {

        colType.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("body"));
        colSentBy.setCellValueFactory(new PropertyValueFactory<>("sentByName"));
        colSentDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

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
                    setOnMouseClicked(null);
                } else {
                    setText(item);
                    setTextOverrun(OverrunStyle.ELLIPSIS);
                    setAlignment(Pos.CENTER);


                    tooltip.setText(item);
                    setTooltip(tooltip);


                    setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            javafx.scene.control.Alert alertPopup = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);

                            if (getTableView().getScene() != null) {
                                alertPopup.initOwner(getTableView().getScene().getWindow());
                            }

                            alertPopup.setTitle("Full Message");
                            alertPopup.setHeaderText(null);
                            alertPopup.setContentText(item);
                            alertPopup.getDialogPane().setPrefWidth(500);
                            alertPopup.showAndWait();
                        }
                    });

                }
            }
        });


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



        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);

        javafx.scene.shape.SVGPath emptyIcon = new javafx.scene.shape.SVGPath();

        emptyIcon.setContent("M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0");
        emptyIcon.setScaleX(5.0);
        emptyIcon.setScaleY(5.0);
        emptyIcon.getStyleClass().add("empty-state-icon");


        Label emptyTitle = new Label("All Quiet Here");
        emptyTitle.getStyleClass().add("empty-state-title");


        Label emptySubtitle = new Label("There are no broadcasts to display right now.\nSent alerts will appear here.");
        emptySubtitle.getStyleClass().add("empty-state-subtitle");
        emptySubtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle);


        alertsTable.setPlaceholder(emptyState);
        formatTableColumns();
        setupActionColumn();
    }

    private void formatTableColumns() {
        alertsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


        alertsTable.getColumns().forEach(column -> {
            column.setReorderable(false);

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
                btnEdit.setStyle("""
                    -fx-background-color: #d4a017;
                    -fx-text-fill: white;
                    -fx-font-size: 11px;
                    -fx-padding: 5 12 5 12;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """);

                btnDelete.setStyle("""
                    -fx-background-color: #c0392b;
                    -fx-text-fill: white;
                    -fx-font-size: 11px;
                    -fx-padding: 5 12 5 12;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """);

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

            if (alertsTable.getScene() != null) {
                dialog.initOwner(alertsTable.getScene().getWindow());
            }

            dialog.setTitle(alert == null ? "Create Broadcast" : "Edit Broadcast");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);

            dialog.setResizable(false);
            dialog.setFullScreen(false);

            dialog.showAndWait();
        } catch (IOException e) {
            showError("Error opening dialog", e);
        }
    }

    private void confirmAndDelete(Alert alert) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);

        if (alertsTable.getScene() != null) {
            confirm.initOwner(alertsTable.getScene().getWindow());
        }

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
            masterData.setAll(alerts);
            applyFilters();
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

        if (alertsTable.getScene() != null && alertsTable.getScene().getWindow() != null) {
            err.initOwner(alertsTable.getScene().getWindow());
        }

        err.setContentText(message + ": " + e.getMessage());
        err.showAndWait();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        String typeFilter = cmbFilterType.getValue();
        String priorityFilter = cmbFilterPriority.getValue();


        FilteredList<Alert> filteredData = new FilteredList<>(masterData, alert -> {


            boolean matchesSearch = searchText.isEmpty() ||
                    alert.getBody().toLowerCase().contains(searchText) ||
                    (alert.getSentByName() != null && alert.getSentByName().toLowerCase().contains(searchText));


            boolean matchesType = typeFilter == null || typeFilter.equals("All Types") ||
                    alert.getTitle().equalsIgnoreCase(typeFilter);


            boolean matchesPriority = priorityFilter == null || priorityFilter.equals("All Priorities") ||
                    alert.getPriority().name().equalsIgnoreCase(priorityFilter);

            return matchesSearch && matchesType && matchesPriority;
        });

        alertsTable.setItems(filteredData);
    }

    @FXML
    private void handleClearFilters() {
        txtSearch.clear();
        cmbFilterType.setValue("All Types");
        cmbFilterPriority.setValue("All Priorities");
    }
}