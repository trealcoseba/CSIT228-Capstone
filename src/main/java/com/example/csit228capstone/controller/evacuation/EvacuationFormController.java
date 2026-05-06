package com.example.csit228capstone.controller.evacuation;

import com.example.csit228capstone.controller.map.MapPickerController;
import com.example.csit228capstone.model.EvacuationCenter;
import com.example.csit228capstone.util.SupabaseConnectionManager;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
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

    @FXML
    public void handleOpenMap() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/csit228capstone/map/map_picker.fxml"));
            Parent root = loader.load();
            MapPickerController mapController = loader.getController();

            Stage stage = new Stage();
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

    @FXML
    public void handleSave(ActionEvent actionEvent) {
        String name        = txtName.getText().trim();
        String address     = txtLocation.getText().trim();
        String manager     = txtManager.getText().trim();
        String contact     = txtContact.getText().trim();
        String capacityStr = txtCapacity.getText().trim();

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

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection()) {

            if (editingId == null) {
                // INSERT
                String sql = """
                    INSERT INTO evacuation_centers
                        (name, address, latitude, longitude, max_capacity,
                         current_occupancy, status, is_active, manager_of_center, contact_number)
                    VALUES (?, ?, ?, ?, ?, 0, 'available', ?, ?, ?)
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, address);
                    stmt.setDouble(3, selectedLat);
                    stmt.setDouble(4, selectedLng);
                    stmt.setInt(5, maxCapacity);
                    stmt.setBoolean(6, chkActive.isSelected());
                    stmt.setString(7, manager.isEmpty() ? null : manager);
                    stmt.setString(8, contact.isEmpty() ? null : manager);
                    stmt.executeUpdate();
                }
            } else {
                // UPDATE
                String sql = """
                    UPDATE evacuation_centers
                    SET name = ?, address = ?, latitude = ?, longitude = ?,
                        max_capacity = ?, is_active = ?, manager_of_center = ?, contact_number = ?
                    WHERE id = ?
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, address);
                    stmt.setDouble(3, selectedLat);
                    stmt.setDouble(4, selectedLng);
                    stmt.setInt(5, maxCapacity);
                    stmt.setBoolean(6, chkActive.isSelected());
                    stmt.setString(7, manager.isEmpty() ? null : manager);
                    stmt.setString(8, contact.isEmpty() ? null : contact);
                    stmt.setObject(9, editingId);
                    stmt.executeUpdate();
                }
            }
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }

    public void populateForEdit(EvacuationCenter center) {
        editingId = center.getId();
        txtName.setText(center.getName());
        txtLocation.setText(center.getAddress() != null ? center.getAddress() : "");
        txtCapacity.setText(String.valueOf(center.getMaxCapacity()));
        txtManager.setText(center.getManagerName() != null ? center.getManagerName() : "");
        chkActive.setSelected(center.isActive());
        selectedLat = center.getLatitude() != null ? center.getLatitude() : 0.0;
        selectedLng = center.getLongitude() != null ? center.getLongitude() : 0.0;
    }
}