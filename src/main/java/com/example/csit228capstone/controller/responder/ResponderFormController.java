package com.example.csit228capstone.controller.responder;

import com.example.csit228capstone.model.responder.Responder;
import com.example.csit228capstone.repository.ResponderRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ResponderFormController implements Initializable {

    @FXML private Label       lblFormTitle;
    @FXML private TextField   tfName;
    @FXML private ComboBox<String> cbAgency;
    @FXML private TextField   tfContact;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Button      btnSave;

    @FXML private Label errName;
    @FXML private Label errAgency;
    @FXML private Label errStatus;

    private final ResponderRepository repo = new ResponderRepository();
    private Responder editTarget;
    private Runnable onSaved;

    private static final List<String> KNOWN_AGENCIES =
            List.of("BFP", "PNP", "MDRRMO", "NBI", "PCG", "AFP", "DOH", "Other");

    private static final List<String> STATUSES =
            List.of("available", "on_mission", "off_duty");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbAgency.setItems(FXCollections.observableArrayList(KNOWN_AGENCIES));
        cbStatus.setItems(FXCollections.observableArrayList(STATUSES));

        cbStatus.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String systemName) {
                if (systemName == null) return "";
                return switch (systemName) {
                    case "available"  -> "Available";
                    case "on_mission" -> "On Mission";
                    case "off_duty"   -> "Off Duty";
                    default           -> systemName;
                };
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        cbStatus.getSelectionModel().select("available");
    }

    public void setResponder(Responder r) {
        this.editTarget = r;
        if (r != null) {
            lblFormTitle.setText("Edit Responder");
            btnSave.setText("Update Responder");
            tfName.setText(r.getName());
            tfContact.setText(r.getContact() != null ? r.getContact() : "");
            cbAgency.setValue(r.getAgency());


            if (r.getStatus() != null) {
                cbStatus.setValue(r.getStatus().toLowerCase());
            }
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validate()) return;

        String name    = tfName.getText().trim();
        String agency  = cbAgency.getValue() != null ? cbAgency.getValue().trim() : "";
        String contact = tfContact.getText().trim();
        String status  = cbStatus.getValue();

        try {
            if (editTarget == null) {
                Responder r = new Responder(name, contact, agency, status);
                repo.save(r);
            } else {
                editTarget.setName(name);
                editTarget.setAgency(agency);
                editTarget.setContact(contact);
                editTarget.setStatus(status);
                repo.update(editTarget);
            }

            if (onSaved != null) onSaved.run();
            closeStage();

        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    private boolean validate() {
        boolean valid = true;

        if (tfName.getText().isBlank()) {
            showFieldError(errName, "Name is required.");
            valid = false;
        } else {
            clearFieldError(errName);
        }

        if (cbAgency.getValue() == null || cbAgency.getValue().isBlank()) {
            showFieldError(errAgency, "Agency is required.");
            valid = false;
        } else {
            clearFieldError(errAgency);
        }

        if (cbStatus.getValue() == null) {
            showFieldError(errStatus, "Status is required.");
            valid = false;
        } else {
            clearFieldError(errStatus);
        }

        return valid;
    }

    private void showFieldError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void clearFieldError(Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);

        if (tfName.getScene() != null && tfName.getScene().getWindow() != null) {
            alert.initOwner(tfName.getScene().getWindow());
        }

        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void closeStage() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }
}