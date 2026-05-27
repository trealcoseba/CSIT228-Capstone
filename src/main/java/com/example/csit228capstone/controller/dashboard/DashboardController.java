package com.example.csit228capstone.controller.dashboard;

import com.example.csit228capstone.controller.incidents.ViewIncidentController;
import com.example.csit228capstone.controller.mainlayout.MainLayoutController;
import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.repository.ResourceRepository;
import com.example.csit228capstone.service.EvacuationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.repository.ResidentRepository;
import com.example.csit228capstone.service.IncidentService;

import java.net.HttpURLConnection;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Label lblTotalResidents, lblActiveIncidents, lblEvacuees;
    @FXML private Label lblSenior, lblPwd, lblPregnant, lblChildren;
    @FXML private Label lblIndigenous, lblSoloParent;
    @FXML private TableView<Incident> incidentTable;
    @FXML private TableColumn<Incident, String> colSeverity, colType, colLocation, colTime, colStatus;
    @FXML private Label lblRelief, lblMed, lblFund, lblEvac;
    @FXML private ProgressBar pbRelief, pbMed, pbFund, pbEvac;
    @FXML private Label lblWeatherTemp, lblWeatherDesc, lblHumidity, lblWind;
    @FXML private ScrollPane mainContent;

    private final IncidentService incidentService     = new IncidentService();
    private final ResidentRepository residentRepo     = new ResidentRepository();
    private final EvacuationService evacuationService = new EvacuationService();
    private final ResourceRepository resourceRepo     = new ResourceRepository();

    private MainLayoutController mainLayoutController;
    private ViewIncidentController viewIncidentController;

    private static final double LAT = 10.3157;
    private static final double LON = 123.8854;

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    public void initialize() {
        setupIncidentTable();
        loadData();
        loadWeather();
    }

    private void setupIncidentTable() {
        colSeverity.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSeverity().name()));
        colSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Circle dot = new Circle(6);
                dot.setFill(switch (item) {
                    case "CRITICAL" -> Color.web("#C0392B");
                    case "MAJOR"    -> Color.web("#BA7517");
                    default         -> Color.web("#3498DB");
                });
                setGraphic(dot);
            }
        });

        colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        colLocation.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getLocationPurok() +
                        (cd.getValue().getLocationDetail() != null
                                ? ", " + cd.getValue().getLocationDetail() : "")));
        colTime.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getReportedAt().format(DateTimeFormatter.ofPattern("HH:mm"))));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("status-badge", "status-" + item.toLowerCase());
                setGraphic(badge);
            }
        });
    }

    private void loadData() {
        try {
            lblTotalResidents.setText(String.valueOf(residentRepo.countAll()));
            lblActiveIncidents.setText(String.valueOf(incidentService.getActiveCount()));
            lblEvacuees.setText(String.valueOf(evacuationService.getTotalEvacuees()));

            List<Incident> active = incidentService.getActiveIncidents();
            active.sort(Comparator.comparing(Incident::getReportedAt).reversed());
            incidentTable.setItems(FXCollections.observableArrayList(active));

            lblSenior.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.SENIOR_CITIZEN)));
            lblPwd.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.PWD)));
            lblPregnant.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.PREGNANT)));
            lblChildren.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.CHILD_0_5)));
            lblIndigenous.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.INDIGENOUS)));
            lblSoloParent.setText(String.valueOf(residentRepo.countByVulnerability(VulnerabilityTag.SOLO_PARENT)));

            loadResourceStatus();

        } catch (Exception e) {
            System.err.println("Dashboard load error: " + e.getMessage());
        }
    }

    private void loadResourceStatus() {
        try {
            List<Resource> resources = resourceRepo.findAll();

            Map<String, double[]> map = new HashMap<>();
            for (Resource r : resources) {
                String cat = (r.getCategory() == null) ? "UNCATEGORIZED" : r.getCategory().trim().toUpperCase();

                map.merge(cat,
                        new double[]{r.getTotalQty(), r.getAvailableQty()},
                        (a, b) -> new double[]{a[0] + b[0], a[1] + b[1]});
            }

            updateUIForCategory(map, "RELIEF",   lblRelief, pbRelief);
            updateUIForCategory(map, "MEDICAL",  lblMed,    pbMed);
            updateUIForCategory(map, "FUND",     lblFund,   pbFund);

        } catch (Exception e) {
            System.err.println("Resource status load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateUIForCategory(Map<String, double[]> map, String keyword, Label lbl, ProgressBar pb) {
        if (lbl == null || pb == null) return;

        double total = 0;
        double available = 0;

        for (Map.Entry<String, double[]> entry : map.entrySet()) {
            if (entry.getKey().contains(keyword.toUpperCase())) {
                total += entry.getValue()[0];
                available += entry.getValue()[1];
            }
        }

        double pct = (total > 0) ? available / total : 0;

        javafx.application.Platform.runLater(() -> {
            lbl.setText(String.format("%.0f%%", pct * 100));
            pb.setProgress(pct);

            if (pct < 0.20) {
                pb.setStyle("-fx-accent: #e74c3c;");
            } else {
                pb.setStyle("-fx-accent: #2ecc71;");
            }
        });
    }

    private void loadWeather() {
        Thread t = new Thread(() -> {
            try {
                String url = String.format(
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
                        LAT, LON);

                HttpURLConnection conn = (HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(5_000);
                conn.setReadTimeout(5_000);

                String body;
                try (java.io.InputStream is = conn.getInputStream();
                     java.util.Scanner sc = new java.util.Scanner(is, "UTF-8")) {
                    sc.useDelimiter("\\A");
                    body = sc.hasNext() ? sc.next() : "";
                }

                String currentSection = extractSection(body, "\"current\":{", "}");

                double temp      = extractDouble(currentSection, "\"temperature_2m\":");
                double windKph   = extractDouble(currentSection, "\"wind_speed_10m\":");
                int humidity     = (int) extractDouble(currentSection, "\"relative_humidity_2m\":");
                int code         = (int) extractDouble(currentSection, "\"weather_code\":");

                String desc = interpretWeatherCode(code);

                javafx.application.Platform.runLater(() -> {
                    if (lblWeatherTemp != null) lblWeatherTemp.setText(String.format("%.0f°C", temp));
                    if (lblWeatherDesc != null) lblWeatherDesc.setText(desc + " · Cebu City");
                    if (lblHumidity != null)    lblHumidity.setText(humidity + "%");
                    if (lblWind != null)        lblWind.setText(String.format("%.0f kph", windKph));
                });

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    if (lblWeatherDesc != null) lblWeatherDesc.setText("Weather unavailable");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private String interpretWeatherCode(int code) {
        return switch (code) {
            case 0          -> "Clear sky";
            case 1, 2, 3    -> "Mainly clear";
            case 45, 48     -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow fall";
            case 80, 81, 82 -> "Rain showers";
            case 95, 96, 99 -> "Thunderstorm";
            default         -> "Cloudy";
        };
    }

    private String extractSection(String json, String startMarker, String endMarker) {
        int start = json.indexOf(startMarker);
        if (start < 0) return json;
        int end = json.indexOf(endMarker, start + startMarker.length());
        return json.substring(start, end + 1);
    }

    private double extractDouble(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return 0.0;

        int start = i + key.length();
        while (start < json.length() && (json.charAt(start) == ':' || json.charAt(start) == ' ')) {
            start++;
        }

        int end = start;
        while (end < json.length() &&
                (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }

        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return 0.0;
        }
    }

    @FXML
    void viewAllIncidents(ActionEvent e) {
        if (mainLayoutController != null) {
            mainLayoutController.showIncidents(null);
        }
    }

    @FXML
    void reportIncident(ActionEvent e) {
        if (mainLayoutController != null) {
            mainLayoutController.showIncidents(null);
            mainLayoutController.openAddIncidentForm();

        }
    }

    @FXML
    void evacuationCenter(ActionEvent e) {
        if (mainLayoutController != null) {
            mainLayoutController.showEvacuation(null);
        }
    }

    @FXML
    void addResident(ActionEvent e) {
        if (mainLayoutController != null) {
            mainLayoutController.showResidents(null);
            mainLayoutController.openAddResidentForm();
        }
    }
}