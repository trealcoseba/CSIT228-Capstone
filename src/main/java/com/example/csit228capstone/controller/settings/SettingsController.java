package com.example.csit228capstone.controller.settings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class SettingsController {

    @FXML
    private void backupNow() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Database backup initiated successfully!", ButtonType.OK);
        alert.setTitle("Backup");
        alert.setHeaderText("Backup in Progress");
        alert.showAndWait();
    }
}
