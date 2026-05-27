package com.example.csit228capstone.controller.settings;

import com.example.csit228capstone.model.AdminProfile;
import com.example.csit228capstone.repository.UserRepository;
import com.example.csit228capstone.service.BackupService;
import com.example.csit228capstone.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private CheckBox notifyIncidents;
    @FXML private CheckBox notifyEvacuation;
    @FXML private CheckBox notifyEmail;

    @FXML private Label lastBackupLabel;
    @FXML private Button backupButton;

    private final UserRepository userRepo = new UserRepository();
    private AdminProfile currentProfile;

    @FXML
    public void initialize() {
        loadProfile();
    }

    // ── Profile ───────────────────────────────────────────────────────────────
    private void loadProfile() {
        String username = SessionManager.getInstance().getLoggedInUsername();
        currentProfile = userRepo.getAdminProfile(username);
        if (currentProfile != null) {
            firstNameField.setText(currentProfile.getFirstName());
            lastNameField.setText(currentProfile.getLastName());
        } else {
            showAlert(Alert.AlertType.ERROR, "Load Error",
                    "Could not load admin profile from the database.");
        }
    }

    @FXML
    private void saveProfile() {
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "First name and last name cannot be empty.");
            return;
        }

        if (currentProfile == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No profile loaded.");
            return;
        }

        boolean success = userRepo.updateProfile(currentProfile.getId(), firstName, lastName);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Profile updated successfully.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Save Error",
                    "Failed to update profile. Please try again.");
        }
    }

    // ── Password ──────────────────────────────────────────────────────────────
    @FXML
    private void changePassword() {
        String oldPwd     = oldPasswordField.getText();
        String newPwd     = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "All password fields are required.");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "New password and confirmation do not match.");
            return;
        }

        if (newPwd.length() < 8) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "New password must be at least 8 characters.");
            return;
        }

        if (currentProfile == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No profile loaded.");
            return;
        }

        boolean success = userRepo.changePassword(currentProfile.getId(), oldPwd, newPwd);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Password Changed",
                    "Your password has been updated successfully.");
            oldPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Current password is incorrect. Please try again.");
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    @FXML
    private void saveNotifications() {
        boolean incidents  = notifyIncidents.isSelected();
        boolean evacuation = notifyEvacuation.isSelected();
        boolean email      = notifyEmail.isSelected();

        System.out.printf("Notifications saved — incidents=%b, evacuation=%b, email=%b%n",
                incidents, evacuation, email);

        showAlert(Alert.AlertType.INFORMATION, "Notifications",
                "Notification preferences saved.");
    }

    // ── Backup ────────────────────────────────────────────────────────────────
    @FXML
    private void backupNow() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will back up all database tables. Proceed?",
                ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Backup");
        confirm.setHeaderText("Confirm Full Database Backup");

        if (firstNameField.getScene() != null && firstNameField.getScene().getWindow() != null) {
            confirm.initOwner(firstNameField.getScene().getWindow());
        }

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Backup As");
            chooser.setInitialFileName("backup_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SQL Files", "*.sql")
            );

            File file = chooser.showSaveDialog(firstNameField.getScene().getWindow());
            if (file == null) return;

            backupButton.setDisable(true);
            backupButton.setText("Backing up...");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    new BackupService().backup(file.getAbsolutePath());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                String timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
                lastBackupLabel.setText("Last Backup: " + timestamp);
                backupButton.setDisable(false);
                backupButton.setText("Backup Now");
                showAlert(Alert.AlertType.INFORMATION, "Backup Complete",
                        "All tables backed up to:\n" + file.getAbsolutePath());
            });

            task.setOnFailed(e -> {
                backupButton.setDisable(false);
                backupButton.setText("Backup Now");
                showAlert(Alert.AlertType.ERROR, "Backup Failed",
                        "Error: " + task.getException().getMessage());
            });

            new Thread(task, "backup-thread").start();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        if (firstNameField.getScene() != null && firstNameField.getScene().getWindow() != null) {
            alert.initOwner(firstNameField.getScene().getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}