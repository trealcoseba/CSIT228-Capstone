package com.example.csit228capstone.service;

import com.example.csit228capstone.model.report.*;
import com.example.csit228capstone.service.report.DocxReportExporter;
import com.example.csit228capstone.service.report.PDFReportExporter;
import com.example.csit228capstone.service.report.ReportExporter;
import javafx.concurrent.Task;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportService {

    public static final String INCIDENT_TYPE = "Natural Disaster Incident Report";

    private final ExecutorService threadPool = Executors.newFixedThreadPool(4,
            r -> {
                Thread t = new Thread(r, "report-worker");
                t.setDaemon(true);
                return t;
            });

    private final Map<String, ReportExporter<?>> exporters = new LinkedHashMap<>();

    private final PDFReportExporter  pdfExporter  = new PDFReportExporter();
    private final DocxReportExporter docxExporter = new DocxReportExporter();
    private final ReportDataProvider dataProvider = new ReportDataProvider();

    public ReportService() {
        exporters.put("pdf",  pdfExporter);
        exporters.put("docx", docxExporter);
    }

    public <T> void execute(Task<T> task) {
        threadPool.submit(task);
    }

    public List<String> getSupportedFormats() {
        return List.copyOf(exporters.keySet());
    }

    public Task<ReportData<List<List<String>>>> createFetchTask(Report report) {
        return new Task<>() {
            @Override
            protected ReportData<List<List<String>>> call() {
                updateMessage("Fetching report data…");
                return dataProvider.getData(report);
            }
        };
    }

    public Task<File> createGenericExportTask(Report report, String format, File destination) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Fetching data…");
                ReportData<List<List<String>>> data = dataProvider.getData(report);
                updateMessage("Rendering " + format.toUpperCase() + "…");
                switch (format.toLowerCase()) {
                    case "pdf"  -> pdfExporter.export(data, destination);
                    case "docx" -> docxExporter.export(data, destination);
                    default     -> throw new IllegalArgumentException("Unsupported format: " + format);
                }
                return destination;
            }
        };
    }

    public Task<File> createIncidentExportTask(Report report, ReportFormData formData,
                                               String format, File destination) {
        ReportData<ReportFormData> data = new ReportData<>();
        data.setTitle(report.getName());
        data.setGeneratedBy(report.getGeneratedBy());
        data.setStartDate(report.getStartDate());
        data.setEndDate(report.getEndDate());
        data.setReportType(ReportType.INCIDENT_REPORT);
        data.setPayload(formData);

        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Rendering " + format.toUpperCase() + "…");
                switch (format.toLowerCase()) {
                    case "pdf"  -> pdfExporter.exportIncident(data, destination);
                    case "docx" -> docxExporter.exportIncident(data, destination);
                    default     -> throw new IllegalArgumentException("Unsupported format: " + format);
                }
                return destination;
            }
        };
    }

    public void shutdown() { threadPool.shutdown(); }
}