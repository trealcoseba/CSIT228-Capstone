package com.example.csit228capstone.controller.alerts;

import com.example.csit228capstone.model.alert.Alert;
import com.example.csit228capstone.model.alert.AlertPriority;
import com.example.csit228capstone.util.SupabaseConnectionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

public class AlertsController implements Initializable {

    // KPI Labels
    @FXML private Label lblAlertsMonth, lblUnread, lblBroadcasts, lblScheduled;

    // Form Fields
    @FXML private ComboBox<String> alertType;
    @FXML private ComboBox<String> alertPriority; // Using String to match your FXML items
    @FXML private TextArea alertMessage;
    @FXML private CheckBox chkAll, chkVulnerable, chkOfficials;

    // Table
    @FXML private TableView<Alert> alertsTable;
    @FXML private TableColumn<Alert, String> colType, colPriority, colMessage, colSentBy, colSentDate, colRecipients;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadAlertData();
        updateKPIs();
    }

    private void setupTable() {
        // Mapping TableColumns to Alert model fields
        colType.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("body"));
        colSentBy.setCellValueFactory(new PropertyValueFactory<>("issuedBy")); // Currently UUID
        colSentDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colRecipients.setCellValueFactory(new PropertyValueFactory<>("targetPurok"));
    }

    @FXML
    public void sendNow(ActionEvent actionEvent) {
        if (validateForm()) {
            Alert alert = createAlertFromUI();
            saveAlert(alert);
            loadAlertData();
            updateKPIs(); // Refresh the numbers at the top
            clearForm();
        }
    }

    @FXML
    public void scheduleAlert(ActionEvent actionEvent) {
        // Similar to sendNow, but you could add a DatePicker logic here
        Alert alert = createAlertFromUI();
        alert.setExpiresAt(LocalDateTime.now().plusDays(3)); // Example schedule
        saveAlert(alert);
        loadAlertData();
    }

    private Alert createAlertFromUI() {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setTitle(alertType.getValue());
        alert.setBody(alertMessage.getText());

        // Convert FXML String priority to your Enum
        String p = alertPriority.getValue().toUpperCase();
        alert.setPriority(AlertPriority.valueOf(p));

        alert.setBroadcast(true);
        alert.setCreatedAt(LocalDateTime.now());

        // Combine Checkboxes into Recipients string
        StringBuilder recipients = new StringBuilder();
        if (chkAll.isSelected()) recipients.append("All Residents; ");
        if (chkVulnerable.isSelected()) recipients.append("Vulnerable; ");
        if (chkOfficials.isSelected()) recipients.append("Officials; ");
        alert.setTargetPurok(recipients.toString());

        return alert;
    }

    private void saveAlert(Alert alert) {
        String sql = "INSERT INTO tblalerts (id, title, body, priority, is_broadcast, target_purok, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, alert.getId());
            pstmt.setString(2, alert.getTitle());
            pstmt.setString(3, alert.getBody());
            pstmt.setString(4, alert.getPriority().name());
            pstmt.setBoolean(5, alert.isBroadcast());
            pstmt.setString(6, alert.getTargetPurok());
            pstmt.setTimestamp(7, Timestamp.valueOf(alert.getCreatedAt()));
            pstmt.setTimestamp(8, alert.getExpiresAt() != null ? Timestamp.valueOf(alert.getExpiresAt()) : null);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAlertData() {
        ObservableList<Alert> data = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tblalerts ORDER BY created_at DESC";
        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {

            while (rs.next()) {
                Alert a = new Alert();
                a.setTitle(rs.getString("title"));
                a.setBody(rs.getString("body"));
                a.setPriority(AlertPriority.valueOf(rs.getString("priority")));
                a.setTargetPurok(rs.getString("target_purok"));
                a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                data.add(a);
            }
            alertsTable.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        if (alertType.getValue() == null || alertPriority.getValue() == null || alertMessage.getText().isEmpty()) {
            // Use the full package name for JavaFX Alert to avoid conflict with your Model
            javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            errorAlert.setTitle("Form Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Please fill in the Alert Type, Priority, and Message before sending.");
            errorAlert.showAndWait();
            return false;
        }
        return true;
    }

    private void clearForm() {
        alertMessage.clear();
        chkAll.setSelected(false);
        chkVulnerable.setSelected(false);
        chkOfficials.setSelected(false);
    }

    private void updateKPIs() {
        // 1. Count alerts created in the current calendar month
        String sqlMonth = "SELECT COUNT(*) FROM tblalerts WHERE created_at >= date_trunc('month', current_date)";

        // 2. Count "Critical" alerts (Assuming 'Unread' in your UI refers to urgent/active issues)
        // Alternatively: SELECT COUNT(*) FROM tblalerts WHERE is_read = false (if you add that column)
        String sqlCritical = "SELECT COUNT(*) FROM tblalerts WHERE priority = 'CRITICAL'";

        // 3. Count total Broadcasts
        String sqlBroadcasts = "SELECT COUNT(*) FROM tblalerts WHERE is_broadcast = true";

        // 4. Count Scheduled (Alerts where the expiration date is in the future)
        String sqlScheduled = "SELECT COUNT(*) FROM tblalerts WHERE expires_at > now()";

        try (Connection conn = SupabaseConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute Month Count
            ResultSet rs = stmt.executeQuery(sqlMonth);
            if (rs.next()) {
                lblAlertsMonth.setText(String.valueOf(rs.getInt(1)));
            }

            // Execute Critical/Unread Count
            rs = stmt.executeQuery(sqlCritical);
            if (rs.next()) {
                lblUnread.setText(String.valueOf(rs.getInt(1)));
            }

            // Execute Broadcast Count
            rs = stmt.executeQuery(sqlBroadcasts);
            if (rs.next()) {
                lblBroadcasts.setText(String.valueOf(rs.getInt(1)));
            }

            // Execute Scheduled Count
            rs = stmt.executeQuery(sqlScheduled);
            if (rs.next()) {
                lblScheduled.setText(String.valueOf(rs.getInt(1)));
            }

        } catch (SQLException e) {
            System.err.println("Error updating KPIs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Required by your FXML header buttons
    public void sendAlert(ActionEvent actionEvent) { sendNow(actionEvent); }
    public void sendBroadcast(ActionEvent actionEvent) { sendNow(actionEvent); }
}