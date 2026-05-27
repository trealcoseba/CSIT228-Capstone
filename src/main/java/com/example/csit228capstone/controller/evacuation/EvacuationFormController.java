package com.example.csit228capstone.controller.evacuation;

import com.example.csit228capstone.controller.map.MapPickerController;
import com.example.csit228capstone.model.EvacuationCenter;
import com.example.csit228capstone.service.EvacuationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.UUID;

public class EvacuationFormController {

    @FXML private TextField txtContact;
    @FXML private TextField txtManager;
    @FXML private TextField txtCapacity;
    @FXML private TextField txtLocation;
    @FXML private TextField txtName;
    @FXML private CheckBox chkActive;

    private double selectedLat = 0.0;
    private double selectedLng = 0.0;

    private UUID editingId = null;

    private final EvacuationService service = EvacuationService.getInstance();

    // -------------------------------------------------------------------------
    // Map picker
    // -------------------------------------------------------------------------

    @FXML
    public void handleOpenMap() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            MapPickerController mapController = loader.getController();

            Stage stage = new Stage();

            if (txtName.getScene() != null) {
                stage.initOwner(txtName.getScene().getWindow());
            }

            stage.setTitle("Pin Evacuation Center Location");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            String pinnedAddress = mapController.getSelectedAddress();
            selectedLat = mapController.getSelectedLatitude();
            selectedLng = mapController.getSelectedLongitude();

            if (pinnedAddress != null && !pinnedAddress.isEmpty()) {
                txtLocation.setText(pinnedAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Map Error", "Could not open map: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Save (insert or update)
    // -------------------------------------------------------------------------

    @FXML
    public void handleSave(ActionEvent actionEvent) {
        String name        = txtName.getText().trim();
        String address     = txtLocation.getText().trim();
        String manager     = txtManager.getText().trim();
        String contact     = txtContact.getText().trim();
        String capacityStr = txtCapacity.getText().trim();

        // --- Validation ---
        if (name.isEmpty() || address.isEmpty() || capacityStr.isEmpty()) {
            showAlert("Validation Error", "Name, location, and capacity are required.");
            return;
        }

        int maxCapacity;
        try {
            maxCapacity = Integer.parseInt(capacityStr);
            if (maxCapacity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Capacity must be a positive number.");
            return;
        }

        if (selectedLat == 0.0 && selectedLng == 0.0) {
            showAlert("Validation Error", "Please pin the location on the map.");
            return;
        }

        // --- Build model ---
        EvacuationCenter center = new EvacuationCenter();
        center.setId(editingId);
        center.setName(name);
        center.setAddress(address);
        center.setLatitude(selectedLat);
        center.setLongitude(selectedLng);
        center.setMaxCapacity(maxCapacity);
        center.setActive(chkActive.isSelected());
        center.setManagerName(manager.isEmpty() ? null : manager);
        center.setContactNumber(contact.isEmpty() ? null : contact);

        // --- Delegate to service ---
        try {
            if (editingId == null) {
                service.insertCenter(center);
            } else {
                service.updateCenter(center);
            }
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    // -------------------------------------------------------------------------
    // Populate for edit mode
    // -------------------------------------------------------------------------

    public void populateForEdit(EvacuationCenter center) {
        editingId = center.getId();
        txtName.setText(center.getName());
        txtLocation.setText(center.getAddress() != null ? center.getAddress() : "");
        txtCapacity.setText(String.valueOf(center.getMaxCapacity()));
        txtManager.setText(center.getManagerName() != null ? center.getManagerName() : "");
        txtContact.setText(center.getContactNumber() != null ? center.getContactNumber() : "");
        chkActive.setSelected(center.isActive());
        selectedLat = center.getLatitude() != null ? center.getLatitude() : 0.0;
        selectedLng = center.getLongitude() != null ? center.getLongitude() : 0.0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        if (txtName.getScene() != null && txtName.getScene().getWindow() != null) {
            alert.initOwner(txtName.getScene().getWindow());
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
}