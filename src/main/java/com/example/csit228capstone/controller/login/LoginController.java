package com.example.csit228capstone.controller.login;

import com.example.csit228capstone.util.SupabaseConnectionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField tfUsername;
    @FXML private PasswordField tfPassword;

    @FXML
    public void handleLogin(ActionEvent actionEvent) {
        String username = tfUsername.getText();
        String password = tfPassword.getText();

        if (authenticate(username, password)) {
            navigateToDashboard(actionEvent);
        } else {
            showError("Invalid Credentials", "The username or password you entered is incorrect.");
        }
    }

    private boolean authenticate(String username, String password) {
        // SQL checks our simplified users table
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // Currently '1234' in your DB

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if a match is found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/csit228capstone/mainlayout/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("LIGTAS-Brgy");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}