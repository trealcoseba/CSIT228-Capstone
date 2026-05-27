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

        String url = getClass().getResource("/map/map.html").toExternalForm();
        engine.load(url);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", this);

                engine.executeScript("fixMapSize()");
            }
        });

        txtSelectedAddress.setOnAction(event -> {
            String manualAddress = txtSelectedAddress.getText();
            if (manualAddress != null && !manualAddress.isEmpty()) {
                engine.executeScript("searchAddress('" + manualAddress.replace("'", "\\'") + "')");
            }
        });
    }

    public String getSelectedAddress() {
        return finalAddress;
    }

    @FXML
    private void handleConfirm() {
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        this.finalAddress = "";
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    public double getSelectedLatitude() { return this.selectedLat; }
    public double getSelectedLongitude() { return this.selectedLng; }
}