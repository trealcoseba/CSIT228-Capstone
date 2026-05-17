package com.example.csit228capstone.controller.login;

import com.example.csit228capstone.repository.UserRepository;
import com.example.csit228capstone.util.SupabaseConnectionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    @FXML private Button btnLogin;
    private final UserRepository repository = new UserRepository();

    @FXML
    public void initialize() {
        tfUsername.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.contains(" ")) { tfUsername.setText(newValue.replaceAll("\\s", ""));}
        });
        tfPassword.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.contains(" ")) {tfPassword.setText(newValue.replaceAll("\\s", ""));}
        });
    }

    @FXML
    private void handleFirstTF() {
        tfPassword.requestFocus();
    }

    @FXML
    private void handleSecondTF() {
        btnLogin.fire();
    }

    @FXML
    public void handleLogin(ActionEvent actionEvent) {
        String username = tfUsername.getText();
        String password = tfPassword.getText();

        if(repository.authenticate(username, password)) {
            navigateToDashboard(actionEvent);
        } else {
            showError("Login Failed", "Invalid username or password.");
        }

    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/csit228capstone/mainlayout/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("LIGTAS-Brgy");
            stage.setFullScreen(true);
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