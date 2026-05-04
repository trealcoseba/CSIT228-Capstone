package com.example.csit228capstone.controller.resident;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.repository.ResidentRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public class ResidentFormController {
    @FXML private Label lblTitle;
    @FXML private TextField txtFirstName, txtMiddleName, txtLastName, txtSuffix, txtPhone, txtPhotoUrl;
    @FXML private DatePicker dpBirthDate;
    @FXML private CheckBox chkSenior, chkPWD, chkPregnant, chkChild, chkSoloParent, chkIndigenous;
    @FXML private ComboBox<String> cbSex, cbCivilStatus;
    @FXML private CheckBox chkIsHead;

    private final ResidentRepository repository = new ResidentRepository();
    private ResidentsController parentController;
    private UUID currentResidentId = null;

    @FXML
    public void initialize() {
        cbSex.setItems(FXCollections.observableArrayList("M", "F"));
        cbCivilStatus.setItems(FXCollections.observableArrayList("Single", "Married", "Widowed", "Separated"));

        // AUTO-CHECK SENIOR: Logic based on DatePicker instead of a text field
        dpBirthDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int age = Period.between(newValue, LocalDate.now()).getYears();
                // Automatically check/uncheck Senior Citizen based on age 60
                chkSenior.setSelected(age >= 60);
                // Automatically check/uncheck Child based on age 0-5
                chkChild.setSelected(age >= 0 && age <= 5);
            }
        });
    }

    public void setParentController(ResidentsController parent) {
        this.parentController = parent;
    }

    public void setResidentData(Resident r) {
        this.currentResidentId = r.getId();
        this.lblTitle.setText("Update Resident Details");

        txtFirstName.setText(r.getFirstName());
        txtMiddleName.setText(r.getMiddleName());
        txtLastName.setText(r.getLastName());
        txtSuffix.setText(r.getSuffix());
        dpBirthDate.setValue(r.getDateOfBirth());
        cbSex.setValue(r.getSex());
        cbCivilStatus.setValue(r.getCivilStatus());
        txtPhone.setText(r.getContactNumber());
        txtPhotoUrl.setText(r.getPhotoUrl());
        chkIsHead.setSelected(r.isHouseholdHead());

        // LIGHT UP CHECKBOXES: Set checkboxes based on existing tags
        if (r.getVulnerabilities() != null) {
            chkSenior.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.SENIOR_CITIZEN));
            chkPWD.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.PWD));
            chkPregnant.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.PREGNANT));
            chkIndigenous.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.INDIGENOUS));
            chkSoloParent.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.SOLO_PARENT));
            chkChild.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.CHILD_0_5));
        }
    }

    @FXML
    private void handleSave() {
        if (!isInputValid()) return;

        Resident r = new Resident();
        r.setFirstName(safeTrim(txtFirstName));
        r.setMiddleName(safeTrim(txtMiddleName));
        r.setLastName(safeTrim(txtLastName));
        r.setSuffix(safeTrim(txtSuffix));
        r.setDateOfBirth(dpBirthDate.getValue());
        r.setSex(cbSex.getValue());
        r.setCivilStatus(cbCivilStatus.getValue());
        r.setContactNumber(safeTrim(txtPhone));
        r.setPhotoUrl(safeTrim(txtPhotoUrl));
        r.setHouseholdHead(chkIsHead.isSelected());

        try {
            UUID residentId;
            if (currentResidentId != null) {
                r.setId(currentResidentId);
                repository.update(r);
                residentId = currentResidentId;
                // CLEAR EXISTING: This allows "Going back to General"
                repository.deleteVulnerabilities(residentId);
            } else {
                residentId = repository.insert(r);
            }

            // RE-ADD ONLY SELECTED: If none are selected, it stays "General"
            if (chkSenior.isSelected()) repository.addVulnerability(residentId, VulnerabilityTag.SENIOR_CITIZEN);
            if (chkPWD.isSelected()) repository.addVulnerability(residentId, VulnerabilityTag.PWD);
            if (chkPregnant.isSelected()) repository.addVulnerability(residentId, VulnerabilityTag.PREGNANT);
            if (chkIndigenous.isSelected()) repository.addVulnerability(residentId, VulnerabilityTag.INDIGENOUS);
            if (chkSoloParent.isSelected()) repository.addVulnerability(residentId, VulnerabilityTag.SOLO_PARENT);
            if (chkChild.isSelected()) repository.addVulnerability(residentId, VulnerabilityTag.CHILD_0_5);

            if (parentController != null) parentController.loadData();
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save: " + e.getMessage());
        }
    }

    private String safeTrim(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtFirstName.getScene().getWindow();
        stage.close();
    }

    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();
        if (txtFirstName.getText().isBlank()) errorMessage.append("- First Name is required.\n");
        if (txtLastName.getText().isBlank()) errorMessage.append("- Last Name is required.\n");
        if (dpBirthDate.getValue() == null) errorMessage.append("- Birth Date is required.\n");
        if (cbSex.getValue() == null) errorMessage.append("- Sex is required.\n");

        if (errorMessage.length() == 0) return true;
        showAlert("Invalid Input", errorMessage.toString());
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}