package com.example.csit228capstone.controller.reports;

import com.example.csit228capstone.model.report.*;
import com.example.csit228capstone.repository.ReportRepository;
import com.example.csit228capstone.service.ReportService;
import com.example.csit228capstone.service.report.DocxReportExporter;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class ReportPreviewController {

    @FXML private VBox   vboxDocument;
    @FXML private Button btnPdf, btnWord, btnClose;
    @FXML private Label  lblStatus;

    private Report         currentReport;
    private ReportFormData currentFormData;
    private ReportData<List<List<String>>> currentGenericData;

    private final ReportService reportService = new ReportService();
    private final ReportRepository repository = new ReportRepository();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public void loadReport(Report report, ReportFormData fd) {
        this.currentReport   = report;
        this.currentFormData = fd;

        if (isIncident(report)) {
            renderDocument();
        } else {
            fetchGenericDataThenRender(report);
        }
    }

    public void loadSavedReport(Report report) {
        this.currentReport = report;
        setStatus("Loading saved incident records...");
        setBtnsDisabled(true);

        Task<ReportFormData> fetchSavedTask = new Task<>() {
            @Override
            protected ReportFormData call() throws Exception {
                ReportFormData fd = new ReportFormData();
                fd.setReportNo(report.getReportNo());
                fd.setDate(report.getDateOfReport());
                fd.setReportedBy(report.getGeneratedBy());
                fd.setRecordedBy(report.getRecordedBy());
                fd.setReporterContactInfo(report.getReporterContact());
                fd.setRecorderContactInfo(report.getRecorderContact());
                fd.setStartDate(report.getStartDate());
                fd.setEndDate(report.getEndDate());

                fd.setDateOfIncident(report.getDateOfIncident());
                fd.setLocation(report.getLocation());
                fd.setDescription(report.getDescription());

                fd.setHasInsurance(report.getHasInsurance());
                fd.setInsurancePolicy(report.getInsurancePolicy());
                fd.setInsuranceCoverageAmt(report.getInsuranceCoverageAmt());
                fd.setIncidentTypeOther(report.getIncidentTypeOther());

                ReportRepository repo = new ReportRepository();

                fd.setDamages(repo.findDamagesByReportId(report.getId()));
                fd.setInjuries(repo.findInjuriesByReportId(report.getId()));
                fd.setIncidentTypes(repo.findIncidentTypesByReportId(report.getId()));

                return fd;
            }
        };

        fetchSavedTask.setOnSucceeded(e -> {
            currentFormData = fetchSavedTask.getValue();
            setStatus("");
            setBtnsDisabled(false);

            if (isIncident(report)) {
                renderDocument();
            } else {
                fetchGenericDataThenRender(report);
            }
        });

        fetchSavedTask.setOnFailed(e -> {
            fetchSavedTask.getException().printStackTrace();
            setStatus("Failed to load saved report data.");
            setBtnsDisabled(false);
            alert("Database Sync Error", fetchSavedTask.getException().getMessage());
        });

        reportService.execute(fetchSavedTask);
    }

    private void fetchGenericDataThenRender(Report report) {
        setStatus("Loading report data…");
        setBtnsDisabled(true);

        Task<ReportData<List<List<String>>>> task = reportService.createFetchTask(report);
        task.setOnSucceeded(e -> {
            currentGenericData = task.getValue();
            setStatus("");
            setBtnsDisabled(false);
            renderDocument();
        });
        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            setStatus("Failed to load: " + task.getException().getMessage());
            setBtnsDisabled(false);
        });
        reportService.execute(task);
    }

    private void renderDocument() {
        Platform.runLater(() -> {
            vboxDocument.getChildren().clear();
            vboxDocument.setSpacing(8);

            addCentered("Republic of the Philippines", 9, false, "#888888");
            addCentered("Barangay Management System — LIGTAS", 9, false, "#888888");
            addSpacer(4);
            addCentered(currentReport.getName().toUpperCase(), 18, true, "#1A2E44");

            ReportType rt = ReportType.fromDisplayName(currentReport.getType());
            addCentered(rt.getDisplayName(), 11, false, "#1D9E75");

            addHRule();

            addDocumentMetadataSection(currentFormData, currentReport);

            if (isIncident(currentReport)) {
                renderIncidentBodyTables();
            } else {
                renderGenericBody();
            }


            addSpacer(20);
            String sigName = (currentFormData != null && currentFormData.getRecordedBy() != null && !currentFormData.getRecordedBy().isBlank())
                    ? currentFormData.getRecordedBy() : currentReport.getGeneratedBy();
            addSignature(sigName);
        });
    }

    private void renderIncidentBodyTables() {
        ReportFormData fd = currentFormData;
        if (fd == null) return;

        List<String> types = fd.getIncidentTypes();
        boolean hasStandardTypes = (types != null && !types.isEmpty());
        boolean hasCustomOther   = (!nvl(fd.getIncidentTypeOther()).isBlank());

        if (hasStandardTypes || hasCustomOther) {
            addSpacer(6);
            FlowPane chips = new FlowPane(8, 6);

            if (hasStandardTypes) {
                for (String t : types) {
                    chips.getChildren().add(chip(t));
                }
            }

            if (hasCustomOther) {
                chips.getChildren().add(chip("Other: " + fd.getIncidentTypeOther().trim()));
            }

            vboxDocument.getChildren().add(chips);
        }

        addSectionHeader("Property Damages");
        List<List<String>> damageRows = fd.getDamages() == null ? List.of() :
                fd.getDamages().stream()
                        .map(r -> List.of(nvl(r.damage), nvl(r.value), nvl(r.repairPlan), nvl(r.repairCost)))
                        .toList();
        addDocTable(List.of("Damage Item", "Estimated Value", "Repair Plan", "Repair Cost"), damageRows);

        addSectionHeader("Casualties / Injuries");
        List<List<String>> injuryRows = fd.getInjuries() == null ? List.of() :
                fd.getInjuries().stream()
                        .map(r -> List.of(nvl(r.injuredPerson), nvl(r.position), nvl(r.medicalCost), nvl(r.insurance)))
                        .toList();
        addDocTable(List.of("Injured Person", "Position", "Medical Cost", "Insurance"), injuryRows);
    }

    private void renderGenericBody() {
        ReportData<List<List<String>>> data = currentGenericData;
        if (data == null) {
            addCentered("Report data could not be loaded.", 11, false, "#888");
            return;
        }
        if (data.getSummaryLines() != null && !data.getSummaryLines().isEmpty()) {
            addSectionHeader("Summary");
            for (String line : data.getSummaryLines()) {
                Label l = new Label("• " + line);
                l.setStyle("-fx-font-size:11;-fx-text-fill:#1A2E44;-fx-padding:2 0 2 10;");
                vboxDocument.getChildren().add(l);
            }
        }
        if (data.getHeaders() != null && !data.getHeaders().isEmpty()) {
            addSectionHeader("Data");
            List<List<String>> rows = data.getPayload() != null ? data.getPayload() : List.of();
            addDocTable(data.getHeaders(), rows);
        }
    }

    @FXML private void handleExportPdf()  { doExport("pdf");  }
    @FXML private void handleExportWord() { doExport("docx"); }

    private void doExport(String format) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Report");
        fc.setInitialFileName(sanitize(currentReport.getName()) +
                ("pdf".equals(format) ? ".pdf" : ".docx"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "pdf".equals(format) ? "PDF (*.pdf)" : "Word (*.docx)",
                "pdf".equals(format) ? "*.pdf" : "*.docx"));
        File dest = fc.showSaveDialog(btnPdf.getScene().getWindow());
        if (dest == null) return;

        setStatus("Exporting " + format.toUpperCase() + "…");
        setBtnsDisabled(true);

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isIncident(currentReport)) {

                    ReportData<ReportFormData> incidentWrapper = new ReportData<>();
                    incidentWrapper.setTitle(currentReport.getName());
                    incidentWrapper.setReportType(ReportType.fromDisplayName(currentReport.getType()));
                    incidentWrapper.setStartDate(currentReport.getStartDate());
                    incidentWrapper.setEndDate(currentReport.getEndDate());
                    incidentWrapper.setPayload(currentFormData);

                    if ("docx".equalsIgnoreCase(format)) {
                        DocxReportExporter exporter = new DocxReportExporter();
                        exporter.exportIncident(incidentWrapper, dest);
                    } else {

                        com.example.csit228capstone.service.report.PDFReportExporter exporter =
                                new com.example.csit228capstone.service.report.PDFReportExporter();
                        exporter.exportIncident(incidentWrapper, dest);
                    }
                } else {

                    ReportData<List<List<String>>> genericWrapper = new ReportData<>();
                    genericWrapper.setTitle(currentReport.getName());
                    genericWrapper.setReportType(ReportType.fromDisplayName(currentReport.getType()));
                    genericWrapper.setStartDate(currentReport.getStartDate());
                    genericWrapper.setEndDate(currentReport.getEndDate());
                    genericWrapper.setHeaders(currentGenericData != null ? currentGenericData.getHeaders() : List.of());
                    genericWrapper.setPayload(currentGenericData != null ? currentGenericData.getPayload() : List.of());
                    genericWrapper.setSummaryLines(currentGenericData != null ? currentGenericData.getSummaryLines() : List.of());
                    genericWrapper.setGeneratedBy(currentReport.getGeneratedBy());

                    genericWrapper.setFormData(currentFormData);

                    if ("docx".equalsIgnoreCase(format)) {
                        DocxReportExporter exporter = new DocxReportExporter();
                        exporter.export(genericWrapper, dest);
                    } else {
                        com.example.csit228capstone.service.report.PDFReportExporter exporter =
                                new com.example.csit228capstone.service.report.PDFReportExporter();
                        exporter.export(genericWrapper, dest);
                    }
                }
                return null;
            }
        };

        exportTask.setOnSucceeded(e -> {
            setStatus("Saved to: " + dest.getAbsolutePath());
            setBtnsDisabled(false);
            alert("Export Complete", "Report saved to:\n" + dest.getAbsolutePath());
        });

        exportTask.setOnFailed(e -> {
            exportTask.getException().printStackTrace();
            setStatus("Export failed: " + exportTask.getException().getMessage());
            setBtnsDisabled(false);
            alert("Export Failed", exportTask.getException().getMessage());
        });

        reportService.execute(exportTask);
    }

    @FXML private void handleClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }

    private void addCentered(String text, double size, boolean bold, String color) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-font-size:%.0f;%s-fx-text-fill:%s;",
                size, bold ? "-fx-font-weight:bold;" : "", color));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        l.setWrapText(true);
        vboxDocument.getChildren().add(l);
    }

    private void addHRule() {
        Separator s = new Separator();
        VBox.setMargin(s, new Insets(4, 0, 6, 0));
        vboxDocument.getChildren().add(s);
    }

    private void addSpacer(double h) {
        Region r = new Region(); r.setPrefHeight(h);
        vboxDocument.getChildren().add(r);
    }

    private void addSectionHeader(String title) {
        addSpacer(6);
        Label l = new Label(title);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1D9E75;" +
                "-fx-border-color:#1D9E75;-fx-border-width:0 0 1 0;-fx-padding:0 0 3 0;");
        vboxDocument.getChildren().add(l);
        addSpacer(4);
    }

    private void addDocumentMetadataSection(ReportFormData fd, Report report) {
        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 10, 0));

        ColumnConstraints c0_lbl = new ColumnConstraints(); c0_lbl.setPercentWidth(18);
        ColumnConstraints c1_val = new ColumnConstraints(); c1_val.setPercentWidth(32); c1_val.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2_lbl = new ColumnConstraints(); c2_lbl.setPercentWidth(18);
        ColumnConstraints c3_val = new ColumnConstraints(); c3_val.setPercentWidth(32); c3_val.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(c0_lbl, c1_val, c2_lbl, c3_val);

        int currentRow = 0;

        String formDate = (fd != null && fd.getDate() != null) ? fd.getDate().format(DATE_FMT)
                : (report.getGeneratedDateTime() != null ? report.getGeneratedDateTime().toLocalDate().format(DATE_FMT) : "—");
        String genOn = report.getGeneratedDateTime() != null ? report.getGeneratedDateTime().format(DT_FMT) : formDate;
        String coveragePeriod = (report.getStartDate() != null || report.getEndDate() != null) ? period() : formDate;

        String reportedBy = (fd != null && !nvl(fd.getReportedBy()).isBlank()) ? fd.getReportedBy()
                : (!nvl(report.getGeneratedBy()).isBlank() ? report.getGeneratedBy() : "—");
        String reportNo   = (fd != null && !nvl(fd.getReportNo()).isBlank()) ? fd.getReportNo() : "—";
        String recordedBy = (fd != null && !nvl(fd.getRecordedBy()).isBlank()) ? fd.getRecordedBy() : "—";
        String repContact = (fd != null && !nvl(fd.getReporterContactInfo()).isBlank()) ? fd.getReporterContactInfo() : "—";
        String recContact = (fd != null && !nvl(fd.getRecorderContactInfo()).isBlank()) ? fd.getRecorderContactInfo() : "—";

        String computedType = report.getType() != null ? report.getType() : "—";

        grid.add(lbl("Date:"), 0, currentRow);
        grid.add(val(formDate), 1, currentRow);
        grid.add(lbl("Report No.:"), 2, currentRow);
        grid.add(val(reportNo), 3, currentRow);
        currentRow++;

        grid.add(lbl("Reported by:"), 0, currentRow);
        grid.add(val(reportedBy), 1, currentRow);
        grid.add(lbl("Recorded by:"), 2, currentRow);
        grid.add(val(recordedBy), 3, currentRow);
        currentRow++;

        grid.add(lbl("Reporter Contact:"), 0, currentRow);
        grid.add(val(repContact), 1, currentRow);
        grid.add(lbl("Recorder Contact:"), 2, currentRow);
        grid.add(val(recContact), 3, currentRow);
        currentRow++;

        grid.add(lbl("Classification Type:"), 0, currentRow);
        grid.add(val(computedType), 1, currentRow);
        grid.add(lbl("Generated on:"), 2, currentRow);
        grid.add(val(genOn), 3, currentRow);
        currentRow++;

        grid.add(lbl("Period Covered:"), 0, currentRow);
        grid.add(val(coveragePeriod), 1, currentRow);
        grid.add(lbl(""), 2, currentRow);
        grid.add(val(""), 3, currentRow);
        currentRow++;

        if (isIncident(report) && fd != null) {
            Region separatorSpace = new Region();
            separatorSpace.setPrefHeight(12);
            grid.add(separatorSpace, 0, currentRow++, 4, 1);

            Label subHeader = new Label("Incident Details");
            subHeader.setStyle("-fx-font-size:11; -fx-font-weight:bold; -fx-text-fill:#1D9E75; -fx-padding: 0 0 2 0;");
            grid.add(subHeader, 0, currentRow++, 4, 1);

            grid.add(lbl("Date of Incident:"), 0, currentRow);
            grid.add(val(fd.getDateOfIncident() != null ? fd.getDateOfIncident().format(DATE_FMT) : "—"), 1, currentRow);
            grid.add(lbl("Location:"), 2, currentRow);
            grid.add(val(nvl(fd.getLocation())), 3, currentRow);
            currentRow++;

            grid.add(lbl("Description:"), 0, currentRow);
            grid.add(val(nvl(fd.getDescription())), 1, currentRow, 3, 1);
            currentRow++;

            Region insSeparator = new Region();
            insSeparator.setPrefHeight(6);
            grid.add(insSeparator, 0, currentRow++, 4, 1);

            Label insHeader = new Label("Insurance Context");
            insHeader.setStyle("-fx-font-size:11; -fx-font-weight:bold; -fx-text-fill:#1D9E75;");
            grid.add(insHeader, 0, currentRow++, 4, 1);

            String insState = fd.getHasInsurance() == null ? "N/A" : fd.getHasInsurance() ? "Yes" : "No";
            grid.add(lbl("Insurance State:"), 0, currentRow);
            grid.add(val(insState), 1, currentRow);
            grid.add(lbl("Policy Number:"), 2, currentRow);
            grid.add(val(nvl(fd.getInsurancePolicy())), 3, currentRow);
            currentRow++;

            grid.add(lbl("Coverage Value:"), 0, currentRow);
            grid.add(val(nvl(fd.getInsuranceCoverageAmt())), 1, currentRow);
            grid.add(lbl(""), 2, currentRow);
            grid.add(val(""), 3, currentRow);
            currentRow++;
        }

        vboxDocument.getChildren().add(grid);
    }

    private void addDocTable(List<String> headers, List<List<String>> rows) {
        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setHgap(0); grid.setVgap(0);

        for (int c = 0; c < headers.size(); c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / headers.size());
            grid.getColumnConstraints().add(cc);
        }

        for (int c = 0; c < headers.size(); c++) {
            Label h = new Label(headers.get(c));
            h.setMaxWidth(Double.MAX_VALUE);
            h.setMaxHeight(Double.MAX_VALUE);
            h.setWrapText(true);
            h.setAlignment(Pos.TOP_LEFT);
            h.setStyle("-fx-font-weight:bold;-fx-font-size:10;" +
                    "-fx-background-color:#1D9E75;-fx-text-fill:white;" +
                    "-fx-padding:6 8;-fx-border-color:#bbb;-fx-border-width:0.5;");
            grid.add(h, c, 0);
            GridPane.setValignment(h, javafx.geometry.VPos.TOP);
        }

        int visualRowIndex = 1;

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);

            boolean isRowBlank = row.stream().allMatch(cellText ->
                    cellText == null || cellText.trim().isBlank() || "-".equals(cellText.trim())
            );
            if (isRowBlank) {
                continue;
            }

            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);

            String bg = visualRowIndex % 2 == 0 ? "white" : "#F4FAF8";

            for (int c = 0; c < headers.size(); c++) {
                String v = c < row.size() ? nvl(row.get(c)) : "";

                if (c == 0 && headers.get(0).equals("#")) {
                    v = String.valueOf(visualRowIndex);
                }

                Label cell = new Label(v);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMaxHeight(Double.MAX_VALUE);
                cell.setWrapText(true);
                cell.setAlignment(Pos.TOP_LEFT);

                cell.setStyle(String.format(
                        "-fx-font-size:10;-fx-padding:6 8;-fx-text-fill:#1A2E44;" +
                                "-fx-background-color:%s;-fx-border-color:#ddd;-fx-border-width:0.5 0.5 0.5 0.5;", bg));

                grid.add(cell, c, visualRowIndex);

                GridPane.setValignment(cell, javafx.geometry.VPos.TOP);
                GridPane.setVgrow(cell, Priority.ALWAYS);
            }
            visualRowIndex++;
        }

        VBox.setMargin(grid, new Insets(4, 0, 4, 0));
        vboxDocument.getChildren().add(grid);

        if (visualRowIndex == 1) {
            Label empty = new Label("No data recorded.");
            empty.setStyle("-fx-font-size:10;-fx-text-fill:#888;-fx-padding:4 8;");
            vboxDocument.getChildren().add(empty);
        }
    }

    private void addSignature(String name) {
        HBox outer = new HBox();
        outer.setAlignment(Pos.CENTER_RIGHT);
        VBox block = new VBox(4);
        block.setAlignment(Pos.CENTER);
        Separator line = new Separator(); line.setPrefWidth(220);
        Label nameLbl = new Label(nvl(name).isBlank() ? "___________________________" : name);
        nameLbl.setStyle("-fx-font-size:11;");
        Label titleLbl = new Label("Authorized Signatory");
        titleLbl.setStyle("-fx-font-size:10;-fx-text-fill:#888;");
        block.getChildren().addAll(line, nameLbl, titleLbl);
        outer.getChildren().add(block);
        vboxDocument.getChildren().add(outer);
    }

    private Label chip(String text) {
        Label l = new Label("☑ " + text);
        l.setStyle("-fx-background-color:#E6F7F2;-fx-text-fill:#1D9E75;" +
                "-fx-border-color:#1D9E75;-fx-border-radius:4;" +
                "-fx-background-radius:4;-fx-padding:2 8;-fx-font-size:10;");
        return l;
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#444;");
        l.setMinWidth(Label.USE_PREF_SIZE);
        return l;
    }

    private Label val(String t) {
        Label l = new Label(nvl(t).isBlank() ? "—" : t);
        l.setStyle("-fx-font-size:10;-fx-text-fill:#111;" +
                "-fx-border-color:transparent transparent #bbb transparent;" +
                "-fx-padding:0 4 1 0;");
        l.setWrapText(true);
        return l;
    }

    private boolean isIncident(Report r) {
        if (r == null || r.getType() == null) return false;
        ReportType rt = ReportType.fromDisplayName(r.getType());
        return rt == ReportType.INCIDENT_REPORT;
    }

    private String period() {
        String s = currentReport.getStartDate() != null
                ? currentReport.getStartDate().format(DATE_FMT) : "";
        String e = currentReport.getEndDate() != null
                ? currentReport.getEndDate().format(DATE_FMT) : "";
        return s.isEmpty() ? e : e.isEmpty() ? s : s + " — " + e;
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> {
            if (lblStatus != null) {
                lblStatus.setText(msg);
                lblStatus.setVisible(!msg.isBlank());
                lblStatus.setManaged(!msg.isBlank());
            }
        });
    }

    private void setBtnsDisabled(boolean d) {
        Platform.runLater(() -> { btnPdf.setDisable(d); btnWord.setDisable(d); });
    }

    private String sanitize(String n) {
        return n == null ? "report" : n.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private void alert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);

            if (btnPdf.getScene() != null && btnPdf.getScene().getWindow() != null) {
                a.initOwner(btnPdf.getScene().getWindow());
            }

            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}