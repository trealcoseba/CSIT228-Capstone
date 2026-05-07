package com.example.csit228capstone.controller.reports;

import com.example.csit228capstone.model.report.Report;
import com.example.csit228capstone.repository.ReportRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReportFormController {
    @FXML private TextField tfReportName;
    @FXML private ComboBox<String> cbReportType;
    @FXML private TextField tfGeneratedBy;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;

    private final ReportRepository repository = new ReportRepository();
    private ReportsController parentController;
    private UUID currentReportId = null;

    public void setParentController(ReportsController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleGenerate() {
        if (!isInputsValid()) return;

        Report report = new Report();
        report.setName(safeTrim(tfReportName));
        report.setType(cbReportType.getValue());
        report.setGeneratedDateTime(LocalDateTime.now());
        report.setGeneratedBy(safeTrim(tfGeneratedBy));
        report.setStartDate(dpStart.getValue());
        report.setEndDate(dpEnd.getValue());

        try {
            if (currentReportId != null) {
                report.setId(currentReportId);
                repository.update(report);
                showAlert("Success", "New report generated successfully.");
            } else {
                UUID newId = repository.insert(report);
                if (newId != null) {
                    showAlert("Success", "New report generated successfully.");
                }
            }

            if (parentController != null) {
                parentController.loadReports();
            }
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not save report: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        tfReportName.getScene().getWindow().hide();
    }

    private String safeTrim(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    private boolean isInputsValid() {
        StringBuilder sb = new StringBuilder();
        if (tfReportName.getText().isBlank()) sb.append("- Report Name is required.\n");
        if (cbReportType.getValue() == null)  sb.append("- Report Type is required.\n");
        if (dpStart.getValue() == null && dpEnd.getValue() == null) sb.append("- Start Date and End Date is required.\n");
        if (sb.isEmpty()) return true;
        showAlert("Invalid Input", sb.toString());
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}