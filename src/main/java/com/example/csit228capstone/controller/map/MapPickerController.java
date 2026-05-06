package com.example.csit228capstone.controller.map;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class MapPickerController {
    @FXML private WebView webView;
    @FXML private TextField txtSelectedAddress;

    private String finalAddress = "";
    private double selectedLat;
    private double selectedLng;
    private WebEngine engine;

    @FXML
    public void initialize() {
        engine = webView.getEngine();

        // Load the local HTML file from your resources
        String url = getClass().getResource("/map/map.html").toExternalForm();
        engine.load(url);

        // Bridge: Allow JavaScript to talk to this Java class
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", this);

                // Ensure the map renders correctly immediately
                engine.executeScript("fixMapSize()");
            }
        });

        txtSelectedAddress.setOnAction(event -> {
            String manualAddress = txtSelectedAddress.getText();
            if (manualAddress != null && !manualAddress.isEmpty()) {
                // Call a JS function to search for this address
                engine.executeScript("searchAddress('" + manualAddress.replace("'", "\\'") + "')");
            }
        });
    }

    /**
     * Called by JavaScript when a user clicks the map.
     * Receives the readable address and raw coordinates.
     */
    public void setAddress(String address, double lat, double lng) {
        this.finalAddress = address;
        this.selectedLat = lat;
        this.selectedLng = lng;

        // Update the UI text field in the FXML
        txtSelectedAddress.setText(address);
    }

    // --- Data Retrieval Methods ---

    public String getSelectedAddress() {
        return finalAddress;
    }

    public double getSelectedLat() {
        return selectedLat;
    }

    public double getSelectedLng() {
        return selectedLng;
    }

    @FXML
    private void handleConfirm() {
        // Just close the window; the calling controller will use the getters above
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        this.finalAddress = ""; // Reset on cancel
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    public double getSelectedLatitude() { return this.selectedLat; }
    public double getSelectedLongitude() { return this.selectedLng; }
}