package com.example.csit228capstone.controller.analytics;

import com.example.csit228capstone.repository.AnalyticsRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsController implements Initializable {

    @FXML private Label lblGrowthRate;
    @FXML private Label lblPrevMonthCount;
    @FXML private Label lblAvgResolution;
    @FXML private Label lblPrevAvgResolution;
    @FXML private Label lblUtilization;
    @FXML private Label lblPrevUtilization;
    @FXML private Label lblVulnerabilityRatio;
    @FXML private Label lblPrevVulnerability;

    @FXML private BarChart<String, Number>        chartIncidentFrequency;
    @FXML private CategoryAxis                    axisIncidentX;
    @FXML private NumberAxis                      axisIncidentY;

    @FXML private BarChart<String, Number>        chartAvgResolution;
    @FXML private CategoryAxis                    axisResolutionX;
    @FXML private NumberAxis                      axisResolutionY;

    @FXML private StackedBarChart<String, Number> chartResourceUsage;
    @FXML private CategoryAxis                    axisResourceX;
    @FXML private NumberAxis                      axisResourceY;

    @FXML private PieChart                        chartDemographic;

    private final AnalyticsRepository repo = new AnalyticsRepository();
    private final ExecutorService executor  = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "analytics-loader");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureChartAxes();
        loadDataAsync();
    }

    private void configureChartAxes() {
        double max = 250;
        axisIncidentY.setUpperBound(Math.ceil(max / 25.0) * 25);
        axisIncidentY.setTickUnit(25);

        axisResolutionY.setUpperBound(Math.ceil(max / 25.0) * 25);
        axisResolutionY.setTickUnit(25);

        axisResourceY.setUpperBound(Math.ceil(max / 25.0) * 25);
        axisResourceY.setTickUnit(25);

        chartIncidentFrequency.setAlternativeRowFillVisible(false);
        chartIncidentFrequency.setAlternativeColumnFillVisible(false);
        chartIncidentFrequency.setHorizontalGridLinesVisible(true);
        chartIncidentFrequency.setVerticalGridLinesVisible(false);

        chartAvgResolution.setAlternativeRowFillVisible(false);
        chartAvgResolution.setAlternativeColumnFillVisible(false);
        chartAvgResolution.setHorizontalGridLinesVisible(true);
        chartAvgResolution.setVerticalGridLinesVisible(false);

        chartIncidentFrequency.setAlternativeRowFillVisible(false);
        chartIncidentFrequency.setAlternativeColumnFillVisible(false);
        chartIncidentFrequency.setVerticalGridLinesVisible(false);

        chartResourceUsage.setAlternativeRowFillVisible(false);
        chartResourceUsage.setAlternativeColumnFillVisible(false);
        chartResourceUsage.setHorizontalGridLinesVisible(true);
        chartResourceUsage.setVerticalGridLinesVisible(false);
    }

    private void loadDataAsync() {


        executor.submit(() -> {
            try {
                int thisMonth  = repo.countIncidentsThisMonth();
                int prevMonth  = repo.countIncidentsPrevMonth();
                double avgRes  = repo.avgResolutionHoursThisMonth();
                double prevRes = repo.avgResolutionHoursPrevMonth();
                double util    = repo.currentShelterUtilizationPct();
                double prevUtil= repo.peakShelterUtilizationPrevMonth();
                double vulnRat = repo.vulnerabilityRatioPct();
                double prevVuln= repo.vulnerabilityRatioPrevMonthPct();

                Map<String, Integer> freqMap   = repo.incidentFrequencyByCategory();
                Map<String, Double>  resMap    = repo.avgResolutionByCategory();
                Map<String, double[]>resUsage  = repo.resourceUsagePerCenter();
                Map<String, Integer> vulnBreak = repo.vulnerabilityTagBreakdown();

                Platform.runLater(() -> {
                    populateKpis(thisMonth, prevMonth, avgRes, prevRes,
                            util, prevUtil, vulnRat, prevVuln);
                    populateIncidentFrequencyChart(freqMap);
                    populateAvgResolutionChart(resMap);
                    populateResourceUsageChart(resUsage);
                    populateDemographicChart(vulnBreak);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        lblGrowthRate.setText("Error loading data"));
            }
        });
    }

    private void populateKpis(int thisMonth, int prevMonth,
                              double avgRes,  double prevRes,
                              double util,    double prevUtil,
                              double vulnRat, double prevVuln) {

        String growthText;
        if (prevMonth == 0) {
            growthText = thisMonth > 0 ? "+100%" : "0%";
        } else {
            double pct = (double)(thisMonth - prevMonth) / prevMonth * 100.0;
            growthText = (pct >= 0 ? "+" : "") + String.format("%.1f%%", pct);
        }
        lblGrowthRate.setText(growthText);
        lblPrevMonthCount.setText(String.valueOf(prevMonth));

        lblAvgResolution.setText(String.format("%.1fh", avgRes));
        lblPrevAvgResolution.setText(String.format("%.1fh", prevRes));

        lblUtilization.setText(String.format("%.1f%%", util));
        lblPrevUtilization.setText(String.format("%.1f%%", prevUtil));

        lblVulnerabilityRatio.setText(String.format("%.1f%%", vulnRat));
        lblPrevVulnerability.setText(String.format("%.1f%%", prevVuln));
    }

    private void populateIncidentFrequencyChart(Map<String, Integer> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Incidents");

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            series.getData().add(
                    new XYChart.Data<>(truncateLabel(entry.getKey(), 18),
                            entry.getValue()));
        }

        chartIncidentFrequency.getData().clear();
        chartIncidentFrequency.getData().add(series);

        int barCount = Math.max(data.size(), 4);
        chartIncidentFrequency.setPrefWidth(Math.max(520, barCount * 80));
    }

    private void populateAvgResolutionChart(Map<String, Double> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Avg Hours");

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            series.getData().add(
                    new XYChart.Data<>(truncateLabel(entry.getKey(), 18),
                            entry.getValue()));
        }

        chartAvgResolution.getData().clear();
        chartAvgResolution.getData().add(series);

        int barCount = Math.max(data.size(), 4);
        chartAvgResolution.setPrefWidth(Math.max(520, barCount * 80));
    }

    private void populateResourceUsageChart(Map<String, double[]> data) {
        XYChart.Series<String, Number> usedSeries  = new XYChart.Series<>();
        XYChart.Series<String, Number> availSeries = new XYChart.Series<>();
        usedSeries.setName("Used");
        availSeries.setName("Available");

        for (Map.Entry<String, double[]> entry : data.entrySet()) {
            String center = truncateLabel(entry.getKey(), 16);
            double used   = entry.getValue()[0];
            double avail  = entry.getValue()[1];
            usedSeries .getData().add(new XYChart.Data<>(center, used));
            availSeries.getData().add(new XYChart.Data<>(center, avail));
        }

        chartResourceUsage.getData().clear();
        chartResourceUsage.getData().addAll(usedSeries, availSeries);

        int centerCount = Math.max(data.size(), 3);
        chartResourceUsage.setPrefWidth(Math.max(520, centerCount * 100));
    }

    private void populateDemographicChart(Map<String, Integer> data) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            pieData.add(new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")",
                    entry.getValue()));
        }
        chartDemographic.setData(pieData);

        if (pieData.isEmpty()) {
            chartDemographic.setData(FXCollections.observableArrayList(
                    new PieChart.Data("No Data", 1)));
        }
    }

    private static String truncateLabel(String text, int maxLen) {
        if (text == null) return "Unknown";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "…" : text;
    }
}