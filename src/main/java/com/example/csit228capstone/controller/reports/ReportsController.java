package com.example.csit228capstone.controller.reports;

import com.example.csit228capstone.model.report.Report;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;


public class ReportsController {
    @FXML private TextField reportName;
    @FXML private ComboBox<String> reportType;
    @FXML private ComboBox<String> reportFormat;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;

    @FXML private Label lblTotalIncidents;
    @FXML private Label lblCritical;
    @FXML private Label lblAvgResponse;
    @FXML private Label lblResolution;

    @FXML private TableView<Report> reportsTable;
    @FXML private TableColumn<Report, String> colName;
    @FXML private TableColumn<Report, String> colType;
    @FXML private TableColumn<Report, String> colGenerated;
    @FXML private TableColumn<Report, String> colGeneratedBy;
    @FXML private TableColumn<Report, String> colStatus;

    private final ObservableList<Report> reportData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colGenerated.setCellValueFactory(new PropertyValueFactory<>("date"));
        colGeneratedBy.setCellValueFactory(new PropertyValueFactory<>("generatedBy"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        reportsTable.setItems(reportData);

        updateAnalytics(24, 5, "12 mins", "88%");
    }

    private void updateAnalytics(int total, int critical, String avgTime, String rate) {
        lblTotalIncidents.setText(String.valueOf(total));
        lblCritical.setText(String.valueOf(critical));
        lblAvgResponse.setText(avgTime);
        lblResolution.setText(rate);
    }

    //TO DO
    public void downloadReports(ActionEvent actionEvent) {
        System.out.println("Triggering file download...");
    }

    //TO DO
    public void generateReport(ActionEvent actionEvent) {
        String name = reportName.getText();
        String type = reportType.getValue();
        LocalDate start = startDate.getValue();

        String dateToDisplay = (start != null) ? start.toString() : LocalDate.now().toString();

        if (name == null || name.isEmpty() || type == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please fill in report details.");
            alert.showAndWait();
            return;
        }

        Report newReport = new Report(
                name, type, dateToDisplay,
                "Admin User", "Ongoing"
        );

        reportData.addFirst(newReport);
        reportName.clear();
        reportsTable.refresh();
    }
}
