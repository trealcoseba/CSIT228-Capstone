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

/**
 * Central report service with multithreading support.
 *
 * <h3>Generics design</h3>
 * Exporters are stored as {@code ReportExporter<?>} (wildcard) so that two
 * differently-typed exporters live in the same map. Dispatch to the correct
 * concrete method happens inside {@link #createExportTask} using the report
 * type string — no unchecked casts at the call site.
 *
 * <h3>Threading</h3>
 * A fixed thread-pool of 4 threads handles all blocking work (DB fetch +
 * file rendering). JavaFX {@link Task} bridges the background work back to
 * the UI thread via {@code setOnSucceeded} / {@code setOnFailed}.
 */
public class ReportService {

    public static final String INCIDENT_TYPE = "Natural Disaster Incident Report";

    // ── Thread pool ───────────────────────────────────────────────────────────
    private final ExecutorService threadPool = Executors.newFixedThreadPool(4,
            r -> {
                Thread t = new Thread(r, "report-worker");
                t.setDaemon(true); // dies when the JVM exits
                return t;
            });

    // ── Exporter registry (wildcard — each exporter owns its own T) ───────────
    private final Map<String, ReportExporter<?>> exporters = new LinkedHashMap<>();

    private final PDFReportExporter  pdfExporter  = new PDFReportExporter();
    private final DocxReportExporter docxExporter = new DocxReportExporter();
    private final ReportDataProvider dataProvider = new ReportDataProvider();

    public ReportService() {
        exporters.put("pdf",  pdfExporter);
        exporters.put("docx", docxExporter);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Submits a {@link Task} to the background thread pool.
     * All callers (controllers) use this to avoid blocking the JavaFX thread.
     */
    public <T> void execute(Task<T> task) {
        threadPool.submit(task);
    }

    /**
     * Returns the supported export formats.
     *
     * @return unmodifiable list of format keys, e.g. {@code ["pdf", "docx"]}
     */
    public List<String> getSupportedFormats() {
        return List.copyOf(exporters.keySet());
    }

    // ── Task factories ────────────────────────────────────────────────────────

    /**
     * Creates a {@link Task} that fetches tabular data for a generic report.
     * The task returns {@code ReportData<List<List<String>>>}.
     */
    public Task<ReportData<List<List<String>>>> createFetchTask(Report report) {
        return new Task<>() {
            @Override
            protected ReportData<List<List<String>>> call() {
                updateMessage("Fetching report data…");
                return dataProvider.getData(report);
            }
        };
    }

    /**
     * Creates a {@link Task} that exports a <em>generic (tabular)</em> report
     * to {@code destination} in the requested {@code format}.
     *
     * <p>DB fetch + file rendering happen entirely on the background thread.</p>
     */
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

    /**
     * Creates a {@link Task} that exports a <em>Natural Disaster Incident Report</em>
     * to {@code destination}. The {@code formData} payload is already in memory
     * (collected from the form), so only the file-rendering step runs on the thread.
     */
    public Task<File> createIncidentExportTask(Report report, ReportFormData formData,
                                               String format, File destination) {
        // Build the typed ReportData<ReportFormData> wrapper up-front on the calling thread
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

    /**
     * Convenience router — picks the right task factory based on report type.
     *
     * @param formData pass the collected form data for incident reports;
     *                 pass {@code null} for generic reports
     */
    public Task<File> createExportTask(Report report, ReportFormData formData,
                                       String format, File destination) {
        if (INCIDENT_TYPE.equals(report.getType()) && formData != null)
            return createIncidentExportTask(report, formData, format, destination);
        else
            return createGenericExportTask(report, format, destination);
    }

    /** Graceful shutdown — call from application stop(). */
    public void shutdown() { threadPool.shutdown(); }
}