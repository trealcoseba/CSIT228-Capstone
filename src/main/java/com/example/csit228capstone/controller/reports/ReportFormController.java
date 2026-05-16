package com.example.csit228capstone.controller.reports;

import com.example.csit228capstone.model.report.*;
import com.example.csit228capstone.model.report.ReportFormData.DamageRow;
import com.example.csit228capstone.model.report.ReportFormData.InjuryRow;
import com.example.csit228capstone.repository.ReportRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportFormController {

    // ── Type / Name ──────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbReportType;
    @FXML private TextField        tfReportName;
    @FXML private Label            lblTypeDescription;

    // ── Universal header ─────────────────────────────────────────────────────
    @FXML private TextField  tfReportNo;
    @FXML private TextField  tfReportedBy;
    @FXML private TextField  tfRecordedBy;
    @FXML private TextField  tfReporterContact;
    @FXML private TextField  tfRecorderContact;
    @FXML private DatePicker dpDate;

    // ── Date range (non-incident types) ─────────────────────────────────────
    @FXML private HBox       hboxDateRange;
    @FXML private DatePicker dpStart, dpEnd;

    // ── Incident-specific section ─────────────────────────────────────────────
    @FXML private VBox       vboxIncidentSection;
    @FXML private DatePicker dpDateOfIncident;
    @FXML private TextField  tfLocation;
    @FXML private TextArea   taDescription;

    // Disaster type checkboxes
    @FXML private CheckBox cbFire, cbFlooding, cbEarthquake, cbHurricane,
            cbTornado, cbTsunami, cbVolcano, cbAvalanche,
            cbBlizzard, cbDrought, cbStorm;
    @FXML private TextField tfOtherType;

    // Insurance
    @FXML private RadioButton rbInsYes, rbInsNo;
    @FXML private TextField   tfInsPolicy, tfInsCoverage;

    // Damages table
    @FXML private TableView<DamageRow>           tblDamages;
    @FXML private TableColumn<DamageRow, String> colDamage, colValue, colRepairPlan, colRepairCost;

    // Injuries table
    @FXML private TableView<InjuryRow>           tblInjuries;
    @FXML private TableColumn<InjuryRow, String> colInjuredPerson, colPosition, colMedCost, colInjInsurance;

    @FXML private Button btnGenerate;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ReportRepository repository = new ReportRepository();
    private ReportsController parentController;

    private final ObservableList<DamageRow> damageRows = FXCollections.observableArrayList();
    private final ObservableList<InjuryRow> injuryRows = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());

        Task<String> initAndGenerateNoTask = new Task<>() {
            @Override
            protected String call() {
                repository.initTable();
                int existingCount = repository.findAll().size();
                int nextSequence = existingCount + 1;
                String currentYear = String.valueOf(LocalDate.now().getYear());
                return String.format("RPT-%s-%04d", currentYear, nextSequence);
            }
        };

        initAndGenerateNoTask.setOnSucceeded(e -> {
            tfReportNo.setText(initAndGenerateNoTask.getValue());
            tfReportNo.setEditable(false);
        });

        initAndGenerateNoTask.setOnFailed(e -> initAndGenerateNoTask.getException().printStackTrace());
        new Thread(initAndGenerateNoTask, "report-init-and-sequence").start();

        List<String> types = new ArrayList<>();
        for (ReportType rt : ReportType.values()) types.add(rt.getDisplayName());
        cbReportType.setItems(FXCollections.observableArrayList(types));

        ToggleGroup insGroup = new ToggleGroup();
        rbInsYes.setToggleGroup(insGroup);
        rbInsNo.setToggleGroup(insGroup);

        setupDamageTable();
        setupInjuryTable();

        cbReportType.setOnAction(e -> onTypeChanged());

        vboxIncidentSection.setVisible(false);
        vboxIncidentSection.setManaged(false);
        hboxDateRange.setVisible(false);
        hboxDateRange.setManaged(false);
        if (lblTypeDescription != null) {
            lblTypeDescription.setText("");
            lblTypeDescription.setVisible(false);
            lblTypeDescription.setManaged(false);
        }
    }

    private void onTypeChanged() {
        String selected = cbReportType.getValue();
        if (selected == null) return;

        ReportType rt = ReportType.fromDisplayName(selected);
        boolean isIncident = rt == ReportType.INCIDENT_REPORT;

        vboxIncidentSection.setVisible(isIncident);
        vboxIncidentSection.setManaged(isIncident);
        hboxDateRange.setVisible(!isIncident);
        hboxDateRange.setManaged(!isIncident);

        if (lblTypeDescription != null) {
            lblTypeDescription.setText(rt.getDescription());
            lblTypeDescription.setVisible(true);
            lblTypeDescription.setManaged(true);
        }

        if (tfReportName.getText().isBlank()) {
            tfReportName.setText(selected + " — " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        }
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupDamageTable() {
        tblDamages.setEditable(true);
        tblDamages.setItems(damageRows);

        colDamage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().damage));
        colDamage.setCellFactory(TextFieldTableCell.forTableColumn());
        colDamage.setOnEditCommit(e -> e.getRowValue().damage = e.getNewValue());

        colValue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().value));
        colValue.setCellFactory(TextFieldTableCell.forTableColumn());
        colValue.setOnEditCommit(e -> e.getRowValue().value = e.getNewValue());

        colRepairPlan.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().repairPlan));
        colRepairPlan.setCellFactory(TextFieldTableCell.forTableColumn());
        colRepairPlan.setOnEditCommit(e -> e.getRowValue().repairPlan = e.getNewValue());

        colRepairCost.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().repairCost));
        colRepairCost.setCellFactory(TextFieldTableCell.forTableColumn());
        colRepairCost.setOnEditCommit(e -> e.getRowValue().repairCost = e.getNewValue());

        for (int i = 0; i < 6; i++) damageRows.add(new DamageRow());
    }

    private void setupInjuryTable() {
        tblInjuries.setEditable(true);
        tblInjuries.setItems(injuryRows);

        colInjuredPerson.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().injuredPerson));
        colInjuredPerson.setCellFactory(TextFieldTableCell.forTableColumn());
        colInjuredPerson.setOnEditCommit(e -> e.getRowValue().injuredPerson = e.getNewValue());

        colPosition.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().position));
        colPosition.setCellFactory(TextFieldTableCell.forTableColumn());
        colPosition.setOnEditCommit(e -> e.getRowValue().position = e.getNewValue());

        colMedCost.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medicalCost));
        colMedCost.setCellFactory(TextFieldTableCell.forTableColumn());
        colMedCost.setOnEditCommit(e -> e.getRowValue().medicalCost = e.getNewValue());

        colInjInsurance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().insurance));
        colInjInsurance.setCellFactory(TextFieldTableCell.forTableColumn());
        colInjInsurance.setOnEditCommit(e -> e.getRowValue().insurance = e.getNewValue());

        for (int i = 0; i < 5; i++) injuryRows.add(new InjuryRow());
    }

    @FXML private void addDamageRow() { damageRows.add(new DamageRow()); }
    @FXML private void addInjuryRow() { injuryRows.add(new InjuryRow()); }

    // ── Generate ──────────────────────────────────────────────────────────────

    @FXML
    private void handleGenerate() {
        if (cbReportType.getValue() == null || tfReportName.getText().isBlank()) {
            alert("Validation", "Please select a report type and enter a report name.");
            return;
        }

        btnGenerate.setDisable(true);

        ReportFormData fd = buildFormData();

        Report report = new Report();
        report.setName(tfReportName.getText().trim());
        report.setType(cbReportType.getValue());
        report.setGeneratedDateTime(LocalDateTime.now());
        report.setReportNo(tfReportNo.getText().trim());
        report.setDateOfReport(dpDate.getValue());
        report.setGeneratedBy(tfReportedBy.getText().trim());
        report.setRecordedBy(tfRecordedBy.getText().trim());
        report.setReporterContact(tfReporterContact.getText().trim());
        report.setRecorderContact(tfRecorderContact.getText().trim());
        report.setStartDate(fd.getStartDate());
        report.setEndDate(fd.getEndDate());

        if (fd.getDateOfIncident() != null) report.setDateOfIncident(fd.getDateOfIncident());
        if (fd.getLocation()  != null)      report.setLocation(fd.getLocation());
        if (fd.getDescription() != null)    report.setDescription(fd.getDescription());

        // ── FIXED: BIND ALL INCIDENT DATA (INCLUDING "OTHER") TO DOMAIN ENTITY BEFORE PERSISTENCE ──
        ReportType rt = ReportType.fromDisplayName(report.getType());
        if (rt == ReportType.INCIDENT_REPORT) {
            report.setHasInsurance(fd.getHasInsurance());
            report.setInsurancePolicy(fd.getInsurancePolicy());
            report.setInsuranceCoverageAmt(fd.getInsuranceCoverageAmt());

            // ADD THIS LINE: Pass the "Other" text value to your report model instance!
            report.setIncidentTypeOther(fd.getIncidentTypeOther());
        }

        Task<UUID> insertTask = new Task<>() {
            @Override protected java.util.UUID call() {
                UUID generatedReportId = repository.insert(report);
                report.setId(generatedReportId);

                if (rt == ReportType.INCIDENT_REPORT) {
                    repository.saveIncidentTypes(generatedReportId, fd.getIncidentTypes());
                    repository.saveDamages(generatedReportId, fd.getDamages());
                    repository.saveInjuries(generatedReportId, fd.getInjuries());
                }
                return generatedReportId;
            }
        };
        insertTask.setOnSucceeded(e -> {
            report.setId(insertTask.getValue());
            if (parentController != null) parentController.loadReports();
            openPreview(report, fd);
            btnGenerate.setDisable(false);
        });
        insertTask.setOnFailed(e -> {
            insertTask.getException().printStackTrace();
            alert("Error", "Could not save report: " + insertTask.getException().getMessage());
            btnGenerate.setDisable(false);
        });
        new Thread(insertTask, "report-insert").start();
    }

    private ReportFormData buildFormData() {
        ReportFormData fd = new ReportFormData();
        fd.setReportNo(tfReportNo.getText().trim());
        fd.setDate(dpDate.getValue());
        fd.setReportedBy(tfReportedBy.getText().trim());
        fd.setRecordedBy(tfRecordedBy.getText().trim());
        fd.setReporterContactInfo(tfReporterContact.getText().trim());
        fd.setRecorderContactInfo(tfRecorderContact.getText().trim());
        fd.setStartDate(dpStart.getValue());
        fd.setEndDate(dpEnd.getValue());

        ReportType rt = ReportType.fromDisplayName(cbReportType.getValue());
        if (rt == ReportType.INCIDENT_REPORT) {
            fd.setDateOfIncident(dpDateOfIncident.getValue());
            fd.setLocation(tfLocation.getText().trim());
            fd.setDescription(taDescription.getText().trim());

            List<String> types = new ArrayList<>();
            CheckBox[] boxes = {cbFire, cbFlooding, cbEarthquake, cbHurricane, cbTornado,
                    cbTsunami, cbVolcano, cbAvalanche, cbBlizzard, cbDrought, cbStorm};
            for (CheckBox cb : boxes) if (cb != null && cb.isSelected()) types.add(cb.getText());
            fd.setIncidentTypes(types);
            fd.setIncidentTypeOther(tfOtherType != null ? tfOtherType.getText().trim() : "");

            if (rbInsYes != null && rbInsYes.isSelected()) fd.setHasInsurance(true);
            else if (rbInsNo != null && rbInsNo.isSelected()) fd.setHasInsurance(false);
            fd.setInsurancePolicy(tfInsPolicy != null ? tfInsPolicy.getText().trim() : "");
            fd.setInsuranceCoverageAmt(tfInsCoverage != null ? tfInsCoverage.getText().trim() : "");

            List<DamageRow> activeDamages = damageRows.stream()
                    .filter(r -> (r.damage != null && !r.damage.isBlank()) ||
                            (r.value != null && !r.value.isBlank()))
                    .toList();
            fd.setDamages(new ArrayList<>(activeDamages));

            List<InjuryRow> activeInjuries = injuryRows.stream()
                    .filter(r -> (r.injuredPerson != null && !r.injuredPerson.isBlank()) ||
                            (r.position != null && !r.position.isBlank()))
                    .toList();
            fd.setInjuries(new ArrayList<>(activeInjuries));
        }
        return fd;
    }

    private void openPreview(Report report, ReportFormData formData) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/report/ReportPreview.fxml"));
            Parent root = loader.load();
            ReportPreviewController ctrl = loader.getController();
            ctrl.loadReport(report, formData);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle("Preview — " + report.getName());
            stage.setMinWidth(820);
            stage.setMinHeight(700);
            stage.show();
            ((Stage) tfReportName.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
            alert("Error", "Could not open preview: " + e.getMessage());
        }
    }

    // ── Misc ─────────────────────────────────────────────────────────────────

    public void setParentController(ReportsController c) { this.parentController = c; }

    @FXML private void handleCancel() {
        ((Stage) tfReportName.getScene().getWindow()).close();
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}