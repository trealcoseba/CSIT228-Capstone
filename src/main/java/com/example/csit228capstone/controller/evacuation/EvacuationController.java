package com.example.csit228capstone.controller.evacuation;

import com.example.csit228capstone.model.EvacuationCenter;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.List;
import java.util.ArrayList;

public class EvacuationController {

    @FXML
    private WebView mapWebView;

    private WebEngine webEngine;

    private final List<EvacuationCenter> centersData = new ArrayList<>();

    @FXML
    public void initialize() {
        if (mapWebView == null) return;


        webEngine = mapWebView.getEngine();

        mapWebView.widthProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView width changed: " + newVal)
        );
        mapWebView.heightProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView height changed: " + newVal)
        );

        // JS console alerts
        webEngine.setOnAlert(event ->
                System.out.println("JS Alert: " + event.getData())
        );

        try {
            String mapUrl = getClass()
                    .getResource("/map/map.html")
                    .toExternalForm();

            webEngine.load(mapUrl);

        } catch (Exception e) {
            System.err.println("Failed to load map.html");
            e.printStackTrace();
        }

        // Wait until HTML is fully loaded
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    webEngine.executeScript("fixMapSize()");
                    refreshMapMarkers();
                });
            }
        });

        mapWebView.widthProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView width changed: " + newVal)
        );
        mapWebView.heightProperty().addListener((obs, oldVal, newVal) ->
                System.out.println("WebView height changed: " + newVal)
        );

        // Fix map on resize
        mapWebView.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("fixMapSize()");
            }
        });

        mapWebView.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("fixMapSize()");
            }
        });
    }

    public void refreshMapMarkers() {
        if (webEngine == null) return;

        if (webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
            return;
        }

        try {
            // Leaflet clear markers
            webEngine.executeScript("clearMarkers()");

            for (EvacuationCenter center : centersData) {
                if (center.getLatitude() != null && center.getLongitude() != null) {

                    String safeName = center.getName().replace("'", "\\'");

                    String script = String.format(
                            "addCenterMarker('%s', %f, %f, '%s', %d, %d)",
                            center.getId(),
                            center.getLatitude(),
                            center.getLongitude(),
                            safeName,
                            center.getCurrentOccupancy(),
                            center.getMaxCapacity()
                    );

                    webEngine.executeScript(script);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCenterSelection(EvacuationCenter center) {
        if (center == null) return;

        if (center.getLatitude() == null || center.getLongitude() == null) return;

        String script = String.format(
                "map.setView([%f, %f], 16)",
                center.getLatitude(),
                center.getLongitude()
        );

        webEngine.executeScript(script);
    }

    @FXML
    public void addCenter(ActionEvent actionEvent) {
        System.out.println("Add center clicked");
    }

    @FXML
    public void activateEvacuation(ActionEvent actionEvent) {
        System.out.println("Evacuation activated");
    }
}