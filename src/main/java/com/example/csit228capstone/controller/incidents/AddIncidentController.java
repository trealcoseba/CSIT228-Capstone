package com.example.csit228capstone.controller.incidents;

import com.example.csit228capstone.model.incident.*;
import com.example.csit228capstone.repository.IncidentRepository;
import com.example.csit228capstone.controller.map.MapPickerController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class AddIncidentController implements Initializable {

    @FXML private TextField txtTitle, txtPurok, txtDetail;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<IncidentType> cbType;
    @FXML private ComboBox<IncidentSeverity> cbSeverity;
    @FXML private ComboBox<IncidentStatus> cbStatus;
    @FXML private DatePicker dpDate;
    @FXML private CheckBox chkMedical, chkFire, chkRescue, chkSecurity, chkWaterFood;
    @FXML private CheckBox chkCritical;

    private final IncidentRepository repository = new IncidentRepository();
    private boolean saveClicked = false;

    private double pendingLatitude = 0.0;
    private double pendingLongitude = 0.0;
    private boolean isLocationPinned = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList(IncidentType.values()));
        cbSeverity.setItems(FXCollections.observableArrayList(IncidentSeverity.values()));
        cbStatus.setItems(FXCollections.observableArrayList(IncidentStatus.values()));

        dpDate.setValue(LocalDate.now());

        chkCritical.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                cbSeverity.setValue(IncidentSeverity.CRITICAL);
            }
        });
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handlePinOnMap(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            MapPickerController mapController = loader.getController();

            Stage stage = new Stage();

            if (txtTitle.getScene() != null) {
                stage.initOwner(txtTitle.getScene().getWindow());
            }
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setFullScreen(false);

            stage.setTitle("Pin Incident Location");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            double lat = mapController.getSelectedLatitude();
            double lng = mapController.getSelectedLongitude();
            String pinnedAddress = mapController.getSelectedAddress();

            if (lat != 0.0 || lng != 0.0) {
                txtDetail.setText(pinnedAddress != null ? pinnedAddress : "");
                this.pendingLatitude = lat;
                this.pendingLongitude = lng;
                this.isLocationPinned = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Map Error", "Could not open map: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (txtTitle.getText().isEmpty() || cbType.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Title and Type are required.");

            if (txtTitle.getScene() != null) alert.initOwner(txtTitle.getScene().getWindow());
            alert.show();
            return;
        }

        try {
            Incident i = new Incident();
            i.setTitle(txtTitle.getText());
            i.setType(cbType.getValue());
            i.setSeverity(cbSeverity.getValue());
            i.setStatus(cbStatus.getValue());
            i.setLocationPurok(txtPurok.getText());
            i.setLocationDetail(txtDetail.getText());
            i.setDescription(txtDescription.getText());

            List<String> needsList = new ArrayList<>();
            if (chkMedical.isSelected()) needsList.add("Medical");
            if (chkFire.isSelected()) needsList.add("Fire");
            if (chkRescue.isSelected()) needsList.add("Rescue");
            if (chkSecurity.isSelected()) needsList.add("Security");
            if (chkWaterFood.isSelected()) needsList.add("Water/Food");

            if (isLocationPinned) {
                i.setLatitude(pendingLatitude);
                i.setLongitude(pendingLongitude);
            }

            LocalDate date = dpDate.getValue();
            i.setReportedAt(date.atTime(LocalTime.now()));

            i.setCreatedAt(LocalDateTime.now());
            i.setUpdatedAt(LocalDateTime.now());

            i.setReportedBy(UUID.randomUUID());

            repository.insert(i);
            saveClicked = true;
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Save failed: " + e.getMessage());

            if (txtTitle.getScene() != null) alert.initOwner(txtTitle.getScene().getWindow());
            alert.show();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) txtTitle.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (txtTitle.getScene() != null && txtTitle.getScene().getWindow() != null) {
            alert.initOwner(txtTitle.getScene().getWindow());
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}