package com.example.csit228capstone.controller.reports;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.report.Report;
import com.example.csit228capstone.repository.ReportRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class ReportsController {
    @FXML private Button btnDownload;
    @FXML private CheckBox selectAllHeader;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterAscOrDesc;

    @FXML private TableView<Report> reportsTable;
    @FXML private TableColumn<Report, Boolean> colSelect;
    @FXML private TableColumn<Report, String> colName;
    @FXML private TableColumn<Report, String> colType;
    @FXML private TableColumn<Report, String> colGeneratedDateTime;
    @FXML private TableColumn<Report, String> colGeneratedBy;
    @FXML private TableColumn<Report, Void> colActions;

    private final ObservableList<Report> reportData = FXCollections.observableArrayList();
    private final ReportRepository repository = new ReportRepository();

    @FXML
    public void initialize() {
        setUpTable();
        loadReports();

        if (filterAscOrDesc != null) {
            filterAscOrDesc.setItems(FXCollections.observableArrayList("Ascending", "Descending"));
            filterAscOrDesc.setValue("Ascending");
        }
    }

    private void setUpTable() {
        setupSelectColumn();
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colGeneratedDateTime.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        colGeneratedBy.setCellValueFactory(new PropertyValueFactory<>("generatedBy"));
        setupActionColumn();

        reportsTable.setItems(reportData);
    }

    private void setupSelectColumn() {
        colSelect.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colSelect.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(event -> {
                    Report report = getTableView().getItems().get(getIndex());
                    report.setSelected(checkBox.isSelected());
                    updateDownloadButtonState(); // Update the button if one is clicked
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Report report = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(report.isSelected());
                    setGraphic(checkBox);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");

            {
                btnDelete.setStyle("""
                        -fx-background-color: #fecaca;
                        -fx-text-fill: #991b1b;
                        -fx-font-size: 10px;
                        -fx-padding: 3 8 3 8;
                        -fx-background-radius: 4;
                        -fx-cursor: hand;
                        """);

                btnDelete.setOnAction(event -> {
                    Report report = getTableView().getItems().get(getIndex());
                    handleDeleteReport(report);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    public void loadReports() {
        try {
            List<Report> reports = repository.findAll();
            reportData.setAll(reports);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load reports: " + e.getMessage());
        }
    }

    private void handleDeleteReport(Report report) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete " + report.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                repository.deleteById(report.getId());
                reportData.remove(report);
                updateDownloadButtonState();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSelectAll(ActionEvent event) {
        boolean selected = selectAllHeader.isSelected();
        for (Report report : reportData) {
            report.setSelected(selected);
        }
        reportsTable.refresh();
        updateDownloadButtonState();
    }

    //TO DO
    @FXML
    private void generateReport(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/report/ReportForm.fxml"));
            Parent root = loader.load();

            ReportFormController controller = loader.getController();
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle("Generate New Report");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load report form.");
        }
    }


    //TO DO
    public void downloadReports(ActionEvent actionEvent) {
        System.out.println("Triggering file download...");
    }

    private void updateDownloadButtonState() {
        boolean anySelected = reportData.stream().anyMatch(Report::isSelected);
        if (btnDownload != null) {
            btnDownload.setDisable(!anySelected);
        }
    }

    private void openForm(Report report) {

    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
