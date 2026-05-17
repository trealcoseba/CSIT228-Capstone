package com.example.csit228capstone.controller.resident;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.controller.map.MapPickerController;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.repository.ResidentRepository;
import com.github.sarxos.webcam.Webcam;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public class ResidentFormController {
    @FXML private Label lblTitle;
    @FXML private TextField txtFirstName, txtMiddleName, txtLastName, txtSuffix, txtPhone;
    @FXML private ImageView ivAvatar;
    @FXML private Label lblFilePath;
    @FXML private DatePicker dpBirthDate;
    @FXML private CheckBox chkSenior, chkPWD, chkPregnant, chkChild, chkSoloParent, chkIndigenous;
    @FXML private ComboBox<String> cbSex, cbCivilStatus;
    @FXML private CheckBox chkIsHead;
    @FXML private TextField txtAddress;

    private final ResidentRepository repository = new ResidentRepository();
    private ResidentsController parentController;
    private UUID currentResidentId = null;

    private File selectedImageFile = null;
    private String existingPhotoPath = null;

    private double pendingLatitude = 0.0;
    private double pendingLongitude = 0.0;

    @FXML
    public void initialize() {
        cbSex.setItems(FXCollections.observableArrayList("M", "F"));
        cbCivilStatus.setItems(FXCollections.observableArrayList("Single", "Married", "Widowed", "Separated"));

        dpBirthDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int age = Period.between(newValue, LocalDate.now()).getYears();
                chkSenior.setSelected(age >= 60);
                chkChild.setSelected(age >= 0 && age <= 5);
            }
        });
    }

    // ─── Photo ────────────────────────────────────────────────────────────────

    @FXML
    public void handleChooseFile(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Resident Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(lblTitle.getScene().getWindow());
        if (file != null) {
            this.selectedImageFile = file;
            ivAvatar.setImage(new Image(file.toURI().toString(), true));
            lblFilePath.setText("Selected: " + file.getName());
        }
    }

    @FXML
    public void handleTakePhoto(ActionEvent actionEvent) {
//        try {
//            Webcam webcam = Webcam.getWebcams().getFirst();
//            if (webcam != null) {
//                webcam.setViewSize(WebcamResolution.VGA.getSize());
//                webcam.open();
//                BufferedImage image = webcam.getImage();
//                File tempFile = new File("temp_capture.jpg");
//                ImageIO.write(image, "JPG", tempFile);
//                this.selectedImageFile = tempFile;
//                ivAvatar.setImage(new Image(tempFile.toURI().toString()));
//                lblFilePath.setText("Captured: " + tempFile.getName());
//                webcam.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            showAlert("Camera Error", "Hardware busy or not found: " + e.getMessage());
//        }
    }

    @FXML
    public void handleDeletePhoto(ActionEvent event) {
        ivAvatar.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.jpg")));
        lblFilePath.setText("Photo removed");
        this.selectedImageFile = null;
        this.existingPhotoPath = "EMPTY";
    }

    // ─── Map ──────────────────────────────────────────────────────────────────

    @FXML
    public void handleOpenMap(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            MapPickerController mapController = loader.getController();

            Stage stage = new Stage();

            if (txtFirstName.getScene() != null) {
                stage.initOwner(txtFirstName.getScene().getWindow());
            }

            stage.setTitle("Pin Resident Location");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            stage.setResizable(false);
            stage.setFullScreen(false);

            String pinnedAddress = mapController.getSelectedAddress();
            double lat = mapController.getSelectedLatitude();   // ← add this
            double lng = mapController.getSelectedLongitude();  // ← add this

            if (pinnedAddress != null && !pinnedAddress.isEmpty()) {
                txtAddress.setText(pinnedAddress);
                this.pendingLatitude = lat;
                this.pendingLongitude = lng;

            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Map Error", "Could not open map: " + e.getMessage());
        }
    }

    // ─── Populate for Edit ────────────────────────────────────────────────────

    public void setResidentData(Resident r) {
        this.currentResidentId = r.getId();
        lblTitle.setText("Update Resident Details");

        txtFirstName.setText(r.getFirstName());
        txtMiddleName.setText(r.getMiddleName());
        txtLastName.setText(r.getLastName());
        txtSuffix.setText(r.getSuffix());
        dpBirthDate.setValue(r.getDateOfBirth());
        cbSex.setValue(r.getSex());
        cbCivilStatus.setValue(r.getCivilStatus());
        txtPhone.setText(r.getContactNumber());
        chkIsHead.setSelected(r.isHouseholdHead());

        // ✅ Restore address — comes from the joined resident_locations row
        txtAddress.setText(r.getAddress() != null ? r.getAddress() : "");

        // Restore photo
        this.existingPhotoPath = r.getPhotoUrl();
        if (existingPhotoPath != null
                && !existingPhotoPath.isBlank()
                && !existingPhotoPath.equalsIgnoreCase("EMPTY")) {
            File photoFile = new File(existingPhotoPath);
            if (photoFile.exists()) {
                ivAvatar.setImage(new Image(photoFile.toURI().toString(), true));
                lblFilePath.setText(photoFile.getName());
            }
        }

        // Restore vulnerability checkboxes
        if (r.getVulnerabilities() != null) {
            chkSenior.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.SENIOR_CITIZEN));
            chkPWD.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.PWD));
            chkPregnant.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.PREGNANT));
            chkIndigenous.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.INDIGENOUS));
            chkSoloParent.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.SOLO_PARENT));
            chkChild.setSelected(r.getVulnerabilities().contains(VulnerabilityTag.CHILD_0_5));
        }
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

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
        r.setHouseholdHead(chkIsHead.isSelected());
        r.setAddress(txtAddress.getText() != null ? txtAddress.getText().trim() : "");
        r.setUpdatedAt(java.time.LocalDateTime.now());
        r.setLatitude(pendingLatitude);   // ← add to Resident model
        r.setLongitude(pendingLongitude); // ← add to Resident model

        try {
            if (selectedImageFile != null) {
                r.setPhotoUrl(saveImageToDisk(selectedImageFile));
            } else {
                r.setPhotoUrl(existingPhotoPath != null ? existingPhotoPath : "EMPTY");
            }

            UUID residentId;
            if (currentResidentId != null) {
                r.setId(currentResidentId);
                repository.update(r);
                residentId = currentResidentId;
                repository.deleteVulnerabilities(residentId);
            } else {
                residentId = repository.insert(r);
            }

            saveVulnerabilityTags(residentId);

            if (parentController != null) parentController.loadData();
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String saveImageToDisk(File sourceFile) throws IOException {
        String STORAGE_DIR = "data/resident_photos/";
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = txtLastName.getText().replaceAll("\\s+", "")
                + "_" + System.currentTimeMillis() + ".jpg";
        File destFile = new File(dir, fileName);
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if (sourceFile.getName().equals("temp_capture.jpg")) sourceFile.delete();

        return destFile.getPath();
    }

    private void saveVulnerabilityTags(UUID id) {
        if (chkSenior.isSelected())     repository.addVulnerability(id, VulnerabilityTag.SENIOR_CITIZEN);
        if (chkPWD.isSelected())        repository.addVulnerability(id, VulnerabilityTag.PWD);
        if (chkPregnant.isSelected())   repository.addVulnerability(id, VulnerabilityTag.PREGNANT);
        if (chkIndigenous.isSelected()) repository.addVulnerability(id, VulnerabilityTag.INDIGENOUS);
        if (chkSoloParent.isSelected()) repository.addVulnerability(id, VulnerabilityTag.SOLO_PARENT);
        if (chkChild.isSelected())      repository.addVulnerability(id, VulnerabilityTag.CHILD_0_5);
    }

    private String safeTrim(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) txtFirstName.getScene().getWindow();
        stage.close();
    }

    private boolean isInputValid() {
        StringBuilder err = new StringBuilder();
        if (txtFirstName.getText().isBlank()) err.append("- First Name is required.\n");
        if (txtLastName.getText().isBlank())  err.append("- Last Name is required.\n");
        if (dpBirthDate.getValue() == null)   err.append("- Birth Date is required.\n");
        if (cbSex.getValue() == null)         err.append("- Sex is required.\n");
        if (err.isEmpty()) return true;
        showAlert("Invalid Input", err.toString());
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        if (txtFirstName.getScene() != null && txtFirstName.getScene().getWindow() != null) {
            alert.initOwner(txtFirstName.getScene().getWindow());
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setParentController(ResidentsController parent) {
        this.parentController = parent;
    }
}