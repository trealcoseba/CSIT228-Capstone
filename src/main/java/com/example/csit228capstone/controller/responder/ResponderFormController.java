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

/**
 * Controller for responder_form.fxml.
 * Open with:
 *   ResponderFormController ctrl = FxmlUtil.openDialog(
 *       getClass(), "/fxml/responder/responder_form.fxml", "Add Responder", owner);
 *   ctrl.setResponder(existingResponder); // for edit mode
 *   ctrl.setOnSaved(this::loadResponders); // callback
 */
public class ResponderFormController implements Initializable {

    // ─── FXML ────────────────────────────────────────────────────────────────────
    @FXML private Label       lblFormTitle;
    @FXML private TextField   tfName;
    @FXML private ComboBox<String> cbAgency;
    @FXML private TextField   tfContact;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Button      btnSave;

    // Error labels
    @FXML private Label errName;
    @FXML private Label errAgency;
    @FXML private Label errStatus;

    // ─── State ────────────────────────────────────────────────────────────────────
    private final ResponderRepository repo = new ResponderRepository();
    private Responder editTarget;   // null = create mode
    private Runnable onSaved;       // callback to refresh parent table

    private static final List<String> KNOWN_AGENCIES =
            List.of("BFP", "PNP", "MDRRMO", "NBI", "PCG", "AFP", "DOH", "Other");

    private static final List<String> STATUSES =
            List.of("available", "on_mission", "off_duty");

    // ─── Init ─────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbAgency.setItems(FXCollections.observableArrayList(KNOWN_AGENCIES));
        cbStatus.setItems(FXCollections.observableArrayList(STATUSES));
        cbStatus.getSelectionModel().select("available");
    }

    // ─── Public API (called by parent before showing) ─────────────────────────────

    public void setResponder(Responder r) {
        this.editTarget = r;
        if (r != null) {
            lblFormTitle.setText("Edit Responder");
            btnSave.setText("Update Responder");
            tfName.setText(r.getName());
            tfContact.setText(r.getContact() != null ? r.getContact() : "");
            cbAgency.setValue(r.getAgency());
            cbStatus.setValue(r.getStatus());
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    // ─── Handlers ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validate()) return;

        String name    = tfName.getText().trim();
        String agency  = cbAgency.getValue() != null ? cbAgency.getValue().trim() : "";
        String contact = tfContact.getText().trim();
        String status  = cbStatus.getValue();

        try {
            if (editTarget == null) {
                // ── CREATE ──
                Responder r = new Responder(name, contact, agency, status);
                repo.save(r);
            } else {
                // ── UPDATE ──
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

    // ─── Validation ───────────────────────────────────────────────────────────────

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
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void closeStage() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }
}