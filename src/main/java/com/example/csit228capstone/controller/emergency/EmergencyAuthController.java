package com.example.csit228capstone.controller.emergency;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class EmergencyAuthController {
    @FXML private PasswordField txtPasskey;
    private boolean authorized = false;
    private static final String SECRET_KEY = "jayvince67";

    public boolean isAuthorized() {
        return authorized;
    }

    @FXML
    private void handleAuthorize() {
        if (txtPasskey.getText().equals(SECRET_KEY)) {
            authorized = true;
            close();
        } else {
            txtPasskey.setStyle("-fx-border-color: #ef4444; -fx-background-color: #161b22; -fx-text-fill: white;");
        }
    }

    @FXML private void handleCancel() { close(); }
    private void close() { ((Stage) txtPasskey.getScene().getWindow()).close(); }
}